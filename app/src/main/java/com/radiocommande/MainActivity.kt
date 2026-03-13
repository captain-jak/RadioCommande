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

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*


// mot cles reserves (a ne pas utiliser dans le dictionnaire
// CHERCHE -- SUIVANT -- PRECEDENT - QUITTER -- PLAYLIST
// Pour la recherche avec locate, ne pas oublier de mettre à jour la base (updatedb -o ~/musique.db -U /srv/Musique)
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
     "radio catho" to "pkill mpv ; nohup mpv https://liveradiokto.akamaized.net/hls/live/20000054/ktoradio/02.m3u8 > /dev/null 2>&1 &",

    "dylan" to "pkill mpv ; nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket '/home/enjoy/Musique//Bob Dylan/' > /dev/null 2>&1 &",
    "douce" to "nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket /home/enjoy/Musique/sweet/ > /dev/null 2>&1 &",
    "tahiti" to "pkill mpv ; nohup mpv --shuffle  --input-ipc-server=/tmp/mpv-socket '/home/enjoy/Musique/Chants tahitiens traditionnels/' > /dev/null 2>&1 &",
    "stevens" to "pkill mpv ; nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket '/home/enjoy/Musique/Cat Stevens/' > /dev/null 2>&1 &",
    "tous" to "pkill mpv ; nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket /home/enjoy/Musique/ > /dev/null 2>&1 &"
)

@Suppress("GrazieInspection", "GrazieInspection", "GrazieInspection", "GrazieInspection",
    "GrazieInspection", "GrazieInspection", "GrazieInspection", "GrazieInspection",
    "GrazieInspection", "GrazieInspection", "GrazieInspection", "GrazieInspection",
    "RedundantSuppression", "RedundantSuppression", "RedundantSuppression", "RedundantSuppression",
    "RedundantSuppression", "RedundantSuppression", "RedundantSuppression", "RedundantSuppression",
    "RedundantSuppression", "RedundantSuppression", "RedundantSuppression", "RedundantSuppression"
)
class MainActivity : AppCompatActivity() {
    companion object {
        //private const val REPERTOIRE_MUSIQUE = "/home/enjoy/Musique/"
        private const val REPERTOIRE_MUSIQUE = "/srv/Musique/"
    }
    private lateinit var tvConsole: TextView
    private lateinit var pulseView: View
    private lateinit var tts: TextToSpeech
    private var isListening = false
    //------ 12-03-2026 --------------------
    private var musiqueJob: Job? = null
    //------------------------------------------

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
            //------ 12-03-2026 --------------------

        //-------------------------------------------------------------------
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
        // On parcourt le dictionnaire pour voir si un mot-clé est dans le texte entendu
        for ((cle, commande) in dictionnaireCommandes) {
            if (texte.contains(cle)) {
                commandeAExecuter = commande
                break // On s'arrête au premier mot trouvé
            }
        }
        if (commandeAExecuter != null) {
            //updateConsole("Action : Recherche de '$commandeAExecuter'...")
            android.util.Log.d("SSH_COMMAND", "MainActivity-133 : $commandeAExecuter")
            Thread {SSHManager.executerCommandeSSH(this, commandeAExecuter)}.start()
        } else {
            parler("OK, je cherche.")
            // Cas spécial pour la recherche dynamique (ex:"cherche erreur")
            if (texte.contains("cherche")) {
                //updateConsole("Je cherche '$texte'")
                val mot = texte.replace("cherche ", "").trim()
                //updateConsole("Je cherche apres trim: '$mot'")
                if (mot.isNotEmpty()) {
                    chercherFichierSurServeur(mot)
                }
             // ------   Cas spécial lecture d'un répertoire  --------------------------------------------------------------------
             } else if  (texte.contains("playlist")){
                //updateConsole("Je cherche '$texte'")
                val mot = texte.replace("playlist ", "").trim()
                //updateConsole("Je cherche apres trim: '$mot'")
                if (mot.isNotEmpty()) {
                    chercherRepertoireSurServeur(mot)
                }
            //------------------------------------------------------------------------------------------------------------------------------
            } else {
               parler("commande inexistante")
               updateConsole("Système : Commande '$texte' inconnue.")
            }
        }
    }

//-------------------------------      Recherche d'un morceau sur le serveur      ---------------------------------------
    private fun chercherFichierSurServeur(motCle: String) {
         // 1- Recherche le fichier dans la base de données musicale 
        val cmdRecherche = """locate -i -d /home/enjoy/musique.db "*$motCle*""""
        arreterSurveillance()
        // Lancement d'un Thread pour ne pas bloquer l'écran
        Thread {
            val resultat = SSHManager.executerCommandeSSH(this, cmdRecherche)
            val listeFichiers = resultat
            .split("\n")
            .filter { it.isNotBlank() }
            // Choisir l’index (ici le premier)
            val indexCible = 0
            // Vérifier que l’index est valide
            if (indexCible in listeFichiers.indices) {
                val cheminAudio = listeFichiers[indexCible]
                val nom = cheminAudio.substringAfterLast("/")
                android.util.Log.d("SSH_COMMAND", "MainActivity-195 : $cheminAudio")
        // 2- Execution mpv
                val cmdLire = """pkill mpv; mpv --no-video --input-ipc-server=/tmp/mpvsocket "$cheminAudio" > /dev/null 2>&1 &"""
                SSHManager.executerCommandeSSH(this, cmdLire)
                updateConsole("Vous écoutez : $nom")
            } else {
                println("Index invalide")
            }
        }.start()
    }

    //----------------------------      Recherche d'un Répertoire sur le serveur      ---------------------------------------
    private fun chercherRepertoireSurServeur(motCle: String) {
         // Construction de la commande find
        val commande = "pkill mpv; find '$REPERTOIRE_MUSIQUE' -type d -iname *'$motCle'* -print -quit | xargs -d '\n' mpv --shuffle --input-ipc-server=/tmp/mpv-socket  > /dev/null 2>&1 &"
        updateConsole("la commande:  '$commande'")
        // Utilisation de votre SSHManager
        Thread {SSHManager.executerCommandeSSH(this, commande)}.start()
        surveillerMusique()
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
    private fun clearConsole() {
        runOnUiThread {
            tvConsole.text = "" // On vide le texte
            // Optionnel : On remet le scroll en haut
            val scroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.consoleScroll)
            scroll?.post { 
                scroll.fullScroll(View.FOCUS_UP) 
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

//-------------------------------      Affichage du titre encours  sur la console   ---------------------------------------
    fun surveillerMusique() {
        // On annule l'ancien job s'il tournait déjà pour éviter les doublons
        musiqueJob?.cancel()
        android.util.Log.d("SSH_COMMAND", "MainActivity-316 :surveille musique")
        // Note : Pensez à utiliser lifecycleScope si vous êtes dans une Activity
        musiqueJob = lifecycleScope.launch {
        //CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // On précise explicitement que withContext va retourner une String
               // val result = withContext<String>(Dispatchers.IO) {
                val result = withContext(Dispatchers.IO) {
                    val cmd = "echo '{ \"command\": [\"get_property\", \"media-title\"] }' | socat - /tmp/mpvsocket"
                    SSHManager.execute(cmd) // Cette fonction doit retourner un String
                }
                // Extraction du titre
                val titre = if (result.contains("\"data\":\"")) {
                    result.substringAfter("\"data\":\"").substringBefore("\"")
                } else {
                    "Aucune musique"
                }
                // Mise à jour de votre UI ici
                println("Lecture en cours : $titre")
                android.util.Log.d("SSH_COMMAND", "MainActivity-335 : $result")
               clearConsole()
               updateConsole("En cours : $titre")
               delay(2000) 
            }
        }
    }
    fun arreterSurveillance() {
        musiqueJob?.cancel() // Arrête net la boucle while(isActive)
        musiqueJob = null
        updateConsole("Surveillance MPV désactivée.")
    }
}


