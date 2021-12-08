package io.posidon.android.launcherutils.appLoading

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import io.posidon.android.launcherutils.IconTheming
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
class AppLoader <EXTRA_ICON_DATA, APPCollection : AppLoader.AppCollection<EXTRA_ICON_DATA>> (
    val collectionConstructor: (appCount: Int) -> APPCollection
) {

    inline fun async(
        context: Context,
        iconConfig: IconConfig,
        noinline onEnd: (apps: APPCollection) -> Unit
    ) { thread(name = "AppLoader thread", isDaemon = true) { sync(context, iconConfig, onEnd) } }

    fun sync(
        context: Context,
        iconConfig: IconConfig,
        onEnd: (apps: APPCollection) -> Unit
    ) {

        val uniformOptions = BitmapFactory.Options().apply {
            inScaled = false
        }

        val packageManager = context.packageManager

        val iconPacks = iconConfig.packPackages.mapNotNull { iconPackPackage ->
            var iconPackInfo: IconTheming.IconPackInfo? = null
            try {
                val themeRes = packageManager.getResourcesForApplication(iconPackPackage)
                iconPackInfo = IconTheming.getIconPackInfo(themeRes, iconPackPackage, uniformOptions)
            } catch (e: Exception) { e.printStackTrace() }
            iconPackInfo
        }

        val userManager = context.getSystemService(UserManager::class.java)

        var appCount = 0
        val appLists = Array(userManager.userProfiles.size) {
            val profile = userManager.userProfiles[it]
            val appList = context.getSystemService(LauncherApps::class.java).getActivityList(null, profile)
            appCount += appList.size
            appList to profile
        }

        val collection = collectionConstructor(appCount)

        for ((appList, profile) in appLists) {
            val isUserRunning = userManager.isUserRunning(profile)
            for (i in appList.indices) addApp(
                collection,
                appList[i],
                iconPacks,
                iconConfig,
                context,
                profile,
                isUserRunning
            )
        }
        collection.finalize(context)
        onEnd(collection)
    }

    interface AppCollection <EXTRA_ICON_DATA> {
        fun finalize(context: Context)
        fun addApp(
            context: Context,
            packageName: String,
            name: String,
            profile: UserHandle,
            label: String,
            icon: Drawable,
            extra: ExtraAppInfo<EXTRA_ICON_DATA>
        )
        fun modifyIcon(
            icon: Drawable,
            packageName: String,
            name: String,
            profile: UserHandle,
            expandableBackground: Drawable?
        ): Pair<Drawable, EXTRA_ICON_DATA>
        fun themeIcon(
            icon: Drawable,
            iconConfig: IconConfig,
            iconPackInfo: IconTheming.IconPackInfo,
            context: Context
        ): Drawable
    }

    private fun addApp(
        collection: APPCollection,
        appListItem: LauncherActivityInfo,
        iconPacks: List<IconTheming.IconPackInfo>,
        iconConfig: IconConfig,
        context: Context,
        profile: UserHandle,
        isUserRunning: Boolean,
    ) {

        val packageName = appListItem.applicationInfo.packageName
        val name = appListItem.name

        val label = appListItem.label.let {
            if (it.isNullOrBlank()) packageName
            else it.toString()
        }

        var background: Drawable? = null

        val icon: Drawable = iconPacks.firstNotNullOfOrNull { iconPackInfo ->
            iconPackInfo.getDrawable(appListItem.applicationInfo.packageName, appListItem.name, iconConfig.density)?.also {
                background = iconPackInfo.getBackground(appListItem.applicationInfo.packageName, appListItem.name)
            }
        } ?: iconPacks.firstNotNullOfOrNull { iconPackInfo ->
            var icon = appListItem.getIcon(iconConfig.density)
            if (iconPackInfo.areUnthemedIconsChanged) {
                icon = collection.themeIcon(icon, iconConfig, iconPackInfo, context)
            }
            icon
        } ?: appListItem.getIcon(iconConfig.density) ?: ColorDrawable()
        val (modifiedIcon, extraIconData) = collection.modifyIcon(icon, packageName, name, profile, background)
        val extra = ExtraAppInfo(
            banner = appListItem.applicationInfo.loadBanner(context.packageManager),
            logo = appListItem.applicationInfo.loadLogo(context.packageManager),
            background = background,
            extraIconData = extraIconData,
            isUserRunning = isUserRunning,
        )
        collection.addApp(
            context,
            packageName,
            name,
            profile,
            context.packageManager.getUserBadgedLabel(label, profile).toString(),
            context.packageManager.getUserBadgedIcon(modifiedIcon, profile),
            extra
        )
    }

    class ExtraAppInfo <EXTRA_ICON_DATA> (
        val banner: Drawable?,
        val logo: Drawable?,
        val background: Drawable?,
        val extraIconData: EXTRA_ICON_DATA,
        val isUserRunning: Boolean,
    )
}