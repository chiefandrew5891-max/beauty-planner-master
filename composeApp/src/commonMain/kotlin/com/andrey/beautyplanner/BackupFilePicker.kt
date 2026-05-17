package com.andrey.beautyplanner

expect object BackupFilePicker {
    fun exportJson(suggestedFileName: String, json: String)
    fun importJson(onPicked: (String) -> Unit, onError: (String) -> Unit)
}