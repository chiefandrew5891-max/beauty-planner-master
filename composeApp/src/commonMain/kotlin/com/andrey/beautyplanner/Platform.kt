package com.andrey.beautyplanner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform