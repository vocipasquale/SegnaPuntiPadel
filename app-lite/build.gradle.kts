plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "it.pv.segnapuntipadel.lite"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "it.pv.segnapuntipadel.lite"
        minSdk = 16
        targetSdk = 36
        versionCode = 3
        versionName = "1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":scoring"))
    testImplementation(libs.junit)
}