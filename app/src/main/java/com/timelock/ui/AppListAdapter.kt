package com.timelock.ui

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.timelock.R

class AppListAdapter(
    private val packageManager: PackageManager,
    private val apps: List<ResolveInfo>
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val selectedPackages = mutableSetOf<String>()

    init {
        // By default, maybe select none
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val cbSelected: CheckBox = view.findViewById(R.id.cbSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val packageName = app.activityInfo.packageName
        
        holder.tvAppName.text = app.loadLabel(packageManager)
        holder.ivIcon.setImageDrawable(app.loadIcon(packageManager))
        
        holder.cbSelected.setOnCheckedChangeListener(null) // Avoid recycling issues
        holder.cbSelected.isChecked = selectedPackages.contains(packageName)
        
        holder.cbSelected.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPackages.add(packageName)
            } else {
                selectedPackages.remove(packageName)
            }
        }
        
        holder.itemView.setOnClickListener {
            holder.cbSelected.toggle()
        }
    }

    override fun getItemCount(): Int = apps.size

    fun getSelectedPackages(): List<String> {
        return selectedPackages.toList()
    }
}
