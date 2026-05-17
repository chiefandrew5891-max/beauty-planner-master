package com.andrey.beautyplanner

actual object BackupFilePicker {

    /** Set in MainActivity.onCreate */
    var exportImpl: ((suggestedFileName: String, json: String) -> Unit)? = null

    /** Set in MainActivity.onCreate */
    var importImpl: ((onPicked: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null

    actual fun exportJson(suggestedFileName: String, json: String) {
        exportImpl?.invoke(suggestedFileName, json)
    }

    actual fun importJson(onPicked: (String) -> Unit, onError: (String) -> Unit) {
        importImpl?.invoke(onPicked, onError)
            ?: onError(Locales.t("backup_import_error_no_activity"))
    }
}