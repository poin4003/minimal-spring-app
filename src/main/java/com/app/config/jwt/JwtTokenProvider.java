package com.app.config.jwt;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private final JwtCryptoService jwtCrypto;

    public KeyPairDto generateKeyPair() {
        try {
            return jwtCrypto.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC keys", e);
        }
    }

    public String generateAccessToken(UUID userId, JwtPayload payload, String privateKeyStr) {
        try {
            long now = System.currentTimeMillis();
            long expiryDate = now + appProperties.getJwt().getAccessTokenExpirationMs();

            Key key = jwtCrypto.getKey(privateKeyStr);

            Map<String, Object> claims = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {
            });

            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(expiryDate))
                    .signWith(key, jwtCrypto.getAlgorithm())
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Could not generate token", e);
        }
    }

    public String generateRefreshToken(UUID userId, String privateKeyStr) {
        try {
            long now = System.currentTimeMillis();
            long expiryDate = now + appProperties.getJwt().getRefreshTokenExpirationMs();

            Key key = jwtCrypto.getKey(privateKeyStr);

            return Jwts.builder()
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(expiryDate))
                    .signWith(key, jwtCrypto.getAlgorithm())
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Could not generate refresh token", e);
        }
    }

    public UUID getUserIdFromTokenUnverified(String token) {
        try {
            String tokenWithoutSignature = token.substring(0, token.lastIndexOf('.') + 1);
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(tokenWithoutSignature)
                    .getBody();

            String sub = claims.getSubject();

            return UUID.fromString(sub);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token, String publicKeyStr) {
        try {
            Key key = jwtCrypto.getKey(publicKeyStr);

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpiryDateFromToken(String token, String publicKeyStr) {
        try {
            Key key = jwtCrypto.getKey(publicKeyStr);

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

        } catch (Exception e) {
            return null;
        }
    }

    public Claims getAllClaimsFromToken(String token, String publicKeyStr) throws Exception {
        Key key = jwtCrypto.getKey(publicKeyStr);

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
