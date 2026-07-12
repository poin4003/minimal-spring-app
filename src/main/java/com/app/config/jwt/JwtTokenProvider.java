package com.app.config.jwt;

import java.io.OutputStream;
import java.io.Reader;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private final JwtCryptoService jwtCrypto;

    public String generateSigningKey() {
        try {
            return jwtCrypto.generateSigningKey();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC signing key", e);
        }
    }

    public String generateAccessToken(UUID userId, JwtPayload payload, String signingKey) {
        try {
            long now = System.currentTimeMillis();
            long expiryDate = now + appProperties.getJwt().getAccessTokenExpirationMs();

            SecretKey key = jwtCrypto.toSecretKey(signingKey);

            Map<String, Object> claims = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {
            });

            return Jwts.builder()
                    .json(jwtSerializer())
                    .claims(claims)
                    .subject(userId.toString())
                    .issuedAt(new Date(now))
                    .expiration(new Date(expiryDate))
                    .signWith(key, jwtCrypto.getAlgorithm())
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Could not generate token", e);
        }
    }

    public String generateRefreshToken(UUID userId, String signingKey) {
        try {
            long now = System.currentTimeMillis();
            long expiryDate = now + appProperties.getJwt().getRefreshTokenExpirationMs();

            SecretKey key = jwtCrypto.toSecretKey(signingKey);

            return Jwts.builder()
                    .json(jwtSerializer())
                    .subject(userId.toString())
                    .issuedAt(new Date(now))
                    .expiration(new Date(expiryDate))
                    .signWith(key, jwtCrypto.getAlgorithm())
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Could not generate refresh token", e);
        }
    }

    public UUID getUserIdFromTokenUnverified(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() {
                    });

            Object sub = payload.get("sub");
            return sub != null ? UUID.fromString(sub.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token, String signingKey) {
        try {
            SecretKey key = jwtCrypto.toSecretKey(signingKey);

            Jwts.parser()
                    .json(jwtDeserializer())
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpiryDateFromToken(String token, String signingKey) {
        try {
            SecretKey key = jwtCrypto.toSecretKey(signingKey);

            return Jwts.parser()
                    .json(jwtDeserializer())
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();

        } catch (Exception e) {
            return null;
        }
    }

    public Claims getAllClaimsFromToken(String token, String signingKey) throws Exception {
        SecretKey key = jwtCrypto.toSecretKey(signingKey);

        return Jwts.parser()
                .json(jwtDeserializer())
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Serializer<Map<String, ?>> jwtSerializer() {
        return new Serializer<>() {
            @Override
            public byte[] serialize(Map<String, ?> map) throws SerializationException {
                try {
                    return objectMapper.writeValueAsBytes(map);
                } catch (Exception e) {
                    throw new SerializationException("Could not serialize JWT payload", e);
                }
            }

            @Override
            public void serialize(Map<String, ?> map, OutputStream out) throws SerializationException {
                try {
                    objectMapper.writeValue(out, map);
                } catch (Exception e) {
                    throw new SerializationException("Could not serialize JWT payload", e);
                }
            }
        };
    }

    private Deserializer<Map<String, ?>> jwtDeserializer() {
        return new Deserializer<>() {
            @Override
            public Map<String, ?> deserialize(byte[] bytes) throws DeserializationException {
                try {
                    return objectMapper.readValue(bytes, new TypeReference<Map<String, ?>>() {
                    });
                } catch (Exception e) {
                    throw new DeserializationException("Could not deserialize JWT payload", e);
                }
            }

            @Override
            public Map<String, ?> deserialize(Reader reader) throws DeserializationException {
                try {
                    return objectMapper.readValue(reader, new TypeReference<Map<String, ?>>() {
                    });
                } catch (Exception e) {
                    throw new DeserializationException("Could not deserialize JWT payload", e);
                }
            }
        };
    }
}
