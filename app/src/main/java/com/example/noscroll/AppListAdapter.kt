package com.example.noscroll

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private var appList: List<AppInfo>,
    private val onAppSelected: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private var filteredList = appList.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredList[position]
        holder.bind(app)
    }

    override fun getItemCount() = filteredList.size

    fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(appList)
        } else {
            val lowerCaseQuery = query.lowercase()
            filteredList.addAll(appList.filter { app ->
                app.name.lowercase().contains(lowerCaseQuery) ||
                        app.packageName.lowercase().contains(lowerCaseQuery)
            })
        }
        notifyDataSetChanged()
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAppIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
        private val txtAppName: TextView = itemView.findViewById(R.id.txtAppName)
        private val txtPackageName: TextView = itemView.findViewById(R.id.txtPackageName)

        fun bind(app: AppInfo) {
            txtAppName.text = app.name
            txtPackageName.text = app.packageName
            imgAppIcon.setImageDrawable(app.icon)

            itemView.setOnClickListener {
                onAppSelected(app, true)
            }
        }
    }
}