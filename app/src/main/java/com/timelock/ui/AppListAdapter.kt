package com.timelock.ui

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.timelock.R

class AppListAdapter(
    private val packageManager: PackageManager,
    private val allApps: List<ResolveInfo>
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>(), android.widget.Filterable {

    private var displayedApps: List<ResolveInfo> = allApps.sortedBy { it.loadLabel(packageManager).toString() }
    private val selectedPackages = mutableSetOf<String>()
    // Map to store duration for each package: PackageName -> Duration String
    private val appDurations = mutableMapOf<String, String>()
    private var isIndividualMode: Boolean = false

    init {
        // Initial sort
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val etAppDuration: EditText = view.findViewById(R.id.etAppDuration)
        val cbSelected: CheckBox = view.findViewById(R.id.cbSelected)
    }

    fun setTimerMode(individual: Boolean) {
        isIndividualMode = individual
        notifyDataSetChanged()
    }
    
    fun getAppDurations(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for ((pkg, durStr) in appDurations) {
            val dur = durStr.toLongOrNull()
            if (dur != null && dur > 0 && selectedPackages.contains(pkg)) {
                result[pkg] = dur
            }
        }
        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = displayedApps[position]
        val packageName = app.activityInfo.packageName
        
        holder.tvAppName.text = app.loadLabel(packageManager)
        holder.ivIcon.setImageDrawable(app.loadIcon(packageManager))
        
        // Remove listener before changing state
        holder.cbSelected.setOnCheckedChangeListener(null)
        holder.cbSelected.isChecked = selectedPackages.contains(packageName)
        
        // Handle Duration Input Visibility
        if (isIndividualMode && selectedPackages.contains(packageName)) {
            holder.etAppDuration.visibility = View.VISIBLE
        } else {
            holder.etAppDuration.visibility = View.GONE
        }
        
        // Handle Text Persistence
        // MUST remove text watcher before setting text
        if (holder.etAppDuration.tag is TextWatcher) {
            holder.etAppDuration.removeTextChangedListener(holder.etAppDuration.tag as TextWatcher)
        }
        
        holder.etAppDuration.setText(appDurations[packageName] ?: "")
        
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                appDurations[packageName] = s.toString()
            }
        }
        holder.etAppDuration.addTextChangedListener(watcher)
        holder.etAppDuration.tag = watcher

        holder.cbSelected.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPackages.add(packageName)
                if (isIndividualMode) holder.etAppDuration.visibility = View.VISIBLE
            } else {
                selectedPackages.remove(packageName)
                holder.etAppDuration.visibility = View.GONE
            }
        }
        
        holder.itemView.setOnClickListener {
            holder.cbSelected.toggle()
        }
    }

    override fun getItemCount(): Int = displayedApps.size

    fun getSelectedPackages(): List<String> {
        return selectedPackages.toList()
    }

    override fun getFilter(): android.widget.Filter {
        return object : android.widget.Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                displayedApps = if (charString.isEmpty()) {
                    allApps.sortedBy { it.loadLabel(packageManager).toString() }
                } else {
                    allApps.filter {
                        val label = it.loadLabel(packageManager).toString()
                        label.contains(charString, true)
                    }.sortedBy { it.loadLabel(packageManager).toString() }
                }
                val filterResults = FilterResults()
                filterResults.values = displayedApps
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                displayedApps = results?.values as List<ResolveInfo>
                notifyDataSetChanged()
            }
        }
    }
}
