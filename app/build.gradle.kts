plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.foadsf.kidlauncher"
    compileSdk = 35
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.github.foadsf.kidlauncher"
        // Written for a Galaxy S4 mini VE on stock KitKat 4.4.4. Do not raise this.
        minSdk = 19
        targetSdk = 35
        versionCode = 7
        versionName = "0.7.0"

        // The child's name, for the greeting ("Hi Mia!") and the app label ("Mia's Home"):
        //   ./gradlew assembleDebug -PkidName=Mia
        // Left out, the greeting is "Hi there!" and the label "Kid Launcher".
        val kidName = (findProperty("kidName") as String?)?.trim().orEmpty()
        // Android string resources need ' and " backslash-escaped, or resource compilation fails.
        fun res(s: String) = s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")
        resValue("string", "kid_name", res(kidName))
        resValue("string", "app_name", if (kidName.isEmpty()) "Kid Launcher" else res("$kidName's Home"))

        // VectorDrawable arrived in API 21, so AGP rasterises res/drawable/*.xml vectors to PNG
        // at build time. The phone is hdpi (240 dpi); xhdpi is kept as a sharper fallback.
        vectorDrawables {
            generatedDensities?.clear()
            generatedDensities?.addAll(listOf("hdpi", "xhdpi"))
        }
    }

    // Optional shared debug key, so builds from any machine can update a copy already on a
    // phone: `debug.keystore` next to settings.gradle.kts (gitignored), or the copy kept in an
    // enclosing private repo's secrets/. Without either, AGP uses the per-machine
    // ~/.android/debug.keystore, and updating an installed copy needs an uninstall first.
    val sharedDebugKey = listOf("debug.keystore", "../../secrets/kid-launcher-debug.keystore")
        .map { rootProject.file(it) }.firstOrNull { it.exists() }
    if (sharedDebugKey != null) {
        signingConfigs {
            getByName("debug") {
                storeFile = sharedDebugKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        resValues = true
    }

    lint {
        // The whole point of this app is API 19; make a too-new API call a build failure.
        error += "NewApi"
        abortOnError = true
    }

    // No Google-encrypted dependency blob in the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
