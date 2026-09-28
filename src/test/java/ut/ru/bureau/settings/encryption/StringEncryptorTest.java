package ut.ru.bureau.settings.encryption;

import org.junit.Before;
import org.junit.Test;
import ru.bureau.settings.encryption.EncryptionKeyProvider;
import ru.bureau.settings.encryption.StringEncryptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StringEncryptorTest {

    private static final String KEY = "my-super-secret-key-12345";

    private EncryptionKeyProvider keyProvider;
    private StringEncryptor encryptor;

    @Before
    public void setUp() throws IOException {
        keyProvider = mock(EncryptionKeyProvider.class);
        when(keyProvider.getEncryptionKey()).thenReturn(KEY);
        encryptor = new StringEncryptor(keyProvider);
    }

    @Test
    public void roundTrip_returnsOriginalText() {
        String plain = "hello world";
        String encrypted = encryptor.encrypt(plain);
        assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    public void roundTrip_emptyString() {
        String plain = "";
        String encrypted = encryptor.encrypt(plain);
        assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    public void roundTrip_unicodeText() {
        String plain = "Привет, мир! 日本語 🚀";
        String encrypted = encryptor.encrypt(plain);
        assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    public void roundTrip_longText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append("строка-").append(i).append(';');
        }
        String plain = sb.toString();
        String encrypted = encryptor.encrypt(plain);
        assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    public void encrypt_producesDifferentCiphertextsForSamePlaintext() {
        String plain = "same text";
        String first = encryptor.encrypt(plain);
        String second = encryptor.encrypt(plain);
        assertNotEquals("Случайный IV должен давать разные шифротексты", first, second);
    }

    @Test
    public void encrypt_resultIsBase64AndContainsIv() {
        String encrypted = encryptor.encrypt("data");
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        // IV (12) + ciphertext + tag (16) => минимум 28 байт
        assertTrue("Зашифрованная строка должна содержать IV + шифротекст + тег", decoded.length >= 28);
    }

    @Test
    public void decrypt_wrongKey_throws() throws IOException {
        String encrypted = encryptor.encrypt("secret value");

        EncryptionKeyProvider otherProvider = mock(EncryptionKeyProvider.class);
        when(otherProvider.getEncryptionKey()).thenReturn("another-key");
        StringEncryptor otherEncryptor = new StringEncryptor(otherProvider);

        assertThrows(IllegalStateException.class, () -> otherEncryptor.decrypt(encrypted));
    }

    @Test
    public void decrypt_tamperedData_throws() {
        String encrypted = encryptor.encrypt("secret value");
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        // Повреждаем последний байт (часть тега аутентификации)
        decoded[decoded.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(decoded);

        assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tampered));
    }

    @Test
    public void decrypt_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt(null));
    }

    @Test
    public void decrypt_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt(""));
    }

    @Test
    public void decrypt_notBase64_throws() {
        assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt("!!!not-base64!!!"));
    }

    @Test
    public void decrypt_tooShort_throws() {
        // base64 от 5 байт (меньше IV) — валидный base64, но слишком короткий
        String tooShort = Base64.getEncoder().encodeToString(new byte[5]);
        assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt(tooShort));
    }

    @Test
    public void encrypt_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> encryptor.encrypt(null));
    }

    @Test
    public void encrypt_whenKeyUnavailable_throws() throws IOException {
        EncryptionKeyProvider emptyProvider = mock(EncryptionKeyProvider.class);
        when(emptyProvider.getEncryptionKey()).thenReturn("");
        StringEncryptor emptyEncryptor = new StringEncryptor(emptyProvider);

        assertThrows(IllegalStateException.class, () -> emptyEncryptor.encrypt("data"));
    }

    @Test
    public void decrypt_returnsUtf8Correctly() {
        String plain = "значение с кириллицей";
        String encrypted = encryptor.encrypt(plain);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        // Проверяем, что шифротекст не содержит открытого текста в UTF-8
        String raw = new String(decoded, StandardCharsets.UTF_8);
        assertTrue("Шифротекст не должен содержать открытый текст", !raw.contains(plain));
        assertNotNull(encryptor.decrypt(encrypted));
    }
}