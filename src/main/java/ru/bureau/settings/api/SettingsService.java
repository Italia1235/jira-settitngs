package ru.bureau.settings.api;




import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.entity.Setting;

import java.util.List;

public interface SettingsService {

    /**
     * Creates a new setting with the given name and value.
     *
     * @param name  The name of the setting. Must not be blank.
     * @param value The value of the setting. Must not be blank.
     * @return The created Setting object.
     * @throws IllegalArgumentException if name or value is blank, or if a setting with the given name already exists.
     */
    Setting createSetting(String name, String value);

    /**
     * Retrieves a setting by its name.
     *
     * @param name The name of the setting to retrieve. Must not be blank.
     * @return The Setting object if found, null otherwise.
     * @throws IllegalArgumentException if name is blank.
     */
    Setting getSetting(String name);

    void updateSettings(String name, String newValue);

    boolean deleteSettingByName(String name);

    List<Setting> getAllSettings();

    SettingDto findById(Integer id);
}