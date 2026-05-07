package ru.bureau.settings.audit;

import com.atlassian.audit.entity.AuditAttribute;

import javax.inject.Named;
import java.util.Collections;

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
        AuditAttribute oldAttributeValue = new AuditAttribute("OLD_VALUE", oldValue);
        
        auditWriter.logDelete(
                "SETTING",
                name,
                name,
                oldValue,
                Collections.singletonList(oldAttributeValue)
        );
    }
}