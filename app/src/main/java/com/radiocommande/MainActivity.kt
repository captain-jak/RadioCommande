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
import androidx.annotation.StringRes

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import android.graphics.Color

import org.json.JSONObject


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
    "tous" to "pkill mpv ; nohup mpv --shuffle --input-ipc-server=/tmp/mpv-socket /srv/Musique/ > /dev/null 2>&1 &"
)

// Pour supprimer warnings à la compilation
@Suppress("GrazieInspection", "GrazieInspection", "GrazieInspection", "GrazieInspection",
    "GrazieInspection", "GrazieInspection", "GrazieInspection", "GrazieInspection",
    "GrazieInspection", "GrazieInspection", "GrazieInspection", "GrazieInspection",
    "RedundantSuppression", "RedundantSuppression", "RedundantSuppression", "RedundantSuppression",
    "RedundantSuppression", "RedundantSuppression", "RedundantSuppression", "RedundantSuppression",
    "RedundantSuppression", "RedundantSuppression", "RedundantSuppression", "RedundantSuppression",
    "SpellCheckingInspection", "SpellCheckingInspection"
)

//---------------------------------------------------------------------------------------------------------------------
//                                               Début de la classe MainActivity
//---------------------------------------------------------------------------------------------------------------------
        
class MainActivity : AppCompatActivity() {
    companion object {
        //Initialisation des valeurs globales
        private const val REPERTOIRE_MUSIQUE = "/srv/Musique/"
        private val DEBUG=false
    }
    private lateinit var tvConsole: TextView
    private lateinit var pulseView: View
    private lateinit var tts: TextToSpeech
    private var isListening = false
    //------ 12-03-2026 --------------------
    private var jobSurveillance: Job? = null
    // Numérotation des lignes de l'application
   val line = Throwable().stackTrace[0].lineNumber
    
        //------------------------------------------------------------------------------------------------------------
    // --------         Fonction  initialisation RadioCommande (vue - permissions              -----
    //---------------------------------------------------------------------------------------------------------------
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
       //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction de Vérification du statut du serveur                              -----
    //---------------------------------------------------------------------------------------------------------------
    private fun verifierStatutServeur() {
        val tvStatus = findViewById<TextView>(R.id.tvStatusConnexion)
        val dot = findViewById<View>(R.id.viewStatusDot)
        tvStatus.text = getString(R.string.verification)
        SSHManager.testerConnexion(this) { success, message ->
            tvStatus.text = message ?: "Résultat inconnu"
            if (success) {
                tvStatus.text = getString(R.string.serveur_connecte)
                tvStatus.setTextColor("#4CAF50".toColorInt()) // Vert
                dot.setBackgroundResource(android.R.drawable.presence_online)
            } else {
                tvStatus.text = getString(R.string.serveur_hors_ligne)
                tvStatus.setTextColor(Color.RED) // Rouge
                dot.setBackgroundResource(android.R.drawable.presence_offline)
            }
        }
    }
    
    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction Anilmation d'un bouton                                                 -----
    //---------------------------------------------------------------------------------------------------------------
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

    //---------------------------------------------------------------------------------------------------------------
    // ---------------------          Fonction Lancement écoute vocale                                   -----
    //---------------------------------------------------------------------------------------------------------------
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
    //---------------------------------------------------------------------------------------------------------------
    // ---------------------          Fonction du retour de la reconnaissance vocale               -----
    //---------------------------------------------------------------------------------------------------------------
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

    //---------------------------------------------------------------------------------------------------------------
    // ---------------------          Fonction commande à envoyer au serveur                       -----
    //---------------------------------------------------------------------------------------------------------------
   private fun interpreterEtEnvoyer(texte: String) {
        var commandeAExecuter: String? = null
        var cleTrouvee: String? = null
        // arrête le processus de lecture ds titres ...
        arreterSurveillance()
        // On parcourt le dictionnaire pour voir si un mot-clé est dans le texte entendu
        for ((cle, commande) in dictionnaireCommandes) {
            if (texte.contains(cle)) {
                commandeAExecuter = commande
                cleTrouvee = cle
                break // On s'arrête au premier mot trouvé
            }
        }
        if (commandeAExecuter != null) {
            val finalKey = cleTrouvee
            // Utilisation des valeurs du dictionnaire
            //updateConsole("Action : Recherche de '$commandeAExecuter'...")
            if (DEBUG) android.util.Log.d("ISDEBUG", "MainActivity-$line : $commandeAExecuter")
            Thread {SSHManager.executerCommandeSSH(this, commandeAExecuter)}.start()
            if (finalKey == "tous" || finalKey == "précédent" || finalKey == "suivant") {
                if (DEBUG) android.util.Log.d("ISDEBUG", "MainActivity-$line : Tout écouter")
                surveillerMusique()
             } else {
                val console = findViewById<TextView>(R.id.textConsole)
                console.text = getString(R.string.console_listening, texte)
             }
        } else {
            // parler("OK, je cherche.")
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
               //parler("commande inexistante")
               parler(R.string.command_unknown)
               updateConsole("Système : Commande '$texte' inconnue.")
            }
        }
    }

    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction Recherche d'un morceau sur le serveur                      -----
    //---------------------------------------------------------------------------------------------------------------
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
                //val nom = cheminAudio.substringAfterLast("/")
                if (DEBUG) android.util.Log.d("ISDEBUG", "MainActivity-$line : $cheminAudio")
        // 2- Execution mpv
                val cmdLire = """pkill mpv; mpv --no-video --input-ipc-server=/tmp/mpv-socket "$cheminAudio" > /dev/null 2>&1 &"""
                SSHManager.executerCommandeSSH(this, cmdLire)
                //updateConsole("Vous écoutez : $nom")
                //val console = findViewById<TextView>(R.id.textConsole)
            } else {
                println("Index invalide")
            }
        }.start()
        surveillerMusique()
    }

    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction Recherche d'un Répertoire sur le serveur                   -----
    //---------------------------------------------------------------------------------------------------------------
    private fun chercherRepertoireSurServeur(motCle: String) {
         // Construction de la commande find
        val commande = "pkill mpv; find '$REPERTOIRE_MUSIQUE' -type d -iname *'$motCle'* -print -quit | xargs -d '\n' mpv --shuffle --input-ipc-server=/tmp/mpv-socket  > /dev/null 2>&1 &"
        //updateConsole("la commande:  '$commande'")
        // Utilisation de votre SSHManager avec un trhread
        Thread {SSHManager.executerCommandeSSH(this, commande)}.start()
        surveillerMusique()
    }

    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction de Rafraichissement de la console                              -----
    //---------------------------------------------------------------------------------------------------------------
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
    
    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction de Nettoyage de la console                                         -----
    //---------------------------------------------------------------------------------------------------------------
//    private fun clearConsole() {
//        runOnUiThread {
//            tvConsole.text = "" // On vide le texte
//            // Optionnel : On remet le scroll en haut
//            val scroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.consoleScroll)
//            scroll?.post {
//                scroll.fullScroll(View.FOCUS_UP)
//            }
//        }
//    }

    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction de Lecture audio de texte                                            -----
    //---------------------------------------------------------------------------------------------------------------
    private fun parler(@StringRes messageRes: Int) {
        val textToSpeak = getString(messageRes)
        if (::tts.isInitialized) { // Vérifie si tts a été bien créé
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction Affichage des infos du titre joué                                   -----
    //---------------------------------------------------------------------------------------------------------------
    fun surveillerMusique() {
        // On annule l'ancienne surveillance si elle tournait déjà
        jobSurveillance?.cancel()
        jobSurveillance = lifecycleScope.launch {
            while (isActive) {
                // script mpv-info.sh sur le serveur - le script retourne les infos du fichier en cours de lecture
                //imite de temps (ex: 30 secondes). S'il boucle à l'infini, il s'arrêtera tout seul
                val cmd = "timeout 10s  ~/admin/mpv-info.sh"
                val line = Throwable().stackTrace[0].lineNumber
                if (DEBUG) android.util.Log.d("ISDEBUG", "MainActivity-$line : $cmd")
                val console = findViewById<TextView>(R.id.textConsole)
                val response = SSHManager.execute(this@MainActivity, cmd)
                if (response.startsWith("Artiste inconnu")) {
                    console.setTextColor(Color.RED)
                } else {
                    console.setTextColor(Color.GREEN)
               }
                console.text = response
            }
        }
    }
    
    //---------------------------------------------------------------------------------------------------------------
    // ---------------        Fonction Arrêt affichage  des infos du titre joué                          -----
    //---------------------------------------------------------------------------------------------------------------
    fun arreterSurveillance() {
        jobSurveillance?.cancel()
        jobSurveillance = null
        SSHManager.disconnect()
        if (DEBUG) android.util.Log.d("ISDEBUG", "MainActivity-$line : SurveillanceMusique arrêter")
        findViewById<TextView>(R.id.textConsole).text = "Surveillance arrêtée"
    }
    
    //---------------------------------------------------------------------------------------------------------------
    // ---------     Fonction de reprise de l'application (onResume et onDestroy)             -----
    //---------------------------------------------------------------------------------------------------------------
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




