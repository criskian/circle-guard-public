package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceTest {

    @Mock private IdentityMappingRepository repository;

    private IdentityVaultService vaultService;

    @BeforeEach
    void setUp() {
        vaultService = new IdentityVaultService(repository);
        ReflectionTestUtils.setField(vaultService, "hashSalt", "test-salt-value");
    }

    @Test
    void getOrCreateAnonymousId_existingIdentity_returnsStoredId() {
        String realIdentity = "john.doe@university.edu";
        UUID existingId = UUID.randomUUID();
        IdentityMapping existing = IdentityMapping.builder()
                .anonymousId(existingId)
                .realIdentity(realIdentity)
                .identityHash("some-hash")
                .salt("some-salt")
                .build();

        when(repository.findByIdentityHash(any())).thenReturn(Optional.of(existing));

        UUID result = vaultService.getOrCreateAnonymousId(realIdentity);

        assertThat(result).isEqualTo(existingId);
        verify(repository, never()).save(any());
    }

    @Test
    void getOrCreateAnonymousId_newIdentity_persistsAndReturnsNewId() {
        String realIdentity = "new.student@university.edu";

        when(repository.findByIdentityHash(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            IdentityMapping m = inv.getArgument(0);
            if (m.getAnonymousId() == null) {
                ReflectionTestUtils.setField(m, "anonymousId", UUID.randomUUID());
            }
            return m;
        });

        UUID result = vaultService.getOrCreateAnonymousId(realIdentity);

        assertThat(result).isNotNull();
        verify(repository).save(any(IdentityMapping.class));
    }

    @Test
    void getOrCreateAnonymousId_sameIdentity_producesSameHash() {
        String realIdentity = "alice@university.edu";
        UUID storedId = UUID.randomUUID();
        IdentityMapping existing = IdentityMapping.builder()
                .anonymousId(storedId)
                .realIdentity(realIdentity)
                .identityHash("hash")
                .salt("s")
                .build();

        when(repository.findByIdentityHash(any())).thenReturn(Optional.of(existing));

        UUID first = vaultService.getOrCreateAnonymousId(realIdentity);
        UUID second = vaultService.getOrCreateAnonymousId(realIdentity);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void resolveRealIdentity_existingId_returnsRealIdentity() {
        UUID anonymousId = UUID.randomUUID();
        String realIdentity = "bob@university.edu";
        IdentityMapping mapping = IdentityMapping.builder()
                .anonymousId(anonymousId)
                .realIdentity(realIdentity)
                .build();

        when(repository.findById(anonymousId)).thenReturn(Optional.of(mapping));

        String resolved = vaultService.resolveRealIdentity(anonymousId);

        assertThat(resolved).isEqualTo(realIdentity);
    }

    @Test
    void resolveRealIdentity_unknownId_throws404() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vaultService.resolveRealIdentity(unknownId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Identity not found");
    }
}
