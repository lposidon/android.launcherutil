package io.posidon.android.launcherutils.appLoading

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import io.posidon.android.computable.Computable
import io.posidon.android.computable.component2
import io.posidon.android.computable.component3
import io.posidon.android.computable.dependentUse
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
        val packageManager = context.packageManager

        val iconPacks = iconConfig.packPackages.mapNotNull { iconPackPackage ->
            var iconPackInfo: IconTheming.IconPackInfo? = null
            try {
                val themeRes = packageManager.getResourcesForApplication(iconPackPackage)
                iconPackInfo = IconTheming.getIconPackInfo(themeRes, iconPackPackage)
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
                context,
                profile,
                isUserRunning,
                iconConfig,
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
            icon: Computable<Drawable>,
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
            iconPackInfo: IconTheming.IconGenerationInfo,
            resources: Resources
        ): Drawable
    }

    private fun addApp(
        collection: APPCollection,
        appListItem: LauncherActivityInfo,
        iconPacks: List<IconTheming.IconPackInfo>,
        context: Context,
        profile: UserHandle,
        isUserRunning: Boolean,
        iconConfig: IconConfig,
    ) {

        val packageName = appListItem.applicationInfo.packageName
        val name = appListItem.name

        val label = appListItem.label.let {
            if (it.isNullOrBlank()) packageName
            else it.toString()
        }

        val (icon, background, extraIconData) = loadGraphics(
            iconPacks,
            appListItem,
            collection,
            context,
            packageName,
            name,
            profile,
            iconConfig,
        )

        val extra = ExtraAppInfo(
            banner = Computable { appListItem.applicationInfo.loadBanner(context.packageManager) },
            logo = Computable { appListItem.applicationInfo.loadLogo(context.packageManager) },
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
            icon,
            extra,
        )
    }

    private fun loadGraphics(
        iconPacks: List<IconTheming.IconPackInfo>,
        appListItem: LauncherActivityInfo,
        collection: APPCollection,
        context: Context,
        packageName: String,
        name: String,
        profile: UserHandle,
        iconConfig: IconConfig
    ): Triple<Computable<Drawable>, Computable<Drawable?>, Computable<EXTRA_ICON_DATA>> {
        val iconPackIconInfo = iconPacks.firstNotNullOfOrNull { iconPackInfo ->
            iconPackInfo.getDrawableResource(
                appListItem.applicationInfo.packageName,
                appListItem.name,
            ).let {
                if (it == 0) null else Triple(
                    iconPackInfo.res, it, iconPackInfo.getBackgroundResource(
                        appListItem.applicationInfo.packageName,
                        appListItem.name,
                    )
                )
            }
        }
        var iconPackInfoToTheme: IconTheming.IconGenerationInfo? = null
        if (iconPackIconInfo == null) {
            iconPackInfoToTheme = iconPacks.firstNotNullOfOrNull {
                if (it.iconModificationInfo.areUnthemedIconsChanged) it.iconModificationInfo.also {
                    it.size = iconConfig.size
                } else null
            }
        }
        val c = iconPackIconInfo?.let { (res, iconRes, backgroundRes) ->
            loadGraphicsComputableA(
                res,
                iconRes,
                backgroundRes,
                iconConfig.density,
                collection,
                packageName,
                name,
                profile
            )
        } ?: run {
            loadGraphicsComputableB(
                appListItem,
                iconConfig.density,
                iconPackInfoToTheme,
                collection,
                context.resources,
                packageName,
                name,
                profile
            )
        }
        return Triple(
            c.dependentUse { context.packageManager.getUserBadgedIcon(it.first, profile) },
            c.component2(),
            c.component3()
        )
    }

    private fun loadGraphicsComputableA(
        res: Resources,
        iconRes: Int,
        backgroundRes: Int,
        density: Int,
        collection: APPCollection,
        packageName: String,
        name: String,
        profile: UserHandle,
    ) = Computable {
        var background: Drawable? = null
        var icon: Drawable? = null
        try {
            icon = res.getDrawableForDensity(
                iconRes,
                density,
                null
            )
        } catch (e: Resources.NotFoundException) {}
        try {
            background = res.getDrawableForDensity(
                backgroundRes,
                density,
                null
            )
        } catch (e: Resources.NotFoundException) {}
        collection.modifyIcon(
            icon ?: ColorDrawable(),
            packageName,
            name,
            profile,
            background
        ).let { Triple(it.first, background, it.second) }
    }

    private fun loadGraphicsComputableB(
        appListItem: LauncherActivityInfo,
        density: Int,
        iconPackInfoToTheme: IconTheming.IconGenerationInfo?,
        collection: APPCollection,
        resources: Resources,
        packageName: String,
        name: String,
        profile: UserHandle,
    ): Computable<Triple<Drawable, Drawable?, EXTRA_ICON_DATA>> =
        Computable {
            var icon = appListItem.getIcon(density) ?: ColorDrawable()
            iconPackInfoToTheme?.let {
                icon = collection.themeIcon(
                    icon,
                    it,
                    resources
                )
            }
            collection.modifyIcon(
                icon,
                packageName,
                name,
                profile,
                null
            ).let { Triple(it.first, null, it.second) }
        }

    class ExtraAppInfo <EXTRA_ICON_DATA> (
        val banner: Computable<Drawable?>,
        val logo: Computable<Drawable?>,
        val background: Computable<Drawable?>,
        val extraIconData: Computable<EXTRA_ICON_DATA>,
        val isUserRunning: Boolean,
    )
}