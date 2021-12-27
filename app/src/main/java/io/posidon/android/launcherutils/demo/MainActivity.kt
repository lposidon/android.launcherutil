package io.posidon.android.launcherutils.demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutils.IconTheming
import io.posidon.android.launcherutils.appLoading.AppLoader
import io.posidon.android.launcherutils.appLoading.IconConfig

class MainActivity : AppCompatActivity() {

    val appsAdapter = AppsAdapter()
    val appLoader = AppLoader(::AppCollection)

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
        val iconConfig = IconConfig(
            size = (resources.displayMetrics.density * 128f).toInt(),
            density = resources.configuration.densityDpi,
            packPackages = iconPack?.let { arrayOf(it) } ?: emptyArray(),
        )

        appLoader.async(this, iconConfig) {
            runOnUiThread {
                appsAdapter.update(it.list)
            }
        }
    }
}