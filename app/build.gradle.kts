plugins {
    id("com.android.application")
}

android {
    namespace = "com.reproductor.kiosk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.reproductor.kiosk"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
