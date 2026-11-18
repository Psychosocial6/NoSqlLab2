package com.example.nosqllab2.users;

import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

public final class PasswordHashes {
    private static final String LEGACY_PREFIX = "{legacy}";

    private PasswordHashes() {
    }

    public static PasswordEncoder encoder() {
        String scheme = "pbkdf2-sha256";
        PasswordEncoder pbkdf2 = new Pbkdf2PasswordEncoder(
                "", 16, 600_000, Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        return new DelegatingPasswordEncoder(scheme, Map.of(
                scheme, pbkdf2,
                "legacy", new LegacyVerifier()));
    }

    static String credential(User user) {
        if (user.getPasswordHash() != null) {
            return user.getPasswordHash();
        }
        return user.getPassword() == null ? null : LEGACY_PREFIX + user.getPassword();
    }

    private static class LegacyVerifier implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            throw new UnsupportedOperationException("Legacy passwords can only be verified");
        }

        @Override
        public boolean matches(CharSequence rawPassword, String stored) {
            return rawPassword != null && stored != null && MessageDigest.isEqual(
                    rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                    stored.getBytes(StandardCharsets.UTF_8));
        }
    }
}
