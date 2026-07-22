import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.0.0"
    alias(libs.plugins.googleServices)
}

kotlin {
    // Настройка для Android-части (нужна для теста)
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Настройка для iPhone (твоя основная цель)
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // ГЛОБАЛЬНОЕ ИСПРАВЛЕНИЕ: Разрешение экспериментальных API для всех sourceSets
    sourceSets.all {
        languageSettings.optIn("androidx.compose.foundation.ExperimentalFoundationApi")
    }

    sourceSets {
        val commonMain by getting {
            //resources.srcDirs("src/commonMain/composeResources")
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.6.11")
                implementation("org.jetbrains.compose.foundation:foundation:1.6.11")
                implementation("org.jetbrains.compose.material:material:1.6.11")
                implementation("org.jetbrains.compose.ui:ui:1.6.11")
                implementation("org.jetbrains.compose.components:components-ui-tooling-preview:1.6.11")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.6.11")

                // ГЛАВНОЕ: Кроссплатформенные ресурсы для iOS и Android
                implementation("org.jetbrains.compose.components:components-resources:1.6.11")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.6.11")
                implementation("androidx.browser:browser:1.8.0")

                // ВАЖНО: фикс совместимости Kotlin metadata.
                // Google требует Billing >= 8.0.0, ставим 8.0.0 (совместимо с текущим стеком).
                // Не используем libs.googlePlayBilling, т.к. он может тянуть 9.0.0.
                implementation("com.android.billingclient:billing-ktx:8.0.0")

                implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
                implementation("com.google.firebase:firebase-auth-ktx:23.2.0")
                implementation("com.google.firebase:firebase-functions-ktx:21.1.0")
                implementation("com.google.firebase:firebase-firestore:25.1.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

                implementation("androidx.credentials:credentials:1.3.0")
                implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
                implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
                implementation("com.google.android.gms:play-services-auth:21.2.0")
            }
        }

        // iosMain создается автоматически, если нужно добавить специфичные для iPhone штуки
    }
}

// ЭТОТ БЛОК НЕОБХОДИМ ДЛЯ ПРОВЕРКИ НА АНДРОИДЕ
android {
    namespace = "com.andrey.beautyplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.andrey.beautyplanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// ГЕНЕРАЦИЯ ОБЩИХ РЕСУРСОВ ДЛЯ ОБЕИХ ПЛАТФОРМ
compose.resources {
    publicResClass = true
    packageOfResClass = "com.andrey.beautyplanner.generated.resources"
    generateResClass = always
}