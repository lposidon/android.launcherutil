package io.posidon.android.launcherutil.demo

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon = itemView.findViewById<ImageView>(R.id.icon)
    val label = itemView.findViewById<TextView>(R.id.label)
}
