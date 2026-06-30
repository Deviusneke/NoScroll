package com.example.noscroll

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConfiguracaoActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var cardPerfil: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracao)

        auth = Firebase.auth

        initViews()
        setupListeners()
    }

    private fun initViews() {
        cardPerfil = findViewById(R.id.cardPerfil)
    }

    private fun setupListeners() {
        cardPerfil.setOnClickListener {
            navegarParaEditarPerfil()
        }
    }

    private fun navegarParaEditarPerfil() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, EditarPerfilActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
        }
    }
}