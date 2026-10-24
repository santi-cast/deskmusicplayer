import java.util.Properties

plugins {
    id("com.android.application")
}

/**
 * Release signing key, kept outside the repository. Its properties file (storeFile, storePassword,
 * keyAlias, keyPassword) is read from DESKMUSICPLAYER_SIGNING or ~/claves-android/DeskMusicPlayer/deskmusicplayer.properties.
 * Without it the release build is left unsigned, so anyone can still build the code.
 */
val signingFile = file(System.getenv("DESKMUSICPLAYER_SIGNING") ?: "${System.getProperty("user.home")}/claves-android/DeskMusicPlayer/deskmusicplayer.properties")
val signing = Properties().apply { if (signingFile.isFile) signingFile.inputStream().use { load(it) } }

android {
    namespace = "com.reproductor.kiosk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.reproductor.kiosk"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"
    }

    signingConfigs {
        if (signingFile.isFile) {
            create("release") {
                storeFile = file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
