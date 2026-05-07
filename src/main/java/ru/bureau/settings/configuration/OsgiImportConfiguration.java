package ru.bureau.settings.configuration;

import com.atlassian.audit.api.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

@Configuration
public class OsgiImportConfiguration {
    @Bean
    public AuditService auditSearchService() {
        return importOsgiService(AuditService.class);
    }
}