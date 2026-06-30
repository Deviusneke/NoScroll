package com.example.noscroll

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class CadastroUsuario : BaseActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnRegister: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Inicializar views
        initViews()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etBirthDate = findViewById(R.id.etBirthDate)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
    }

    private fun setupListeners() {
        // Listener para data de nascimento
        etBirthDate.setOnClickListener {
            showDatePicker()
        }

        // Listener para botão de cadastro
        btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val birthDate = etBirthDate.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validações básicas
        if (fullName.isEmpty() || birthDate.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loading
        btnRegister.isEnabled = false
        btnRegister.text = "Cadastrando..."

        // Criar usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Usuário criado com sucesso no Auth, agora salvar dados no Firestore
                    val userId = auth.currentUser?.uid
                    saveUserData(userId, fullName, birthDate, email)
                } else {
                    // Erro ao criar usuário
                    btnRegister.isEnabled = true
                    btnRegister.text = "Cadastrar"
                    Toast.makeText(
                        this,
                        "Erro no cadastro: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserData(userId: String?, fullName: String, birthDate: String, email: String) {
        if (userId == null) {
            Toast.makeText(this, "Erro: ID do usuário não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Criar objeto do usuário
        val user = hashMapOf(
            "fullName" to fullName,
            "birthDate" to birthDate,
            "email" to email,
            "createdAt" to Calendar.getInstance().time,
            "userId" to userId,
            "level" to 1,
            "xp" to 0
        )

        // Salvar no Firestore na coleção "users"
        db.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                // Sucesso!
                btnRegister.isEnabled = true
                btnRegister.text = "Cadastrar"
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                // Limpar campos ou ir para próxima tela
                clearFields()
                // finish() // Se quiser fechar a activity
            }
            .addOnFailureListener { exception ->
                // Erro ao salvar no Firestore
                btnRegister.isEnabled = true
                btnRegister.text = "Cadastrar"
                Toast.makeText(
                    this,
                    "Erro ao salvar dados: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun clearFields() {
        etFullName.text?.clear()
        etBirthDate.text?.clear()
        etEmail.text?.clear()
        etPassword.text?.clear()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate =
                    String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                etBirthDate.setText(selectedDate)
            },
            year,
            month,
            day
        )

        // Definir data máxima como hoje
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }
}