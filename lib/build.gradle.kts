plugins {
    id("com.android.library")
    id("kotlin-android")
    `maven-publish`
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 26
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.3.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "io.posidon"
                artifactId = "android.launcherutil"
                version = "22.0"
                from(components["release"])
            }
        }
    }
}