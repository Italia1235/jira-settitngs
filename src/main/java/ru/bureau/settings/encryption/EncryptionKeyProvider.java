package ru.bureau.settings.encryption;

import java.io.IOException;

public interface EncryptionKeyProvider {
    /**
     * Возвращает расшифрованный ключ. При первом вызове читает файл, далее возвращает кэш.
     * @throws IOException если файл недоступен, пуст или не имеет прав чтения
     */
    String getEncryptionKey() throws IOException;

    /**
     * Принудительно сбрасывает кэш. Используется при ротации ключа или изменении файла.
     */
    void refreshKey();
}