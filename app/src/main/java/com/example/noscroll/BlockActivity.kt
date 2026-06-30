package com.example.noscroll

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class BlockActivity : Activity() {

    private lateinit var tvMessage: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnClose: Button

    private var packageName: String = ""
    private var appName: String = ""
    private var isReopen: Boolean = false

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_IS_REOPEN = "is_reopen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        // Configurar para aparecer sobre outros apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Este aplicativo"
        isReopen = intent.getBooleanExtra(EXTRA_IS_REOPEN, false)

        initViews()
        setupBlockScreen()
        setupButton()
    }

    private fun initViews() {
        tvMessage = findViewById(R.id.tvBlockMessage)
        tvTimer = findViewById(R.id.tvBlockTimer)
        btnClose = findViewById(R.id.btnCloseBlock)
    }

    private fun setupBlockScreen() {
        val message = """
            ⏰ LIMITE DE TEMPO EXCEDIDO!
            
            $appName já atingiu o limite diário de uso.
            
            ❌ O aplicativo foi bloqueado ❌
            
            Você poderá usar novamente amanhã.
            
            Respeite seu tempo de uso!
        """.trimIndent()

        tvMessage.text = message
        tvTimer.text = "App bloqueado até meia-noite"
        btnClose.text = "OK"
        btnClose.isEnabled = true
    }

    private fun setupButton() {
        btnClose.setOnClickListener {
            // Em vez de apenas finalizar, vamos para a tela inicial (Home) do Android
            // Isso força o app bloqueado a ir para segundo plano (simulando que foi "fechado")
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)

            // Aguarda um instante para o app ir pro segundo plano e entao o mata
            Handler(Looper.getMainLooper()).postDelayed({
                forceCloseBlockedApp()
                finishAffinity()
            }, 300)
        }
    }

    private fun forceCloseBlockedApp() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activityManager.killBackgroundProcesses(packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        // Impedir voltar - o botão OK é a única saída
        // Não fazer nada
    }
}