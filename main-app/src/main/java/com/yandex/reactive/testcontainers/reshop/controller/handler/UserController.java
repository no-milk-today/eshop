package com.yandex.reactive.testcontainers.reshop.controller.handler;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    /**
     * display access token (for testing)
     * @param authorizedClient authorized client
     * @return access token
     */
    @GetMapping("/token")
    public String getToken(@RegisteredOAuth2AuthorizedClient("storefront") OAuth2AuthorizedClient authorizedClient) {
        return authorizedClient.getAccessToken().getTokenValue();
    }

}
