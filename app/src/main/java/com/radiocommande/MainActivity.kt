package com.radiocommande
// RadioCommande2
import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import androidx.core.graphics.toColorInt

//import android.content.Context
//CHEMIN_AUDIO=$(locate -i -d /home/enjoy/musique.db "*blood*" | sed -n "${OCCURENCE}p")
//echo "Le fichier sélectionné est : $CHEMIN_AUDIO"
//mpv --no-video "$CHEMIN_AUDIO"

// mot cles reserves (a ne pas utiliser dans le dictionnaire
// CHERCHE -- SUIVANT -- PRECEDENT - QUITTER -- PLAYLIST
private val dictionnaireCommandes = mapOf(
    "stop"    to "pkill mpv",
    "quitter"    to "pkill mpv",
    "suivant" to """echo '{ "command": ["playlist-next"] }' | socat - /tmp/mpv-socket > /dev/null 2>&1 &""",
    "précédent" to """echo '{"command": ["playlist-prev"] }' | socat - /tmp/mpv-socket > /dev/null 2>&1 &""",
    "culture"    to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/franceculture/franceculture_hifi.m3u8 > /dev/null 2>&1 &",
    "fip culte" to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/fipcultes/fipcultes_hifi.m3u8 > /dev/null 2>&1 &",
    "france inter" to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/franceinter/franceinter_hifi.m3u8 > /dev/null 2>&1 &",
    "france musique" to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/francemusique/francemusique_hifi.m3u8 > /dev/null 2>&1 &",
    "radio 50" to "pkill mpv ; nohup mpv https://stream.radio5050.com/hls/live.m3u8 > /dev/null 2>&1 &",
    "radio fip" to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/fip/fip_hifi.m3u8 > /dev/null 2>&1 &",
    "radio catho" to "pkill mpv ; nohup mpv https://liveradiokto.akamaized.net/hls/live/20000054/ktoradio/02.m3u8> /dev/null 2>&1 &",

    "dylan" to "pkill mpv ; nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket '/home/enjoy/Musique//Bob Dylan/' > /dev/null 2>&1 &",
    "douce" to "nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket /home/enjoy/Musique/sweet/ > /dev/null 2>&1 &",
    "tahiti" to "pkill mpv ; nohup mpv --shuffle  --input-ipc-server=/tmp/mpv-socket '/home/enjoy/Musique/Chants tahitiens traditionnels/' > /dev/null 2>&1 &",
    "stevens" to "pkill mpv ; nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket '/home/enjoy/Musique/Cat Stevens/' > /dev/null 2>&1 &",
    "tous" to "pkill mpv ; nohup pki > /dev/null 2>&1 &"
)

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REPERTOIRE_MUSIQUE = "/home/enjoy/Musique/"
    }
    private lateinit var tvConsole: TextView
    private lateinit var pulseView: View
    private lateinit var tts: TextToSpeech
    private var isListening = false

    // Gestion du retour de la reconnaissance vocale
    private val getSpeechInput = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        pulseView.visibility = View.GONE // Arrête l'animation visuelle
        
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val texteEntendu = results?.get(0)?.lowercase() ?: ""
            
            updateConsole("Micro : \"$texteEntendu\"")
            interpreterEtEnvoyer(texteEntendu)
        }
    }
//---------------------------------------------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        tvConsole = findViewById(R.id.tvConsole)
        pulseView = findViewById(R.id.pulseView1)
        val btnMic: FloatingActionButton = findViewById(R.id.btnMic)
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // 1. Vérification des permissions au démarrage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }

        // 2. Initialisation du Text-to-Speech (Le téléphone parle)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                //tts.language = Locale.FRENCH
                val result = tts.setLanguage(Locale.FRENCH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    updateConsole("Erreur : Langue française non supportée.")
                }
            }
        }
        // 3. Action du bouton
        btnMic.setOnClickListener {
            lancerEcouteVocale()
        }
    }

//-------------------------------      Lancer l'ecoute vocale      ---------------------------------------
    private fun lancerEcouteVocale() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites une commande...")
        }
        try {
            isListening = true
            startPulseAnimation()
            getSpeechInput.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur micro : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
   
//-------------------------------      Commande a envoyer au serveur      ---------------------------------------

    private fun interpreterEtEnvoyer(texte: String) {
    var commandeAExecuter: String? = null

    // Cherche dans le dictionnaire si un mot-clé correspond
    for ((cle, commande) in dictionnaireCommandes) {
        if (texte.contains(cle)) {
            commandeAExecuter = commande
            break
        }
    }
    if (commandeAExecuter != null) {
        // Commande fixe trouvée dans le dictionnaire
        android.util.Log.d("SSH_COMMAND", "MainActivity 135 : $commandeAExecuter")
        SSHManager.executerCommandeSSH(this, commandeAExecuter) { reussite, nomFichier ->
            if (reussite) {
                updateConsole("Commande envoyée : ${nomFichier ?: "OK"}")
                android.util.Log.d("SSH_COMMAND", "MainActivity 139 : ${nomFichier ?: "OK"}")
            } else {
                updateConsole("Erreur d'exécution SSH !")
                android.util.Log.d("SSH_COMMAND", "MainActivity 142 : ${nomFichier ?: "raté"}")
            }
        }
        
    } else if (texte.contains("cherche") || texte.contains("look for")) {
        // Cas dynamique : recherche de fichier
        val mot = texte.replace("cherche ", "").trim()
        if (mot.isNotEmpty()) {
            updateConsole("Je cherche : '$mot'")
            chercherFichierSurServeur(mot)
        } else {
            parler("Je n'ai pas compris ce que vous voulez chercher.")
        }
    } else if (texte.contains("playlist")) {
        // Cas dynamique : lecture d'un répertoire
        val mot = texte.replace("playlist ", "").trim()
        if (mot.isNotEmpty()) {
            updateConsole("Recherche playlist : '$mot'")
            chercherRepertoireSurServeur(mot)
        } else {
            parler("Je n'ai pas compris le nom de la playlist.")
        }
    } else {
        // Commande inconnue
        parler("Désolé, cette commande n'est pas dans mon dictionnaire.")
        updateConsole("Commande inconnue : '$texte'")
    }
}

//-------------------------------      Recherche d'un morceau sur le serveur      ---------------------------------------
   private fun chercherFichierSurServeur(motCle: String) {
    //--1- Recherche du mot clé
    val cmdRecherche = """pkill mpv; locate -i -d /home/enjoy/musique.db "*${motCle}*""""
    SSHManager.executerCommandeSSH(this, cmdRecherche) { reussite, resultat ->
        //--2- Si recherche positive, lancement de mpv
        android.util.Log.d("SSH_COMMAND", "MainActivity 180 - $resultat")
        updateConsole("Vous écoutez $resultat.")
        val cmdLire = """mpv --no-video "$resultat" > /dev/null 2>&1 &"""
        SSHManager.executerCommandeSSH(this, cmdLire) { reussite, resultat ->
//#=>            if (reussite) {
//#=>                updateConsole("Lecture du fichier : $resultat")
//#=>            } else {
//#=>                updateConsole("Erreur lors de la lecture de $resultat.")
//#=>            }
        }
    }
}
//        if (reussite && !resultat.isNullOrBlank()) {
//            // Transformer le résultat en liste
//            android.util.Log.d("SSH_COMMAND", "MainActivity 183 - Reussite $resultat")
//            val listeFichiers = resultat.split("\n").filter { it.isNotBlank() }
//            val indexCible = 0
//            if (indexCible in listeFichiers.indices) {
//                val cheminAudio = listeFichiers[indexCible]
//                 android.util.Log.d("SSH_COMMAND", "MainActivity 187 - $cheminAudio")
//                 updateConsole("Vous écoutez : $cheminAudio")
//                val cmdLire = """mpv --no-video "$cheminAudio" > /dev/null 2>&1 &"""
                
                //CHEMIN_AUDIO=$(locate -i -d /home/enjoy/musique.db "*blood*" | sed -n "${OCCURENCE}p")
                //echo "Le fichier sélectionné est : $CHEMIN_AUDIO"
                //mpv --no-video "$CHEMIN_AUDIO"
               // val cmdLire = "pkill mpv; find '$REPERTOIRE_MUSIQUE' -type f -iname *'$motCle'* -print -quit | xargs -d '\n' mpv > /dev/null 2>&1 &"
//            } else {
//                updateConsole("Aucun fichier trouvé pour '$motCle'")
//            }
//        } else {
//            updateConsole("Erreur SSH ou aucun résultat pour '$motCle'")
//        }
//    }


    //-------------------------------      Recherche d'un Répertoire sur le serveur      ---------------------------------------
    private fun chercherRepertoireSurServeur(motCle: String) {
         // Construction de la commande find
        val commande = "pkill mpv; find '$REPERTOIRE_MUSIQUE' -type d -iname *'$motCle'* -print -quit | xargs -d '\n' mpv --shuffle --input-ipc-server=/tmp/mpv-socket  > /dev/null 2>&1 &"
        //updateConsole("la commande:  '$commande'")
        // Utilisation de votre SSHManager
        SSHManager.executerCommandeSSH(this, commande)
    }

//-------------------------------      Lecture audio de texte      ---------------------------------------
    private fun updateConsole(msg: String) {
        runOnUiThread {
            tvConsole.append("\n$msg")
            // On utilise android.widget.ScrollView ici pour correspondre au XML
            val scroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.consoleScroll)
            scroll?.post { 
                scroll.fullScroll(View.FOCUS_DOWN) 
            }
        }
    }

//-------------------------------      Lecture audio de texte      ---------------------------------------
    private fun parler(message: String) {
        if (::tts.isInitialized) { // Vérifie si tts a été bien créé
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

//-------------------------------      Verifier le statut du serveur     ---------------------------------------
    private fun verifierStatutServeur() {
        val tvStatus = findViewById<TextView>(R.id.tvStatusConnexion)
        val dot = findViewById<View>(R.id.viewStatusDot)
        tvStatus.text = getString(R.string.verification)
        SSHManager.testerConnexion(this) { success ->
            if (success) {
                tvStatus.text = getString(R.string.serveur_connecte)
                //tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Vert
                tvStatus.setTextColor("#4CAF50".toColorInt())
                dot.setBackgroundResource(android.R.drawable.presence_online)
            } else {
                tvStatus.text = getString(R.string.serveur_hors_ligne)
                tvStatus.setTextColor(android.graphics.Color.RED)
                dot.setBackgroundResource(android.R.drawable.presence_offline)
            }
        }
    }
//-------------------------------      Anilmation d'un bouton     ---------------------------------------
    private fun startPulseAnimation() {
        pulseView.visibility = View.VISIBLE
        val scaleX = ObjectAnimator.ofFloat(pulseView, "scaleX", 1f, 2f)
        val scaleY = ObjectAnimator.ofFloat(pulseView, "scaleY", 1f, 2f)
        val alpha = ObjectAnimator.ofFloat(pulseView, "alpha", 1f, 0f)

        AnimatorSet().apply {
            duration = 1000
            playTogether(scaleX, scaleY, alpha)
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        verifierStatutServeur()
    }
    
    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}