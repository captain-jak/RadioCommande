#!/bin/bash
# ce script permet d''afficher les informations du titre en train de jouer dans une playlist 

# Récupération du JSON complet
JSON=$(echo '{ "command": ["get_property", "metadata"] }' | socat - /tmp/mpv-socket 2>/dev/null)
# Extraction propre avec jq
TITRE=$(echo "$JSON" | jq -r '.data.title // "Titre inconnu"')
ALBUM=$(echo "$JSON" | jq -r '.data.album // "Album inconnu"')
ARTISTE=$(echo "$JSON" | jq -r '.data.artist // "Artiste inconnu"' | cut -d';' -f1) # On prend le 1er artiste

# JSON metadatat ne retourne pas DURATION, il faut réinterroger mpv en précisant duration
JSON=$(echo '{ "command": ["get_property", "duration"] }' | socat - /tmp/mpv-socket 2>/dev/null)
DURATION=$(echo $JSON | jq -r '.data')

DURATION=${DURATION%.*}
min=$(( DURATION / 60 ))
sec=$(( DURATION % 60 ))

# Affichage 
MSG=$(printf "%s - %s  - %02d:%02d" " $ARTISTE" "$TITRE" "$min" "$sec")
echo $MSG
