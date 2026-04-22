AJS.toInit((jQuery) => {
    console.log("=== JS ЗАГРУЖЕН ===");

    let tableElement = jQuery("#settings-table");
    if (tableElement.length === 0) return;

    let config = {
        el: tableElement,
        loadingMsg: "Загрузка...",
        noEntriesMsg: "Записей нет.",
        allowCreate: true,
        allowEdit: false,
        allowDelete: false,
        allowReorder: false,
        autoFocus: false,
        resources: {
            all: AJS.contextPath() + "/rest/bureau/1/settings",
            self: AJS.contextPath() + "/rest/bureau/1/settings"
        },
        columns: [
            { id: "name", header: "Название", allowEdit: true, emptyText: "-" },
            { id: "value", header: "Значение", allowEdit: true, emptyText: "-" }
        ]
    };

    if (typeof AJS.RestfulTable === "undefined") {
        console.error("RestfulTable не найден");
        return;
    }

    let tableInstance = new AJS.RestfulTable(config);
    let createRow = tableInstance.getCreateRow();

    // =========================================
    // ОШИБКА: Перехватываем на уровне RestfulTable
    // =========================================
    if (createRow) {
        createRow.bind(AJS.RestfulTable.Events.SUBMIT_STARTED, function() {
            createRow.showLoading();
        });
        createRow.bind(AJS.RestfulTable.Events.SUBMIT_FINISHED, function() {
            createRow.hideLoading();
        });

        // ВНИМАНИЕ: Здесь перехватываем ВНЕШНЮЮ ошибку (400/500)
        // RestfulTable не передает данные в VALIDATION_ERROR при нестандартном ответе
        // Поэтому используем jQuery.ajaxError, но с правильной проверкой
        jQuery(document).ajaxError(function(event, jqXHR, settings) {
                // Если это не наш запрос — пропускаем
                if (!settings || !settings.url || !settings.url.includes("/rest/bureau/1/settings")) {
                    return;
                }

                let errorMsg = "Ошибка";

                // Парсим ответ
                if (jqXHR.responseJSON && jqXHR.responseJSON.errorMessage) {
                    errorMsg = jqXHR.responseJSON.errorMessage;
                } else if (jqXHR.responseText) {
                    try {
                        errorMsg = JSON.parse(jqXHR.responseText).errorMessage || jqXHR.responseText;
                    } catch (e) {
                        errorMsg = jqXHR.responseText;
                    }
                }

                let $msgBox = jQuery("#bureau-settings-error-msg");

                // 1. Скрываем предыдущий таймер, если он был запущен (например, если пользователь нажал "ОК" в первом окне)
                if (window.errorTimer) {
                    clearTimeout(window.errorTimer);
                }

                // 2. Формируем HTML с ошибкой и крестиком
                $msgBox.html(
                    '<span class="error-text">' + errorMsg + '</span> ' +
                    '<a href="#" class="error-close" title="Закрыть">×</a>'
                );
                $msgBox.show();

                // 3. Таймер на 3 секунды
                window.errorTimer = setTimeout(function() {
                    $msgBox.fadeOut(300); // Плавно скрыть
                }, 3000); // 3000 мс = 3 секунды

                // 4. Клик по крестику закрывает сообщение сразу
                $msgBox.off('click'); // Убираем старые обработчики
                $msgBox.on('click', '.error-close', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    clearTimeout(window.errorTimer); // Очищаем таймер
                    $msgBox.fadeOut(300);
                });
            }
        );
    }

    console.log("Init complete");
});