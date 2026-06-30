package com.example.noscroll

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editNome: EditText
    private lateinit var editEmail: EditText
    private lateinit var editSenha: EditText
    private lateinit var editBirthDate: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        auth = Firebase.auth
        db = Firebase.firestore

        initViews()
        setupListeners()
        carregarDadosUsuario()
    }

    private fun initViews() {
        editNome = findViewById(R.id.editNome)
        editEmail = findViewById(R.id.editEmail)
        editSenha = findViewById(R.id.editSenha)
        editBirthDate = findViewById(R.id.editBirthDate)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnCancelar = findViewById(R.id.btnCancelar)
    }

    private fun setupListeners() {
        btnSalvar.setOnClickListener {
            salvarAlteracoesPerfil()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun carregarDadosUsuario() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Carrega email do Firebase Auth
            editEmail.setText(currentUser.email ?: "")

            // Carrega dados do Firestore baseado na sua estrutura
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Usando os campos exatos do seu banco
                        val fullName = document.getString("fullName") ?: ""
                        val birthDate = document.getString("birthDate") ?: ""

                        editNome.setText(fullName)
                        editBirthDate.setText(birthDate)
                    } else {
                        Toast.makeText(this, "Dados do usuário não encontrados", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun salvarAlteracoesPerfil() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }

        val fullName = editNome.text.toString().trim()
        val birthDate = editBirthDate.text.toString().trim()
        val senha = editSenha.text.toString().trim()

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Nome completo é obrigatório", Toast.LENGTH_SHORT).show()
            return
        }

        // Atualiza dados no Firestore (conforme sua estrutura)
        val userData = hashMapOf(
            "fullName" to fullName,
            "birthDate" to birthDate,
            "email" to currentUser.email,
            "userId" to currentUser.uid
        )

        db.collection("users")
            .document(currentUser.uid)
            .set(userData)
            .addOnSuccessListener {
                // Se houver senha para atualizar
                if (senha.isNotEmpty()) {
                    if (senha.length < 6) {
                        Toast.makeText(this, "Senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Atualiza a senha no Firebase Auth
                    currentUser.updatePassword(senha)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Perfil e senha atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Perfil atualizado, mas erro ao alterar senha: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}