package com.example.noscroll

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MetasActivity : BaseActivity() {

    private lateinit var btnAdicionarMeta: FloatingActionButton
    private lateinit var rvFacil: RecyclerView
    private lateinit var rvMedio: RecyclerView
    private lateinit var rvDificil: RecyclerView
    private lateinit var txtEmptyFacil: TextView
    private lateinit var txtEmptyMedio: TextView
    private lateinit var txtEmptyDificil: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val metasFacil = mutableListOf<Meta>()
    private val metasMedio = mutableListOf<Meta>()
    private val metasDificil = mutableListOf<Meta>()

    private lateinit var adapterFacil: MetaAdapter
    private lateinit var adapterMedio: MetaAdapter
    private lateinit var adapterDificil: MetaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metas)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Inicializar views
        initViews()

        // Configurar adapters
        setupAdapters()

        // Configurar listeners
        setupListeners()

        // Carregar metas do Firestore
        carregarMetas()
    }

    private fun initViews() {
        btnAdicionarMeta = findViewById(R.id.btnAdicionarMeta)
        rvFacil = findViewById(R.id.rvFacil)
        rvMedio = findViewById(R.id.rvMedio)
        rvDificil = findViewById(R.id.rvDificil)
        txtEmptyFacil = findViewById(R.id.txtEmptyFacil)
        txtEmptyMedio = findViewById(R.id.txtEmptyMedio)
        txtEmptyDificil = findViewById(R.id.txtEmptyDificil)
    }

    private fun setupAdapters() {
        // Configurar RecyclerViews
        rvFacil.layoutManager = LinearLayoutManager(this)
        rvMedio.layoutManager = LinearLayoutManager(this)
        rvDificil.layoutManager = LinearLayoutManager(this)

        adapterFacil = MetaAdapter(metasFacil) { atualizarMensagensVazias() }
        adapterMedio = MetaAdapter(metasMedio) { atualizarMensagensVazias() }
        adapterDificil = MetaAdapter(metasDificil) { atualizarMensagensVazias() }

        rvFacil.adapter = adapterFacil
        rvMedio.adapter = adapterMedio
        rvDificil.adapter = adapterDificil
    }

    private fun setupListeners() {
        btnAdicionarMeta.setOnClickListener {
            val intent = Intent(this, CriarMetaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun carregarMetas() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Buscar metas do usuário atual
        db.collection("metas")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(this, "Erro ao carregar metas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                // Limpar listas
                metasFacil.clear()
                metasMedio.clear()
                metasDificil.clear()

                // Organizar metas por nível
                snapshot?.documents?.forEach { document ->
                    val meta = Meta(
                        id = document.id,
                        descricao = document.getString("descricao") ?: "",
                        nivel = document.getString("nivel") ?: "",
                        concluida = document.getBoolean("concluida") ?: false,
                        userId = document.getString("userId") ?: ""
                    )

                    when (meta.nivel) {
                        "facil" -> metasFacil.add(meta)
                        "medio" -> metasMedio.add(meta)
                        "dificil" -> metasDificil.add(meta)
                    }
                }

                // Atualizar adapters
                adapterFacil.notifyDataSetChanged()
                adapterMedio.notifyDataSetChanged()
                adapterDificil.notifyDataSetChanged()

                // Mostrar/ocultar mensagens de lista vazia
                atualizarMensagensVazias()
            }
    }

    private fun atualizarMensagensVazias() {
        txtEmptyFacil.visibility = if (metasFacil.isEmpty()) TextView.VISIBLE else TextView.GONE
        txtEmptyMedio.visibility = if (metasMedio.isEmpty()) TextView.VISIBLE else TextView.GONE
        txtEmptyDificil.visibility = if (metasDificil.isEmpty()) TextView.VISIBLE else TextView.GONE
    }

    override fun onResume() {
        super.onResume()
        // Recarregar metas quando a activity voltar ao foco
        carregarMetas()
    }
}