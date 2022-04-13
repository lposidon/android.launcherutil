package io.posidon.android.launcherutil.demo

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutil.IconTheming
import io.posidon.android.launcherutil.Launcher
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.launcherutil.loader.AppIconLoader

class MainActivity : AppCompatActivity() {

    val appsAdapter = AppsAdapter(this)
    lateinit var iconLoader: AppIconLoader<Nothing?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<RecyclerView>(R.id.recycler).run {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = appsAdapter
        }
        findViewById<View>(R.id.choose_icon_pack).run {
            setOnClickListener {
                val iconPacks = IconTheming.getAvailableIconPacks(packageManager)
                AlertDialog.Builder(context)
                    .setSingleChoiceItems(iconPacks.mapTo(ArrayList()) { it.loadLabel(packageManager) }.apply { add(0, "System") }.toTypedArray(), 0) { d, i ->
                        loadApps(if (i == 0) null else iconPacks[i - 1].activityInfo.packageName)
                        d.dismiss()
                    }
                    .show()
            }
        }
        loadApps()
    }

    fun loadApps(iconPack: String? = null) {
        iconLoader = Launcher.iconLoader(
            this,
            size = (resources.displayMetrics.density * 128f).toInt(),
            density = resources.configuration.densityDpi,
            packPackages = iconPack?.let { arrayOf(it) } ?: emptyArray(),
        ) { _, _, profile, icon ->
            if (!isUserRunning(profile)) {
                icon?.convertToGrayscale() to null
            } else icon to null
        }
        val list = ArrayList<App>()
        Launcher.appLoader.loadAsync(
            this,
            onEnd = {
                list.sortWith { o1, o2 ->
                    o1.label.compareTo(o2.label, ignoreCase = true)
                }
                runOnUiThread {
                    appsAdapter.update(list)
                }
            },
            forEachApp = {
                list += App(
                    it.packageName,
                    it.name,
                    it.profile,
                    it.getBadgedLabel(this)
                )
            }
        )
    }

    private fun Drawable.convertToGrayscale(): Drawable = apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f)
        })
    }
}