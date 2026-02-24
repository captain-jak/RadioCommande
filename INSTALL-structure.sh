1️⃣ Structure du projet Android Studio RadioCommande

├─ app/
│   ├─ src/main/java/com/example/voiceserver/
│   │   └─ MainActivity.kt           # Logique micro + Vosk + SSH
│   ├─ src/main/res/layout/
│   │   └─ activity_main.xml         # Bouton micro
│   ├─ src/main/assets/
│   │   ├─ model-fr/                 # Modèle Vosk français
│   │   └─ id_rsa                    # Clé privée SSH (user@serveur)
│   └─ AndroidManifest.xml           # Permissions micro + internet
├─ build.gradle
└─ settings.gradle
