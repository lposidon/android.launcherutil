package io.posidon.android.launcherutils.demo

import android.graphics.drawable.Drawable
import android.os.UserHandle
import io.posidon.android.launcherutils.AppLoader

class App(
    val packageName: String,
    val name: String,
    val profile: UserHandle,
    val label: String,
    val icon: Drawable,
    extraAppInfo: AppLoader.ExtraAppInfo
)