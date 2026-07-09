package com.app.config.jwt;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtCryptoService {

    public KeyPairDto generateKey() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        String secret = Base64.getEncoder().encodeToString(keyBytes);

        return new KeyPairDto(secret, secret);
    }

    public Key getKey(String keyStr) {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public SignatureAlgorithm getAlgorithm() {
        return SignatureAlgorithm.HS512;
    }
}
