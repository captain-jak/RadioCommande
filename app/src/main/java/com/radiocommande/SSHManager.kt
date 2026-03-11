package com.radiocommande
//RadioCommande2
import android.content.Context
import com.jcraft.jsch.ChannelExec


// ----------  Operation sur le serveur   ----------------
// Mise a jour de la base musicale
//updatedb -l 0 -U /home/enjoy/Musique -o /home/enjoy/musique.db
//COMBIEN=$(locate -i -d /home/enjoy/musique.db "*blood*" | wc -l)
// je joue la 3eme occurence trouvée
//OCCURENCE=3
//CHEMIN_AUDIO=$(locate -i -d /home/enjoy/musique.db "*blood*" | sed -n "${OCCURENCE}p")
//echo "Le fichier sélectionné est : $CHEMIN_AUDIO"
//mpv --no-video "$CHEMIN_AUDIO"
//
//--------------------------------------------------------

//  telechargement de cle

object SSHManager {
    //  Test de connexion SSH
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
                // On tente d'ajouter la clé si elle existe
                //val keyFile = File(context.filesDir, "ssh_key")
                //if (keyFile.exists()) jsch.addIdentity(keyFile.absolutePath)
                //val session = jsch.getSession(user, ip, 2523)
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
            } 
            catch (e: Exception) {
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false)
                }
                android.util.Log.e("SSH_COMMAND", "SSHManage 59 - Impossible de contacter le serveur", e)
            }
        }.start()
    }
//  fin test de connexion

    fun executerCommandeSSH(
        context: Context,
        commande: String,
        callback: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        val lip = ip.substringBefore(":", ip)
        val portString = ip.substringAfter(":", "22")
        val port = portString.toIntOrNull() ?: 22
        Thread {
            try {
                val jsch = com.jcraft.jsch.JSch()
                val session = jsch.getSession(user, lip, port)
                session.setPassword(pass)
                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                session.connect(2000)
                val channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                channel.setCommand(commande)
                val inputStream = channel.inputStream
                channel.connect()
                val reader = inputStream.bufferedReader()
                val cheminRecupere = reader.readLine()
                val nomFichier = java.io.File(cheminRecupere).name
                android.util.Log.d("SSH_COMMAND", "SSHManager 96 - Ecouter: $cheminRecupere")
                var ligne: String? = null
//#=>                while (!channel.isClosed || reader.ready()) {
//#=>                    ligne = reader.readLine()
//#=>                    if (ligne != null) break
//#=>                    Thread.sleep(100)
//#=>                }
                val reussite = channel.exitStatus == 0
                callback(reussite, cheminRecupere)
                Thread.sleep(500)
                channel.disconnect()
                session.disconnect()
            } 
            catch (e: Exception) {
                e.printStackTrace()
                //android.util.Log.d("SSH_COMMAND", "SSHManager 107 -Erreur: $commande")
                callback(false, null)
            }

        }.start()
    }
}