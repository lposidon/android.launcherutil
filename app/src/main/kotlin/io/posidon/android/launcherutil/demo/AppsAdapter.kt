package io.posidon.android.launcherutil.demo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Future

class AppsAdapter(val mainActivity: MainActivity) : RecyclerView.Adapter<AppViewHolder>() {

    private var list = emptyList<App>()

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false))
    }

    private var workers = HashMap<Int, Future<Unit>>()

    override fun onBindViewHolder(holder: AppViewHolder, i: Int) {
        val img = holder.icon
        workers[i]?.cancel(true)
        img.setImageDrawable(null)
        val app = list[i]
        workers[i] = mainActivity.iconLoader.load(
            holder.itemView.context,
            app.packageName,
            app.name,
            app.profile,
        ) {
            img.post {
                img.setImageDrawable(it.icon)
            }
        }
        holder.label.text = app.label
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        workers[holder.adapterPosition]?.cancel(true)
        holder.icon.setImageDrawable(null)
    }

    fun update(list: List<App>) {
        this.list = list
        notifyDataSetChanged()
    }
}
