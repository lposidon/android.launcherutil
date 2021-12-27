package io.posidon.android.launcherutils.appLoading

import android.content.Context
import android.content.res.Resources
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
        iconPackInfo: IconTheming.IconGenerationInfo,
        resources: Resources
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
                Bitmap.createBitmap(iconPackInfo.size, iconPackInfo.size, Bitmap.Config.ARGB_8888)
            Canvas(scaledBitmap).run {
                val uniformOptions = BitmapFactory.Options().apply {
                    inScaled = false
                }
                val back = iconPackInfo.getBackBitmap(uniformOptions)
                if (back != null) {
                    drawBitmap(
                        back,
                        Rect(0, 0, back.width, back.height),
                        Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                        p
                    )
                    back.recycle()
                }
                val scaledOrig =
                    Bitmap.createBitmap(iconPackInfo.size, iconPackInfo.size, Bitmap.Config.ARGB_8888)
                Canvas(scaledOrig).run {
                    val s = (iconPackInfo.size * iconPackInfo.scaleFactor).toInt()
                    val oldOrig = orig
                    orig = Bitmap.createScaledBitmap(orig, s, s, true)
                    oldOrig.recycle()
                    drawBitmap(
                        orig,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        p
                    )
                    val mask = iconPackInfo.getMaskBitmap(uniformOptions)
                    if (mask != null) {
                        drawBitmap(
                            mask,
                            Rect(0, 0, mask.width, mask.height),
                            Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                            maskp
                        )
                        mask.recycle()
                    }
                }
                drawBitmap(
                    Bitmap.createScaledBitmap(scaledOrig, iconPackInfo.size, iconPackInfo.size, true),
                    0f,
                    0f,
                    p
                )
                val front = iconPackInfo.getFrontBitmap(uniformOptions)
                if (front != null) {
                    drawBitmap(
                        front,
                        Rect(0, 0, front.width, front.height),
                        Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                        p
                    )
                    front.recycle()
                }
                orig.recycle()
                scaledOrig.recycle()
            }
            icon1 = BitmapDrawable(resources, scaledBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return icon1
    }
}