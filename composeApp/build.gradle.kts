import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.20"
    alias(libs.plugins.googleServices)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets.all {
        languageSettings.optIn("androidx.compose.foundation.ExperimentalFoundationApi")
    }

    sourceSets {
        val commonMain by getting {
            resources.srcDirs("src/commonMain/composeResources")

            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.ui.tooling.preview)
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation(libs.compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.browser)

                implementation(libs.googlePlayBilling)

                implementation(libs.firebase.auth.ktx)
                implementation(libs.firebase.functions.ktx)
                implementation(libs.firebase.firestore)
                implementation(libs.kotlinx.coroutines.play.services)

                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)
                implementation(libs.play.services.auth)
            }
        }
    }
}

android {
    namespace = "com.andrey.beautyplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.andrey.beautyplanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "1.4.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.andrey.beautyplanner.generated.resources"
    generateResClass = auto
}