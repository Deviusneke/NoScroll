package com.example.noscroll

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noscroll.data.AppLimitEntity
import com.example.noscroll.data.AppUsageRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AplicativosActivity : BaseActivity() {

    private lateinit var rvApps: RecyclerView
    private lateinit var btnAdicionar: MaterialButton
    private lateinit var tvEmptyStatement: TextView
    private lateinit var repository: AppUsageRepository
    private lateinit var adapter: ConfiguredAppAdapter
    private val configuredApps = mutableListOf<AppLimitEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aplicativo)

        repository = AppUsageRepository(this)

        rvApps = findViewById(R.id.rvApps)
        btnAdicionar = findViewById(R.id.btnSelecionar)
        tvEmptyStatement = findViewById(R.id.tvEmptyStatement)

        setupRecyclerView()

        btnAdicionar.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadConfiguredApps()
    }

    private fun setupRecyclerView() {
        adapter = ConfiguredAppAdapter(
            apps = configuredApps,
            onRemoveClick = { app ->
                lifecycleScope.launch {
                    repository.deleteAppLimit(app.packageName)
                    com.example.noscroll.service.AppUsageMonitor.forceUpdate = true
                    loadConfiguredApps()
                }
            },
            onItemClick = { app ->
                val intent = Intent(this, CadastroAplicativo::class.java).apply {
                    putExtra("APP_PACKAGE_NAME", app.packageName)
                    putExtra("APP_NAME", app.appName)
                }
                startActivity(intent)
            }
        )
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = adapter
    }

    private fun loadConfiguredApps() {
        lifecycleScope.launch {
            val limits = repository.getAllAppLimits().filter { it.timeLimitInMillis > 0 }
            configuredApps.clear()
            configuredApps.addAll(limits)

            runOnUiThread {
                adapter.notifyDataSetChanged()
                if (configuredApps.isEmpty()) {
                    tvEmptyStatement.visibility = View.VISIBLE
                    rvApps.visibility = View.GONE
                } else {
                    tvEmptyStatement.visibility = View.GONE
                    rvApps.visibility = View.VISIBLE
                }
            }
        }
    }
}
