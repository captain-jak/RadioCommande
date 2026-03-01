//package com.example.radiocommande // <--- VÉRIFIEZ BIEN VOTRE NOM DE PACKAGE ICI
package com.radiocommande

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
//import com.example.radiocommande.R
import com.radiocommande.R

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
            updateConsole("Action : Recherche de '$commandeAExecuter'...")
            SSHManager.executerCommandeSSH(this, commandeAExecuter)
        } else {
            // Cas spécial pour la recherche dynamique (ex: "cherche erreur")
            if (texte.contains("cherche")) {
                updateConsole("Je cherche '$texte'")
                val mot = texte.replace("cherche ", "").trim()
                updateConsole("Je cherche apres trim: '$mot'")
                if (mot.isNotEmpty()) {
                    chercherFichierSurServeur("/home/enjoy/Musique/", mot)
                }
            } else {
               parler("Désolé, cette commande n'est pas dans mon dictionnaire.")
               updateConsole("Système : Commande '$texte' inconnue.")
            }
        }
    }

//-------------------------------      Recherche d'un morceau sur le serveur      ---------------------------------------

    private fun chercherFichierSurServeur(repertoire: String, motCle: String) {
         // Construction de la commande find
        val commande = "pkill mpv; find '$repertoire' -type f -iname *'$motCle'* -print -quit | xargs -d '\n' mpv > /dev/null 2>&1 &"
        updateConsole("la commande:  '$commande'")
        // Utilisation de votre SSHManager
        SSHManager.executerCommandeSSH(this, commande)
    }

//-------------------------------      Lecture audio de texte      ---------------------------------------


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
        tvStatus.text = "Vérification..."
        SSHManager.testerConnexion(this) { success ->
            if (success) {
                tvStatus.text = "Serveur : Connecté"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Vert
                dot.setBackgroundResource(android.R.drawable.presence_online)
            } else {
                tvStatus.text = "Serveur : Hors ligne"
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