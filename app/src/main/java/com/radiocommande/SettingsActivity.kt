package com.radiocommande

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

private var isMuted = false

class SettingsActivity : AppCompatActivity() {

    // Déclaration du sélecteur de fichier (doit être au niveau de la classe)
    private val pickKeyFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val content = contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                val file = File(filesDir, "ssh_key")
                file.writeText(content)
                findViewById<TextView>(R.id.tvKeyStatus).text = "Statut : Clé importée avec succès"
                Toast.makeText(this, "Clé SSH enregistrée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialisation des vues
        val etIp = findViewById<EditText>(R.id.etIp)
        val etUser = findViewById<EditText>(R.id.etUser)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnImportKey = findViewById<Button>(R.id.btnImportKey) // Ajouté ici
        val sbVolume = findViewById<SeekBar>(R.id.sbVolume)
        val tvVolumeLabel = findViewById<TextView>(R.id.tvVolumeLabel)
        val btnMute = findViewById<ImageButton>(R.id.btnMute)

        val prefs = getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)

        // Charger les anciennes valeurs
        etIp.setText(prefs.getString("ip", ""))
        etUser.setText(prefs.getString("user", ""))
        etPass.setText(prefs.getString("pass", ""))

        // --- CLIC : IMPORTATION CLÉ (Déplacé ici) ---
        btnImportKey.setOnClickListener {
            pickKeyFile.launch("*/*")
        }

        // --- CLIC : MUTE ---
        btnMute.setOnClickListener {
             isMuted = !isMuted
             if (isMuted) {
                 SSHManager.executerCommandeSSH(this, "pactl set-sink-mute @DEFAULT_SINK@ 1")
                 btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode)
                 tvVolumeLabel.text = "Volume : MUET"
            } else {
                SSHManager.executerCommandeSSH(this, "pactl set-sink-mute @DEFAULT_SINK@ 0")
                btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                tvVolumeLabel.text = "Volume : ${sbVolume.progress}%"
            }
        }

        // --- GESTION VOLUME ---
        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVolumeLabel.text = "Volume du Serveur : $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                 val volumeValue = seekBar?.progress ?: 50
                 SSHManager.executerCommandeSSH(this@SettingsActivity, "pactl set-sink-volume @DEFAULT_SINK@ ${volumeValue}%")
            }
        })

        // --- BOUTON SAUVEGARDER ---
        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("ip", etIp.text.toString())
                putString("user", etUser.text.toString())
                putString("pass", etPass.text.toString())
                apply()
            }
            finish()
        }
    } // Fin du onCreate
} // Fin de la classe