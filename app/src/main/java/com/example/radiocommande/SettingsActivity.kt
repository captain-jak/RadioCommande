package com.example.radiocommande

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etIp = findViewById<EditText>(R.id.etIp)
        val etUser = findViewById<EditText>(R.id.etUser)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val prefs = getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)

        // Charger les anciennes valeurs si elles existent
        etIp.setText(prefs.getString("ip", ""))
        etUser.setText(prefs.getString("user", ""))
        etPass.setText(prefs.getString("pass", ""))

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("ip", etIp.text.toString())
                putString("user", etUser.text.toString())
                putString("pass", etPass.text.toString())
                apply()
            }
            finish() // Retour à l'écran principal
        }
    }
}