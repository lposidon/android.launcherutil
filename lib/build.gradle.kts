import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    id("com.android.library")
    id("kotlin-android")
    `maven-publish`
}

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdk = 26
        targetSdk = 30

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
    implementation("io.posidon:android.convenienceLib:master-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${getKotlinPluginVersion()}")
    implementation("androidx.annotation:annotation:1.2.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "io.posidon"
                artifactId = "android.launcherUtils"
                version = "1.0.0"
                from(components["release"])
            }
        }
    }
}