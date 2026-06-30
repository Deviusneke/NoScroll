package com.example.noscroll

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.noscroll.data.AppLimitEntity
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class ConfiguredAppAdapter(
    private val apps: List<AppLimitEntity>,
    private val onRemoveClick: (AppLimitEntity) -> Unit,
    private val onItemClick: (AppLimitEntity) -> Unit
) : RecyclerView.Adapter<ConfiguredAppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.tvAppName)
        val appLimit: TextView = view.findViewById(R.id.tvAppLimit)
        val appIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_configured_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val pm = holder.itemView.context.packageManager
        
        holder.appName.text = app.appName
        
        val limitHours = (app.timeLimitInMillis / (1000 * 60 * 60)).toInt()
        val limitMinutes = ((app.timeLimitInMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val parts = mutableListOf<String>()
        if (limitHours > 0) parts.add("${limitHours}h")
        if (limitMinutes > 0) parts.add("${String.format("%02d", limitMinutes)}m")
        holder.appLimit.text = "Limite: ${parts.joinToString(" ")}"

        try {
            val icon: Drawable = pm.getApplicationIcon(app.packageName)
            holder.appIcon.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            holder.appIcon.setImageResource(R.mipmap.ic_launcher)
        }

        holder.btnDelete.setOnClickListener {
            onRemoveClick(app)
        }

        holder.itemView.setOnClickListener {
            onItemClick(app)
        }
    }

    override fun getItemCount() = apps.size
}
