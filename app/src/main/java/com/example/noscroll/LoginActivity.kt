package com.example.noscroll

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        initViews()
        setupClickListeners()

        // Verificar se já está logado e redirecionar
        checkIfUserIsLoggedIn()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.btnCadastrese).setOnClickListener {
            val intent = Intent(this, CadastroUsuario::class.java)
            startActivity(intent)
        }
    }

    private fun checkIfUserIsLoggedIn() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuário já está logado, ir para tela principal
            navigateToMainScreen()
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateInputs(email, password)) {
            return
        }

        loginWithFirebase(email, password)
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        // Validação do email
        if (email.isEmpty()) {
            etEmail.error = "Email é obrigatório"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            isValid = false
        } else {
            etEmail.error = null
        }

        // Validação da senha
        if (password.isEmpty()) {
            etPassword.error = "Senha é obrigatória"
            isValid = false
        } else if (password.length < 6) {
            etPassword.error = "Senha deve ter pelo menos 6 caracteres"
            isValid = false
        } else {
            etPassword.error = null
        }

        return isValid
    }

    private fun loginWithFirebase(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login bem-sucedido no Firebase Auth
                    val user = auth.currentUser
                    if (user != null) {
                        // Verificar se usuário existe no Firestore também
                        verifyUserInFirestore(user.uid)
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Login falhou
                    showLoading(false)
                    handleLoginError(task.exception)
                }
            }
    }

    private fun verifyUserInFirestore(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    // Usuário existe no Firestore, login completo
                    Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    navigateToMainScreen()
                } else {
                    // Usuário não existe no Firestore (cadastro incompleto)
                    Toast.makeText(this, "Usuário não encontrado no sistema", Toast.LENGTH_LONG).show()
                    auth.signOut() // Faz logout pois o cadastro está incompleto
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Erro ao verificar usuário: ${exception.message}", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    private fun handleLoginError(exception: Exception?) {
        val error = exception?.message ?: "Erro desconhecido"

        when {
            error.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                    error.contains("invalid-credential", ignoreCase = true) -> {
                Toast.makeText(this, "Email ou senha incorretos", Toast.LENGTH_LONG).show()
                etPassword.error = "Senha incorreta"
            }
            error.contains("invalid-email", ignoreCase = true) -> {
                etEmail.error = "Email inválido"
            }
            error.contains("user-not-found", ignoreCase = true) -> {
                Toast.makeText(this, "Nenhuma conta encontrada com este email", Toast.LENGTH_LONG).show()
                etEmail.error = "Usuário não encontrado"
            }
            error.contains("wrong-password", ignoreCase = true) -> {
                Toast.makeText(this, "Senha incorreta", Toast.LENGTH_LONG).show()
                etPassword.error = "Senha incorreta"
            }
            error.contains("too-many-requests", ignoreCase = true) -> {
                Toast.makeText(this, "Muitas tentativas. Tente novamente mais tarde.", Toast.LENGTH_LONG).show()
            }
            error.contains("network", ignoreCase = true) -> {
                Toast.makeText(this, "Erro de conexão. Verifique sua internet.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Erro no login: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, ActivityInicio::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        btnLogin.isEnabled = !show
        btnLogin.text = if (show) "Validando..." else "Entrar"
    }
}