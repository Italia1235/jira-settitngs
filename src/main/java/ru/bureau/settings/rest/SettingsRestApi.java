package ru.bureau.settings.rest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bureau.settings.api.SettingsService;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.error.DuplicateKeyException;
import ru.bureau.settings.error.ErrorMessage;
import ru.bureau.settings.mapper.SettingMapper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/settings") // ← КОРНЕВОЙ ПУТЬ REST
@Named
@Produces({MediaType.APPLICATION_JSON})
public class SettingsRestApi {

    private static final Logger log = LoggerFactory.getLogger(SettingsRestApi.class);
    private SettingsService settingsService;
    private final SettingMapper settingMapper;

    @Inject
    public SettingsRestApi(SettingsService settingsService, SettingMapper settingMapper) {
        this.settingsService = settingsService;
        this.settingMapper = settingMapper;
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


    @GET
    @Path("/{settingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSetting(@PathParam("settingId") String mappingIdParam) {
        final int settingId = Integer.parseInt(mappingIdParam);
        SettingDto sd = settingsService.findById(settingId);
        return Response.ok(sd,MediaType.APPLICATION_JSON).build();
    }


    @POST
    @Path("/")
    public Response createSetting(SettingDto dto){
        try {
            String name = dto.getName();
            String value = dto.getValue();
            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
                ErrorMessage errorMessage = new ErrorMessage("Mapping key and value can not be empty.");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
            }
            Setting set = settingsService.createSetting(name, value);
            SettingDto settingDto = settingMapper.toDto(set);
            return Response.ok(settingDto).build();
        }
        catch (DuplicateKeyException e) {
            log.warn("Дубликат: {}", e.getMessage());
            // Возвращаем ПРОСТОЙ JSON
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(e.getMessage())) // <-- Ключевое
                    .build();
        }catch (Exception e){
            return Response.status(Response.Status.CONFLICT).build();
        }

    }

}