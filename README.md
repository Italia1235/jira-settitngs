# Bureau Settings Plugin

## 📋 Описание

Плагин **Bureau Settings** предназначен для управления настройками в системе Atlassian Jira. Он предоставляет возможность хранения, получения и управления различными параметрами конфигурации через удобный интерфейс администратора и программный REST API.

Плагин построен на **многослойной архитектуре** (Layered Architecture) с чётким разделением ответственности между слоями DAO, сервиса и аудита, что повышает поддерживаемость и тестируемость кода.

## ✨ Основные функции

### 🔧 Административный интерфейс
- Кнопка в админ-панели Jira (раздел **Add-ons → Bureau Settings Plugin**) для быстрого доступа к настройкам
- Веб-интерфейс для просмотра, создания, редактирования и удаления настроек
- Поддержка кэширования для повышения производительности
- Доступ только для пользователей с глобальным правом **SYSTEM_ADMIN**

### ⚙️ Управление настройками
- Создание новых настроек (имя, значение, объяснение)
- Получение значений настроек по ключам и по ID
- Обновление существующих настроек (значение, имя, объяснение)
- Удаление настроек
- Получение всех настроек одновременно
- Поле **explanation** (объяснение) для каждой настройки

### 📤 Экспорт и импорт настроек
- **Экспорт** всех настроек в JSON-файл (без ID из БД — пригоден для переноса между системами)
- **Импорт** настроек из JSON-файла с полной заменой текущих настроек
- Формат файла содержит версию (`formatVersion`) и метку времени экспорта (`exportedAt`)
- Импорт выполняется атомарно (в одной транзакции) и с подтверждением в UI
- Доступно только администраторам (SYSTEM_ADMIN)

### 🛡️ Аудит действий
- Логирование **создания** настройки
- Логирование **обновления** настройки (с сохранением старого значения в атрибуте `OLD_VALUE`)
- Логирование **удаления** настройки (с сохранением удалённого значения)
- Логирование **импорта** настроек (сохраняются все старые настройки до замены)
- Аудит выполняется **после** успешной операции в БД, чтобы ошибка аудита не откатывала транзакцию

### 🛠️ Технические особенности
- Использование **Active Objects** для хранения данных в базе данных
- Кэширование данных (Atlassian Cache) для повышения производительности
- Поддержка интернационализации
- REST API для программного доступа к настройкам
- Интеграция с Atlassian Audit API

## 🏗️ Архитектура

### Слои и компоненты

1. **Entity** — модель данных настройки ([`Setting`](src/main/java/ru/bureau/settings/entity/Setting.java))
2. **DAO** — слой доступа к данным, отвечает только за работу с БД ([`SettingDao`](src/main/java/ru/bureau/settings/dao/SettingDao.java))
3. **DTO** — объекты передачи данных ([`SettingDto`](src/main/java/ru/bureau/settings/dto/SettingDto.java), [`SettingsExportDto`](src/main/java/ru/bureau/settings/dto/SettingsExportDto.java), [`SettingsExportFile`](src/main/java/ru/bureau/settings/dto/SettingsExportFile.java))
4. **Mapper** — маппер между Entity и DTO ([`SettingMapper`](src/main/java/ru/bureau/settings/mapper/SettingMapper.java))
5. **Service** — бизнес-логика и оркестрация ([`SettingsServiceImpl`](src/main/java/ru/bureau/settings/service/SettingsServiceImpl.java))
6. **API** — публичный интерфейс сервиса ([`SettingsService`](src/main/java/ru/bureau/settings/api/SettingsService.java))
7. **Audit** — слой аудита ([`AuditService`](src/main/java/ru/bureau/settings/audit/AuditService.java), [`AuditWriter`](src/main/java/ru/bureau/settings/audit/AuditWriter.java))
8. **REST** — REST API точки ([`SettingsRestApi`](src/main/java/ru/bureau/settings/rest/SettingsRestApi.java))
9. **Servlet** — главная страница настроек ([`SettingsServletMainPage`](src/main/java/ru/bureau/settings/servlet/SettingsServletMainPage.java))
10. **Шаблон** — визуальный интерфейс настроек ([`bureu-settings.vm`](src/main/resources/templates/bureu-settings.vm))

### Принципы разделения ответственности

- **DAO** — отвечает **только** за взаимодействие с базой данных (Active Objects). Никакой бизнес-логики.
- **AuditService** — отвечает **только** за форматирование и отправку аудиторских событий (обёртка над `AuditWriter`).
- **Service** — содержит бизнес-логику (оркестрация), вызывает DAO и AuditService.

## ▶️ Использование

### Через админ-панель Jira
1. Перейдите в раздел **"Add-ons"** в админ-панели Jira
2. Найдите раздел **"Bureau Settings Plugin"**
3. Нажмите на кнопку **"Settings"** для перехода к интерфейсу настроек

### Программный доступ
```java
// Получение значения настройки
String value = settingsService.getSettingValue("setting_name");

// Создание новой настройки (с объяснением)
Setting newSetting = settingsService.createSetting("setting_name", "setting_value", "explanation");

// Обновление настройки
settingsService.updateSettings("setting_name", "new_value");

// Получение всех настроек
List<Setting> allSettings = settingsService.getAllSettings();

// Экспорт настроек
List<SettingsExportDto> exported = settingsService.exportSettings();

// Импорт настроек (полная замена)
settingsService.importSettings(exported);
```

## 🌐 REST API

Плагин предоставляет REST API для программного доступа к настройкам (базовый путь `/rest/bureau/1`):

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/settings` | Получить все настройки |
| `GET` | `/settings/{settingId}` | Получить настройку по ID |
| `GET` | `/settings/name/{key}` | Получить значение настройки по имени |
| `POST` | `/settings` | Создать новую настройку |
| `PUT` | `/settings/{settingId}` | Обновить настройку по ID |
| `DELETE` | `/settings/{settingId}` | Удалить настройку по ID |
| `GET` | `/settings/export` | Экспорт всех настроек в JSON-файл (только SYSTEM_ADMIN) |
| `POST` | `/settings/import` | Импорт настроек из JSON-файла (только SYSTEM_ADMIN) |

### Формат файла экспорта/импорта
```json
{
  "formatVersion": "1.0",
  "exportedAt": "2026-08-21T05:00:00Z",
  "settings": [
    {
      "name": "setting_name",
      "value": "setting_value",
      "explanation": "описание настройки"
    }
  ]
}
```

## 📦 Конфигурация

Настройки хранятся в базе данных через **Active Objects** и кэшируются для повышения производительности. Все настройки имеют уникальные имена (`@Unique`) и могут содержать произвольные строки значений и объяснений (`@StringLength(UNLIMITED)`).

## ⚙️ Зависимости

- Atlassian Jira (8.22.0)
- Active Objects
- Atlassian Cache
- Atlassian Audit API
- AUI (Atlassian UI)
- Spring (OSGi)
- Lombok
- Gson

## 📊 Логирование

Плагин предоставляет детальное логирование для отслеживания работы:
- Информационные сообщения о загрузке данных из кэша или БД
- Предупреждения при отсутствии настроек
- Ошибки при работе с базой данных
- Логирование операций экспорта/импорта

## 💻 Разработка

### Сборка
```bash
mvn clean package
```

### Запуск в тестовой среде
```bash
atlas-run
```

### Отладка
```bash
atlas-debug
```

## 📚 Поддержка

Для получения дополнительной информации обратитесь к документации Atlassian или к разработчику плагина.

---
*Создано с использованием Atlassian Plugin SDK*