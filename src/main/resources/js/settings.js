AJS.toInit(function () {
    // Кнопка выгрузки настроек в JSON
    AJS.$("#export-settings-btn").on("click", function () {
        // Скачиваем файл через REST-эндпоинт
        var exportUrl = AJS.contextPath() + "/rest/bureau/1/settings/export";
        window.location.href = exportUrl;
    });
});