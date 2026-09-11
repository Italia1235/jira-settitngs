package ut.ru.bureau.settings;

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
import ru.bureau.settings.audit.AuditService;
import ru.bureau.settings.audit.AuditWriter;
import ru.bureau.settings.dao.SettingDao;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.mapper.SettingMapper;
import ru.bureau.settings.service.SettingsServiceImpl;
import ut.ru.bureau.settings.cache.TestCacheManager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(SettingDbTest.SettingDatabaseUpdater.class)
@Jdbc(value = H2Memory.class)
public class SettingDbTest {

    private EntityManager entityManager;
    private TestActiveObjects ao;
    private SettingsServiceImpl service;

    @Before
    public void setUp() throws SQLException {
        try {
            entityManager.migrate(Setting.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to migrate schema", e);
        }
        ao = new TestActiveObjects(entityManager);
        service = new SettingsServiceImpl(
                new SettingDao(ao),
                new SettingMapper(),
                new TestCacheManager(),
                new AuditService(mock(AuditWriter.class))
        );
    }

    /**
     * Создаёт настройку через сервис {@link SettingsServiceImpl#createSetting(String, String, String)},
     * чтобы тестировать именно сервис (а не прямой доступ к Active Objects).
     */
    private Setting createSettingInDb(String name, String value, String explanation) {
        return service.createSetting(name, value, explanation);
    }

    // ---------------------------------------------------------------
    // Прямая работа с БД (как в примере)
    // ---------------------------------------------------------------

    @Test
    public void insertSetting_existsInDb() {
        assertEquals("База должна быть пустой", 0, ao.find(Setting.class).length);

        service.createSetting("my.setting", "my.value", "my.explanation");

        assertEquals("В базе должна быть 1 запись", 1, ao.find(Setting.class).length);

        Setting found = ao.find(Setting.class, "NAME = ?", "my.setting")[0];
        assertNotNull("Настройка должна быть найдена", found);
        assertEquals("my.setting", found.getName());
        assertEquals("my.value", found.getValue());
        assertEquals("my.explanation", found.getExplanation());
    }

    // ---------------------------------------------------------------
    // createSetting
    // ---------------------------------------------------------------

    @Test
    public void createSetting_createsRecord() {
        assertEquals("База должна быть пустой", 0, ao.find(Setting.class).length);

        Setting created = service.createSetting("name", "value", "exp");

        assertEquals("В базе должна быть 1 запись", 1, ao.find(Setting.class).length);
        assertNotNull(created);
        assertEquals("name", created.getName());
        assertEquals("value", created.getValue());
        assertEquals("exp", created.getExplanation());
    }

    @Test
    public void createSetting_duplicate_throws() {
        createSettingInDb("dup", "v1", "e1");

        try {
            createSettingInDb("dup", "v2", "e2");
            fail("Должно быть выброшено исключение при дубликате имени");
        } catch (Exception e) {
            // ожидаемо: уникальность @Unique
        }
    }

    // ---------------------------------------------------------------
    // getSetting / getSettingValue
    // ---------------------------------------------------------------

    @Test
    public void getSetting_returnsRecord() {
        createSettingInDb("getme", "value", "exp");

        Setting found = service.getSetting("getme");

        assertNotNull("Запись должна быть найдена", found);
        assertEquals("getme", found.getName());
        assertEquals("value", found.getValue());
    }

    @Test
    public void getSetting_returnsNullWhenAbsent() {
        assertNull("Отсутствующая запись должна вернуть null", service.getSetting("missing"));
    }

    @Test
    public void getSetting_blankName_throws() {
        try {
            service.getSetting("");
            fail("Пустое имя должно вызвать IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ожидаемо
        }
        try {
            service.getSetting(null);
            fail("null имя должно вызвать IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ожидаемо
        }
    }

    @Test
    public void getSettingValue_returnsValue() {
        createSettingInDb("val", "someValue", "exp");

        assertEquals("someValue", service.getSettingValue("val"));
    }

    @Test
    public void getSettingValue_returnsNullWhenAbsent() {
        assertNull(service.getSettingValue("missing"));
    }

    // ---------------------------------------------------------------
    // findById
    // ---------------------------------------------------------------

    @Test
    public void findById_returnsDto() {
        Setting created = createSettingInDb("byid", "value", "exp");

        SettingDto dto = service.findById(created.getID());

        assertNotNull("DTO не должен быть null", dto);
        assertEquals("byid", dto.getName());
        assertEquals("value", dto.getValue());
        assertEquals("exp", dto.getExplanation());
    }

    @Test
    public void findById_returnsNullWhenAbsent() {
        assertNull(service.findById(999999));
    }

    // ---------------------------------------------------------------
    // updateSettings
    // ---------------------------------------------------------------

    @Test
    public void updateSettings_byName_changesValue() {
        createSettingInDb("upd", "old", "exp");

        service.updateSettings("upd", "newValue");

        Setting reloaded = service.getSetting("upd");
        assertNotNull(reloaded);
        assertEquals("newValue", reloaded.getValue());
    }

    @Test
    public void updateSettings_byId_changesValue() {
        Setting created = createSettingInDb("upd2", "old", "exp");

        service.updateSettings(created.getID(), "upd2", "newValue");

        Setting reloaded = service.getSetting("upd2");
        assertNotNull(reloaded);
        assertEquals("newValue", reloaded.getValue());
    }

    @Test
    public void updateSettings_byId_withExplanation() {
        Setting created = createSettingInDb("upd3", "old", "oldExp");

        service.updateSettings(created.getID(), "upd3", "newValue", "newExp");

        Setting reloaded = service.getSetting("upd3");
        assertNotNull(reloaded);
        assertEquals("newValue", reloaded.getValue());
        assertEquals("newExp", reloaded.getExplanation());
    }

    @Test
    public void updateSettingsExplanation_changesExplanation() {
        Setting created = createSettingInDb("upd4", "value", "oldExp");

        service.updateSettingsExplanation(created.getID(), "newExp");

        Setting reloaded = service.getSetting("upd4");
        assertNotNull(reloaded);
        assertEquals("newExp", reloaded.getExplanation());
        assertEquals("value", reloaded.getValue());
    }

    // ---------------------------------------------------------------
    // deleteSettingById
    // ---------------------------------------------------------------

    @Test
    public void deleteSettingById_removesRecord() {
        Setting created = createSettingInDb("del", "value", "exp");

        boolean deleted = service.deleteSettingById(created.getID());

        assertTrue("Должно вернуть true", deleted);
        assertEquals("Запись должна быть удалена", 0, ao.find(Setting.class).length);
    }

    @Test
    public void deleteSettingById_absent_returnsFalse() {
        assertFalse("Отсутствующий id должен вернуть false", service.deleteSettingById(999999));
    }

    // ---------------------------------------------------------------
    // getAllSettings / exportSettings
    // ---------------------------------------------------------------

    @Test
    public void getAllSettings_returnsAll() {
        createSettingInDb("a", "1", null);
        createSettingInDb("b", "2", null);
        createSettingInDb("c", "3", null);

        List<Setting> all = service.getAllSettings();

        assertEquals("Должно быть 3 записи", 3, all.size());
    }

    @Test
    public void exportSettings_returnsDtos() {
        createSettingInDb("a", "1", "expA");
        createSettingInDb("b", "2", null);

        List<SettingsExportDto> exported = service.exportSettings();

        assertEquals("Должно быть 2 DTO", 2, exported.size());
        for (SettingsExportDto dto : exported) {
            assertNotNull("Имя не должно быть null", dto.getName());
            assertNotNull("Значение не должно быть null", dto.getValue());
        }
    }

    // ---------------------------------------------------------------
    // importSettings
    // ---------------------------------------------------------------

//    @Test
//    public void importSettings_replacesAll() {
//        createSettingInDb("old1", "oldVal1", null);
//        createSettingInDb("old2", "oldVal2", null);
//        assertEquals("В базе должно быть 2 записи", 2, ao.find(Setting.class).length);
//
//        List<SettingsExportDto> newSettings = Arrays.asList(
//                new SettingsExportDto("new1", "v1", "e1"),
//                new SettingsExportDto("new2", "v2", null)
//        );
//
//        service.importSettings(newSettings);
//
//        assertEquals("Старые записи должны быть заменены", 2, ao.find(Setting.class).length);
//        assertNull("old1 не должен существовать", service.getSetting("old1"));
//        assertNotNull("new1 должен существовать", service.getSetting("new1"));
//        assertEquals("v2", service.getSettingValue("new2"));
//    }

    @Test
    public void importSettings_null_throws() {
        try {
            service.importSettings(null);
            fail("null список должен вызвать IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ожидаемо
        }
    }

    @Test
    public void importSettings_empty_throws() {
        try {
            service.importSettings(Collections.emptyList());
            fail("Пустой список должен вызвать IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ожидаемо
        }
    }

    @Test
    public void importSettings_tooMany_throws() {
        // SEC-009: лимит 1000 записей
        SettingsExportDto[] many = new SettingsExportDto[1001];
        for (int i = 0; i < many.length; i++) {
            many[i] = new SettingsExportDto("name" + i, "value", null);
        }

        try {
            service.importSettings(Arrays.asList(many));
            fail("Список > 1000 должен вызвать IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ожидаемо
        }
    }

    public static final class SettingDatabaseUpdater implements DatabaseUpdater {
        @Override
        public void update(EntityManager entityManager) throws Exception {
            entityManager.migrate(Setting.class);
        }
    }
}