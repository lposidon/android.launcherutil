package io.posidon.android.launcherutils.appLoading

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import io.posidon.android.launcherutils.IconTheming

abstract class SimpleAppCollection : AppLoader.AppCollection<Nothing?> {

    val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
    }
    val maskp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    override fun finalize(context: Context) {}

    open fun modifyIcon(
        icon: Drawable,
        packageName: String,
        name: String,
        profile: UserHandle,
    ): Drawable {
        return icon
    }

    final override fun modifyIcon(
        icon: Drawable,
        packageName: String,
        name: String,
        profile: UserHandle,
        expandableBackground: Drawable?
    ): Pair<Drawable, Nothing?> {
        return modifyIcon(icon, packageName, name, profile) to null
    }

    override fun themeIcon(
        icon: Drawable,
        iconConfig: IconConfig,
        iconPackInfo: IconTheming.IconPackInfo,
        context: Context
    ): Drawable {
        var icon1 = icon
        try {
            var orig = Bitmap.createBitmap(
                icon1.intrinsicWidth,
                icon1.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            icon1.setBounds(0, 0, icon1.intrinsicWidth, icon1.intrinsicHeight)
            icon1.draw(Canvas(orig))
            val scaledBitmap =
                Bitmap.createBitmap(iconConfig.size, iconConfig.size, Bitmap.Config.ARGB_8888)
            Canvas(scaledBitmap).run {
                if (iconPackInfo.back != null) {
                    val b = iconPackInfo.back!!
                    drawBitmap(
                        b,
                        Rect(0, 0, b.width, b.height),
                        Rect(0, 0, iconConfig.size, iconConfig.size),
                        p
                    )
                }
                val scaledOrig =
                    Bitmap.createBitmap(iconConfig.size, iconConfig.size, Bitmap.Config.ARGB_8888)
                Canvas(scaledOrig).run {
                    val s = (iconConfig.size * iconPackInfo.scaleFactor).toInt()
                    orig = Bitmap.createScaledBitmap(orig, s, s, true)
                    drawBitmap(
                        orig,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        p
                    )
                    if (iconPackInfo.mask != null) {
                        val b = iconPackInfo.mask!!
                        drawBitmap(
                            b,
                            Rect(0, 0, b.width, b.height),
                            Rect(0, 0, iconConfig.size, iconConfig.size),
                            maskp
                        )
                    }
                }
                drawBitmap(
                    Bitmap.createScaledBitmap(scaledOrig, iconConfig.size, iconConfig.size, true),
                    0f,
                    0f,
                    p
                )
                if (iconPackInfo.front != null) {
                    val b = iconPackInfo.front!!
                    drawBitmap(
                        b,
                        Rect(0, 0, b.width, b.height),
                        Rect(0, 0, iconConfig.size, iconConfig.size),
                        p
                    )
                }
                scaledOrig.recycle()
            }
            icon1 = BitmapDrawable(context.resources, scaledBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return icon1
    }
}