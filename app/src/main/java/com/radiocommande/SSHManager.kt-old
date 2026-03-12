package com.radiocommande
//RadioCommande2
import android.content.Context
import com.jcraft.jsch.ChannelExec
import java.io.InputStream
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

//  telechargement de cle

object SSHManager {
    //  Test de connexion SSH
    // 1. Déclarer la session au niveau de la classe pour qu'elle soit accessible partout
    var session: Session? = null
//    fun connect(user: String, host: String, port: Int = 22, pass: String) {
//        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
//        val ip = prefs.getString("ip", "") ?: ""
//        val user = prefs.getString("user", "") ?: ""
//        val pass = prefs.getString("pass", "") ?: ""
//        val lip = ip.substringBefore(":", ip) // Prend tout si pas de ":"
//        val portString = ip.substringAfter(":", "22") // 22 par défaut si pas de ":"
//        val port = portString.toIntOrNull() ?: 22
//        val jsch = JSch()
//        session_main = jsch.getSession(user, host, port)
//        session_main?.setPassword(pass)
//        session_main?.setConfig("StrictHostKeyChecking", "no")
//        session_main?.connect()
//    }
    
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
                android.util.Log.e("SSH_REGLAGES", "SSHManager-46: Impossible de contacter le serveur", e)
            }
        }.start()
    }
//  fin test de connexion

    // Cette fonction peut être appelée de n'importe où : MainActivity, SettingsActivity...
    //     connexion SSH avec mot de passe 
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
        //channel.disconnect()
        //session.disconnect()
        return resultat
    }

    fun execute(command: String): String {
        val currentSession = session ?: return "Erreur : Session non connectée"
        if (!currentSession.isConnected) return "Erreur : SSH déconnecté"
        val channel = currentSession.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        val inputStream: InputStream = channel.inputStream
        channel.connect()
        // Lecture de la réponse
        val result = inputStream.bufferedReader().use { it.readText() }
        channel.disconnect()
        return result.trim()
    }
    
    fun disconnect() {
        session?.disconnect()
    }
}