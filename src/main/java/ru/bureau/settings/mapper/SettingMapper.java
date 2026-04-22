package ru.bureau.settings.mapper;

import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.entity.Setting;

import javax.inject.Named;
import java.util.Optional;
@Named
public class SettingMapper {

    public SettingDto toDto(Setting entity){
        return Optional.ofNullable(entity).map(e-> {
            return new SettingDto(e.getID(),e.getName(), e.getValue());
        }).orElse(null);
    }


    public void mapDTOtoEntity(SettingDto dto , Setting entity){
        entity.setValue(dto.getValue());
        entity.setName(dto.getName());
    }
}
