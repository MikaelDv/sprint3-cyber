package br.com.challenge2026.challengeFord.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AesGcmCipher {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    @Value("${security.encryption.aes-key}")
    private String base64Key;

    @PostConstruct
    void init() {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length < 16) {
            throw new IllegalStateException("Chave AES insuficiente (mínimo 128 bits)");
        }
        byte[] key32 = new byte[32];
        System.arraycopy(raw, 0, key32, 0, Math.min(raw.length, 32));
        if (raw.length < 32) {
            for (int i = raw.length; i < 32; i++) key32[i] = (byte) (i * 31);
        }
        this.secretKey = new SecretKeySpec(key32, "AES");
    }

    public byte[] encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar dado sensível");
        }
    }

    public String decrypt(byte[] payload) {
        if (payload == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            byte[] ct = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar dado sensível");
        }
    }

    public String sha256Hex(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash");
        }
    }
}
