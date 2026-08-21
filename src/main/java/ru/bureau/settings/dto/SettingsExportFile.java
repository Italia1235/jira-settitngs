package ru.bureau.settings.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Конверт для экспорта/импорта настроек.
 * Содержит версию формата и метаданные, что критично для будущего импорта.
 */
public class SettingsExportFile implements Serializable {

    private String formatVersion;
    private String exportedAt;
    private List<SettingsExportDto> settings;

    public SettingsExportFile() {
    }

    public SettingsExportFile(String formatVersion, String exportedAt, List<SettingsExportDto> settings) {
        this.formatVersion = formatVersion;
        this.exportedAt = exportedAt;
        this.settings = settings;
    }

    public static SettingsExportFile of(List<SettingsExportDto> settings) {
        return new SettingsExportFile("1.0", Instant.now().toString(), settings);
    }

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
    }

    public List<SettingsExportDto> getSettings() {
        return settings;
    }

    public void setSettings(List<SettingsExportDto> settings) {
        this.settings = settings;
    }
}