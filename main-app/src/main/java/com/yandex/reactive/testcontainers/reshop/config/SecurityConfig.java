package com.yandex.reactive.testcontainers.reshop.config;

import com.yandex.reactive.testcontainers.reshop.service.KeycloakUserSyncService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean для обработки загрузки OIDC-юзера во время OAuth2 логина.
     * <p>
     * Использует дефолтный OidcReactiveOAuth2UserService
     * для получения юзера от провайдера, а затем синхронизирует его с БД
     * через KeycloakUserSyncService
     */
    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oauth2UserService(
            KeycloakUserSyncService syncService) {
        OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();
        return userRequest -> delegate.loadUser(userRequest)
                .flatMap(syncService::syncUser);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/", "/main/items", "/login", "/oauth2/**").permitAll()
                .anyExchange().authenticated()
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .oauth2Login(Customizer.withDefaults())
            .oauth2Client(Customizer.withDefaults())  //main-app выступает как OAuth2 client
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
            );

        return http.build();
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
        var successHandler =
            new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);

        successHandler.setPostLogoutRedirectUri("{baseUrl}/");

        return successHandler;
    }
}

