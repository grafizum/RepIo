import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
}

// Configuration for Play Store release naming
val appName = "RepIo"
val appVersion = "1.1.1"

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

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    iosX64 {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
}

compose.resources {
    packageOfResClass = "com.grafizum.repio.generated.resources"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.room.runtime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.room.ktx)
            
            // Android Specific Lifecycle & Navigation
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
        }
        iosMain.dependencies {
        }
    }
}

android {
    namespace = "com.grafizum.repio"
    compileSdk = 37

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        java.srcDirs("src/androidMain/java")
        res.srcDirs("src/androidMain/res")
    }

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
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
    
    // Explicitly add Firebase dependencies for Android with the BOM
    val firebaseBom = platform(libs.firebase.bom)
    add("androidMainImplementation", firebaseBom)
    add("androidMainImplementation", libs.firebase.ai)
    add("androidMainImplementation", libs.firebase.firestore)
    add("androidMainImplementation", libs.firebase.appcheck.recaptcha)
    add("androidMainImplementation", libs.firebase.appcheck.playintegrity)
    add("androidMainImplementation", libs.firebase.appcheck.debug)
    add("androidMainImplementation", libs.firebase.auth)
}
