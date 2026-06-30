package com.example.noscroll

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MetaAdapter(
    private val metas: MutableList<Meta>,
    private val onMetaConcluida: () -> Unit
) : RecyclerView.Adapter<MetaAdapter.MetaViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    class MetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtDescricao: TextView = itemView.findViewById(R.id.txtMetaDescricao)
        val btnConcluir: Button = itemView.findViewById(R.id.btnConcluir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meta, parent, false)
        return MetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        val meta = metas[position]

        // Formatar texto
        holder.txtDescricao.text = meta.descricao

        // Configurar botão concluir
        holder.btnConcluir.text = "Concluir"
        holder.btnConcluir.isEnabled = true
        holder.btnConcluir.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))

        holder.btnConcluir.setOnClickListener {
            concluirMeta(meta.id, position, holder)
        }
    }

    private fun concluirMeta(metaId: String, position: Int, holder: MetaViewHolder) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(
                holder.itemView.context,
                "Usuário não autenticado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val meta = metas[position]
        val xpGanho = when (meta.nivel) {
            "facil" -> 15
            "medio" -> 30
            "dificil" -> 50
            else -> 0
        }

        holder.btnConcluir.isEnabled = false

        db.collection("metas")
            .document(metaId)
            .delete()
            .addOnSuccessListener {
                // Remover da lista local
                if (position >= 0 && position < metas.size) {
                    metas.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, metas.size - position)
                    onMetaConcluida()
                }

                if (xpGanho > 0) {
                    adicionarXPUsuario(userId, xpGanho, holder)
                } else {
                    Toast.makeText(holder.itemView.context, "Meta concluída! 🎉", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                holder.btnConcluir.isEnabled = true
                Toast.makeText(
                    holder.itemView.context,
                    "Erro ao concluir meta: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun adicionarXPUsuario(userId: String, xpGanho: Int, holder: MetaViewHolder) {
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { doc ->
            var currentXp = doc.getLong("xp") ?: 0L
            var currentLevel = doc.getLong("level") ?: 1L

            currentXp += xpGanho

            var xpNext = calcularXpParaProximoNivel(currentLevel)
            var subiuDeNivel = false

            while (currentXp >= xpNext) {
                currentXp -= xpNext
                currentLevel++
                xpNext = calcularXpParaProximoNivel(currentLevel)
                subiuDeNivel = true
            }

            userRef.update(
                "xp", currentXp,
                "level", currentLevel
            ).addOnSuccessListener {
                if (subiuDeNivel) {
                    Toast.makeText(holder.itemView.context, "Meta concluída! 🎉 Parabéns, você avançou para o Nível $currentLevel!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(holder.itemView.context, "Meta concluída! +$xpGanho XP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        fun calcularXpParaProximoNivel(nivelAtual: Long): Long {
            var xp = 100.0
            for (i in 1 until nivelAtual) {
                xp *= 1.15
            }
            return Math.ceil(xp).toLong()
        }
    }

    override fun getItemCount(): Int = metas.size
}