package com.example.noscroll

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.noscroll.service.AppUsageMonitor
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.noscroll.data.AppUsageRepository
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ActivityInicio : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnMetas: MaterialCardView
    private lateinit var btnAplicativos: MaterialCardView
    private lateinit var btnConfiguracao: MaterialCardView

    private lateinit var metaFacil: TextView
    private lateinit var metaMedio: TextView
    private lateinit var metaDificil: TextView

    private lateinit var tvLevel: TextView
    private lateinit var barraXP: android.widget.ProgressBar
    private lateinit var tvXpTexto: TextView

    private lateinit var graficoProgresso: LineChart
    private val appUsageRepository by lazy { AppUsageRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // VERIFICAÇÃO DE AUTENTICAÇÃO
        auth = Firebase.auth
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_inicio)

        db = Firebase.firestore

        initViews()
        setupListeners()
        carregarPrimeirasMetas()
        carregarDadosUsuario()
        carregarGraficoSemanal()

        // Iniciar serviço de monitoramento de foreground
        startAppMonitorService()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        btnLogout = findViewById(R.id.btnLogout)
        btnMetas = findViewById(R.id.btnMetas)
        btnAplicativos = findViewById(R.id.btnAplicativos)
        btnConfiguracao = findViewById(R.id.btnConfiguracao)
        metaFacil = findViewById(R.id.metaFacil)
        metaMedio = findViewById(R.id.metaMedio)
        metaDificil = findViewById(R.id.metaDificil)
        
        tvLevel = findViewById(R.id.tvLevel)
        barraXP = findViewById(R.id.barraXP)
        tvXpTexto = findViewById(R.id.tvXpTexto)
        
        graficoProgresso = findViewById(R.id.graficoProgresso)
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            logoutUser()
        }

        btnMetas.setOnClickListener {
            val intent = Intent(this, MetasActivity::class.java)
            startActivity(intent)
        }

        btnAplicativos.setOnClickListener {
            startActivity(Intent(this, AplicativosActivity::class.java))
        }

        btnConfiguracao.setOnClickListener {
            val intent = Intent(this, ConfiguracaoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun carregarPrimeirasMetas() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("metas")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(this, "Erro ao carregar metas: ${exception.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val metasFacil = mutableListOf<String>()
                val metasMedio = mutableListOf<String>()
                val metasDificil = mutableListOf<String>()

                snapshot?.documents?.forEach { document ->
                    val descricao = document.getString("descricao") ?: ""
                    val nivel = document.getString("nivel") ?: ""
                    val concluida = document.getBoolean("concluida") ?: false

                    if (!concluida) {
                        when (nivel) {
                            "facil" -> metasFacil.add(descricao)
                            "medio" -> metasMedio.add(descricao)
                            "dificil" -> metasDificil.add(descricao)
                        }
                    }
                }

                val primeiraFacil = if (metasFacil.isNotEmpty()) metasFacil[0] else "Nenhuma meta fácil"
                val primeiraMedia = if (metasMedio.isNotEmpty()) metasMedio[0] else "Nenhuma meta média"
                val primeiraDificil = if (metasDificil.isNotEmpty()) metasDificil[0] else "Nenhuma meta difícil"

                runOnUiThread {
                    metaFacil.text = primeiraFacil
                    metaMedio.text = primeiraMedia
                    metaDificil.text = primeiraDificil
                }
            }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logout realizado!", Toast.LENGTH_SHORT).show()
        redirectToLogin()
    }

    private fun carregarDadosUsuario() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid)
            .addSnapshotListener { document, exception ->
                if (exception != null || document == null) return@addSnapshotListener
                
                val xp = document.getLong("xp") ?: 0L
                val level = document.getLong("level") ?: 1L
                
                val xpNext = MetaAdapter.calcularXpParaProximoNivel(level)
                
                runOnUiThread {
                    tvLevel.text = "Nível $level"
                    barraXP.max = xpNext.toInt()
                    barraXP.progress = xp.toInt()
                    tvXpTexto.text = "$xp/$xpNext XP"
                }
            }
    }

    private fun carregarGraficoSemanal() {
        lifecycleScope.launch {
            val dadosDaSemana = appUsageRepository.getWeeklyUsage()
            val entries = mutableListOf<Entry>()
            val rotulos = mutableListOf<String>()
            
            dadosDaSemana.forEachIndexed { index, pair ->
                // pair.second é em milissegundos, converter para horas
                val horasUsadas = pair.second / (1000f * 60f * 60f)
                entries.add(Entry(index.toFloat(), horasUsadas))
                rotulos.add(pair.first)
            }

            val dataSet = LineDataSet(entries, "Horas Usadas")
            dataSet.color = Color.parseColor("#BB86FC") // Roxo Claro
            dataSet.valueTextColor = Color.WHITE
            dataSet.valueTextSize = 10f
            dataSet.lineWidth = 3f
            dataSet.setCircleColor(Color.parseColor("#BB86FC"))
            dataSet.circleRadius = 5f
            dataSet.setDrawCircleHole(false)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.setDrawFilled(true)
            dataSet.fillColor = Color.parseColor("#6200EE") // Roxo Escuro
            dataSet.fillAlpha = 60

            val lineData = LineData(dataSet)
            graficoProgresso.data = lineData

            graficoProgresso.description.isEnabled = false
            graficoProgresso.legend.textColor = Color.WHITE
            
            val xAxis = graficoProgresso.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.WHITE
            xAxis.valueFormatter = IndexAxisValueFormatter(rotulos)
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true

            val yAxisLeft = graficoProgresso.axisLeft
            yAxisLeft.textColor = Color.WHITE
            yAxisLeft.setDrawGridLines(false)
            yAxisLeft.axisMinimum = 0f

            graficoProgresso.axisRight.isEnabled = false
            graficoProgresso.animateX(1000)
            graficoProgresso.invalidate()
        }
    }

    private fun startAppMonitorService() {
        val serviceIntent = Intent(this, AppUsageMonitor::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }
        carregarPrimeirasMetas()
        carregarGraficoSemanal()
    }
}