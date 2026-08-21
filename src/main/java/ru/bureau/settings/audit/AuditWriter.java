package ru.bureau.settings.audit;

import com.atlassian.audit.api.AuditService;
import com.atlassian.audit.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Named
public class AuditWriter {
    private static final Logger log = LoggerFactory.getLogger(AuditWriter.class);

    private final AuditService auditService;

    // I18n ключи для локализуемости (переименуйте под свой плагин)
    private static final String CATEGORY_KEY = "pluginSettings";
    private static final String SUMMARY_DELETE_KEY = "plugin.settings.audit.delete";
    private static final String SUMMARY_CREATE_KEY = "com.example.audit.summary.create";
    private static final String SUMMARY_UPDATE_KEY = "com.example.audit.summary.update";
    private static final String SUMMARY_IMPORT_KEY = "plugin.settings.audit.import";


    public AuditWriter(AuditService auditService) {
        this.auditService = requireNonNull(auditService);
    }

    public void logDelete(String resourceType, String resourceId, String resourceName,
                          String authorKey, List<AuditAttribute> extraAttributes) {

        auditResource(resourceType, resourceId, resourceName, SUMMARY_DELETE_KEY, CoverageLevel.BASE, extraAttributes);
    }

    public void logImport(String resourceType, String resourceId, String resourceName,
                          String authorKey, List<AuditAttribute> extraAttributes) {

        auditResource(resourceType, resourceId, resourceName, SUMMARY_IMPORT_KEY, CoverageLevel.BASE, extraAttributes);
    }


    private void auditResource(String resourceType, String resourceId, String resourceName,
                               String summaryKey, CoverageLevel level, List<AuditAttribute> extraAttributes) {

        // 1. Создаем список атрибутов (если есть)
        Set<AuditAttribute> attributes = (extraAttributes != null)
                ? new HashSet<>(extraAttributes)
                : Collections.emptySet();

        // 2. Создаем событие через fromI18nKeys (судя по decompiled коду)
        AuditEvent auditEvent = AuditEvent.fromI18nKeys(
                        CATEGORY_KEY,
                        summaryKey,
                        level,
                        CoverageArea.AUDIT_LOG
                )
                // 3. Добавляем объект, на который повлияли (Project, Issue и т.д.)
                .affectedObject(
                        AuditResource.builder(resourceName, resourceType)
                                .id(resourceId)
                                .build()
                )
                // 4. Добавляем дополнительные атрибуты (если нужны)
                .extraAttributes(attributes)
                .build();

        // 5. Отправляем событие
        auditService.audit(auditEvent);
    }
}
