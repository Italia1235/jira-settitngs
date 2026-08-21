AJS.toInit((jQuery) => {
    jQuery("<style>")
        .text(
            `
            /* Принудительно задаем стили для всех инпутов в нашей таблице, чтобы они совпадали с родными */
            #settings-table input {
                height: 2.14285714em;          /* Высота как в AUI */
                padding: 3px 4px;              /* Отступы как в AUI */
                width: 100%;                   /* Растягиваем на всю ширину */
                box-sizing: border-box;        /* Учитываем паддинги в ширине */
                border: 2px solid var(--aui-form-field-border-color);
                border-radius: 3.01px;
                background-color: var(--aui-form-field-default-bg-color);
                color: var(--aui-form-field-default-text-color);
                font-size: inherit;
                font-family: inherit;
            }

            /* Для полей readonly (редактирование) делаем фон чуть серым */
            #settings-table input[readonly] {
                background-color: var(--aui-input-disabled-bg-color, #f4f5f7);
                border-color: var(--aui-input-disabled-border-color, #dfe3e6);
                cursor: not-allowed;
            }
            `
        )
        .appendTo("head");
    console.log("=== JS ЗАГРУЖЕН ===");

    let tableElement = jQuery("#settings-table");
    if (tableElement.length === 0) return;

    let config = {
        el: tableElement,
        loadingMsg: "Загрузка...",
        noEntriesMsg: "Записей нет.",
        allowCreate: true,
        allowDelete: true,
        deleteConfirmation: true,
        allowReorder: false,
        autoFocus: false,
        allowEdit: true,
        resources: {
            all: AJS.contextPath() + "/rest/bureau/1/settings",
            self: AJS.contextPath() + "/rest/bureau/1/settings"
        },
        columns: [
            {
                id: "name",
                header: "Название",
                allowEdit: false,  // <-- Ключевое: отключаем редактирование колонки
                emptyText: "-",
                createView: Backbone.View.extend({
                    render: function() {
                        var input = jQuery('<input type="text" name="name" class="aui-input aui-input-full">');
                        if (this.model) input.val(this.model.get("name"));
                        return input;
                    }
                })
            },
            {
                id: "value",
                header: "Значение",
                allowEdit: true,   // <-- Это значение можно редактировать
                emptyText: "-"
            },
            {
                id: "explanation",
                header: "Описание",
                allowEdit: true,   // <-- Это значение можно редактировать
                emptyText: "-"
            }
        ]
    };
    AJS.$("#export-settings-btn").on("click", function () {
        // Скачиваем файл через REST-эндпоинт
        var exportUrl = AJS.contextPath() + "/rest/bureau/1/settings/export";
        window.location.href = exportUrl;
    });

    // =========================================
    // ИМПОРТ: Загрузка настроек из JSON-файла
    // =========================================

    // Кнопка "Выбрать файл" открывает скрытый file input
    AJS.$("#import-settings-choose-btn").on("click", function () {
        AJS.$("#import-settings-file").trigger("click");
    });

    // При выборе файла показываем имя и активируем кнопку "Применить"
    AJS.$("#import-settings-file").on("change", function () {
        var file = this.files[0];
        if (file) {
            AJS.$("#import-settings-file-name").text(file.name);
            AJS.$("#import-settings-apply-btn").prop("disabled", false);
        } else {
            AJS.$("#import-settings-file-name").text("");
            AJS.$("#import-settings-apply-btn").prop("disabled", true);
        }
    });

    // Кнопка "Применить" открывает диалог подтверждения (с оверлеем)
    AJS.$("#import-settings-apply-btn").on("click", function () {
        AJS.$("#import-overlay").show();
        AJS.$("#import-confirm-dialog").show();
    });

    // Кнопка "Отмена" в диалоге
    AJS.$("#import-confirm-cancel-btn").on("click", function () {
        AJS.$("#import-confirm-dialog").hide();
        AJS.$("#import-overlay").hide();
    });

    // Кнопка "Применить" в диалоге — отправка файла на сервер
    AJS.$("#import-confirm-btn").on("click", function () {
        var file = AJS.$("#import-settings-file")[0].files[0];
        if (!file) {
            AJS.$("#import-confirm-dialog").hide();
            AJS.$("#import-overlay").hide();
            return;
        }

        var reader = new FileReader();
        reader.onload = function (e) {
            var json;
            try {
                json = JSON.parse(e.target.result);
            } catch (parseError) {
                AJS.$("#import-confirm-dialog").hide();
                AJS.$("#import-overlay").hide();
                AJS.flag({ type: "error", title: "Ошибка", body: "Некорректный JSON-файл." });
                return;
            }

            AJS.$.ajax({
                url: AJS.contextPath() + "/rest/bureau/1/settings/import",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify(json),
                success: function () {
                    AJS.$("#import-confirm-dialog").hide();
                    AJS.$("#import-overlay").hide();
                    AJS.flag({ type: "success", title: "Настройки успешно загружены" });
                    // Перезагружаем страницу, чтобы обновить таблицу
                    setTimeout(function () { location.reload(); }, 1000);
                },
                error: function (xhr) {
                    AJS.$("#import-confirm-dialog").hide();
                    AJS.$("#import-overlay").hide();
                    var msg = "Ошибка загрузки настроек.";
                    if (xhr.responseJSON && xhr.responseJSON.errorMessage) {
                        msg = xhr.responseJSON.errorMessage;
                    }
                    AJS.flag({ type: "error", title: "Ошибка", body: msg });
                }
            });
        };
        reader.readAsText(file);
    });
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