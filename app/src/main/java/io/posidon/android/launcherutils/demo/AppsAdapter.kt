package io.posidon.android.launcherutils.demo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute

class AppsAdapter : RecyclerView.Adapter<AppViewHolder>() {

    private var list = emptyList<App>()

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false))
    }

    override fun onBindViewHolder(holder: AppViewHolder, i: Int) {
        val app = list[i]
        holder.icon.setImageDrawable(null)
        app.icon.compute {
            holder.icon.post {
                holder.icon.setImageDrawable(it)
            }
        }
        holder.label.text = app.label
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        val i = holder.adapterPosition
        if (i != -1)
            list[i].icon.offload()
    }

    fun update(list: List<App>) {
        this.list = list
        notifyDataSetChanged()
    }
}
