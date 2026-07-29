package com.app.features.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegistrationCredentialCodec {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        int length = appProperties.getAuth()
                .getRegistration()
                .getOtpLength();

        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    public String hash(String credential) {
        try {
            String secret = appProperties.getAuth()
                    .getRegistration()
                    .getOtpHashSecret();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));

            return HexFormat.of().formatHex(
                    mac.doFinal(credential.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw ExceptionFactory.serverError(
                    "error.registration.credentialHashFailed",
                    exception);
        }
    }
}
