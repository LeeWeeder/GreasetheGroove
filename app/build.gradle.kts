import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.isFile) {
    versionProps.load(FileInputStream(versionPropsFile))
} else {
    // Provide default values if the file doesn't exist yet
    versionProps["VERSION_MAJOR"] = "0"
    versionProps["VERSION_MINOR"] = "0"
    versionProps["VERSION_PATCH"] = "1"
}

var major = versionProps["VERSION_MAJOR"].toString().toInt()
var minor = versionProps["VERSION_MINOR"].toString().toInt()
var patch = versionProps["VERSION_PATCH"].toString().toInt()

if (project.hasProperty("bump")) {
    when (project.property("bump")) {
        "major" -> {
            major++
            minor = 0
            patch = 0
        }
        "minor" -> {
            minor++
            patch = 0
        }
        "patch" -> {
            patch++
        }
    }

    // 4. Update the properties object with the new values
    versionProps["VERSION_MAJOR"] = major.toString()
    versionProps["VERSION_MINOR"] = minor.toString()
    versionProps["VERSION_PATCH"] = patch.toString()

    // 5. Write the new version back to the file
    versionProps.store(versionPropsFile.writer(), "Version properties updated by Gradle")
}

val keystorePropsFile = rootProject.file("key.properties")
val keystoreProps = Properties()
if (keystorePropsFile.isFile) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}


android {
    namespace = "com.leeweeder.greasethegroove"
    compileSdk = 36

    signingConfigs {
        create("release") {
            // Only configure signing if the properties file exists
            if (keystorePropsFile.isFile) {
                storeFile = file(keystoreProps["storeFile"].toString())
                storePassword = keystoreProps["storePassword"].toString()
                keyAlias = keystoreProps["keyAlias"].toString()
                keyPassword = keystoreProps["keyPassword"].toString()
            } else {
                // This else block prevents build failures on machines that don't
                // have the keystore (e.g., a CI server doing a debug build).
                println("Signing config not found. Using debug signing for release builds.")
                // Fallback to the debug keystore for local "release" builds if needed.
                // This is optional but good practice.
                getByName("debug").let {
                    storeFile = it.storeFile
                    storePassword = it.storePassword
                    keyAlias = it.keyAlias
                    keyPassword = it.keyPassword
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.leeweeder.greasethegroove"
        minSdk = 35
        targetSdk = 36
        versionCode = (major * 1_000_000) + (minor * 1_000) + patch
        versionName = "$major.$minor.$patch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose (using the BOM for version alignment)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Koin (using the BOM for version alignment)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // DataStore & WorkManager for background tasks
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.accompanist.permissions)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}