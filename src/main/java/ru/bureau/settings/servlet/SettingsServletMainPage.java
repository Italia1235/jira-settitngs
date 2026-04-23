package ru.bureau.settings.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SettingsServletMainPage extends HttpServlet {

    private static final String TEMPLATE_PATH_PARAM_NAME = "template";

    private final UserPermissionChecker userPermissionChecker;
    @ComponentImport
    private final TemplateRenderer templateRenderer;

    public SettingsServletMainPage(UserPermissionChecker userPermissionChecker, TemplateRenderer templateRenderer) {
        this.userPermissionChecker = userPermissionChecker;
        this.templateRenderer = templateRenderer;
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String templatePath = getInitParameter(TEMPLATE_PATH_PARAM_NAME);
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null || !userPermissionChecker.isUserHasPermissionForMappingManagement(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещен");
            return;
        }
        // Данные, которые мы передадим в шаблон .vm
        Map<String, Object> context = new HashMap<>();

        try {
            // Рендерим шаблон
            templateRenderer.render(templatePath, context, response.getWriter());
        } catch (Exception e) {

            throw new IOException(e);
        }
    }
}
