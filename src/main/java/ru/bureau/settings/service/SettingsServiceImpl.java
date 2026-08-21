package ru.bureau.settings.service;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bureau.settings.api.SettingsService;
import ru.bureau.settings.dao.SettingDao;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.error.DuplicateKeyException;
import ru.bureau.settings.mapper.SettingMapper;
import ru.bureau.settings.audit.AuditService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;


@ExportAsService({SettingsService.class})
@Named

public class SettingsServiceImpl implements SettingsService {
    private static final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);
    private final SettingDao settingDao;
    private final CacheSettings cacheSettings;
    private final SettingMapper settingMapper;
    @ComponentImport
    private final CacheManager cacheManager;
    private final Cache<String, Setting> cache;
    private final AuditService auditService;

    @Inject
    public SettingsServiceImpl(SettingDao settingDao, SettingMapper settingMapper, CacheManager cacheManager, AuditService auditService) {
        this.settingDao = settingDao;
        this.settingMapper = settingMapper;
        this.cacheManager = cacheManager;
        this.auditService = auditService;
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
        Setting newSetting = settingDao.create(name, value, exp);
        auditService.logCreated(name, value);
        return newSetting;


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
        Setting entity = settingDao.findById(id);
        return settingMapper.toDto(entity);
    }

    public void updateSettings(String name, String newValue) {
        settingDao.update(settingDao.findByName(name), newValue);
        // Получаем старое значение для аудита
        Setting oldSetting = settingDao.findByName(name);
        if (oldSetting != null) {
            auditService.logUpdated(name, oldSetting.getValue(), newValue);
        }
        cache.remove(name);
    }

    public void updateSettings(int settingId, String name, String newValue) {
        Setting setting = settingDao.findById(settingId);
        if (setting != null) {
            // Проверяем, изменилось ли имя
            if (!name.equals(setting.getName())) {
                cache.remove(setting.getName());
            }
            settingDao.updateName(setting, name);
            settingDao.update(setting, newValue);
            cache.remove(name);
            log.info("Setting {} (ID: {}) updated to value: {}", name, settingId, newValue);
            
            // Аудит: получаем старое значение для аудита
            Setting oldSetting = settingDao.findByName(name);
            if (oldSetting != null) {
                auditService.logUpdated(name, oldSetting.getValue(), newValue);
            }
        } else {
            log.warn("Setting with ID {} not found", settingId);
        }
    }

    public void updateSettings(int settingId, String name, String newValue, String explanation) {
        Setting setting = settingDao.findById(settingId);
        if (setting != null) {
            boolean nameChanged = !setting.getName().equals(name);

            if (nameChanged) {
                cache.remove(setting.getName()); // Удалить по старому
            }

            settingDao.updateWithExplanation(setting, newValue, explanation);
            // Удалить по новому имени (или старому, если имя не менялось)
            String cacheKey = name;
            cache.remove(cacheKey);

            log.info("Setting {} (ID: {}) updated to value: {} (Explanation: {})",
                    cacheKey, settingId, newValue, explanation);
            
            // Аудит: получаем старое значение для аудита
            Setting oldSetting = settingDao.findByName(name);
            if (oldSetting != null) {
                auditService.logUpdated(name, oldSetting.getValue(), newValue);
            }
        } else {
            log.warn("Setting with ID {} not found", settingId);
        }
    }
    
    public void updateSettingsExplanation(int settingId, String explanation) {
        Setting setting = settingDao.findById(settingId);
        if (setting != null) {
            settingDao.updateWithExplanation(setting, setting.getValue(), explanation);
            cache.remove(setting.getName());
            log.info("Setting {} (ID: {}) explanation updated to: {}", setting.getName(), settingId, explanation);
            
            // Аудит: получаем старое значение для аудита
            Setting oldSetting = settingDao.findByName(setting.getName());
            if (oldSetting != null) {
                auditService.logUpdated(setting.getName(), oldSetting.getValue(), setting.getValue());
            }
        } else {
            log.warn("Setting with ID {} not found", settingId);
        }
    }

    private Setting loadSettingFromDatabase(@Nonnull String name) {
        return settingDao.findByName(name);
    }



    @Override
    public List<Setting> getAllSettings() {
        try {
            return settingDao.findAll();
        } catch (Exception e) {
            log.error("Ошибка получения всех настроек", e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public boolean deleteSettingById(int id) {
        try {
            // Сначала получаем информацию о настройке перед удалением
            Setting settingToDelete = settingDao.findById(id);
            
            // Выполняем удаление
            String deletedName = settingDao.deleteById(id);
            
            // Если запись была удалена, то делаем аудит
            if (deletedName != null && settingToDelete != null) {
                // Записываем в аудит только если у нас есть данные для записи
                auditService.logDeleted(settingToDelete.getName(), settingToDelete.getValue());
                cache.remove(settingToDelete.getName());
                log.info("Cache entry for '{}' removed", settingToDelete.getName());
            }

            return deletedName != null;
        } catch (Exception e) {
            log.error("Error deleting setting with ID {}", id, e);
            throw new RuntimeException("Failed to delete setting with ID " + id, e);
        }
    }

    @Override
    public List<SettingsExportDto> exportSettings() {
        try {
            return settingDao.findAll().stream()
                    .map(settingMapper::toExportDto)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка экспорта настроек", e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public void importSettings(List<SettingsExportDto> settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings list must not be null");
        }
        try {
            // 0. Сохраняем старые настройки для аудита (до удаления)
            List<SettingsExportDto> oldSettings = settingDao.findAll().stream()
                    .map(settingMapper::toExportDto)
                    .collect(java.util.stream.Collectors.toList());

            // 1. Удаляем все текущие настройки
            settingDao.deleteAll();
            // 2. Создаём новые настройки из файла (в одной транзакции)
            settingDao.createAll(settings);
            // 3. Очищаем кэш
            cache.removeAll();
            // 4. Записываем в аудит старые настройки (после успешного импорта)
            auditService.logImport(oldSettings);
            log.info("Импорт настроек завершён, количество: {}", settings.size());
        } catch (Exception e) {
            log.error("Ошибка импорта настроек", e);
            throw new RuntimeException("Failed to import settings", e);
        }
    }
}