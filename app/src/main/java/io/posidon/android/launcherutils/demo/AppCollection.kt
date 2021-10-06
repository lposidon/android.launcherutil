package io.posidon.android.launcherutils.demo

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.UserHandle
import io.posidon.android.launcherutils.AppLoader

class AppCollection(size: Int) : AppLoader.AppCollection<Nothing?> {

    val list = ArrayList<App>(size)

    override fun addApp(
        context: Context,
        packageName: String,
        name: String,
        profile: UserHandle,
        label: String,
        icon: Drawable,
        extra: AppLoader.ExtraAppInfo<Nothing?>
    ) {
        list.add(App(packageName, name, profile, label, icon))
    }

    override fun finalize(context: Context) {
        list.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }
    }

    override fun modifyIcon(icon: Drawable, expandableBackground: Drawable?): Pair<Drawable, Nothing?> {
        return icon to null
    }
}