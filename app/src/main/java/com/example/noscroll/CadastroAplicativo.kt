package com.example.noscroll

import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.noscroll.data.AppUsageRepository
import com.example.noscroll.helper.UsageStatsManagerHelper
import kotlinx.coroutines.launch

class CadastroAplicativo : BaseActivity() {

    private lateinit var pickerHours: NumberPicker
    private lateinit var pickerMinutes: NumberPicker
    private lateinit var tvTimeLimit: TextView
    private lateinit var tvAppName: TextView
    private lateinit var tvCurrentUsage: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var usageHelper: UsageStatsManagerHelper
    private lateinit var repository: AppUsageRepository

    private var packageName: String = ""
    private var appName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadatrosapp)

        usageHelper = UsageStatsManagerHelper(this)
        repository = AppUsageRepository(this)

        packageName = intent.getStringExtra("APP_PACKAGE_NAME") ?: ""
        appName = intent.getStringExtra("APP_NAME") ?: packageName

        initViews()
        checkUsageStatsPermission()
        loadCurrentUsage()
        loadSavedLimit()
        setupListeners()
    }

    private fun initViews() {
        pickerHours = findViewById(R.id.pickerHours)
        pickerMinutes = findViewById(R.id.pickerMinutes)
        tvTimeLimit = findViewById(R.id.tvTimeLimit)
        tvAppName = findViewById(R.id.tv_app_name)
        tvCurrentUsage = findViewById(R.id.tvCurrentUsage)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        tvAppName.text = "Configurando limite para: $appName"

        // Horas: 0 a 23
        pickerHours.minValue = 0
        pickerHours.maxValue = 23
        pickerHours.value = 0

        // Minutos: 0 a 59, exibindo com zero à esquerda (00, 05, 10...)
        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59
        pickerMinutes.displayedValues = Array(60) { i -> String.format("%02d", i) }
        pickerMinutes.value = 0

        updateTimeLabel(0, 0)
    }

    private fun checkUsageStatsPermission() {
        if (!usageHelper.hasUsageStatsPermission()) {
            Toast.makeText(
                this,
                "Por favor, conceda permissão de uso para monitorar os apps",
                Toast.LENGTH_LONG
            ).show()
            usageHelper.requestUsageStatsPermission()
        }
    }

    private fun loadCurrentUsage() {
        lifecycleScope.launch {
            val timeUsed = repository.getTodayUsage(packageName)
            val hours = timeUsed / (1000 * 60 * 60)
            val minutes = (timeUsed % (1000 * 60 * 60)) / (1000 * 60)
            tvCurrentUsage.text = "Uso hoje: ${hours}h ${String.format("%02d", minutes)}m"
        }
    }

    private fun loadSavedLimit() {
        lifecycleScope.launch {
            val limitMillis = repository.getAppLimit(packageName)
            if (limitMillis > 0) {
                val limitHours = (limitMillis / (1000 * 60 * 60)).toInt()
                val limitMinutes = ((limitMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                pickerHours.value = limitHours
                pickerMinutes.value = limitMinutes
                updateTimeLabel(limitHours, limitMinutes)
            } else {
                updateTimeLabel(0, 0)
            }
        }
    }

    private fun setupListeners() {
        val changeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            updateTimeLabel(pickerHours.value, pickerMinutes.value)
        }
        pickerHours.setOnValueChangedListener(changeListener)
        pickerMinutes.setOnValueChangedListener(changeListener)

        btnSave.setOnClickListener { saveTimeLimit() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun updateTimeLabel(hours: Int, minutes: Int) {
        if (hours == 0 && minutes == 0) {
            tvTimeLimit.text = "Sem limite"
        } else {
            val parts = mutableListOf<String>()
            if (hours > 0) parts.add("${hours}h")
            if (minutes > 0) parts.add("${String.format("%02d", minutes)}m")
            tvTimeLimit.text = "Limite: ${parts.joinToString(" ")}"
        }
    }

    private fun saveTimeLimit() {
        val selectedHours = pickerHours.value
        val selectedMinutes = pickerMinutes.value
        val totalMillis = (selectedHours * 60L * 60 * 1000) + (selectedMinutes * 60L * 1000)

        lifecycleScope.launch {
            if (totalMillis > 0) {
                repository.saveAppLimit(packageName, appName, totalMillis)

                val parts = mutableListOf<String>()
                if (selectedHours > 0) parts.add("${selectedHours}h")
                if (selectedMinutes > 0) parts.add("${String.format("%02d", selectedMinutes)}m")
                val timeStr = parts.joinToString(" ")

                Toast.makeText(
                    this@CadastroAplicativo,
                    "Limite de $timeStr definido para $appName",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                repository.deleteAppLimit(packageName)
                Toast.makeText(
                    this@CadastroAplicativo,
                    "Limite removido para $appName",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Notificar o serviço para atualizar o cache imediatamente
            com.example.noscroll.service.AppUsageMonitor.forceUpdate = true
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (usageHelper.hasUsageStatsPermission()) {
            loadCurrentUsage()
        }
    }
}