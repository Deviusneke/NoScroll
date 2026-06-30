import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.add
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main) // Ou seu setContent {} do Compose

        // 1. Obtenha uma instância do banco de dados
        val db = Firebase.firestore

        // 2. Crie um novo usuário com nome e sobrenome (exemplo)
        val usuario = hashMapOf(
            "primeiro_nome" to "Ada",
            "ultimo_nome" to "Lovelace",
            "nascimento" to 1815
        )

        // 3. Adicione um novo documento com um ID gerado automaticamente
        // na coleção "usuarios"
        db.collection("usuarios")
            .add(usuario)
            .addOnSuccessListener { documentReference ->
                // Sucesso!
                Log.d("Firestore", "Documento adicionado com ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                // Erro!
                Log.w("Firestore", "Erro ao adicionar documento", e)
            }

        // 4. Leia todos os documentos da coleção "usuarios"
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                // Sucesso!
                for (document in result) {
                    Log.d("Firestore", "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                // Erro!
                Log.w("Firestore", "Erro ao buscar documentos.", exception)
            }
    }
}