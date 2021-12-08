import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "io.posidon.android.launcherutils.demo"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

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
    implementation(project(":lib"))
    implementation("io.posidon:android.convenienceLib:master-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${getKotlinPluginVersion()}")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("com.google.android.material:material:1.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}