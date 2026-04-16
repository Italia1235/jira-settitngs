AJS.toInit((jQuery) => {
    console.log("Bureau Plugin: Start Init");

    // Инициализация только если таблица есть в DOM
    let tableElement = jQuery("#settings-table");
    if (tableElement.length === 0) {
        console.warn("Таблица #settings-table не найдена в DOM");
        return;
    }

    // Настройки таблицы (только чтение, т.к. нет методов на сервере)
    let config = {
        el: tableElement,
        allowReorder: false,
        allowCreate: false,
        allowEdit: false,
        allowDelete: false,
        autoFocus: false,

        resources: {
            // Обязательно используем AJS.contextPath(), чтобы не было 404
            all: AJS.contextPath() + "/rest/bureau/1/settings",
            self: AJS.contextPath() + "/rest/bureau/1/settings"
        },

        columns: [
            {
                id: "id",
                header: "ID"
            },
            {
                id: "name",
                header: "Название"
            },
            {
                id: "value",
                header: "Значение"
            }
        ]
    };

    // Проверяем наличие класса перед инициализацией, чтобы получить понятную ошибку
    if (typeof AJS.RestfulTable === "undefined") {
        console.error("AJS.RestfulTable не доступен. Проверьте atlassian-plugin.xml");
        return;
    }

    // Инициализация
    try {
        window.bureauSettingsTable = new AJS.RestfulTable(config);
        console.log("Bureau Plugin: RestfulTable инициализирован успешно");
    } catch (error) {
        console.error("Ошибка RestfulTable:", error);
    }
});