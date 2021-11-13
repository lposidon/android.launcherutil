package io.posidon.android.launcherutils.demo

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.UserHandle
import io.posidon.android.launcherutils.appLoading.AppLoader
import io.posidon.android.launcherutils.appLoading.SimpleAppCollection

class AppCollection(size: Int) : SimpleAppCollection() {

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
        if (!extra.isUserRunning) {
            icon.convertToGrayscale()
        }
        list.add(App(packageName, name, profile, label, icon))
    }

    override fun finalize(context: Context) {
        list.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }
    }

    fun Drawable.convertToGrayscale() {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f)
        })
    }
}