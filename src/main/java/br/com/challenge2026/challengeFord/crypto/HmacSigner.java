package br.com.challenge2026.challengeFord.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class HmacSigner {

    private static final String ALGO = "HmacSHA256";

    @Value("${security.hmac.secret}")
    private String base64Secret;

    private byte[] secret;

    @PostConstruct
    void init() {
        this.secret = Base64.getDecoder().decode(base64Secret);
    }

    public String sign(String timestamp, String method, String path, String body) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret, ALGO));
            String canonical = timestamp + "\n" + method.toUpperCase() + "\n" + path + "\n" + (body == null ? "" : body);
            byte[] result = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar HMAC");
        }
    }

    public boolean verify(String expectedHex, String timestamp, String method, String path, String body) {
        if (expectedHex == null) return false;
        String computed = sign(timestamp, method, path, body);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                expectedHex.getBytes(StandardCharsets.UTF_8)
        );
    }
}
