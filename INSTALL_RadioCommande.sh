#!/bin/bash

#----------------------------------------------------------------------------------
#                                          Prerequis pour la compilation android
#----------------------------------------------------------------------------------
sudo apt install android-ndk
# Since most Android devices are arm64-v8a or armeabi-v7a, you can exclude x86_64 libraries if you don’t need them:
# This: - Prevents warnings for x86_64 - Reduces APK size - Speeds up build
nano app/build.gradle.kt
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a") // only package ARM ABIs
        }
    }

#*************************************************************************************************
######                compilation du projet en mode terminal          ###################
#*************************************************************************************************
# 2️⃣ Nettoyer le projet  (script clean.sh)
cd ~/dev/Reconnaissance_vocale/RadioCommande
./gradlew --stop
ps aux | grep java
rm -rf ~/.gradle/caches && rm -rf ~/.gradle/daemon && rm -rf .gradle/
# 3️⃣ Vérifier les dépendances
# verifier dependances dans app/build.gradle.kts
repositories {
    google()
    mavenCentral() // nécessaire pour jsch et kotlin-stdlib
}
# 4️⃣ Compiler l’application (APK debug)
#1 
# pour verifier en mode debug:
./gradlew --info clean assembleDebug
# puis le build:
 ./gradlew build --refresh-dependencies
# 5️⃣ Installer sur un appareil connecté
# Si tu as un téléphone ou un émulateur branché :
./gradlew installDebug
# Ou manuellement :
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 6️⃣ Optimisation des builds
# Activer le cache de configuration :
# fichier gradle.properties
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true
# Exclure les grandes architectures inutiles (x86_64) via abiFilters
# fichier app/buils.gradle.kts, ajouter dans la section android {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a") // only package ARM ABIs
        }
# Garder seulement le modèle Vosk petit si possible pour réduire la taille de l’APK et le temps de packaging.

#*************************************************************************************************
######                compilation du projet avec AndroidStudio         ##################
#*************************************************************************************************
sudo apt install openjdk-25-jdk unzip wget -y
sudo apt install lib32z1 lib32ncurses6 lib32stdc++6 lib32gcc-s1 -y
# check java version
java -version
# Download Android Studio
wget https://edgedl.me.gvt1.com/android/studio/ide-zips/2025.3.1.8/android-studio-panda1-patch1-linux.tar.gz
# Extract the Archive
sudo tar -xvzf android-studio* -C /srv/
# supprimer le fichier telecharge
rm -rf android-studio-panda*
#Rename folder for convenience:
sudo mv /srv/android-studio /srv/android-studio
cd /srv/android-studio
# Run Android Studio
./bin/studio.sh 
# The first time, it will launch a setup wizard to download the SDK and configure the IDE.
# onfigure Environment Variables (Optional but Useful)
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc