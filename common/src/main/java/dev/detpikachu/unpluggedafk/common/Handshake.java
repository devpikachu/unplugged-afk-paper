package dev.detpikachu.unpluggedafk.common;

import org.jetbrains.annotations.ApiStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApiStatus.Internal
public final class Handshake {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int NONCE_BYTES = 32;

    public static String newNonce() {
        return newToken(NONCE_BYTES);
    }

    public static String newToken(int bytes) {
        final var token = new byte[bytes];
        new SecureRandom().nextBytes(token);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public static String sign(String secret, String nonce) {
        try {
            final var mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            final var signed = mac.doFinal(nonce.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available in this JVM.", exception);
        }
    }

    public static boolean verify(String secret, String nonce, String signature) {
        final var expected = sign(secret, nonce).getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}
