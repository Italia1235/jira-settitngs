package ru.bureau.settings.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bureau.settings.api.SettingsService;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.dto.SettingsExportFile;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.error.DuplicateKeyException;
import ru.bureau.settings.error.ErrorMessage;
import ru.bureau.settings.mapper.SettingMapper;
import ru.bureau.settings.servlet.UserPermissionChecker;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/settings") // ← КОРНЕВОЙ ПУТЬ REST
@Named
@Produces({MediaType.APPLICATION_JSON})
public class SettingsRestApi {

    private static final Logger log = LoggerFactory.getLogger(SettingsRestApi.class);
    private final SettingsService settingsService;
    private final SettingMapper settingMapper;
    private final UserPermissionChecker userPermissionChecker;

    @Inject
    public SettingsRestApi(SettingsService settingsService, SettingMapper settingMapper, UserPermissionChecker userPermissionChecker) {
        this.settingsService = settingsService;
        this.settingMapper = settingMapper;
        this.userPermissionChecker = userPermissionChecker;
    }

    @GET
    @Path("/")
    public Response getAllSettingsJson() {
        try {
            List<Setting> rawSettings = settingsService.getAllSettings();

            // 2. Конвертируем в DTO (удаляем хвосты)
            List<SettingDto> dtoList = rawSettings.stream()
                    .map(setting -> new SettingDto(
                            setting.getID(),
                            setting.getName(),
                            setting.getValue(),
                            setting.getExplanation()
                    ))
                    .collect(Collectors.toList());

            log.info("REST: Успех, настроек: {}", dtoList.size());

            // 3. Возвращаем DTO
            return Response.ok(dtoList, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            log.error("Ошибка при получении настроек", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal Server Error\"}").build();
        }
    }


    @GET
    @Path("/{settingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSetting(@PathParam("settingId") String mappingIdParam) {
        final int settingId = Integer.parseInt(mappingIdParam);
        SettingDto sd = settingsService.findById(settingId);
        return Response.ok(sd, MediaType.APPLICATION_JSON).build();
    }


    @GET
    @Path("name/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettingByKey(@PathParam("key") String mappingIdParam) {
        Setting sd = settingsService.getSetting(mappingIdParam);
        return Response.ok(sd.getValue(), MediaType.APPLICATION_JSON).build();
    }


    @POST
    @Path("/")
    public Response createSetting(SettingDto dto) {
        try {
            String name = dto.getName();
            String value = dto.getValue();
            String explanation = dto.getExplanation();
            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
                ErrorMessage errorMessage = new ErrorMessage("Mapping key and value can not be empty.");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
            }
            Setting set = settingsService.createSetting(name, value,explanation);
            SettingDto settingDto = settingMapper.toDto(set);
            return Response.ok(settingDto).build();
        } catch (DuplicateKeyException e) {
            log.warn("Дубликат: {}", e.getMessage());
            // Возвращаем ПРОСТОЙ JSON
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(e.getMessage())) // <-- Ключевое
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT).build();
        }

    }


    @DELETE
    @Path("/{settingId}")

    public Response deleteSetting(@PathParam("settingId") String settingIdParam) {
        try {
            int settingId = Integer.parseInt(settingIdParam);

            log.info("DELETE: Удаление настройки с ID: {}", settingId);

            // 1. Сначала проверяем, существует ли настройка
            SettingDto existing = settingsService.findById(settingId);
            if (existing == null) {
                log.warn("Попытка удалить несуществующую настройку: {}", settingId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorMessage("Настройка не найдена."))
                        .build();
            }

            // 2. Удаляем из БД (ваш сервис должен уметь удалять)
            settingsService.deleteSettingById(settingId);

            return Response.noContent().build();

        } catch (NumberFormatException e) {
            log.warn("Некорректный ID настройки: {}", settingIdParam);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("Некорректный формат ID."))
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при удалении настройки", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage("Ошибка при удалении: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{settingId}")
    public Response updateSetting(@PathParam("settingId") String settingIdParam, SettingDto dto) {
        try {
            int settingId = Integer.parseInt(settingIdParam);

            // Получаем существующую настройку
            SettingDto existing = settingsService.findById(settingId);
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorMessage("Настройка не найдена."))
                        .build();
            }

            // Обновляем значение и объяснение
            if (dto.getValue() != null) {
                existing.setValue(dto.getValue());
            }

            if (dto.getExplanation() != null) {
                existing.setExplanation(dto.getExplanation());
            }
            settingsService.updateSettings(settingId, existing.getName(), existing.getValue(), existing.getExplanation());

            return Response.ok(existing).build();

        } catch (Exception e) {
            log.error("Ошибка обновления", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage("Ошибка при обновлении: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Экспорт всех настроек в JSON-файл.
     * Доступно только администраторам (SYSTEM_ADMIN).
     */
    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportSettings() {
        // Проверка прав: только администратор может выгружать настройки
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null || !userPermissionChecker.isUserHasPermissionForMappingManagement(user)) {
            log.warn("Доступ запрещен для экспорта настроек. Пользователь: {}", user != null ? user.getUsername() : "anonymous");
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorMessage("Доступ запрещен. Только администратор может выгружать настройки."))
                    .build();
        }

        try {
            List<SettingsExportDto> settings = settingsService.exportSettings();
            SettingsExportFile exportFile = SettingsExportFile.of(settings);

            log.info("REST: Экспорт настроек, количество: {}", settings.size());

            return Response.ok(exportFile, MediaType.APPLICATION_JSON)
                    .header("Content-Disposition", "attachment; filename=\"settings-export.json\"")
                    .build();
        } catch (Exception e) {
            log.error("Ошибка экспорта настроек", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage("Ошибка экспорта: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Импорт настроек из JSON-файла.
     * Все текущие настройки перезатираются настройками из файла.
     * Доступно только администраторам (SYSTEM_ADMIN).
     */
    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importSettings(SettingsExportFile file) {
        // Проверка прав: только администратор может загружать настройки
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null || !userPermissionChecker.isUserHasPermissionForMappingManagement(user)) {
            log.warn("Доступ запрещен для импорта настроек. Пользователь: {}", user != null ? user.getUsername() : "anonymous");
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorMessage("Доступ запрещен. Только администратор может загружать настройки."))
                    .build();
        }

        try {
            // Валидация входных данных
            if (file == null || file.getSettings() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorMessage("Некорректный формат файла."))
                        .build();
            }

            // Валидация версии формата
            if (!"1.0".equals(file.getFormatVersion())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorMessage("Неподдерживаемая версия формата: " + file.getFormatVersion()))
                        .build();
            }

            settingsService.importSettings(file.getSettings());
            log.info("REST: Импорт настроек, количество: {}", file.getSettings().size());

            return Response.ok().build();
        } catch (Exception e) {
            log.error("Ошибка импорта настроек", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage("Ошибка импорта: " + e.getMessage()))
                    .build();
        }
    }
}