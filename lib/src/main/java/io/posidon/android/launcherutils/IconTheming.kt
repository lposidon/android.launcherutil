package io.posidon.android.launcherutils

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

object IconTheming {

    const val ICON_PACK_CATEGORY = "com.anddoes.launcher.THEME"

    inline fun getAvailableIconPacks(packageManager: PackageManager): MutableList<ResolveInfo> {
        return packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN)
                .addCategory(ICON_PACK_CATEGORY),
            0
        )
    }

    class IconPackInfo(
        val res: Resources,
        val iconPackPackageName: String
    ) {
        var scaleFactor = 1f
        val iconResourceNames = HashMap<String, String>()
        val calendarPrefixes = HashMap<String, String>()
        val backgrounds = HashMap<String, String>()
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var areUnthemedIconsChanged: Boolean = false

        fun getDrawable(packageName: String, name: String, density: Int): Drawable? {
            val key = "ComponentInfo{$packageName/$name}"
            val iconResource = calendarPrefixes[key]
                ?.let { it + Calendar.getInstance()[Calendar.DAY_OF_MONTH] }
                ?: iconResourceNames[key]
                ?: return null
            val drawableRes = res.getIdentifier(
                iconResource,
                "drawable",
                iconPackPackageName
            )
            if (drawableRes == 0) return null
            return try {
                res.getDrawableForDensity(drawableRes, density, null)
            } catch (e: Resources.NotFoundException) {
                null
            }
        }

        fun getBackground(packageName: String, name: String): Drawable? {
            val key = "ComponentInfo{$packageName/$name}"
            val background = backgrounds[key] ?: return null

            val backgroundRes = res.getIdentifier(
                background,
                "drawable",
                iconPackPackageName
            )
            if (backgroundRes == 0) return null
            return try {
                res.getDrawable(backgroundRes, null)
            } catch (e: Resources.NotFoundException) {
                null
            }
        }
    }

    fun getIconPackInfo(
        res: Resources,
        iconPackPackageName: String,
        uniformOptions: BitmapFactory.Options
    ): IconPackInfo {
        val info = IconPackInfo(res, iconPackPackageName)
        try {
            val n = res.getIdentifier("appfilter", "xml", iconPackPackageName)
            val x = if (n != 0) {
                res.getXml(n)
            } else {
                val factory = XmlPullParserFactory.newInstance()
                factory.isValidating = false
                val xpp = factory.newPullParser()
                val raw = res.assets.open("appfilter.xml")
                xpp.setInput(raw, null)
                xpp
            }
            while (x.eventType != XmlResourceParser.END_DOCUMENT) {
                if (x.eventType == 2) {
                    try {
                        when (x.name) {
                            "scale" -> {
                                info.scaleFactor = x.getAttributeValue(0).toFloat()
                            }
                            "item" -> {
                                val key = x.getAttributeValue(null, "component")
                                val value = x.getAttributeValue(null, "drawable")
                                val background = x.getAttributeValue(null, "background")
                                if (key != null) {
                                    if (value != null)
                                        info.iconResourceNames[key] = value
                                    if (background != null)
                                        info.backgrounds[key] = background
                                }
                            }
                            "calendar" -> {
                                val key = x.getAttributeValue(null, "component")
                                val value = x.getAttributeValue(null, "prefix")
                                val background = x.getAttributeValue(null, "background")
                                if (key != null) {
                                    if (value != null)
                                        info.calendarPrefixes[key] = value
                                    if (background != null)
                                        info.backgrounds[key] = background
                                }
                            }
                            "iconback" -> info.back = loadIconMod(
                                x.getAttributeValue(0),
                                res,
                                iconPackPackageName,
                                uniformOptions,
                                info
                            )
                            "iconmask" -> info.mask = loadIconMod(
                                x.getAttributeValue(0),
                                res,
                                iconPackPackageName,
                                uniformOptions,
                                info
                            )
                            "iconupon" -> info.front = loadIconMod(
                                x.getAttributeValue(0),
                                res,
                                iconPackPackageName,
                                uniformOptions,
                                info
                            )
                        }
                    } catch (e: Exception) {}
                }
                x.next()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return info
    }

    private fun loadIconMod(name: String, res: Resources, iconPackPackageName: String, uniformOptions: BitmapFactory.Options, info: IconPackInfo): Bitmap? {
        val i = res.getIdentifier(
            name,
            "drawable",
            iconPackPackageName
        )
        if (i != 0) {
            info.areUnthemedIconsChanged = true
            return BitmapFactory.decodeResource(res, i, uniformOptions)
        }
        return null
    }

    fun getResourceNames(res: Resources, iconPack: String?): ArrayList<String> {
        val strings = ArrayList<String>()
        try {
            val n = res.getIdentifier("drawable", "xml", iconPack)
            if (n != 0) {
                val xrp = res.getXml(n)
                while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                    try {
                        if (xrp.eventType == 2 && !strings.contains(xrp.getAttributeValue(0))) {
                            if (xrp.name == "item") {
                                strings.add(xrp.getAttributeValue(0))
                            }
                        }
                    } catch (ignore: Exception) {}
                    xrp.next()
                }
            } else {
                val factory = XmlPullParserFactory.newInstance()
                factory.isValidating = false
                val xpp = factory.newPullParser()
                val raw = res.assets.open("drawable.xml")
                xpp.setInput(raw, null)
                while (xpp!!.eventType != XmlPullParser.END_DOCUMENT) {
                    try {
                        if (xpp.eventType == 2 && !strings.contains(xpp.getAttributeValue(0))) {
                            if (xpp.name == "item") {
                                strings.add(xpp.getAttributeValue(0))
                            }
                        }
                    } catch (ignore: Exception) {}
                    xpp.next()
                }
            }
        } catch (ignore: Exception) {}
        return strings
    }

    @RequiresApi(Build.VERSION_CODES.O)
    inline fun getSystemAdaptiveIconPath(width: Int, height: Int): Path {
        val minSize = min(width, height)
        val path = AdaptiveIconDrawable(null, null).iconMask
        val rect = RectF()
        path.computeBounds(rect, true)
        val matrix = Matrix()
        matrix.setScale(minSize / rect.right, minSize / rect.bottom)
        path.transform(matrix)
        path.fillType = Path.FillType.INVERSE_EVEN_ODD
        return path
    }
}