package com.radiocommande
// RadioCommande2
//import android.content.Context
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
                findViewById<TextView>(R.id.tvKeyStatus).text =
                    getString(R.string.statut_cle_import_succes)
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

        val prefs = getSharedPreferences("SSH_REGLAGES", MODE_PRIVATE)

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
                 Thread{SSHManager.executerCommandeSSH(this, "pactl set-sink-mute @DEFAULT_SINK@ 1")}.start()
                 btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode)
                 tvVolumeLabel.text = getString(R.string.volume_muet)
            } else {
                Thread{SSHManager.executerCommandeSSH(this, "pactl set-sink-mute @DEFAULT_SINK@ 0")}.start()
                btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                //tvVolumeLabel.text = getString(R.string.volume3, sbVolume.progress)
            tvVolumeLabel.text = getString(R.string.volume_format, sbVolume.progress)
            }
        }

        // --- GESTION VOLUME ---
        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVolumeLabel.text = getString(R.string.volume_du_serveur, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                 val volumeValue = seekBar?.progress ?: 50
                 Thread{SSHManager.executerCommandeSSH(this@SettingsActivity, "pactl set-sink-volume @DEFAULT_SINK@ ${volumeValue}%")}.start()
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