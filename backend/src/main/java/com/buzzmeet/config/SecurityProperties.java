package com.buzzmeet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record SecurityProperties(String secret, long expiration) {
}