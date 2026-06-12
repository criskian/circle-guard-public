package com.circleguard.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class IdentityClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${identity.service.url}")
    private String identityServiceUrl;

    public UUID getAnonymousId(String realIdentity) {
        String url = identityServiceUrl + "/api/v1/identities/map";
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        Map<?, ?> response = restTemplate.postForObject(url, request, Map.class);
        if (response == null || response.get("anonymousId") == null) {
            throw new IllegalStateException("Identity service returned invalid response for: " + realIdentity);
        }
        return UUID.fromString(response.get("anonymousId").toString());
    }
}
