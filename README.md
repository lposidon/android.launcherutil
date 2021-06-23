# Launcher utils
A library for android launcher creation (very much in alpha stage)

## Features
- App loading
- Kustom variables
- Icon-related utilities
- Send signals to live wallpaper

## How to use
Make sure you have the jitpack.io repo added in your project
```kotlin
allprojects {
    repositories {
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
```
Add the dependency
```kotlin
dependencies {
    implementation("io.posidon:android.launcherUtils:${VERSION}")
}
```