AJS.toInit((jQuery) => {
    jQuery("<style>")
        .text(
            `
            #settings-table input {
                height: 2.14285714em;
                padding: 3px 4px;
                width: 100%;
                box-sizing: border-box;
                border: 2px solid var(--aui-form-field-border-color);
                border-radius: 3.01px;
                background-color: var(--aui-form-field-default-bg-color);
                color: var(--aui-form-field-default-text-color);
                font-size: inherit;
                font-family: inherit;
            }

            #settings-table input[readonly] {
                background-color: var(--aui-input-disabled-bg-color, #f4f5f7);
                border-color: var(--aui-input-disabled-border-color, #dfe3e6);
                cursor: not-allowed;
            }

            #settings-table th.encrypted,
            #settings-table td.encrypted {
                width: 60px;
                min-width: 60px;
                max-width: 60px;
                text-align: center !important;
                vertical-align: middle !important;
                padding-left: 4px !important;
                padding-right: 4px !important;
            }

            #settings-table td.encrypted > * {
                display: inline-flex;
                align-items: center;
                justify-content: center;
            }

            #settings-table td.encrypted input[type="checkbox"] {
                display: block;
                width: auto;
                margin: 0 auto;
            }

            #settings-table .encrypted-cell-ctrl {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 24px;
                height: 24px;
                line-height: 0;
            }

            #settings-table .encrypted-cell-ctrl.encrypted-locked {
                color: #16a34a;
            }

            #settings-table .encrypted-cell-ctrl.encrypted-unlocked {
                color: #c1c7d0;
            }

            #settings-table .encrypted-cell-ctrl svg {
                display: block;
            }
            `
        )
        .appendTo("head");

    jQuery.ajaxPrefilter(function (options) {
        if (options.url && options.url.indexOf("/rest/bureau/1/settings") !== -1
            && typeof options.data === "string") {
            try {
                var payload = JSON.parse(options.data);
                if (payload && typeof payload === "object") {
                    if ("__encrypted_changed" in payload) {
                        delete payload.__encrypted_changed;
                    }
                    if (payload.id != null && payload.id in encryptedState) {
                        payload.encrypted = encryptedState[payload.id];
                    }
                    if ("encrypted" in payload) {
                        var v = payload.encrypted;
                        payload.encrypted = (v === true || v === "on" || v === "true" || v === "1" || v === "yes");
                    }
                    options.data = JSON.stringify(payload);
                }
            } catch (e) {
            }
        }
    });

    let tableElement = jQuery("#settings-table");
    if (tableElement.length === 0) return;

    var encryptedState = {};

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
        model: AJS.RestfulTable.EntryModel.extend({
            toJSON: function() {
                var json = AJS.RestfulTable.EntryModel.prototype.toJSON.call(this);
                if ("__encrypted_changed" in json) {
                    delete json.__encrypted_changed;
                }
                var id = this.get("id");
                if (id != null && id in encryptedState) {
                    json.encrypted = encryptedState[id];
                }
                if ("encrypted" in json) {
                    var v = json.encrypted;
                    json.encrypted = (v === true || v === "on" || v === "true" || v === "1" || v === "yes");
                }
                return json;
            },
            sync: function(method, model, options) {
                return AJS.RestfulTable.EntryModel.prototype.sync.call(this, method, model, options);
            }
        }),
        columns: [
            {
                id: "name",
                header: "Название",
                allowEdit: false,
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
                allowEdit: true,
                emptyText: "-"
            },
            {
                id: "explanation",
                header: "Описание",
                allowEdit: true,
                emptyText: "-"
            },
            {
                id: "encrypted",
                header: "Секрет",
                allowEdit: true,
                emptyText: "-",
                readView: Backbone.View.extend({
                    render: function() {
                        var isEncrypted = this.model && !!this.model.get("encrypted");
                        var $ctrl = jQuery('<span class="encrypted-cell-ctrl" title="' + (isEncrypted ? "Зашифровано" : "Не зашифровано") + '"></span>');
                        if (isEncrypted) {
                            $ctrl.addClass("encrypted-locked");
                            $ctrl.html(
                                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
                                '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>' +
                                '<path d="M7 11V7a5 5 0 0 1 10 0v4"></path>' +
                                '</svg>'
                            );
                        } else {
                            $ctrl.addClass("encrypted-unlocked");
                            $ctrl.html(
                                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">' +
                                '<circle cx="12" cy="12" r="6"></circle>' +
                                '</svg>'
                            );
                        }
                        return $ctrl;
                    }
                }),
                createView: Backbone.View.extend({
                    render: function() {
                        var $wrap = jQuery('<div>');
                        var $checkbox = jQuery('<input type="checkbox">');
                        var $hidden = jQuery('<input type="hidden" name="encrypted" value="false">');
                        var $marker = jQuery('<input type="hidden" name="__encrypted_changed" value="0">');
                        var isEncrypted = this.model && !!this.model.get("encrypted");
                        if (isEncrypted) {
                            $checkbox.prop("checked", true);
                            $hidden.val("true");
                        }
                        var self = this;
                        $checkbox.on("change", function() {
                            var checked = jQuery(this).is(":checked");
                            $hidden.val(checked ? "true" : "false");
                            $marker.val($marker.val() === "0" ? "1" : "0");
                            if (self.model) {
                                self.model.set("encrypted", checked);
                                var id = self.model.get("id");
                                if (id != null) {
                                    encryptedState[id] = checked;
                                }
                            }
                        });
                        $wrap.append($checkbox).append($hidden).append($marker);
                        return $wrap;
                    }
                }),
                editView: Backbone.View.extend({
                    render: function() {
                        var $wrap = jQuery('<div>');
                        var $checkbox = jQuery('<input type="checkbox">');
                        var $hidden = jQuery('<input type="hidden" name="encrypted" value="false">');
                        var $marker = jQuery('<input type="hidden" name="__encrypted_changed" value="0">');
                        var isEncrypted = this.model && !!this.model.get("encrypted");
                        if (isEncrypted) {
                            $checkbox.prop("checked", true);
                            $hidden.val("true");
                        }
                        var self = this;
                        $checkbox.on("change", function() {
                            var checked = jQuery(this).is(":checked");
                            $hidden.val(checked ? "true" : "false");
                            $marker.val($marker.val() === "0" ? "1" : "0");
                            if (self.model) {
                                self.model.set("encrypted", checked);
                                var id = self.model.get("id");
                                if (id != null) {
                                    encryptedState[id] = checked;
                                }
                            }
                        });
                        $wrap.append($checkbox).append($hidden).append($marker);
                        return $wrap;
                    }
                })
            }
        ]
    };
    AJS.$("#export-settings-btn").on("click", function () {
        var exportUrl = AJS.contextPath() + "/rest/bureau/1/settings/export";
        window.location.href = exportUrl;
    });

    AJS.$("#import-settings-choose-btn").on("click", function () {
        AJS.$("#import-settings-file").trigger("click");
    });

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

    AJS.$("#import-settings-apply-btn").on("click", function () {
        AJS.$("#import-overlay").show();
        AJS.$("#import-confirm-dialog").show();
    });

    AJS.$("#import-confirm-cancel-btn").on("click", function () {
        AJS.$("#import-confirm-dialog").hide();
        AJS.$("#import-overlay").hide();
    });

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
        return;
    }

    let tableInstance = new AJS.RestfulTable(config);
    let createRow = tableInstance.getCreateRow();

    if (createRow) {
        createRow.bind(AJS.RestfulTable.Events.SUBMIT_STARTED, function() {
            createRow.showLoading();
        });
        createRow.bind(AJS.RestfulTable.Events.SUBMIT_FINISHED, function() {
            createRow.hideLoading();
        });

        jQuery(document).ajaxError(function(event, jqXHR, settings) {
                if (!settings || !settings.url || !settings.url.includes("/rest/bureau/1/settings")) {
                    return;
                }

                let errorMsg = "Ошибка";

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

                if (window.errorTimer) {
                    clearTimeout(window.errorTimer);
                }

                $msgBox.html(
                    '<span class="error-text">' + errorMsg + '</span> ' +
                    '<a href="#" class="error-close" title="Закрыть">×</a>'
                );
                $msgBox.show();

                window.errorTimer = setTimeout(function() {
                    $msgBox.fadeOut(300);
                }, 3000);

                $msgBox.off('click');
                $msgBox.on('click', '.error-close', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    clearTimeout(window.errorTimer);
                    $msgBox.fadeOut(300);
                });
            }
        );
    }
});