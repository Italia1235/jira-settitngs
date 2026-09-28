package ru.bureau.settings.api;

import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.entity.Setting;

import java.util.List;

public interface SettingsService {

    /**
     * Creates a new setting with the given name, value, explanation and encryption flag.
     *
     * @param name      The name of the setting. Must not be blank.
     * @param value     The value of the setting. Must not be blank.
     * @param exp       The explanation of the setting.
     * @param encrypted Whether the value should be stored encrypted in the DB.
     * @return The created Setting object.
     * @throws IllegalArgumentException if name or value is blank, or if a setting with the given name already exists.
     */
    Setting createSetting(String name, String value, String exp, boolean encrypted);

    /**
     * Retrieves a setting by its name.
     *
     * @param name The name of the setting to retrieve. Must not be blank.
     * @return The Setting object if found, null otherwise.
     * @throws IllegalArgumentException if name is blank.
     */
    Setting getSetting(String name);

    void updateSettings(String name, String newValue);


    List<Setting> getAllSettings();

    SettingDto findById(Integer id);

     boolean deleteSettingById(int id);
     void updateSettings(int settingId, String name, String newValue);

     /**
      * Updates a setting with the given id, name, value, explanation and encryption flag.
      *
      * @param settingId the id of the setting
      * @param name      the (possibly new) name of the setting
      * @param newValue  the new value (plain text)
      * @param explanation the new explanation
      * @param encrypted whether the value should be stored encrypted in the DB
      */
     void updateSettings(int settingId, String name, String newValue, String explanation, boolean encrypted);
     void updateSettingsExplanation(int settingId, String explanation);
     String getSettingValue(String name);

    /**
     * Exports all settings as a list of export DTOs (without DB ids).
     *
     * @return list of settings for export
     */
    List<SettingsExportDto> exportSettings();

    /**
     * Imports settings from a list of export DTOs, replacing all current settings.
     *
     * @param settings list of settings to import
     */
    void importSettings(List<SettingsExportDto> settings);
}