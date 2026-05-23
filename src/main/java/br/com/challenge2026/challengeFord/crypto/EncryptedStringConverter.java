package br.com.challenge2026.challengeFord.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, byte[]>, ApplicationContextAware {

    private static AesGcmCipher cipher;

    @Autowired
    public void setCipher(AesGcmCipher cipher) {
        EncryptedStringConverter.cipher = cipher;
    }

    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext) {
        if (cipher == null) {
            cipher = applicationContext.getBean(AesGcmCipher.class);
        }
    }

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        return cipher.decrypt(dbData);
    }
}
