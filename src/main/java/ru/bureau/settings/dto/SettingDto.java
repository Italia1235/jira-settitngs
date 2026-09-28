package ru.bureau.settings.dto;


import java.io.Serializable;

public class SettingDto implements Serializable {
    private Integer id;
    private String name;
    private String value;
    private String explanation;
    private boolean encrypted;

    // Конструкторы
    public SettingDto() {}

    public SettingDto(Integer id, String name, String value,String explanation) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.explanation = explanation;
    }

    public SettingDto(Integer id, String name, String value, String explanation, boolean encrypted) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.explanation = explanation;
        this.encrypted = encrypted;
    }


    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
}