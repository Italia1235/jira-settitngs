package ru.bureau.settings.rest;

import ru.bureau.settings.api.SettingsService;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.entity.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/settings") // ← КОРНЕВОЙ ПУТЬ REST
@Named
public class SettingsRestApi {

    private static final Logger log = LoggerFactory.getLogger(SettingsRestApi.class);
    private SettingsService settingsService;

    @Inject
    public SettingsRestApi(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GET
    @Path("/") // ← ПУТЬ КОНКРЕТНОГО МЕТОДА
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSettingsJson() {
        try {
            List<Setting> rawSettings = settingsService.getAllSettings();

            // 2. Конвертируем в DTO (удаляем хвосты)
            List<SettingDto> dtoList = rawSettings.stream()
                    .map(setting -> new SettingDto(
                            (long) setting.getID(),
                            setting.getName(),
                            setting.getValue()
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
}