package ru.bureau.settings.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Named
public class DefaultEncryptionKeyProvider implements EncryptionKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultEncryptionKeyProvider.class);
    private static final String KEY_PATH = "/opt/jira-secrets/encryption.key";


    private static final String CACHED_KEY;

    static {
        String key = null;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(KEY_PATH));
            key = new String(bytes, StandardCharsets.UTF_8).trim();
            if (key.isEmpty()) {
                throw new IOException("Файл ключа пуст или содержит только пробелы");
            }
        } catch (Exception e) {
            log.error("Не удалось прочитать ключ при инициализации: {}", e.getMessage());
        }
        CACHED_KEY = key;
    }

    @Override
    public String getEncryptionKey() {
        if (CACHED_KEY == null) {
            throw new IllegalStateException("Ключ не загружен. Проверьте наличие файла и права на: " + KEY_PATH);
        }
        return CACHED_KEY;
    }

    @Override
    public void refreshKey() {

    }
}