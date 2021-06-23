package io.posidon.android.launcherutils.demo

import io.posidon.android.launcherutils.AppLoader

class AppCollection(size: Int) : AppLoader.AppCollection<App> {
    val list = ArrayList<App>(size)

    override fun add(app: App) {
        list.add(app)
    }

    override fun finalize() {
        list.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }
    }
}