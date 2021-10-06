package io.posidon.android.launcherutils.demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutils.AppLoader
import io.posidon.android.launcherutils.IconTheming

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
                    .setSingleChoiceItems(iconPacks.map { it.loadLabel(packageManager) }.toTypedArray(), 0) { d, i ->
                        loadApps(iconPacks[i].activityInfo.packageName)
                        d.dismiss()
                    }
                    .show()
            }
        }
        loadApps()
    }

    fun loadApps(iconPack: String? = null) {
        appLoader.async(this, iconPackPackages = iconPack?.let { arrayOf(it) } ?: emptyArray()) {
            runOnUiThread {
                appsAdapter.update(it.list)
            }
        }
    }
}