package com.example.radiocommande // <--- VÉRIFIEZ BIEN VOTRE NOM DE PACKAGE ICI

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
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.*
import android.content.Context

private val dictionnaireCommandes = mapOf(
    "stop"    to "pkill mpv",
    "quitter"    to "pkill mpv",
    "culture"    to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/franceculture/franceculture_hifi.m3u8 > /dev/null 2>&1 &",
    "fip culte" to "pkill mpv ; nohup mpv https://stream.radiofrance.fr/fipcultes/fipcultes_hifi.m3u8 > /dev/null 2>&1 &",
    "dylan"    to "pkill mpv ; nohup mpv --shuffle '/home/enjoy/Musique//Bob Dylan/' > /dev/null 2>&1 &",
    "douce" to "pkill mpv ; nohup mpv --shuffle /home/enjoy/Musique/sweet/ > /dev/null 2>&1 &",
    "tahiti"    to "pkill mpv ; nohup mpv --shuffle '/home/enjoy/Musique/Chants tahitiens traditionnels/' > /dev/null 2>&1 &",
    "stevens" to "pkill mpv ; nohup mpv --shuffle '/home/enjoy/Musique/Cat Stevens/' > /dev/null 2>&1 &", // Utile pour Raspberry Pi
    "tous"   to "pkill mpv ; nohup mpv --shuffle /home/enjoy/Musique/ > /dev/null 2>&1 &"
)

class MainActivity : AppCompatActivity() {

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
//---------------------------------------------------------------------------------------------------------------------------------
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
            updateConsole("Action : Recherche de '$commandeAExecuter'...")
            executerCommandeSSH(commandeAExecuter)
        } else {
            // Cas spécial pour la recherche dynamique (ex: "cherche erreur")
            if (texte.contains("cherche")) {
                val mot = texte.replace("cherche", "").trim()
                if (mot.isNotEmpty()) {
                    executerCommandeSSH("grep -ri '$mot' /var/log/")
                }
            } else {
               parler("Désolé, cette commande n'est pas dans mon dictionnaire.")
               updateConsole("Système : Commande '$texte' inconnue.")
            }
        }
    }

//---------------------------------------------------------------------------------------------------------------------------------
//----------------------------      connexion SSH avec mot de passe -------------------------
    private fun executerCommandeSSH(commande: String) {
        val prefs = getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        if (ip.isEmpty() || user.isEmpty()) {
            updateConsole("Erreur : Configurez les accès dans les paramètres.")
            return
        }
        // Exécution en arrière-plan (Coroutine)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsch = JSch()
                // --- À MODIFIER AVEC VOS INFOS ---
                val session = jsch.getSession(user, ip, 2523)
                session.setPassword(pass)
                
                val config = Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                session.connect(5000)

                val channel = session.openChannel("exec") as ChannelExec
                channel.setCommand(commande)
                val inputStream = channel.inputStream
                channel.connect()

                val output = inputStream.bufferedReader().use { it.readText() }
                
                withContext(Dispatchers.Main) {
                   updateConsole("Réponse : \n$output")
                   parler("Commande envoyée.")
                }
               channel.disconnect()
               session.disconnect()
            } catch (e: Exception) { /* ... */ }
        }
    }
////----------------------------      connexion SSH avec cle -------------------------
//private fun executerCommandeSSH(commande: String) {
//        updateConsole("SSH : Connexion par clé...")
        
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val jsch = JSch()

//                // --- MODIFICATION POUR LA CLÉ PRIVÉE ---
//                try {
//                    // Lecture de la clé depuis res/raw/id_ssh
//                    val inputStream: InputStream = resources.openRawResource(R.raw.id_ssh)
//                    val keyBytes = inputStream.readBytes()
                    
//                    // On ajoute l'identité (Si votre clé a une passphrase, ajoutez-la en 2ème argument)
//                    jsch.addIdentity("identite_android", keyBytes, null, null)
//                } catch (e: Exception) {
//                    withContext(Dispatchers.Main) { updateConsole("Erreur lecture clé : ${e.message}") }
//                    return@launch
//                }

//                // --- CONFIGURATION SESSION ---
//                // Note : On ne met plus de mot de passe ici
//                val session = jsch.getSession("enjoy", "192.168.1.80", 2523)
                
//                val config = Properties()
//                config["StrictHostKeyChecking"] = "no"
//                session.setConfig(config)
                
//                session.connect(5000)

//                val channel = session.openChannel("exec") as ChannelExec
//                channel.setCommand(commande)
                
//                val sshInput = channel.inputStream
//                channel.connect()

//                val output = sshInput.bufferedReader().use { it.readText() }
                
//                withContext(Dispatchers.Main) {
//                    updateConsole("Réponse : \n$output")
//                    parler("Commande exécutée.")
//                }

//                channel.disconnect()
//                session.disconnect()

//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    updateConsole("Erreur SSH : ${e.message}")
//                    parler("Échec de la connexion.")
//                    // Log détaillé en console pour le debug
//                    e.printStackTrace() 
//                }
//            }
//        }
//    }

    private fun updateConsole(msg: String) {
        runOnUiThread {
            tvConsole.append("\n$msg")
        
            // On utilise android.widget.ScrollView ici pour correspondre au XML
            //val scroll = findViewById<android.widget.ScrollView>(R.id.consoleScroll)
            val scroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.consoleScroll)
            scroll?.post { 
                scroll.fullScroll(View.FOCUS_DOWN) 
            }
        }
    }

    private fun parler(message: String) {
        if (::tts.isInitialized) { // Vérifie si tts a été bien créé
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

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

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
