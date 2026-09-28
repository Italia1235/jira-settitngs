package ut.ru.bureau.settings.dao;

import com.atlassian.activeobjects.test.TestActiveObjects;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.jdbc.H2Memory;
import net.java.ao.test.jdbc.Jdbc;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.bureau.settings.dao.SettingDao;
import ru.bureau.settings.encryption.EncryptionKeyProvider;
import ru.bureau.settings.encryption.StringEncryptor;
import ru.bureau.settings.entity.Setting;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(SettingDaoEncryptionTest.SettingDatabaseUpdater.class)
@Jdbc(value = H2Memory.class)
public class SettingDaoEncryptionTest {

    private static final String KEY = "test-encryption-key";

    private EntityManager entityManager;
    private TestActiveObjects ao;
    private SettingDao dao;

    @Before
    public void setUp() throws SQLException, IOException {
        try {
            entityManager.migrate(Setting.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to migrate schema", e);
        }
        ao = new TestActiveObjects(entityManager);

        EncryptionKeyProvider keyProvider = mock(EncryptionKeyProvider.class);
        when(keyProvider.getEncryptionKey()).thenReturn(KEY);
        StringEncryptor encryptor = new StringEncryptor(keyProvider);

        dao = new SettingDao(ao, encryptor);
    }

    @Test
    public void create_encrypted_storesCiphertextInDb() {
        dao.create("secret", "mySecretValue", "exp", true);

        // Прямой доступ к БД (без расшифровки DAO) — значение должно быть зашифровано
        Setting raw = ao.find(Setting.class, "NAME = ?", "secret")[0];
        assertTrue("Флаг encrypted должен быть true", raw.isEncrypted());
        assertNotEquals("В БД должен лежать шифротекст, а не открытый текст",
                "mySecretValue", raw.getValue());
    }

    @Test
    public void create_encrypted_readReturnsPlaintext() {
        dao.create("secret", "mySecretValue", "exp", true);

        Setting found = dao.findByName("secret");
        assertNotNull(found);
        assertEquals("Чтение через DAO должно вернуть открытый текст", "mySecretValue", found.getValue());
    }

    @Test
    public void create_notEncrypted_storesPlaintext() {
        dao.create("plain", "plainValue", "exp", false);

        Setting raw = ao.find(Setting.class, "NAME = ?", "plain")[0];
        assertFalse("Флаг encrypted должен быть false", raw.isEncrypted());
        assertEquals("Без шифрования значение хранится как есть", "plainValue", raw.getValue());
    }

    @Test
    public void roundTrip_createThenFind_returnsOriginal() {
        String value = "round-trip-value";
        dao.create("rt", value, "exp", true);

        Setting found = dao.findByName("rt");
        assertEquals(value, found.getValue());
    }

    @Test
    public void update_enableEncryption_storesCiphertext() {
        // Создаём без шифрования
        Setting created = dao.create("upd", "oldValue", "exp", false);

        // Включаем шифрование при обновлении
        dao.update(created, "newValue", true);

        Setting raw = ao.find(Setting.class, "NAME = ?", "upd")[0];
        assertTrue("Флаг должен стать true", raw.isEncrypted());
        assertNotEquals("В БД должен лежать шифротекст", "newValue", raw.getValue());

        // Чтение через DAO возвращает открытый текст
        Setting found = dao.findByName("upd");
        assertEquals("newValue", found.getValue());
    }

    @Test
    public void update_disableEncryption_storesPlaintext() {
        // Создаём с шифрованием
        Setting created = dao.create("upd2", "oldValue", "exp", true);

        // Выключаем шифрование при обновлении
        dao.update(created, "newValue", false);

        Setting raw = ao.find(Setting.class, "NAME = ?", "upd2")[0];
        assertFalse("Флаг должен стать false", raw.isEncrypted());
        assertEquals("В БД должен лежать открытый текст", "newValue", raw.getValue());
    }

    @Test
    public void update_keepEncryption_roundTrips() {
        Setting created = dao.create("upd3", "oldValue", "exp", true);

        dao.update(created, "newValue", true);

        Setting found = dao.findByName("upd3");
        assertEquals("newValue", found.getValue());
    }

    @Test
    public void findAll_decryptsEncryptedValues() {
        dao.create("a", "valueA", null, true);
        dao.create("b", "valueB", null, false);

        java.util.List<Setting> all = dao.findAll();
        assertEquals(2, all.size());
        for (Setting s : all) {
            if ("a".equals(s.getName())) {
                assertEquals("valueA", s.getValue());
            } else if ("b".equals(s.getName())) {
                assertEquals("valueB", s.getValue());
            }
        }
    }

    public static final class SettingDatabaseUpdater implements DatabaseUpdater {
        @Override
        public void update(EntityManager entityManager) throws Exception {
            entityManager.migrate(Setting.class);
        }
    }
}