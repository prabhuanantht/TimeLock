package com.timelock.ui

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.timelock.R

class KioskAdapter(
    private val context: Context,
    private val packageManager: PackageManager,
    private val appPackages: List<String>,
    private val onAppClick: (String) -> Unit
) : RecyclerView.Adapter<KioskAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(android.R.id.icon)
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // We can reuse a simple list item layout or create a grid one. 
        // Let's use a standard grid item or similar. 
        // For simplicity reusing item_app but modifying binding or creating new simple layout.
        // Actually, let's just inflate a standard android simple_list_item_1 equivalent but custom.
        // Let's create a quick layout in code or cleaner: use a new layout file `item_kiosk_app.xml`.
        // Since I can't create multiple files in one step easily without overhead, I'll use `item_app.xml` 
        // but hide the checkbox programmatically if I can, or just ignore it.
        // `item_app.xml` has a checkbox.
        // Let's create `item_kiosk_app.xml` properly.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kiosk_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packageName = appPackages[position]
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            holder.text.text = packageManager.getApplicationLabel(appInfo)
            holder.icon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            
            holder.itemView.setOnClickListener {
                onAppClick(packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            holder.text.text = packageName
        }
    }

    override fun getItemCount(): Int = appPackages.size
}
