package com.circleguard.auth.controller;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final LocalUserRepository localUserRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "anonymousId", auth.getName(),
                "roles", roles
        ));
    }

    @GetMapping("/permissions/{permissionName}")
    public ResponseEntity<List<Map<String, String>>> getUsersByPermission(@PathVariable String permissionName) {
        // Internal endpoint, usually secured by network or specific internal authority.
        // For this proof-of-concept, we'll allow access to fetch the priority alerts.
        List<LocalUser> users = localUserRepository.findUsersByPermissionName(permissionName);
        
        List<Map<String, String>> response = users.stream()
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "email", u.getEmail() != null ? u.getEmail() : ""
                ))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }
}
