package com.andrey.beautyplanner

expect object StoreOpener {
    fun open(url: String): Boolean
}