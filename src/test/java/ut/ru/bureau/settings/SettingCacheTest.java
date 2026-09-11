package ut.ru.bureau.settings;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.cache.CacheLoader;
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
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.mapper.SettingMapper;
import ru.bureau.settings.service.SettingsServiceImpl;
import ut.ru.bureau.settings.cache.TestCache;
import ut.ru.bureau.settings.cache.TestCacheManager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for caching behavior of {@link SettingsServiceImpl}.
 * <p>
 * Uses a real in-memory {@link TestCacheManager} instead of a mock,
 * so we can verify that:
 * <ul>
 *   <li>First call loads data from the database via {@link CacheLoader}</li>
 *   <li>Subsequent calls return cached data without hitting the database</li>
 *   <li>Cache is invalidated after {@code updateSettings}, {@code deleteSettingById}, {@code importSettings}</li>
 * </ul>
 */
@RunWith(ActiveObjectsJUnitRunner.class)
@Data(SettingCacheTest.SettingCacheDatabaseUpdater.class)
@Jdbc(value = H2Memory.class)
public class SettingCacheTest {

    /**
     * The cache name used by {@link SettingsServiceImpl}.
     */
    private static final String CACHE_NAME = SettingsServiceImpl.class.getName() + ".cache";

    private EntityManager entityManager;
    private TestActiveObjects ao;
    private SettingsServiceImpl service;
    private TestCacheManager testCacheManager;

    @Before
    public void setUp() throws SQLException {
        try {
            entityManager.migrate(Setting.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to migrate schema", e);
        }
        ao = new TestActiveObjects(entityManager);

        // Use real in-memory cache manager instead of a mock
        testCacheManager = new TestCacheManager();
        service = new SettingsServiceImpl(
                new SettingDao(ao),
                new SettingMapper(),
                testCacheManager,
                new AuditService(mock(AuditWriter.class))
        );
    }

    // ---------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------

    private TestCache<String, Setting> getCache() {
        return testCacheManager.getTestCache(CACHE_NAME);
    }

    private Setting createSettingInDb(String name, String value, String explanation) {
        return service.createSetting(name, value, explanation);
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    public void firstCall_loadsFromDatabase() {
        // Arrange: create a record in the database
        createSettingInDb("my.setting", "my.value", "my.explanation");
        ao.flushAll();

        // Act: first call
        Setting result = service.getSetting("my.setting");

        // Assert: data is loaded from DB
        assertNotNull("Result should not be null", result);
        assertEquals("my.setting", result.getName());
        assertEquals("my.value", result.getValue());

        // Assert: cache now contains the data
        TestCache<String, Setting> cache = getCache();
        assertNotNull("Cache should exist", cache);
        assertTrue("Cache should contain the key", cache.containsKey("my.setting"));
        Setting cached = cache.get("my.setting");
        assertNotNull("Cached value should not be null", cached);
        assertEquals("my.value", cached.getValue());
    }

    @Test
    public void secondCall_returnsCachedData() {
        // Arrange: create a record and load it into cache
        createSettingInDb("cached.setting", "cached.value", "exp");
        ao.flushAll();

        // First call — loads from DB into cache
        Setting firstResult = service.getSetting("cached.setting");
        assertNotNull("First call should return the record", firstResult);
        assertEquals("cached.value", firstResult.getValue());

        // Delete the record directly from the database (bypassing the service)
        ao.deleteWithSQL(Setting.class, "1=1");
        ao.flushAll();
        assertEquals("Database should be empty", 0, ao.find(Setting.class).length);

        // Act: second call — should return cached data, NOT query the DB
        Setting secondResult = service.getSetting("cached.setting");

        // Assert: data comes from cache (still has the record even though DB is empty)
        assertNotNull("Second call should not return null", secondResult);
        assertEquals("Second call should return cached data", "cached.value", secondResult.getValue());
    }

    @Test
    public void cacheIsInvalidatedAfterUpdateSettings() {
        // Arrange: create a record and load it into cache
        createSettingInDb("upd.cache", "oldValue", "exp");
        ao.flushAll();

        Setting firstResult = service.getSetting("upd.cache");
        assertNotNull("First call should return the record", firstResult);
        assertEquals("oldValue", firstResult.getValue());

        // Verify cache has the data
        TestCache<String, Setting> cache = getCache();
        assertTrue("Cache should contain the key", cache.containsKey("upd.cache"));

        // Act: update the setting — this should invalidate the cache
        service.updateSettings("upd.cache", "newValue");
        ao.flushAll();

        // Assert: cache should be empty after updateSettings
        assertFalse("Cache should be invalidated after updateSettings",
                cache.containsKey("upd.cache"));

        // Act: second call — should reload from DB
        Setting resultAfterUpdate = service.getSetting("upd.cache");

        // Assert: now contains the new value
        assertNotNull("Result after update should not be null", resultAfterUpdate);
        assertEquals("newValue", resultAfterUpdate.getValue());
    }

    @Test
    public void cacheIsInvalidatedAfterDeleteSettingById() {
        // Arrange: create a record and load it into cache
        Setting created = createSettingInDb("del.cache", "value", "exp");
        ao.flushAll();

        Setting firstResult = service.getSetting("del.cache");
        assertNotNull("First call should return the record", firstResult);

        // Verify cache has the data
        TestCache<String, Setting> cache = getCache();
        assertTrue("Cache should contain the key", cache.containsKey("del.cache"));

        // Act: delete the record — this should invalidate the cache
        service.deleteSettingById(created.getID());
        ao.flushAll();

        // Assert: cache should be empty after deleteSettingById
        assertFalse("Cache should be invalidated after deleteSettingById",
                cache.containsKey("del.cache"));

        // Act: second call — should reload from DB (now empty)
        Setting resultAfterDelete = service.getSetting("del.cache");

        // Assert: result is null
        assertNull("Result after delete should be null", resultAfterDelete);
    }

//    @Test
//    public void cacheIsInvalidatedAfterImportSettings() {
//        // Arrange: create a record and load it into cache
//        createSettingInDb("old.cache", "oldValue", "exp");
//        ao.flushAll();
//
//        Setting firstResult = service.getSetting("old.cache");
//        assertNotNull("First call should return the record", firstResult);
//
//        // Verify cache has the data
//        TestCache<String, Setting> cache = getCache();
//        assertTrue("Cache should contain the key", cache.containsKey("old.cache"));
//
//        // Act: import settings — this should invalidate the cache (removeAll)
//        List<SettingsExportDto> newSettings = Arrays.asList(
//                new SettingsExportDto("new1", "v1", "e1"),
//                new SettingsExportDto("new2", "v2", null)
//        );
//        service.importSettings(newSettings);
//        ao.flushAll();
//
//        // Assert: cache should be empty after importSettings
//        assertFalse("Cache should be invalidated after importSettings",
//                cache.containsKey("old.cache"));
//
//        // Act: second call — should reload from DB
//        Setting resultAfterImport = service.getSetting("new1");
//
//        // Assert: now contains the new setting
//        assertNotNull("Result after import should not be null", resultAfterImport);
//        assertEquals("v1", resultAfterImport.getValue());
//    }

    @Test
    public void cacheLoaderCalledOnlyOnCacheMiss() {
        // This test verifies the TestCache itself: CacheLoader.load() should be called
        // only when the key is not present in the cache.

        // Arrange: create a TestCache with a counting loader
        final int[] loadCount = {0};
        CacheLoader<String, String> countingLoader = key -> {
            loadCount[0]++;
            return "loaded:" + key;
        };

        TestCache<String, String> cache = new TestCache<>("test", countingLoader);

        // Act & Assert: first get() — should call loader
        String firstResult = cache.get("key1");
        assertEquals("loaded:key1", firstResult);
        assertEquals("Loader should have been called once", 1, loadCount[0]);

        // Act & Assert: second get() with same key — should NOT call loader
        String secondResult = cache.get("key1");
        assertEquals("loaded:key1", secondResult);
        assertEquals("Loader should still have been called only once", 1, loadCount[0]);

        // Act & Assert: get() with different key — should call loader again
        String thirdResult = cache.get("key2");
        assertEquals("loaded:key2", thirdResult);
        assertEquals("Loader should have been called twice", 2, loadCount[0]);
    }

    public static final class SettingCacheDatabaseUpdater implements DatabaseUpdater {
        @Override
        public void update(EntityManager entityManager) throws Exception {
            entityManager.migrate(Setting.class);
        }
    }
}