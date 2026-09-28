package ru.bureau.settings.encryption;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Сервис симметричного шифрования/расшифровки строк по ключу из {@link EncryptionKeyProvider}.
 *
 * <p>Протокол: <b>AES/GCM/NoPadding</b> (аутентифицированное шифрование).</p>
 * <ul>
 *     <li>Ключ: 256 бит (32 байта), выводится из строки ключа через SHA-256.</li>
 *     <li>IV: 12 байт, генерируется случайно для каждого шифрования.</li>
 *     <li>Тег аутентификации: 128 бит (16 байт).</li>
 *     <li>Формат результата: {@code base64(IV || ciphertext || tag)}.</li>
 * </ul>
 *
 * <p>Благодаря случайному IV один и тот же открытый текст даёт разные шифротексты,
 * а тег аутентификации защищает от подмены данных.</p>
 */
@Named
public class StringEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final EncryptionKeyProvider keyProvider;

    @Inject
    public StringEncryptor(EncryptionKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * Шифрует открытую строку.
     *
     * @param plainText открытый текст (может быть пустым, но не {@code null})
     * @return зашифрованная строка в формате {@code base64(IV || ciphertext || tag)}
     * @throws IllegalArgumentException если {@code plainText} равен {@code null}
     * @throws IllegalStateException    если ключ недоступен или произошла ошибка шифрования
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("plainText must not be null");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(TAG_BITS, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка шифрования строки", e);
        }
    }

    /**
     * Расшифровывает строку, полученную методом {@link #encrypt(String)}.
     *
     * @param cipherText зашифрованная строка в формате {@code base64(IV || ciphertext || tag)}
     * @return открытый текст
     * @throws IllegalArgumentException если {@code cipherText} равен {@code null}, пуст или не является корректным base64
     * @throws IllegalStateException    если ключ недоступен, данные повреждены/подменены или произошла ошибка расшифровки
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            throw new IllegalArgumentException("cipherText must not be null or empty");
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            if (all.length < IV_LENGTH) {
                throw new IllegalArgumentException("cipherText слишком короткий: не содержит IV");
            }

            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(TAG_BITS, iv));

            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка расшифровки строки (возможно, неверный ключ или повреждённые данные)", e);
        }
    }

    /**
     * Выводит 256-битный ключ AES из строки ключа через SHA-256.
     * Строка ключа берётся из {@link EncryptionKeyProvider#getEncryptionKey()}.
     */
    private SecretKeySpec deriveKey() {
        try {
            String keyString = keyProvider.getEncryptionKey();
            if (keyString == null || keyString.isEmpty()) {
                throw new IllegalStateException("Ключ шифрования пуст или недоступен");
            }
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] keyBytes = digest.digest(keyString.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось вывести ключ шифрования", e);
        }
    }
}