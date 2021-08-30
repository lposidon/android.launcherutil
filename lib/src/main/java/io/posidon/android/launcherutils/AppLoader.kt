package io.posidon.android.launcherutils

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
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

    inline fun async(context: Context, iconPackPackages: Array<String> = emptyArray(), noinline onEnd: (apps: APPCollection) -> Unit) { thread(name = "AppLoader thread", isDaemon = true) { sync(context, iconPackPackages, onEnd) } }
    fun sync(context: Context, iconPackPackages: Array<String> = emptyArray(), onEnd: (apps: APPCollection) -> Unit) {

        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }
        val maskp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        val uniformOptions = BitmapFactory.Options().apply {
            inScaled = false
        }
        val iconSize = context.dp(128).toInt()

        val packageManager = context.packageManager

        val iconPacks = iconPackPackages.mapNotNull { iconPackPackage ->
            var iconPackInfo: IconTheming.IconPackInfo? = null
            try {
                val themeRes = packageManager.getResourcesForApplication(iconPackPackage)
                iconPackInfo = IconTheming.getIconPackInfo(themeRes, iconPackPackage, uniformOptions)
            } catch (e: Exception) {}
            iconPackInfo
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
                    iconPacks,
                    iconSize,
                    p,
                    maskp,
                    context,
                    profile
                )
                collection.add(context, app)
            }
        }
        collection.finalize(context)
        onEnd(collection)
    }

    interface AppCollection <APP> {
        fun add(context: Context, app: APP)
        fun finalize(context: Context)
    }

    private fun loadApp(
        appListItem: LauncherActivityInfo,
        iconPacks: List<IconTheming.IconPackInfo>,
        iconSize: Int,
        p: Paint,
        maskp: Paint,
        context: Context,
        profile: UserHandle
    ): APP {

        val packageName = appListItem.applicationInfo.packageName
        val name = appListItem.name

        val label = appListItem.label.let {
            if (it.isNullOrBlank()) packageName
            else it.toString()
        }

        val icon: Drawable = iconPacks.firstNotNullOfOrNull { iconPackInfo ->
            iconPackInfo.getDrawable(appListItem.applicationInfo.packageName, appListItem.name)
        } ?: iconPacks.firstNotNullOfOrNull { iconPackInfo ->
            var icon = appListItem.getIcon(0)
            if (iconPackInfo.areUnthemedIconsChanged) {
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
                        if (iconPackInfo.back != null) {
                            drawBitmap(
                                iconPackInfo.back!!,
                                Graphics.getResizedMatrix(iconPackInfo.back!!, iconSize, iconSize),
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
                            if (iconPackInfo.mask != null) {
                                drawBitmap(
                                    iconPackInfo.mask!!,
                                    Graphics.getResizedMatrix(iconPackInfo.mask!!, iconSize, iconSize),
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
                        if (iconPackInfo.front != null) {
                            drawBitmap(
                                iconPackInfo.front!!,
                                Graphics.getResizedMatrix(iconPackInfo.front!!, iconSize, iconSize),
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
            icon
        } ?: appListItem.getIcon(0) ?: ColorDrawable()
        return appConstructor(packageName, name, profile, label, icon)
    }
}