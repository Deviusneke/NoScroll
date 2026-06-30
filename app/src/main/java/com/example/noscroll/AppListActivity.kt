package com.example.noscroll

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : BaseActivity() {

    private lateinit var recyclerViewApps: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: AppListAdapter

    private val installedApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        initViews()
        loadInstalledApps()
        setupRecyclerView()
        setupSearchView()
    }

    private fun initViews() {
        recyclerViewApps = findViewById(R.id.recyclerViewApps)
        searchView = findViewById(R.id.searchView)
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        installedApps.clear()
        installedApps.addAll(
            packages
                .filter { appInfo ->
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } // Filtra apps do sistema
                .map { packageInfo ->
                    AppInfo(
                        packageName = packageInfo.packageName,
                        name = pm.getApplicationLabel(packageInfo).toString(),
                        icon = pm.getApplicationIcon(packageInfo)
                    )
                }
                .sortedBy { it.name.lowercase() } // Ordena por nome
        )
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(installedApps) { app, _ ->
            val intent = Intent(this, CadastroAplicativo::class.java).apply {
                putExtra("APP_PACKAGE_NAME", app.packageName)
                putExtra("APP_NAME", app.name)
            }
            startActivity(intent)
            finish()
        }

        recyclerViewApps.layoutManager = LinearLayoutManager(this)
        recyclerViewApps.adapter = adapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
    }
}