package ru.bureau.settings.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.DBParam;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.entity.Setting;

import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

@Named
public class SettingDao {

    private final ActiveObjects activeObjects;

    public SettingDao(@ComponentImport  ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public Setting findByName(String name) {
        Setting[] settings = activeObjects.find(Setting.class, "NAME = ?", name);
        if (settings.length > 0) {
            return settings[0];
        }
        return null;
    }

    public Setting findById(int id) {
        return activeObjects.get(Setting.class, id);
    }

    public List<Setting> findAll() {
        Setting[] all = activeObjects.find(Setting.class);
        return Arrays.asList(all);
    }

    public Setting create(String name, String value, String explanation) {
        return activeObjects.executeInTransaction(() -> {
            Setting newSetting = activeObjects.create(Setting.class,
                    new DBParam("NAME", name),
                    new DBParam("VALUE", value),
                    new DBParam("EXPLANATION", explanation)
            );
            newSetting.save();
            return newSetting;
        });
    }

    public void update(Setting setting, String newValue) {
        activeObjects.executeInTransaction(() -> {
            setting.setValue(newValue);
            setting.save();
            return null;
        });
    }

    public void updateWithExplanation(Setting setting, String newValue, String explanation) {
        activeObjects.executeInTransaction(() -> {
            setting.setValue(newValue);
            if (explanation != null) {
                setting.setExplanation(explanation);
            }
            setting.save();
            return null;
        });
    }

    public void updateName(Setting setting, String newName) {
        activeObjects.executeInTransaction(() -> {
            setting.setName(newName);
            setting.save();
            return null;
        });
    }

    public String deleteById(int id) {
        return activeObjects.executeInTransaction(() -> {
            Setting[] setting = activeObjects.find(Setting.class, "ID = ?", id);
            if (setting.length > 0) {
                String name = setting[0].getName();
                activeObjects.delete(setting);
                return name;
            }
            return null;
        });
    }

    /**
     * Удаляет все настройки из БД.
     * Используется при импорте для полной замены настроек.
     */
    public void deleteAll() {
        activeObjects.executeInTransaction(() -> {
            activeObjects.deleteWithSQL(Setting.class, "1 = 1");
            return null;
        });
    }

    /**
     * Создаёт несколько настроек в одной транзакции.
     * Используется при импорте для атомарности (все или ничего).
     */
    public void createAll(List<SettingsExportDto> settings) {
        activeObjects.executeInTransaction(() -> {
            for (SettingsExportDto dto : settings) {
                Setting newSetting = activeObjects.create(Setting.class,
                        new DBParam("NAME", dto.getName()),
                        new DBParam("VALUE", dto.getValue()),
                        new DBParam("EXPLANATION", dto.getExplanation())
                );
                newSetting.save();
            }
            return null;
        });
    }
}