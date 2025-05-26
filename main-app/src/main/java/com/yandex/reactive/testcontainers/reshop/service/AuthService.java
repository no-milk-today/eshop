// Java
package com.yandex.reactive.testcontainers.reshop.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    public Mono<Authentication> getAuthentication(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken));
    }

    public Mono<String> getUsername(ServerRequest request) {
        return getAuthentication(request)
                .map(Authentication::getName)
                .defaultIfEmpty("Гость");
    }

    public Mono<Boolean> isAuthenticated(ServerRequest request) {
        return getAuthentication(request)
                .hasElement();
    }
}
