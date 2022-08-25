package io.posidon.android.launcherutil

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import io.posidon.android.launcherutil.loader.*

object Launcher {

    val appLoader = AppLoader()

    inline fun iconLoader(
        context: Context,
        size: Int,
        density: Int = 0,
        packPackages: Array<String> = emptyArray(),
    ): AppIconLoader<Nothing?> = iconLoader(context, size, density, packPackages, DEFAULT_ICON_MODIFIER)

    inline fun <EXTRA> iconLoader(
        context: Context,
        size: Int,
        density: Int = 0,
        packPackages: Array<String> = emptyArray(),
        noinline iconModifier: IconModifier<EXTRA>,
    ): AppIconLoader<EXTRA> = AppIconLoader(context, IconConfig(size, density, packPackages), iconModifier)

    inline fun <EXTRA> iconLoader(
        context: Context,
        size: Int,
        density: Int = 0,
        packPackages: Array<String> = emptyArray(),
        crossinline iconModifier: Context.(
            packageName: String,
            name: String,
            profile: UserHandle,
            icon: Drawable?,
        ) -> Pair<Drawable?, EXTRA>,
    ): AppIconLoader<EXTRA> = AppIconLoader(context, IconConfig(size, density, packPackages)) { a, b, c, d, _ ->
        this.iconModifier(a, b, c, d)
    }

    /**
     * @return the package name of the default launcher, null if there are no launchers
     */
    inline fun getDefaultLauncher(packageManager: PackageManager): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.resolvePackageName
    }
}

fun Context.isUserRunning(profile: UserHandle): Boolean =
    getSystemService(UserManager::class.java).isUserRunning(profile)