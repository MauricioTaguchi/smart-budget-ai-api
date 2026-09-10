package com.mauriciotaguchi.smartbudget.infrastructure.security;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class OpaqueTokenService {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean validFormat(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return false;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            return decoded.length == 32
                    && Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(token);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public String hash(String token) {
        if (!validFormat(token)) throw new IllegalArgumentException("Formato de credencial inválido.");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}

