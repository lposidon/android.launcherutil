package io.posidon.android.launcherutils

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.res.ResourcesCompat
import posidon.android.conveniencelib.Graphics
import posidon.android.conveniencelib.dp
import kotlin.concurrent.thread

/**
 * Loads the apps, instead of just loading the app list,
 * you can decide how your app data is organized, you can
 * have 2 differently sorted lists, or one list of the
 * apps in alphabetical order and the other of them
 * grouped by their first letter...
 * Also you supply the loader with your own app class, so
 * that you don't have to build your project
 * around the loader
 */
class AppLoader <APP, APPCollection : AppLoader.AppCollection<APP>> (
    val appConstructor: (packageName: String, name: String, profile: UserHandle, label: String, icon: Drawable) -> APP,
    val collectionConstructor: (appCount: Int) -> APPCollection
) {

    inline fun async(context: Context, iconPackPackage: String? = null, noinline onEnd: (apps: APPCollection) -> Unit) { thread(name = "AppLoader thread", isDaemon = true) { sync(context, iconPackPackage, onEnd) } }
    fun sync(context: Context, iconPackPackage: String? = null, onEnd: (apps: APPCollection) -> Unit) {

        val packageManager = context.packageManager
        val iconSize = context.dp(65).toInt()
        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }
        val maskp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        var iconPackInfo: LauncherIcons.IconPackInfo? = null
        var themeRes: Resources? = null
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var areUnthemedIconsChanged = false
        run {
            val uniformOptions = BitmapFactory.Options().apply {
                inScaled = false
            }
            if (iconPackPackage != null) {
                try {
                    themeRes = packageManager.getResourcesForApplication(iconPackPackage)
                    val themeRes = themeRes!!
                    iconPackInfo = LauncherIcons.getIconPackInfo(themeRes, iconPackPackage)
                    val iconPackInfo = iconPackInfo!!
                    if (iconPackInfo.iconBack != null) {
                        val intresiconback = themeRes.getIdentifier(
                            iconPackInfo.iconBack,
                            "drawable",
                            iconPackPackage
                        )
                        if (intresiconback != 0) {
                            back =
                                BitmapFactory.decodeResource(themeRes, intresiconback, uniformOptions)
                            areUnthemedIconsChanged = true
                        }
                    }
                    if (iconPackInfo.iconMask != null) {
                        val intresiconmask = themeRes.getIdentifier(
                            iconPackInfo.iconMask,
                            "drawable",
                            iconPackPackage
                        )
                        if (intresiconmask != 0) {
                            mask =
                                BitmapFactory.decodeResource(themeRes, intresiconmask, uniformOptions)
                            areUnthemedIconsChanged = true
                        }
                    }
                    if (iconPackInfo.iconFront != null) {
                        val intresiconfront = themeRes.getIdentifier(
                            iconPackInfo.iconFront,
                            "drawable",
                            iconPackPackage
                        )
                        if (intresiconfront != 0) {
                            front = BitmapFactory.decodeResource(themeRes, intresiconfront, uniformOptions)
                            areUnthemedIconsChanged = true
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

        var appCount = 0
        val appLists = Array(userManager.userProfiles.size) {
            val profile = userManager.userProfiles[it]
            val appList = (context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps).getActivityList(null, profile)
            appCount += appList.size
            appList to profile
        }

        val collection = collectionConstructor(appCount)

        for ((appList, profile) in appLists) {
            for (i in appList.indices) {
                val app = loadApp(
                    appList[i],
                    iconPackInfo,
                    themeRes,
                    iconPackPackage,
                    areUnthemedIconsChanged,
                    iconSize,
                    back,
                    p,
                    mask,
                    maskp,
                    front,
                    context,
                    profile
                )
                collection.add(app)
            }
        }
        collection.finalize()
        onEnd(collection)
    }

    interface AppCollection <APP> {
        fun add(app: APP)
        fun finalize()
    }

    private fun loadApp(
        appListItem: LauncherActivityInfo,
        iconPackInfo: LauncherIcons.IconPackInfo?,
        themeRes: Resources?,
        iconPackPackage: String?,
        areUnthemedIconsChanged: Boolean,
        iconSize: Int,
        back: Bitmap?,
        p: Paint,
        mask: Bitmap?,
        maskp: Paint,
        front: Bitmap?,
        context: Context,
        profile: UserHandle
    ): APP {

        val packageName = appListItem.applicationInfo.packageName
        val name = appListItem.name

        val label = appListItem.label.let {
            if (it.isNullOrBlank()) packageName
            else it.toString()
        }

        val icon = run {
            var intres = 0
            if (iconPackInfo != null) {
                val iconResource =
                    iconPackInfo.iconResourceNames["ComponentInfo{$packageName/$name}"]
                if (iconResource != null) {
                    intres = themeRes!!.getIdentifier(
                        iconResource,
                        "drawable",
                        iconPackPackage
                    )
                }
            }
            var icon: Drawable? = null
            if (intres != 0) try {
                icon = ResourcesCompat.getDrawable(themeRes!!, intres, null)
            } catch (e: Resources.NotFoundException) {
            }
            if (icon == null) {
                icon = appListItem.getIcon(0)
                if (iconPackInfo != null && areUnthemedIconsChanged) {
                    try {
                        var orig = Bitmap.createBitmap(
                            icon.intrinsicWidth,
                            icon.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
                        icon.draw(Canvas(orig))
                        val scaledBitmap =
                            Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                        Canvas(scaledBitmap).run {
                            if (back != null) {
                                drawBitmap(
                                    back,
                                    Graphics.getResizedMatrix(back, iconSize, iconSize),
                                    p
                                )
                            }
                            val scaledOrig =
                                Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                            Canvas(scaledOrig).run {
                                orig = Graphics.getResizedBitmap(
                                    orig,
                                    (iconSize * iconPackInfo.scaleFactor).toInt(),
                                    (iconSize * iconPackInfo.scaleFactor).toInt()
                                )
                                drawBitmap(
                                    orig,
                                    scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                                    scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                                    p
                                )
                                if (mask != null) {
                                    drawBitmap(
                                        mask,
                                        Graphics.getResizedMatrix(mask, iconSize, iconSize),
                                        maskp
                                    )
                                }
                            }
                            drawBitmap(
                                Graphics.getResizedBitmap(scaledOrig, iconSize, iconSize),
                                0f,
                                0f,
                                p
                            )
                            if (front != null) {
                                drawBitmap(
                                    front,
                                    Graphics.getResizedMatrix(front, iconSize, iconSize),
                                    p
                                )
                            }
                            scaledOrig.recycle()
                        }
                        icon = BitmapDrawable(context.resources, scaledBitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            //icon = Icons.generateAdaptiveIcon(icon)
            //icon = Icons.badgeMaybe(icon, appListItem.user != Process.myUserHandle())
            icon ?: ColorDrawable()
        }
        return appConstructor(packageName, name, profile, label, icon)
    }
}