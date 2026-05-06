package ru.bureau.settings.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.SneakyThrows;
import net.java.ao.DBParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bureau.settings.api.SettingsService;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.error.DuplicateKeyException;
import ru.bureau.settings.mapper.SettingMapper;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;


@ExportAsService({SettingsService.class})
@Named

public class SettingsServiceImpl implements SettingsService {
    private static final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);
    @ComponentImport
    private final ActiveObjects activeObjects;
    private final CacheSettings cacheSettings;
    private final SettingMapper settingMapper;
    @ComponentImport
    private final CacheManager cacheManager;
    private final Cache<String, Setting> cache;

    @Inject
    public SettingsServiceImpl(ActiveObjects activeObjects, SettingMapper settingMapper, CacheManager cacheManager) {
        this.activeObjects = activeObjects;
        this.settingMapper = settingMapper;
        this.cacheManager = cacheManager;
        this.cacheSettings = new CacheSettingsBuilder().remote().replicateViaInvalidation().build();
        this.cache = cacheManager.getCache(SettingsServiceImpl.class.getName() + ".cache", this::loadSettingFromDatabase
                , cacheSettings
        );


    }


    @SneakyThrows
    public Setting createSetting(String name, String value,String exp) {

        Setting setting = loadSettingFromDatabase(name);
        if (setting != null) {
            throw new DuplicateKeyException("Setting with name '" + name + "' already exists");
        } //explanation
        return activeObjects.executeInTransaction(() -> {
            Setting newSetting = activeObjects.create(Setting.class,
                    new DBParam("NAME", name),
                    new DBParam("VALUE", value),
                    new DBParam("EXPLANATION",exp)
            );
            newSetting.save();
            return newSetting;
        });


    }


    public Setting getSetting(@Nonnull String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        try {
            Setting setting = cache.get(name);
            if (setting != null) {
                log.info("Setting '{}' loaded from cache", name);
            } else {
                log.info("Setting '{}' not found in cache, loading from database", name);
            }
            return setting;
        } catch (Exception e) {
            log.error("Error loading setting from cache", e);
            return null;
        }

    }
    /**
     * Returns the value of a setting by its name.
     * This method bypasses the DTO mapping and returns the raw value.
     *
     * @param name The name of the setting to retrieve. Must not be blank.
     * @return The value of the setting if found, null otherwise.
     * @throws IllegalArgumentException if name is blank.
     */
    @Override
    public String getSettingValue(String name) {
        Setting setting = getSetting(name);
        return setting != null ? setting.getValue() : null;
    }

    public SettingDto findById(@Nonnull Integer id) {
//        if () {
//            throw new IllegalArgumentException("Name must not be blank");
//        }
        Setting entity = activeObjects.get(Setting.class, id);
        return settingMapper.toDto(entity);
    }

    public void updateSettings(String name, String newValue) {
        activeObjects.executeInTransaction(() -> {
            Setting[] settings = activeObjects.find(Setting.class, "NAME = ?", name);
            if (settings.length > 0) {
                Setting setting = settings[0];
                setting.setValue(newValue);
                setting.save();
                cache.remove(name);

            }
            return null;
        });
    }

    public void updateSettings(int settingId, String name, String newValue) {
        activeObjects.executeInTransaction(() -> {
            Setting[] settings = activeObjects.find(Setting.class, "ID = ?", settingId);
            if (settings.length > 0) {
                Setting setting = settings[0];
                if (!name.equals(setting.getName())) {
                    cache.remove(setting.getName());
                }
                setting.setValue(newValue);
                setting.save();
                cache.remove(setting.getName());
                log.info("Setting {} (ID: {}) updated to value: {}", setting.getName(), settingId, newValue);
            } else {
                log.warn("Setting with ID {} not found", settingId);
            }
            return null;
        });
    }

    public void updateSettings(int settingId, String name, String newValue, String explanation) {
        activeObjects.executeInTransaction(() -> {
            Setting[] settings = activeObjects.find(Setting.class, "ID = ?", settingId);

            if (settings.length > 0) {
                Setting setting = settings[0];
                boolean nameChanged = !setting.getName().equals(name);

                if (nameChanged) {
                    cache.remove(setting.getName()); // Удалить по старому
                }

                setting.setValue(newValue);
                // Устанавливаем объяснение только если оно не null (опциональность)
                if (explanation != null) {
                    setting.setExplanation(explanation);
                }

                setting.save();

                // Удалить по новому имени (или старому, если имя не менялось)
                String cacheKey = setting.getName();
                cache.remove(cacheKey);

                log.info("Setting {} (ID: {}) updated to value: {} (Explanation: {})",
                        cacheKey, settingId, newValue, explanation);
            } else {
                log.warn("Setting with ID {} not found", settingId);
            }

            return null;
        });
    }
    
    public void updateSettingsExplanation(int settingId, String explanation) {
        activeObjects.executeInTransaction(() -> {
            Setting[] settings = activeObjects.find(Setting.class, "ID = ?", settingId);
            if (settings.length > 0) {
                Setting setting = settings[0];
                setting.setExplanation(explanation);
                setting.save();
                cache.remove(setting.getName());
                log.info("Setting {} (ID: {}) explanation updated to: {}", setting.getName(), settingId, explanation);
            } else {
                log.warn("Setting with ID {} not found", settingId);
            }
            return null;
        });
    }

    private Setting loadSettingFromDatabase(@Nonnull String name) {

        Setting[] settings = activeObjects.find(Setting.class, "NAME = ?", name);
        if (settings.length > 0) {
            return settings[0];
        } else {
            log.warn("Setting '{}' not found in database", name);
            return null;
        }
    }

    public boolean deleteSettingByName(@Nonnull String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        try {
            return activeObjects.executeInTransaction(() -> {
                int deleted = activeObjects.deleteWithSQL(Setting.class, "NAME = ?", name);

                if (deleted > 0) {
                    cache.remove(name);
                    log.info("Setting '{}' successfully deleted, rows affected: {}", name, deleted);
                    return true;
                } else {
                    log.error("Setting '{}' not found for deletion", name);
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("Error deleting setting '{}'", name, e);
            throw new RuntimeException("Failed to delete setting: " + name, e);
        }
    }

    @Override
    public List<Setting> getAllSettings() {
        try {
            Setting[] all = activeObjects.find(Setting.class);
            // Конвертируем массив в список
            return Arrays.asList(all);
        } catch (Exception e) {
            log.error("Ошибка получения всех настроек", e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public boolean deleteSettingById(int id) {
        String deletedName = null;

        try {
            deletedName = activeObjects.executeInTransaction(() -> {
                // 1. Находим запись по ID
                Setting[] setting = activeObjects.find(Setting.class, "ID = ?", id);
                if (setting.length < 1) {
                    log.warn("Setting with ID {} not found", id);
                    return null;
                }

                // 2. Запоминаем имя (нужно для очистки кэша)
                String name = setting[0].getName();

                // 3. Удаляем из БД
                activeObjects.delete(setting);
                log.info("Setting '{}' deleted successfully, ID: {}", name, id);

                return name;
            });

            // 4. Очищаем кэш, только если запись была удалена
            if (deletedName != null) {
                cache.remove(deletedName);
                log.info("Cache entry for '{}' removed", deletedName);
            }

            return deletedName != null;
        } catch (Exception e) {
            log.error("Error deleting setting with ID {}", id, e);
            throw new RuntimeException("Failed to delete setting with ID " + id, e);
        }
    }

}