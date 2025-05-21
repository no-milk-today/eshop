package com.yandex.reactive.testcontainers.reshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Configuration
@EnableReactiveMethodSecurity
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilter(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .pathMatchers("/profile/**").authenticated()
                        .anyExchange().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}


@Service
class AdminService {
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> getSecretData() {
        return Mono.just("Top secret data");
    }
}

@RestController
@RequestMapping("/admin")
class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/data")
    public Mono<String> getData() {
        return adminService.getSecretData();
    }
}

@RestController
@RequestMapping("/profile")
class ProfileController {

    @GetMapping("/info")
    public Mono<String> getProfile() {
        return Mono.just("This is your profile info");
    }
}
