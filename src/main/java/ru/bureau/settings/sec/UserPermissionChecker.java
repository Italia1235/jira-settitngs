package ru.bureau.settings.sec;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.inject.Named;

@Named
public class UserPermissionChecker {
    private final GlobalPermissionManager globalPermissionManager;

    public UserPermissionChecker( @ComponentImport GlobalPermissionManager globalPermissionManager) {
        this.globalPermissionManager = globalPermissionManager;
    }
    public boolean isUserHasPermissionForMappingManagement(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user);
    }
}
