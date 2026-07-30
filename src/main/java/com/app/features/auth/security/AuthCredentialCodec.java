package com.app.features.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthCredentialCodec {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    public String generateToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public String hash(String credential) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    appProperties.getAuth()
                            .getCredentialHashSecret()
                            .getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));

            return HexFormat.of().formatHex(
                    mac.doFinal(credential.getBytes(
                            StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw ExceptionFactory.serverError(
                    "error.auth.credentialHashFailed",
                    exception);
        }
    }

    public boolean matches(
            String rawCredential,
            String expectedHash) {
        byte[] actual = HexFormat.of().parseHex(
                hash(rawCredential));
        byte[] expected = HexFormat.of().parseHex(expectedHash);
        return MessageDigest.isEqual(actual, expected);
    }
}
