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

    inline fun getAvailableIconPacks(packageManager: PackageManager): MutableList<ResolveInfo> {
        return packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN)
                .addCategory("com.anddoes.launcher.THEME"),
            0
        )
    }

    class IconPackInfo(
        val res: Resources,
        val iconPackPackageName: String
    ) {
        var scaleFactor = 1f
        val iconResourceNames = HashMap<String, String>()
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var areUnthemedIconsChanged: Boolean = false

        fun getDrawable(packageName: String, name: String): Drawable? {
            val iconResource =
                iconResourceNames["ComponentInfo{$packageName/$name}"]
                    ?: return null
            val intres = res.getIdentifier(
                iconResource,
                "drawable",
                iconPackPackageName
            )
            if (intres == 0) return null
            return try {
                res.getDrawable(intres, null)
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
            if (n != 0) {
                val xrp = res.getXml(n)
                while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                    if (xrp.eventType == 2) {
                        try {
                            when (xrp.name) {
                                "scale" -> {
                                    info.scaleFactor = xrp.getAttributeValue(0).toFloat()
                                }
                                "item" -> {
                                    info.iconResourceNames[xrp.getAttributeValue(0)] = xrp.getAttributeValue(1)
                                }
                                "iconback" -> info.back = loadIconMod(
                                    xrp.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    uniformOptions,
                                    info
                                )
                                "iconmask" -> info.mask = loadIconMod(
                                    xrp.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    uniformOptions,
                                    info
                                )
                                "iconupon" -> info.front = loadIconMod(
                                    xrp.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    uniformOptions,
                                    info
                                )
                            }
                        } catch (e: Exception) {}
                    }
                    xrp.next()
                }
            } else {
                val factory = XmlPullParserFactory.newInstance()
                factory.isValidating = false
                val xpp = factory.newPullParser()
                val raw = res.assets.open("appfilter.xml")
                xpp.setInput(raw, null)
                while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                    if (xpp.eventType == 2) {
                        try {
                            when (xpp.name) {
                                "scale" -> {
                                    info.scaleFactor = xpp.getAttributeValue(0).toFloat()
                                }
                                "item" -> {
                                    info.iconResourceNames[xpp.getAttributeValue(0)] = xpp.getAttributeValue(1)
                                }
                                "iconback" -> info.back = loadIconMod(
                                    xpp.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    uniformOptions,
                                    info
                                )
                                "iconmask" -> info.mask = loadIconMod(
                                    xpp.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    uniformOptions,
                                    info
                                )
                                "iconupon" -> info.front = loadIconMod(
                                    xpp.getAttributeValue(0),
                                    res,
                                    iconPackPackageName,
                                    uniformOptions,
                                    info
                                )
                            }
                        } catch (e: Exception) {}
                    }
                    xpp.next()
                }
            }
        } catch (ignore: Exception) {}
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