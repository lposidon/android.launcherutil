package io.posidon.android.launcherutil.demo

import android.os.UserHandle

class App(
    val packageName: String,
    val name: String,
    val profile: UserHandle,
    val label: String
)