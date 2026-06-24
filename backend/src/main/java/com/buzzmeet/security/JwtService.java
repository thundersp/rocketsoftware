package com.buzzmeet.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.buzzmeet.config.SecurityProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecurityProperties securityProperties;

	public JwtService(SecurityProperties securityProperties) {
		this.securityProperties = securityProperties;
	}

	public String generateToken(ApplicationUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusMillis(securityProperties.expiration());
		return Jwts.builder()
			.subject(user.getUsername())
			.claim("employeeId", user.getEmployeeId())
			.claim("roles", user.roleNames())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(signingKey())
			.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		Claims claims = parseClaims(token);
		return userDetails.getUsername().equalsIgnoreCase(claims.getSubject())
			&& claims.getExpiration().toInstant().isAfter(Instant.now());
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(signingKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private SecretKey signingKey() {
        String secret = securityProperties.secret();
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException exception) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}