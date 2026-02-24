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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        tvConsole = findViewById(R.id.tvConsole)
        pulseView = findViewById(R.id.pulseView1)
        val btnMic: FloatingActionButton = findViewById(R.id.btnMic)

        // 1. Vérification des permissions au démarrage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }

        // 2. Initialisation du Text-to-Speech (Le téléphone parle)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.FRENCH
            }
        }

        // 3. Action du bouton
        btnMic.setOnClickListener {
            lancerEcouteVocale()
        }
    }

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
        // Mapping simple voix -> commandes Linux
        val commandeSSH = when {
            texte.contains("stop") || texte.contains("quitter") -> "pkill mpv"
            texte.contains("culture") || texte.contains("france culture") -> "pkill mpv ; nohup mpv https://stream.radiofrance.fr/franceculture/franceculture_hifi.m3u8 > /dev/null 2>&1 &"
            texte.contains("dylan") -> "pkill mpv ; nohup mpv --shuffle '/home/enjoy/Musique//Bob Dylan/' > /dev/null 2>&1 &"
            texte.contains("fip culte") -> "pkill mpv ; nohup mpv https://stream.radiofrance.fr/fipcultes/fipcultes_hifi.m3u8 > /dev/null 2>&1 &"
            texte.contains("douce") -> "pkill mpv ; nohup mpv --shuffle /home/enjoy/Musique/sweet/ > /dev/null 2>&1 &"
            else -> null
        }

        if (commandeSSH != null) {
            executerCommandeSSH(commandeSSH)
        } else {
            parler("Je ne connais pas cette commande.")
            updateConsole("Système : Commande non reconnue.")
        }
    }

////----------------------------      connexion SSH avec mot de passe -------------------------
//    private fun executerCommandeSSH(commande: String) {
//        updateConsole("SSH : Connexion en cours...")
        
//        // Exécution en arrière-plan (Coroutine)
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val jsch = JSch()
//                // --- À MODIFIER AVEC VOS INFOS ---
//                val session = jsch.getSession("enjoy", "192.168.1.80", 2523)
//                session.setPassword("Verdun1914!!")
                
//                val config = Properties()
//                config["StrictHostKeyChecking"] = "no"
//                session.setConfig(config)
//                session.connect(5000)

//                val channel = session.openChannel("exec") as ChannelExec
//                channel.setCommand(commande)
//                val inputStream = channel.inputStream
//                channel.connect()

//                val output = inputStream.bufferedReader().use { it.readText() }
                
//                withContext(Dispatchers.Main) {
//                   updateConsole("Réponse : \n$output")
//                   parler("Commande envoyée.")
//                }

//               channel.disconnect()
//                session.disconnect()
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    updateConsole("Erreur SSH : ${e.message}")
//                    parler("Échec de la connexion.")
//                }
//            }
//        }
//    }

//----------------------------      connexion SSH avec cle -------------------------
private fun executerCommandeSSH(commande: String) {
        updateConsole("SSH : Connexion par clé...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsch = JSch()

                // --- MODIFICATION POUR LA CLÉ PRIVÉE ---
                try {
                    // Lecture de la clé depuis res/raw/id_ssh
                    val inputStream: InputStream = resources.openRawResource(R.raw.id_ssh)
                    val keyBytes = inputStream.readBytes()
                    
                    // On ajoute l'identité (Si votre clé a une passphrase, ajoutez-la en 2ème argument)
                    jsch.addIdentity("identite_android", keyBytes, null, null)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { updateConsole("Erreur lecture clé : ${e.message}") }
                    return@launch
                }

                // --- CONFIGURATION SESSION ---
                // Note : On ne met plus de mot de passe ici
                val session = jsch.getSession("enjoy", "192.168.1.80", 2523)
                
                val config = Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                
                session.connect(5000)

                val channel = session.openChannel("exec") as ChannelExec
                channel.setCommand(commande)
                
                val sshInput = channel.inputStream
                channel.connect()

                val output = sshInput.bufferedReader().use { it.readText() }
                
                withContext(Dispatchers.Main) {
                    updateConsole("Réponse : \n$output")
                    parler("Commande exécutée.")
                }

                channel.disconnect()
                session.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConsole("Erreur SSH : ${e.message}")
                    parler("Échec de la connexion.")
                    // Log détaillé en console pour le debug
                    e.printStackTrace() 
                }
            }
        }
    }

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
