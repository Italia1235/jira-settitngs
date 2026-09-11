package ut.ru.bureau.settings.mapper;

import org.junit.Test;
import ru.bureau.settings.dto.SettingDto;
import ru.bureau.settings.dto.SettingsExportDto;
import ru.bureau.settings.entity.Setting;
import ru.bureau.settings.mapper.SettingMapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SettingMapperTest {

    private final SettingMapper mapper = new SettingMapper();

    private Setting mockSetting(int id, String name, String value, String explanation) {
        Setting setting = mock(Setting.class);
        when(setting.getID()).thenReturn(id);
        when(setting.getName()).thenReturn(name);
        when(setting.getValue()).thenReturn(value);
        when(setting.getExplanation()).thenReturn(explanation);
        return setting;
    }

    // ---------------------------------------------------------------
    // toDto
    // ---------------------------------------------------------------

    @Test
    public void toDto_mapsFields() {
        Setting setting = mockSetting(42, "name", "value", "exp");

        SettingDto dto = mapper.toDto(setting);

        assertNotNull("DTO не должен быть null", dto);
        assertEquals(Integer.valueOf(42), dto.getId());
        assertEquals("name", dto.getName());
        assertEquals("value", dto.getValue());
        assertEquals("exp", dto.getExplanation());
    }

    @Test
    public void toDto_null_returnsNull() {
        assertNull("null entity должен вернуть null", mapper.toDto(null));
    }

    // ---------------------------------------------------------------
    // toExportDto
    // ---------------------------------------------------------------

    @Test
    public void toExportDto_mapsFields() {
        Setting setting = mockSetting(42, "name", "value", "exp");

        SettingsExportDto dto = mapper.toExportDto(setting);

        assertNotNull("DTO не должен быть null", dto);
        assertEquals("name", dto.getName());
        assertEquals("value", dto.getValue());
        assertEquals("exp", dto.getExplanation());
    }

    @Test
    public void toExportDto_null_returnsNull() {
        assertNull("null entity должен вернуть null", mapper.toExportDto(null));
    }

    // ---------------------------------------------------------------
    // mapDTOtoEntity
    // ---------------------------------------------------------------

    @Test
    public void mapDTOtoEntity_updatesEntity() {
        Setting setting = mock(Setting.class);
        SettingDto dto = new SettingDto(1, "newName", "newValue", "newExp");

        mapper.mapDTOtoEntity(dto, setting);

        verify(setting).setName("newName");
        verify(setting).setValue("newValue");
        verify(setting).setExplanation("newExp");
    }
}