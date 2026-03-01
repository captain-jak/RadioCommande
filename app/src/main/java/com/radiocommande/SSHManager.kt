//package com.example.radiocommande
package com.radiocommande

import android.content.Context
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import java.util.Properties

//  telechargement de cle
import com.jcraft.jsch.ChannelSftp
import java.io.File
import java.io.FileOutputStream


object SSHManager {

    //  Test de connexion SSH
    fun testerConnexion(context: Context, callback: (Boolean) -> Unit) {
        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        Thread {
            try {
                val jsch = com.jcraft.jsch.JSch()
                // On tente d'ajouter la clé si elle existe
                //val keyFile = File(context.filesDir, "ssh_key")
                //if (keyFile.exists()) jsch.addIdentity(keyFile.absolutePath)
                val session = jsch.getSession(user, ip, 2523)
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
                (context as? android.app.Activity)?.runOnUiThread {
                     callback(false)
                }
            }
        }.start()
    }
//  fin test de connexion

    // Cette fonction peut être appelée de n'importe où : MainActivity, SettingsActivity...
    //     connexion SSH avec mot de passe 
    fun executerCommandeSSH(context: Context, commande: String) {
        val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
        
        val ip = prefs.getString("ip", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        if (ip.isEmpty() || user.isEmpty()) {
            android.util.Log.d("SSH_DEBUG", "Pas de connexion ")
            return
        }

        Thread {
            try {
                val jsch = com.jcraft.jsch.JSch()
                //  UTILISATION DE LA CLÉ 
                //val keyFile = File(context.filesDir, "ssh_key")
//#=>                if (keyFile.exists()) {
//#=>                    jsch.addIdentity(keyFile.absolutePath)
//#=>                } else {
//#=>                    // Si pas de clé, on pourrait utiliser le mot de passe en secours
//#=>                    val pass = prefs.getString("pass", "") ?: ""
//#=>                    //session.setPassword(pass)
//#=>                }
                val session = jsch.getSession(user, ip, 2523)
                // On définit le mot de passe pour la session
                session.setPassword(pass)
                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                // Timeout court (2 secondes) pour ne pas faire attendre l'utilisateur
                session.connect(2000) 
                val channel = session.openChannel("exec") as ChannelExec
                channel.setCommand(commande)
                channel.connect()
                android.util.Log.d("SSH_COMMAND", "Exécution de : $commande")
                // On laisse un peu de temps pour l'exécution
                Thread.sleep(500)
                
                channel.disconnect()
                session.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}

// Dans votre object SSHManager
fun telechargerFichier(context: Context, cheminServeur: String, nomFichier: String) {
    val prefs = context.getSharedPreferences("SSH_REGLAGES", Context.MODE_PRIVATE)
    val host = prefs.getString("host", "") ?: ""
    val user = prefs.getString("user", "") ?: ""
    // On ne récupère plus le mot de passe, mais le chemin de la clé si besoin
    
    Thread {
        try {
            val jsch = com.jcraft.jsch.JSch()
            
            // --- CONFIGURATION DE LA CLÉ PRIVÉE ---
            // Supposons que votre clé est dans les fichiers internes de l'app
            val pathKey = File(context.filesDir, "id_rsa").absolutePath
            jsch.addIdentity(pathKey) 
            
            val session = jsch.getSession(user, host, 2523)
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(3000)

            // Ouverture du canal SFTP
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()

            // Dossier de destination sur le téléphone (Téléchargements)
            val localDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val localFile = File(localDir, nomFichier)
            
            val outputStream = FileOutputStream(localFile)
            
            // Téléchargement effectif
            channel.get(cheminServeur, outputStream)

            outputStream.close()
            channel.disconnect()
            session.disconnect()

            (context as? android.app.Activity)?.runOnUiThread {
                android.widget.Toast.makeText(context, "Téléchargé dans Downloads !", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                android.widget.Toast.makeText(context, "Erreur SFTP : ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }.start()
}


///----------------------------      connexion SSH avec cle -------------------------
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
