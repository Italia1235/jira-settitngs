package ru.bureau.settings.dto;

import java.io.Serializable;

/**
 * DTO для экспорта/импорта настроек.
 * Не содержит id из БД, т.к. при импорте в другую систему ID будут другими.
 */
public class SettingsExportDto implements Serializable {

    private String name;
    private String value;
    private String explanation;

    public SettingsExportDto() {
    }

    public SettingsExportDto(String name, String value, String explanation) {
        this.name = name;
        this.value = value;
        this.explanation = explanation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}