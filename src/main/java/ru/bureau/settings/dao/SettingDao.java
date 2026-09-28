package ru.bureau.settings.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.DBParam;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.encryption.StringEncryptor;
import ru.bureau.settings.entity.Setting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

@Named
public class SettingDao {

    private final ActiveObjects activeObjects;
    private final StringEncryptor stringEncryptor;

    @Inject
    public SettingDao(@ComponentImport ActiveObjects activeObjects, StringEncryptor stringEncryptor) {
        this.activeObjects = activeObjects;
        this.stringEncryptor = stringEncryptor;
    }

    public Setting findByName(String name) {
        Setting[] settings = activeObjects.find(Setting.class, "NAME = ?", name);
        if (settings.length > 0) {
            return decryptIfNeeded(settings[0]);
        }
        return null;
    }

    public Setting findById(int id) {
        Setting setting = activeObjects.get(Setting.class, id);
        return decryptIfNeeded(setting);
    }

    public List<Setting> findAll() {
        Setting[] all = activeObjects.find(Setting.class);
        for (Setting setting : all) {
            decryptIfNeeded(setting);
        }
        return Arrays.asList(all);
    }

    public Setting create(String name, String value, String explanation, boolean encrypted) {
        String valueToStore = encryptIfNeeded(value, encrypted);
        Setting newSetting = activeObjects.create(Setting.class,
                new DBParam("NAME", name),
                new DBParam("VALUE", valueToStore),
                new DBParam("EXPLANATION", explanation),
                new DBParam("ENCRYPTED", encrypted)
        );
        newSetting.save();
        return decryptIfNeeded(newSetting);
    }

    /**
     * Обновляет значение настройки с учётом возможной смены флага {@code encrypted}.
     *
     * @param setting   сущность (значение в ней — открытый текст, т.к. DAO расшифровывает при чтении)
     * @param newValue  новое значение (открытый текст)
     * @param encrypted новое значение флага «шифровать»
     */
    public void update(Setting setting, String newValue, boolean encrypted) {
        String valueToStore = encryptIfNeeded(newValue, encrypted);
        setting.setValue(valueToStore);
        setting.setEncrypted(encrypted);
        setting.save();
    }

    public void updateWithExplanation(Setting setting, String newValue, String explanation, boolean encrypted) {
        String valueToStore = encryptIfNeeded(newValue, encrypted);
        setting.setValue(valueToStore);
        setting.setEncrypted(encrypted);
        if (explanation != null) {
            setting.setExplanation(explanation);
        }
        setting.save();
    }

    public void updateName(Setting setting, String newName) {
        setting.setName(newName);
        setting.save();
    }

    public String deleteById(int id) {
        Setting[] setting = activeObjects.find(Setting.class, "ID = ?", id);
        if (setting.length > 0) {
            String name = setting[0].getName();
            activeObjects.delete(setting);
            return name;
        }
        return null;
    }

    /**
     * Удаляет все настройки из БД.
     * Используется при импорте для полной замены настроек.
     */
    public void deleteAll() {
        activeObjects.deleteWithSQL(Setting.class, "1 = 1");
    }

    /**
     * Создаёт несколько настроек в одной транзакции.
     * Используется при импорте для атомарности (все или ничего).
     */
    public void createAll(List<SettingsExportDto> settings) {
        activeObjects.executeInTransaction(() -> {
            for (SettingsExportDto dto : settings) {
                String valueToStore = encryptIfNeeded(dto.getValue(), dto.isEncrypted());
                Setting newSetting = activeObjects.create(Setting.class,
                        new DBParam("NAME", dto.getName()),
                        new DBParam("VALUE", valueToStore),
                        new DBParam("EXPLANATION", dto.getExplanation()),
                        new DBParam("ENCRYPTED", dto.isEncrypted())
                );
                newSetting.save();
            }
            return null;
        });
    }

    /**
     * Атомарно заменяет все настройки: удаляет текущие и создаёт новые в одной транзакции.
     * Используется при импорте, чтобы при сбое не осталось «полуимпортированного» состояния.
     */
    public void replaceAll(List<SettingsExportDto> settings) {
        activeObjects.executeInTransaction(() -> {
            activeObjects.deleteWithSQL(Setting.class, "1 = 1");
            for (SettingsExportDto dto : settings) {
                String valueToStore = encryptIfNeeded(dto.getValue(), dto.isEncrypted());
                Setting newSetting = activeObjects.create(Setting.class,
                        new DBParam("NAME", dto.getName()),
                        new DBParam("VALUE", valueToStore),
                        new DBParam("EXPLANATION", dto.getExplanation()),
                        new DBParam("ENCRYPTED", dto.isEncrypted())
                );
                newSetting.save();
            }
            return null;
        });
    }

    /**
     * Шифрует значение перед сохранением, если {@code encrypted == true} и доступен шифратор.
     * Если шифратор недоступен (null) — значение сохраняется как есть (обратная совместимость).
     */
    private String encryptIfNeeded(String value, boolean encrypted) {
        if (encrypted && stringEncryptor != null) {
            return stringEncryptor.encrypt(value);
        }
        return value;
    }

    /**
     * Расшифровывает значение после чтения, если {@code encrypted == true} и доступен шифратор.
     * Если шифратор недоступен (null) — значение возвращается как есть.
     */
    private Setting decryptIfNeeded(Setting setting) {
        if (setting != null && setting.isEncrypted() && stringEncryptor != null) {
            setting.setValue(stringEncryptor.decrypt(setting.getValue()));
        }
        return setting;
    }
}