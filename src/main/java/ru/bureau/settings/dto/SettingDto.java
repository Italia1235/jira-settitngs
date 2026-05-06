package ru.bureau.settings.dto;


import java.io.Serializable;

public class SettingDto implements Serializable {
    private Integer id;
    private String name;
    private String value;
    private String explanation;

    // Конструкторы
    public SettingDto() {}

    public SettingDto(Integer id, String name, String value,String explanation) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.explanation = explanation;
    }


    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}