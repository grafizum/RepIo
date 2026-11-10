import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.baseline.profile)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
}

// Configuration for Play Store release naming
val appName = "RepIo"

// version.properties is the single source of truth for the app version.
// VERSION_NAME is bumped deliberately via the root `bumpVersion` task (see AGENTS.md).
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}
val appVersion = versionProps.getProperty("VERSION_NAME") ?: "1.2.2"
var playStoreVersion = (versionProps.getProperty("VERSION_CODE") ?: "9").toInt()

// Automatic increment logic for signed builds: building an actual release
// bundle/APK claims a fresh version code, so each Play upload stays unique
// without touching VERSION_NAME. Deliberately NOT triggered by intermediate
// tasks like compileReleaseKotlin (verification compiles must not burn codes).
//
// `--dry-run` MUST NOT burn a code. It builds nothing at all - it prints the task
// graph and stops - so charging it for a release is a silent mutation of a
// TRACKED file (version.properties is committed) for work that never happened.
// That was measured: two `./gradlew assembleRelease --dry-run` calls took
// VERSION_CODE from 48 to 50 with nothing built.
//
// This is a configuration-time side effect, so it runs once per invocation and a
// second build in the same session legitimately claims another code. That is the
// intended "one code per signed build" behaviour, not the bug above.
val releaseBuildRequested = gradle.startParameter.taskNames.any {
    it.contains("bundleRelease", true) || it.contains("assembleRelease", true)
}
if (releaseBuildRequested && !gradle.startParameter.isDryRun) {
    playStoreVersion++
    versionProps.setProperty("VERSION_CODE", playStoreVersion.toString())
    versionPropsFile.outputStream().use { versionProps.store(it, null) }
}

android {
    namespace = "com.grafizum.repio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grafizum.repio"
        // 24 is Google Play's upload floor since ~Sep 2026: "Play automatic
        // protection requires a minimum SDK version of 24 or higher" rejects
        // a bundle at 23 outright (the dependency set only needs 23, so Play
        // is the binding constraint here, not the compile floor).
        minSdk = 24
        targetSdk = 36
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

        val keystorePath = System.getenv("KEYSTORE_PATH") ?: env.getProperty("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        val relKeystoreFile = file(keystorePath)
        if (relKeystoreFile.exists()) {
            create("release") {
                storeFile = relKeystoreFile
                storePassword = System.getenv("STORE_PASSWORD") ?: env.getProperty("STORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("KEY_ALIAS") ?: env.getProperty("KEY_ALIAS") ?: "upload_key"
                keyPassword = System.getenv("KEY_PASSWORD") ?: env.getProperty("KEY_PASSWORD") ?: "android"
            }
        } else {
            // No `release` signing config was created above, so `buildTypes.release`
            // would otherwise fall back to the DEBUG key below - and a debug-signed
            // bundle is not merely "a different signature", it is one Play will
            // reject, so the failure surfaces at upload time with nothing pointing
            // back here. The upload key lives outside the repo (see .env
            // KEYSTORE_PATH), which makes "the path is wrong / the file is gone"
            // a realistic cause rather than a hypothetical one.
            logger.warn(
                "No upload keystore at '$keystorePath' (KEYSTORE_PATH from the " +
                    "environment or .env), so NO 'release' signing config exists. " +
                    "Debug builds and unit tests are unaffected."
            )
        }
    }

    buildTypes {
        release {
            isCrunchPngs = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val relConfig = signingConfigs.findByName("release")
            if (relConfig != null) {
                signingConfig = relConfig
            } else if (releaseBuildRequested) {
                // Only when a release artifact was actually asked for. This block is
                // evaluated for EVERY build - including `testDebugUnitTest` and
                // `assembleDebug` - so throwing unconditionally would break the
                // tasks that legitimately do not need an upload key.
                throw GradleException(
                    "A release build was requested but no upload keystore was found, " +
                        "so there is no 'release' signing config. Falling back to the " +
                        "debug key would produce a bundle Play cannot accept.\n" +
                        "Point KEYSTORE_PATH (in .env or the environment) at the upload " +
                        "key. It is expected at ~/.grafizum/keys/repio-upload.jks."
                )
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")

            // A DEV BUILD MUST BE ABLE TO COEXIST WITH AN INSTALLED RELEASE.
            //
            // Android refuses to replace an installed package whose signer differs
            // from the incoming APK's, and that is not a key mismatch this project
            // can fix: a Play-installed copy carries Play's app-signing certificate,
            // which no local keystore matches. The failure is
            // INSTALL_FAILED_UPDATE_INCOMPATIBLE, and the only ways out are
            // uninstalling (which deletes the user's workout data) or installing
            // under a different application id. This is the second one.
            //
            // Everything the app derives from its own package name rides on
            // `context.packageName` - the launcher icon aliases (IconSwitchHelper),
            // the app-details deep links (AppUpdateChecker) and resource lookup
            // (Resources.kt) - so the id change is transparent to all of them.
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            // Required for Robolectric tests to access the app's merged resources and manifest.
            isIncludeAndroidResources = true
        }
    }
}

// Automatically rename the .aab file
base {
  archivesName.set("$appName-v$appVersion-v$playStoreVersion")
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    // Build-time only: read by the root `bumpVersion` task to report to the Grafizum
    // dashboard. Ignored here so the token is never baked into BuildConfig / the APK.
    // Only add these to .env, never to .env.example: an active key with an empty
    // value makes the plugin emit invalid Java (`= ;`) and breaks compilation.
    ignoreList.add("GRAFIZUM_DASHBOARD_URL")
    ignoreList.add("GRAFIZUM_DASHBOARD_TOKEN")
    // Optional override for the dashboard project the bump is reported against.
    ignoreList.add("GRAFIZUM_PROJECT_ID")
    // The SIGNING secrets are read by `signingConfigs` above to sign the release,
    // but the secrets plugin does not know that: `propertiesFileName = ".env"`
    // turns EVERY listed key into a BuildConfig field, so the upload key's
    // passwords were being generated into BuildConfig.java for release too.
    //
    // Measured before this change: KEY_PASSWORD, STORE_PASSWORD and KEY_ALIAS were
    // all present in the generated release BuildConfig. They did NOT reach any
    // shipped artifact - all 20 historical AABs and the release dex are clean -
    // because R8 drops the constants as unreferenced dead code. That is a
    // side effect of shrinking, not a guarantee: nothing here prevents a future
    // reflective read, a -keep, or `isMinifyEnabled = false` from shipping the
    // upload key's passwords to every Play install. Ignore them at the source
    // instead of relying on the stripper to save us.
    //
    // Like the GRAFIZUM_* keys above, these belong in .env ONLY and must never be
    // added to .env.example (an active key with an empty value emits invalid Java).
    ignoreList.add("STORE_PASSWORD")
    ignoreList.add("KEY_PASSWORD")
    ignoreList.add("KEY_ALIAS")
    // The keystore's PATH is the same class of value: it is read by
    // `signingConfigs` above, and the key itself now lives outside the repo at
    // ~/.grafizum/keys/repio-upload.jks. It is a filesystem location to this
    // machine, not a build input, so it must not become a BuildConfig field
    // either - that would publish the developer's home directory layout to every
    // Play install.
    ignoreList.add("KEYSTORE_PATH")

    // The dashboard SYNC key, used only by the sync CLI when mirroring TODO.md
    // to the command center. It lives in .env for the same reason the token
    // does, and for the same hazard if it is forgotten here: `propertiesFileName
    // = ".env"` turns every listed key into a BuildConfig field, so an ACTIVE
    // value that is not ignored is compiled into the APK and ships. Never add
    // it to .env.example either - the plugin reads that as the default set, and
    // an empty active value emits invalid Java (`= ;`) and breaks compilation.
    ignoreList.add("GRAFIZUM_SYNC_KEY")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Installs the baseline profile baked into the AAB. Without this the profile is generated at
    // build time and then silently dropped on the device: ART keeps interpreting the app's hot
    // methods (day wheel, exercise list, the Compose recompose paths) instead of AOT-compiling
    // them, which is the single largest startup and scroll win available for a Play release.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)

    // Google Play In-App Updates
    implementation(libs.app.update)
    implementation(libs.kotlinx.coroutines.play.services)
    
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
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local unit tests (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    // Instrumented tests (need a connected device or emulator). The runner is named by
    // testInstrumentationRunner above, so it has to be on the androidTest classpath too.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)

    // The baseline profile is GENERATED on a device and PACKAGED into the release bundle, so the
    // plugin needs the same androidTest classpath the generator runs against.
    baselineProfile(project(":composeApp"))
}

// ------------------------------------------------------------------ baseline profile
//
// WHY THIS IS THE BIGGEST PLAY RELEASE LEVER IN THIS FILE. Without a profile, ART interprets the
// app on first runs and compiles only what it happens to execute; the Compose hot paths this app
// lives on - the day wheel, the exercise list, the level card - are reached through reflection-
// shaped call graphs that a JIT profile alone warms slowly. A bundled baseline profile makes the
// installer AOT-compile exactly those methods before first launch.
//
// WHAT IT COSTS TO GENERATE: `./gradlew :composeApp:generateBaselineProfile` with a connected
// device or emulator. That task is deliberately NOT wired into assembleRelease - generation needs
// hardware and takes minutes, and a release build must stay reproducible from source alone. Until
// it has been run once, the plugin packages no profile and the build still succeeds; the profile
// simply stays absent. Run it on the owner's phone (or any API 24+ device) before the first real
// Play upload, then commit composeApp/src/main/baseline-prof.txt.
baselineProfile {
    // Left at the plugin defaults deliberately. The generate task instruments the app's own
    // startup and main-activity paths, which is what a first Play launch actually exercises;
    // a custom target package or a narrowed instrumentation would only shrink the profile.
}
