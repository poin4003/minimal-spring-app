package com.app.config.jwt;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

@Component
public class JwtCryptoService {

    public String generateSigningKey() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public SecretKey toSecretKey(String signingKey) {
        byte[] keyBytes = Base64.getDecoder().decode(signingKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public MacAlgorithm getAlgorithm() {
        return Jwts.SIG.HS512;
    }
}
