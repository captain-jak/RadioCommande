#!/bin/bash

# Parcourir uniquement les fichiers .mp3 dans un dossier spécifique
# Trouver tous les fichiers (.mp3) et exécuter une action dans répertoire et sous répertoire
# Récupération du JSON complet dfgbsdhsrthserthtr
ARTIST="Brassens"

find /srv/Musique "$ARTIST" -type f -name "*.mp3" -print0 | while IFS= read -r -d '' fichier; do
    echo "Lecture de : $fichier"
    JSON=$(echo '{ "command": ["get_property", "metadata"] }' $fichier
    # Extraction propre avec jq
    TITRE=$(echo "$JSON" | jq -r '.data.title // "Titre inconnu"')
    ALBUM=$(echo "$JSON" | jq -r '.data.album // "Album inconnu"')
    ARTISTE=$(echo "$JSON" | jq -r '.data.artist // "Artiste inconnu"' | cut -d';' -f1) # On prend le 1er artiste
    # Recherche de l'année (on teste plusieurs clés possibles)
    ANNEE=$(echo "$JSON" | jq -r '.data.date // .data.year // empty')
    # Si l'année est vide, on cherche 4 chiffres dans le chemin du fichier
    if [ -z "$ANNEE" ]; then
        PATH_FILE=$(echo '{ "command": ["get_property", "path"] }' | socat - /tmp/mpv-socket | jq -r '.data')
        ANNEE=$(echo "$PATH_FILE" | grep -oE '[0-9]{4}' | head -n 1)
    fi
    MSG=$(printf "%s - %s (%s)" " $ARTISTE" "$TITRE" "$ANNEE")
    echo "🎵 $MSG\n"
    #id3v2 -a "$ARTIST" "$fichier" && id3v2 -y "1964" "$FILE_PATH"
done


#FILE_PATH=$(echo '{ "command": ["get_property", "path"] }' | socat - /tmp/mpv-socket | jq -r '.data')
#id3v2 -a "Jacques Brel" "$FILE_PATH"
#id3v2 -y "1964" "$FILE_PATH"

