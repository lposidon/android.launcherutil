package io.posidon.android.launcherutils.demo

import android.content.Context
import io.posidon.android.launcherutils.AppLoader

class AppCollection(size: Int) : AppLoader.AppCollection<App> {
    val list = ArrayList<App>(size)

    override fun add(context: Context, app: App) {
        list.add(app)
    }

    override fun finalize(context: Context) {
        list.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }
    }
}