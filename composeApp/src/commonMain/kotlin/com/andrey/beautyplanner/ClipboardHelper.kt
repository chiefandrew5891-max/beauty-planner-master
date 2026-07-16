package com.andrey.beautyplanner

expect object ClipboardHelper {
    fun copyText(label: String, text: String): Boolean
}