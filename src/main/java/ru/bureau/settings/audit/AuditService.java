package ru.bureau.settings.audit;

import com.atlassian.audit.entity.AuditAttribute;
import ru.bureau.settings.dto.SettingsExportDto;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named
public class AuditService {

    private final AuditWriter auditWriter;

    public AuditService(AuditWriter auditWriter) {
        this.auditWriter = auditWriter;
    }

    public void logCreated(String name, String value) {
        auditWriter.logDelete(
                "SETTING",
                name,
                name,
                value,
                Collections.emptyList()
        );
    }

    public void logUpdated(String name, String oldValue, String newValue) {
        // Создаем атрибут для хранения старого значения
        AuditAttribute oldAttributeValue = new AuditAttribute("OLD_VALUE", oldValue);
        
        auditWriter.logDelete(
                "SETTING",
                name,
                name,
                newValue,
                Collections.singletonList(oldAttributeValue)
        );
    }

    public void logDeleted(String name, String oldValue) {
        // Создаем атрибут для хранения старого значения
        AuditAttribute oldAttributeValue = new AuditAttribute("VALUE", oldValue);
        
        auditWriter.logDelete(
                "SETTING",
                name,
                name,
                oldValue,
                Collections.singletonList(oldAttributeValue)
        );
    }

    /**
     * Логирует импорт настроек, записывая все старые настройки (до замены).
     *
     * @param oldSettings список старых настроек, которые были заменены
     */
    public void logImport(List<SettingsExportDto> oldSettings) {
        List<AuditAttribute> attributes = new ArrayList<>();
        if (oldSettings != null) {
            for (SettingsExportDto setting : oldSettings) {
                attributes.add(new AuditAttribute("OLD_" + setting.getName(), setting.getValue()));
            }
        }

        auditWriter.logImport(
                "SETTINGS",
                "import",
                "settings-import",
                "",
                attributes
        );
    }
}