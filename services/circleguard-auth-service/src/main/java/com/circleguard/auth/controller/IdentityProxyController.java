package com.circleguard.auth.controller;

import com.circleguard.auth.client.IdentityClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identities")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IdentityProxyController {

    private final IdentityClient identityClient;

    @PostMapping("/map")
    public ResponseEntity<Map<String, UUID>> mapIdentity(@RequestBody Map<String, String> request) {
        String realIdentity = request.get("realIdentity");
        UUID anonymousId = identityClient.getAnonymousId(realIdentity);
        return ResponseEntity.ok(Map.of("anonymousId", anonymousId));
    }
}
