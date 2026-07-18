package com.andrey.beautyplanner

interface Platform {
    val name: String
    val backendPlatform: String
}

expect fun getPlatform(): Platform
