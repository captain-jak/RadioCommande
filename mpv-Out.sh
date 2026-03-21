#!/bin/bash

# syntaxe:  ./mpv-Out.sh "Chants tahitiens traditionnels/"
# --- ----------------------   CONFIGURATION ----------------------------------
album=""
genre="Folklore du monde"
#BASE_DIR="$HOME/Musique/$SEARCH_DIR"
#BASE_DIR="/media/enjoy/Data/musique/$SEARCH_DIR"
#--------------------------------------------------------------------------------------------------------
#-----------------------------------------------------------------------------------------
#    Analyse de la commande passée:
#-----------------------------------------------------------------------------------------
UPDATE_MODE=false
BASE_DIR=""
SEARCH_DIR=""
ANNEE=""
GENRE=""
album=""

# Boucle pour lire les arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --base_dir)
      BASE_DIR="$2"
      shift 2 # On décale de 2 pour passer à l'argument suivant
      ;;
      --dir)
      SEARCH_DIR="$2"
      shift 2 # On décale de 2 pour passer à l'argument suivant
      ;;
    *)
      echo "Argument inconnu : $1"
      shift # On décale de 1 seulement si l'argument n'est pas reconnu
      ;;
  esac
done
BASE_DIR="$BASE_DIR/$SEARCH_DIR"
#-----------------------------------------------------------------------------------------------------------------------------------------------
# Vérification : si l'argument est vide (-z)
if [ -z "$SEARCH_DIR" ]; then
    echo "❌ Erreur : Vous devez entrer un répertoire de recherche"
    echo "Usage : $0 \"Georges Brassens\""
    exit 1
fi
# --- VÉRIFICATION ---
if [ ! -d "$BASE_DIR" ]; then
    echo "❌ Erreur : Le répertoire $BASE_DIR n'existe pas."
    exit 1
fi

echo "🔍 Analyse de : $BASE_DIR"
echo "-------------------------------------------------------"

# --- BOUCLE DE TRAITEMENT ---
echo "✅  Recherche de fichiers mp3 dans $BASE_DIR\n"
compteur=1
# Tri par ordre alphabétique des Titres
find "$BASE_DIR" -type f -iname "*.mp3" -print0 | sort -z | while IFS= read -r -d '' fichier; do
    
    # 1. Extraction des métadonnées actuelles
    JSON=$(ffprobe -v quiet -print_format json -show_format "$fichier")
    
    # 2. Récupération des tags (Artiste, Titre, Album, Ane)
    ARTISTE=$(echo "$JSON" | jq -r '.format.tags.artist // empty')
    TITRE=$(echo "$JSON" | jq -r '.format.tags.title // empty')
    ALBUM=$(echo "$JSON" | jq -r '.format.tags.album // empty')
    ANNEE=$(echo "$JSON" | jq -r '.format.tags.date // .format.tags.year // empty' | grep -oE '[0-9]{4}' | head -n 1)
    FOLKLORE=$(echo "$JSON" | jq -r '.format.tags.genre // empty')
    DURATION=$(echo "$JSON" | jq -r '.format.duration // empty')
    
DURATION=${DURATION%.*}
min=$(( DURATION / 60 ))
sec=$(( DURATION % 60 ))

    ##=># 3. Logique de repli si les tags sont vides ou génériques
    ##=># Pour l'Artiste
    ##=>[ -z "$ARTISTE" ] || [[ "$ARTISTE" == *"inconnu"* ]] && ARTISTE="$ARTIST_SEARCH"

    ##=># Pour le Titre (on extrait du nom de fichier après le " - ")
    ##=>if [ -z "$TITRE" ] || [[ "$TITRE" == *"inconnu"* ]]; then
        ##=>NOM_SANS_EXT=$(basename "$fichier" .mp3)
        ##=># Utilisation de sed pour éviter l'erreur xargs avec les apostrophes
        ##=>TITRE=$(echo "$NOM_SANS_EXT" | cut -d'-' -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    ##=>fi

    ##=># Pour l'Année (on cherche dans le chemin si vide)
    ##=>if [ -z "$ANNEE" ]; then
        ##=>ANNEE=$(echo "$fichier" | grep -oE '[0-9]{4}' | head -n 1)
    ##=>fi

    # 4. Affichage du résultat
    printf "$compteur🎵 %s - %s [Album: %s] - %s - %02d:%02d \n" "$ARTISTE" "$TITRE" "${ALBUM:-Inconnu}" "${ANNEE:-0000}" "$min" "$sec"

    # 5. Mise à jour réelle des tags (ID3v2)
    # id3v2 -a "$ARTISTE" -t "$TITRE" -y "${ANNEE:-0000}" "$fichier"

    ##=># 6. Rafraîchissement de MPV (si le fichier est en cours de lecture)
    ##=>if [ -S /tmp/mpv-socket ]; then
        ##=>MPV_PATH=$(echo '{ "command": ["get_property", "path"] }' | socat - /tmp/mpv-socket 2>/dev/null | jq -r '.data')
        ##=>if [ "$fichier" == "$MPV_PATH" ]; then
            ##=>echo '{ "command": ["rescan_external_files"] }' | socat - /tmp/mpv-socket > /dev/null
            ##=>echo '{ "command": ["show_text", "Tags actualisés ✅"] }' | socat - /tmp/mpv-socket > /dev/null
        ##=>fi
    ##=>fi
# Incrémentation du compteur
((compteur++))
done

