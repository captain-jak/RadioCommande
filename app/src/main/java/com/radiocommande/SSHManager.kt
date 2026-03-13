package com.radiocommande
//RadioCommande2
import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelExec
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking // Optionnel, selon l'usage

object SSHManager {
    //  Test de connexion SSH
    // 1. Déclarer la session au niveau de la classe pour qu'elle soit accessible partout
    var session: Session? = null
    private var sessionMain: Session? = null
    //-------------------------------      Création d'une connexion ssh permanente      ---------------------------------------
    suspend fun connect(context: Context): Boolean = withContext(Dispatchers.IO) {
        // Si déjà connecté, on ne fait rien
        if (sessionMain?.isConnected == true) return@withContext true
        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        if (ip.isEmpty() || user.isEmpty()) return@withContext false
        val lip = ip.substringBefore(":", ip) // Prend tout si pas de ":"
        val portString = ip.substringAfter(":", "22") // 22 par défaut si pas de ":"
        val port = portString.toIntOrNull() ?: 22
        try {
            val jsch = JSch()
            session = jsch.getSession(user, lip, port)
            session?.setPassword(pass)
            session?.setConfig("StrictHostKeyChecking", "no")
            session?.connect(1000) // Timeout 1s
            // 2. On assigne la session réussie à notre variable persistante
            sessionMain = session
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
 
    fun testerConnexion(context: Context, callback: (Boolean) -> Unit) {
        // Récupération sécurisée des données
        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        val lip = ip.substringBefore(":", ip) // Prend tout si pas de ":"
        val portString = ip.substringAfter(":", "22") // 22 par défaut si pas de ":"
        val port = portString.toIntOrNull() ?: 22
        // On lance le test dans un Thread pour ne pas bloquer l'écran
        Thread {
            try {
                val jsch = com.jcraft.jsch.JSch()
                val session = jsch.getSession(user, lip, port)
                // On définit le mot de passe pour la session
                session.setPassword(pass)
                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                // Timeout court (2 secondes) pour ne pas faire attendre l'utilisateur
                session.connect(2000) 
                val isConnected = session.isConnected
                session.disconnect()
                // Retour à l'interface graphique pour mettre à jour le texte
                (context as? android.app.Activity)?.runOnUiThread {
                     callback(isConnected)
                }
            } catch (e: Exception) {
                //(context as? android.app.Activity)?.runOnUiThread {
                 //    callback(false)
                //}
            }
        }.start()
    }
//  fin test de connexion

    // Cette fonction peut être appelée de n'importe où : MainActivity, SettingsActivity...
    fun executerCommandeSSH(context: Context, commande: String): String {
        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
       val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        val lip = ip.substringBefore(":", ip) // Prend tout si pas de ":"
        val portString = ip.substringAfter(":", "22") // 22 par défaut si pas de ":"
        val port = portString.toIntOrNull() ?: 22
        val jsch = com.jcraft.jsch.JSch()
        val session = jsch.getSession(user, lip, port)
        // On définit le mot de passe pour la session
        session.setPassword(pass)
        val config = java.util.Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        // Timeout court (2 secondes) pour ne pas faire attendre l'utilisateur
        session.connect(2000) 
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(commande)
        val input = channel.inputStream
        channel.connect()
        val resultat = input.bufferedReader().readText()
        // On laisse un peu de temps pour l'exécution
        //Thread.sleep(500)
        channel.disconnect()
        session.disconnect()
        return resultat
    }
    
//-------------------------------      Fonction qui réutilise la session ouverte  par connect()   ---------------------------------------
    suspend fun execute(context: Context, command: String): String = withContext(Dispatchers.IO) {
        // 3. Tentative de reconnexion automatique si besoin
        if (sessionMain == null || !sessionMain!!.isConnected) {
            val isConnected = connect(context)
            if (!isConnected) return@withContext "Erreur : Impossible de se connecter"
        }
        // 2. Utiliser sessionMain qui est maintenant garanti comme connecté
        try {
            //val channel = sessionMain?.openChannel("exec") as com.jcraft.jsch.ChannelExec
            val channel = sessionMain?.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            android.util.Log.d("SSH_COMMAND", "SSHManager-122 : $command")
            // Il est important de récupérer le flux de sortie AVANT le connect() du channel
            val inputStream = channel.inputStream
            channel.connect()
            // Lire la réponse du serveur
            val response = inputStream.bufferedReader().use { it.readText() }
            channel.disconnect()
            if (response.isEmpty()) "Commande exécutée (pas de retour)" else response
        } catch (e: Exception) {
            "Erreur d'exécution : ${e.message}"
        }
    }

    fun disconnect() {
        session?.disconnect()
        session = null
    }
}