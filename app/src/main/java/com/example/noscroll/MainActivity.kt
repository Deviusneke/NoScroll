package com.example.noscroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.noscroll.service.AppUsageMonitor
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var btnLogin: MaterialButton
    private lateinit var btnCadastrese: MaterialTextView
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST = 101
        private const val NOTIFICATION_PERMISSION_REQUEST = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        checkCurrentUser()
        initViews()
        setupListeners()
        checkAndRequestPermissions()

        // Iniciar APENAS um serviço de monitoramento
        startAppMonitorService()
    }

    private fun initViews() {
        btnLogin = findViewById(R.id.btnLogin)
        btnCadastrese = findViewById(R.id.btnCadastrese)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            loginUser()
        }

        btnCadastrese.setOnClickListener {
            val intent = Intent(this, CadastroUsuario::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        btnLogin.isEnabled = false
        btnLogin.text = "Entrando..."

        val intent = Intent(this, ActivityInicio::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, ActivityInicio::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkAndRequestPermissions() {
        // Permissão de uso de dados
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        // Permissão de sobreposição de tela
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            }
        }

        // Permissão de notificação (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission()
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        try {
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            return false
        }
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(
            this,
            "Por favor, conceda permissão de uso de aplicativos para o monitoramento funcionar",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        Toast.makeText(
            this,
            "Permissão de sobreposição necessária para mostrar o bloqueio",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST
        )
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
        btnLogin.isEnabled = true
        btnLogin.text = "Entrar"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Permissão de sobreposição concedida!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Permissão de sobreposição necessária para o bloqueio", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notificações permitidas!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}