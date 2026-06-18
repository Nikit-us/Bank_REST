package com.example.bankcards.util;

import com.example.bankcards.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Component;

@Component
public class CardNumberEncryptor {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final BytesEncryptor encryptor;
    private final SecretKeySpec hmacKey;

    public CardNumberEncryptor(AppProperties properties) {
        AppProperties.Encryption config = properties.getEncryption();
        this.encryptor = Encryptors.stronger(config.getPassword(), config.getSalt());
        byte[] hmac = Base64.getDecoder().decode(config.getHmacKey());
        this.hmacKey = new SecretKeySpec(hmac, HMAC_ALGORITHM);
    }

    public String encrypt(String plaintext) {
        byte[] ciphertext = encryptor.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    public String decrypt(String encrypted) {
        byte[] plaintext = encryptor.decrypt(Base64.getDecoder().decode(encrypted));
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public String hash(String plaintext) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return new String(Hex.encode(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash card number", e);
        }
    }
}
