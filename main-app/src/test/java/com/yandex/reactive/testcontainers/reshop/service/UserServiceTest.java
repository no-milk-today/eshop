package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService underTest;

    @Test
    void testFindByUsername_UserExists() {
        var user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .email("testuser@example.com")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(user));

        UserDetails userDetails = underTest.findByUsername("testuser").block();

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("password");
        assertThat(userDetails.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void testFindByUsername_UserNotFound() {
        when(userRepository.findByUsername("nouser")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> underTest.findByUsername("nouser").block())
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nouser");
    }
}