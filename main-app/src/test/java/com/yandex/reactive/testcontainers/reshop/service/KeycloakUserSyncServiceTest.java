package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class KeycloakUserSyncServiceTest {

    @Autowired
    private KeycloakUserSyncService underTest;

    @MockitoBean
    private UserRepository userRepo;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void syncUser_createsNewUser_whenNotExists() {
        // given
        var username = "user123";
        var email = "user123@example.com";

        var oidcUser = mock(OidcUser.class);
        when(oidcUser.getPreferredUsername()).thenReturn(username);
        when(oidcUser.getEmail()).thenReturn(email);

        when(userRepo.findByUsername(username)).thenReturn(Mono.empty());
        when(passwordEncoder.encode("oauth2")).thenReturn("encoded-pass");

        var newUser = User.builder()
                .username(username)
                .email(email)
                .password("encoded-pass")
                .build();

        when(userRepo.save(any(User.class))).thenReturn(Mono.just(newUser));

        // when
        var result = underTest.syncUser(oidcUser).block();

        // then
        assertThat(result).isEqualTo(oidcUser);

        verify(userRepo).findByUsername(username);
        verify(userRepo).save(any(User.class));
        verify(passwordEncoder).encode("oauth2");
    }

    @Test
    void syncUser_doesNotCreate_whenUserExists() {
        // given
        var username = "user123";

        var oidcUser = mock(OidcUser.class);
        when(oidcUser.getPreferredUsername()).thenReturn(username);
        when(oidcUser.getEmail()).thenReturn("irrelevant@example.com");

        var existingUser = User.builder()
                .username(username)
                .email("stored@example.com")
                .password("hashed")
                .build();

        when(userRepo.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(userRepo.save(any())).thenReturn(Mono.just(User.builder().build()));

        // when
        var result = underTest.syncUser(oidcUser).block();

        // then
        assertThat(result).isEqualTo(oidcUser);
        verify(userRepo).findByUsername(username);
        verify(userRepo, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
