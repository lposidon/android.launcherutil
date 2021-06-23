package io.posidon.android.launcherutils.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutils.AppLoader

class MainActivity : AppCompatActivity() {

    val appsAdapter = AppsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<RecyclerView>(R.id.recycler).run {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = appsAdapter
        }
        val appLoader = AppLoader(::App, ::AppCollection)
        appLoader.async(this) {
            runOnUiThread {
                appsAdapter.update(it.list)
            }
        }
    }
}