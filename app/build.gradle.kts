import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// CI numbers each build so a newer APK always installs over an older one.
val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

// Release signing comes from the environment (GitHub Secrets in CI). When anything is
// missing, release builds are left unsigned so PRs, forks and local builds still pass.
fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
val releaseKeystore = env("RELEASE_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.isFile }
val releaseStorePassword = env("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = env("RELEASE_KEY_ALIAS")
val releaseKeyPassword = env("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseKeystore != null && releaseStorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.polisheasy.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.polisheasy.app"
        minSdk = 26
        targetSdk = 36
        versionCode = ciRunNumber ?: 1
        versionName = "0.1.0" + (ciRunNumber?.let { ".$it" } ?: "")
    }

    // A committed debug keystore gives every build, on any machine, the same signature,
    // so each new APK installs as an update over the previous one.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        // The suffix lets the debug build and the Play build, which are signed with
        // different keys, sit side by side on one phone.
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The Authored Content lives at the repository root and is packaged as app assets.
    sourceSets["main"].assets.srcDir(rootProject.file("content"))
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.media3.exoplayer)
}
