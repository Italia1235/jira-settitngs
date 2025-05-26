package ru.bureau.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.DBParam;
import org.apache.commons.lang3.StringUtils;
import ru.bureau.api.SettingsService;
import ru.bureau.entity.Setting;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;


@Slf4j
@ExportAsService({SettingsService.class})
@Named

public class SettingsServiceImpl implements SettingsService {
    @ComponentImport
    private final ActiveObjects activeObjects;
    private final CacheSettings cacheSettings;
    @ComponentImport
    private final CacheManager cacheManager;
    private final Cache<String, Setting> cache;

    @Inject
    public SettingsServiceImpl(ActiveObjects activeObjects, CacheManager cacheManager) {
        this.activeObjects = activeObjects;
        this.cacheManager = cacheManager;
        this.cacheSettings = new CacheSettingsBuilder().remote().replicateViaInvalidation().build();
        this.cache = cacheManager.getCache(SettingsServiceImpl.class.getName() + ".cache", this::loadSettingFromDatabase
                , cacheSettings
        );


    }


    @SneakyThrows
    public Setting createSetting(String name, String value) {
        try {
            return activeObjects.executeInTransaction(() -> {
                Setting newSetting = activeObjects.create(Setting.class,
                        new DBParam("NAME", name),
                        new DBParam("VALUE", value)
                );
                newSetting.save();
                return newSetting;
            });


        } catch (Exception e) {
            log.warn("Ошибка при создании настройки: {}", e.getMessage(), e);
            return null;
        }
    }


    public Setting getSetting(@Nonnull String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        try {
            return cache.get(name);
        } catch (Exception e) {
            log.error("Error loading setting from cache", e);
            return null;
        }
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



}