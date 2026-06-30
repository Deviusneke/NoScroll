package com.example.noscroll

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class CriarMetaActivity : BaseActivity() {

    private lateinit var etDescricaoMeta: TextInputEditText
    private lateinit var radioGroupNivel: RadioGroup
    private lateinit var btnCriarMeta: MaterialButton
    private lateinit var btnVoltar: MaterialButton

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_meta)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Inicializar views
        initViews()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        etDescricaoMeta = findViewById(R.id.etDescricaoMeta)
        radioGroupNivel = findViewById(R.id.radioGroupNivel)
        btnCriarMeta = findViewById(R.id.btnCriarMeta)
        btnVoltar = findViewById(R.id.btnVoltar)

        // Selecionar nível médio por padrão
        val radioMedio = findViewById<RadioButton>(R.id.radioMedio)
        radioMedio.isChecked = true
    }

    private fun setupListeners() {
        btnCriarMeta.setOnClickListener {
            criarMeta()
        }

        btnVoltar.setOnClickListener {
            voltarParaMetas()
        }
    }

    private fun criarMeta() {
        val descricao = etDescricaoMeta.text.toString().trim()
        val nivel = obterNivelSelecionado()

        // Validações
        if (descricao.isEmpty()) {
            Toast.makeText(this, "Descreva sua meta", Toast.LENGTH_SHORT).show()
            return
        }

        if (nivel.isEmpty()) {
            Toast.makeText(this, "Selecione um nível de dificuldade", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar se usuário está logado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mostrar loading
        btnCriarMeta.isEnabled = false
        btnCriarMeta.text = "Criando..."

        // Criar objeto da meta
        val meta = hashMapOf(
            "descricao" to descricao,
            "nivel" to nivel,
            "userId" to currentUser.uid,
            "dataCriacao" to Calendar.getInstance().time,
            "concluida" to false
        )

        // Salvar no Firestore
        db.collection("metas")
            .add(meta)
            .addOnSuccessListener { documentReference ->
                btnCriarMeta.isEnabled = true
                btnCriarMeta.text = "Criar Meta"

                Toast.makeText(this, "Meta criada com sucesso!", Toast.LENGTH_SHORT).show()

                // Voltar para MetasActivity após criar a meta
                voltarParaMetas()
            }
            .addOnFailureListener { exception ->
                btnCriarMeta.isEnabled = true
                btnCriarMeta.text = "Criar Meta"

                Toast.makeText(
                    this,
                    "Erro ao criar meta: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun obterNivelSelecionado(): String {
        return when (radioGroupNivel.checkedRadioButtonId) {
            R.id.radioFacil -> "facil"
            R.id.radioMedio -> "medio"
            R.id.radioDificil -> "dificil"
            else -> ""
        }
    }

    private fun voltarParaMetas() {
        val intent = Intent(this, MetasActivity::class.java)
        startActivity(intent)
        finish() // Fecha a activity atual para não voltar com back button
    }

    // Opcional: Sobrescrever o botão físico voltar também
    override fun onBackPressed() {
        voltarParaMetas()
    }

    override fun onResume() {
        super.onResume()
        // Reativar botão se a activity voltar ao foco
        btnCriarMeta.isEnabled = true
        btnCriarMeta.text = "Criar Meta"
    }
}