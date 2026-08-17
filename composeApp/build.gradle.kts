import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
}

// Configuration for Play Store release naming
val appName = "RepIo"
val appVersion = "1.2.2"

// Load Version Code from version.properties
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}
var playStoreVersion = (versionProps.getProperty("VERSION_CODE") ?: "9").toInt()

// Automatic increment logic for signed builds
if (gradle.startParameter.taskNames.any { it.contains("Release") }) {
    playStoreVersion++
    versionProps.setProperty("VERSION_CODE", playStoreVersion.toString())
    versionPropsFile.outputStream().use { versionProps.store(it, null) }
}

android {
    namespace = "com.grafizum.repio"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.grafizum.repio"
        minSdk = 23
        targetSdk = 37
        versionCode = playStoreVersion
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        val ksFile = file("${rootDir}/debug.keystore")
        if (ksFile.exists()) {
            getByName("debug") {
                storeFile = ksFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }

        // Load signing properties from .env if available
        val envFile = rootProject.file(".env")
        val env = Properties()
        if (envFile.exists()) {
            FileInputStream(envFile).use { env.load(it) }
        }

        val keystorePath = (System.getenv("KEYSTORE_PATH") ?: env.getProperty("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks") as String
        val relKeystoreFile = file(keystorePath)
        if (relKeystoreFile.exists()) {
            create("release") {
                storeFile = relKeystoreFile
                storePassword = System.getenv("STORE_PASSWORD") ?: env.getProperty("STORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("KEY_ALIAS") ?: env.getProperty("KEY_ALIAS") ?: "upload_key"
                keyPassword = System.getenv("KEY_PASSWORD") ?: env.getProperty("KEY_PASSWORD") ?: "android"
            }
        }
    }

    buildTypes {
        release {
            isCrunchPngs = true
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val relConfig = signingConfigs.findByName("release")
            if (relConfig != null) {
                signingConfig = relConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// Automatically rename the .aab file
base {
  archivesName.set("$appName-v$appVersion-v$playStoreVersion")
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    
    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    "ksp"(libs.androidx.room.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Moshi
    "ksp"(libs.moshi.kotlin.codegen)
    
    // Firebase
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.appcheck.recaptcha)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.auth)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
