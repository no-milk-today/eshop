package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class KeycloakUserSyncService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public KeycloakUserSyncService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Synchronizes the OIDC user with MySQL.
     *
     * @param oidcUser from Keycloak
     * @return Mono with the same OidcUser after synchronization
     */
    public Mono<OidcUser> syncUser(OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername();
        String email = oidcUser.getEmail();
        log.debug("Attempt to synchronize User: username={}, email={}", username, email);

        return userRepo.findByUsername(username)
                .doOnNext(user -> log.debug("User found in DB: {}", user.getUsername()))
                .switchIfEmpty(
                        Mono.defer(() ->
                                userRepo.save(User.builder()
                                        .username(username)
                                        .email(email != null ? email : "unknown@example.com")
                                        .password(passwordEncoder.encode("oauth2"))
                                        .build()
                                ).doOnSuccess(user -> log.info("User has been created: {}", user.getUsername()))
                        )
                )
                .thenReturn(oidcUser);
    }
}