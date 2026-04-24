# Bureau Settings Plugin

## 📋 Описание

Плагин Bureau Settings предназначен для управления настройками в системе Atlassian Jira. Он предоставляет возможность хранения, получения и управления различными параметрами конфигурации через удобный интерфейс администратора и программный API.

## ✨ Основные функции

### 🔧 Административный интерфейс
- Кнопка в админ панели Jira для быстрого доступа к настройкам
- Веб-интерфейс для просмотра и редактирования настроек
- Поддержка кэширования для повышения производительности

### ⚙️ Управление настройками
- Создание новых настроек
- Получение значений настроек по ключам
- Обновление существующих настроек
- Удаление настроек
- Получение всех настроек одновременно

### 🛠️ Технические особенности
- Использование Active Objects для хранения данных в базе данных
- Кэширование данных для повышения производительности
- Поддержка интернационализации
- REST API для программного доступа к настройкам

## 🏗️ Архитектура

### Основные компоненты

1. **Entity** - модель данных настройки (`ru.bureau.settings.entity.Setting`)
2. **DTO** - объекты передачи данных (`ru.bureau.settings.dto.SettingDto`)
3. **Mapper** - маппер между Entity и DTO (`ru.bureau.settings.mapper.SettingMapper`)
4. **Service** - бизнес-логика (`ru.bureau.settings.service.SettingsServiceImpl`)
5. **API** - публичный интерфейс сервиса (`ru.bureau.settings.api.SettingsService`)
6. **REST** - REST API точки (`ru.bureau.settings.rest.SettingsRestApi`)
7. **Servlet** - главная страница настроек (`ru.bureau.settings.servlet.SettingsServletMainPage`)
8. **Шаблон** - визуальный интерфейс настроек (`templates/bureu-settings.vm`)

## ▶️ Использование

### Через админ панель Jira
1. Перейдите в раздел **"Add-ons"** в админ панели Jira
2. Найдите раздел **"Bureau Settings"**
3. Нажмите на кнопку **"Bureau Settings"** для перехода к интерфейсу настроек

### Программный доступ
```java
// Получение значения настройки
String value = settingsService.getSettingValue("setting_name");

// Создание новой настройки
Setting newSetting = settingsService.createSetting("setting_name", "setting_value");

// Обновление настройки
settingsService.updateSettings("setting_name", "new_value");

// Получение всех настроек
List<Setting> allSettings = settingsService.getAllSettings();
```

## 🌐 REST API

Плагин предоставляет REST API для программного доступа к настройкам:

- `GET /rest/bureau/1/settings` - получить все настройки
- `GET /rest/bureau/1/settings/{settingId}` - получить настройку по ID
- `POST /rest/bureau/1/settings` - создать новую настройку
- `PUT /rest/bureau/1/settings/{settingId}` - обновить настройку по ID
- `DELETE /rest/bureau/1/settings/{settingId}` - удалить настройку по ID

## 📦 Конфигурация

Настройки хранятся в базе данных через Active Objects и кэшируются для повышения производительности. Все настройки имеют уникальные имена и могут содержать произвольные строки значений.

## ⚙️ Зависимости

- Atlassian Jira
- Active Objects
- Atlassian Cache
- AUI (Atlassian UI)

## 📊 Логирование

Плагин предоставляет детальное логирование для отслеживания работы:
- Информационные сообщения о загрузке данных из кэша или БД
- Предупреждения при отсутствии настроек
- Ошибки при работе с базой данных

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