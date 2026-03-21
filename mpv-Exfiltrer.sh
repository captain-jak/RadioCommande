#!/bin/bash
# créé le 20-03/2026
# syntaxe:  ./mpv-Exfilt.sh "Chants tahitiens traditionnels/"
#-----------------------------------------------------------------------------------------
#    Analyse de la commande passée:
#-----------------------------------------------------------------------------------------
UPDATE_MODE=false
while getopts "U" opt; do
  case $opt in
    U) UPDATE_MODE=true ;;
    *) echo "Usage: $0 [-U] dossier"; exit 1 ;;
  esac
done
# TRÈS IMPORTANT : décale les arguments
# Après ça, $1 ne sera plus "-U", mais votre dossier "Chants tahitiens..."
shift $((OPTIND-1))
# --- ----------------------   CONFIGURATION ----------------------------------
FORCE_ANNEE="1978"
SEARCH_DIR="$1"
BASE_DIR="/srv/Musique/Elvis Costello/$SEARCH_DIR"
#--------------------------------------------------------------------------------------------------------
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

# --- BOUCLE DE TRAITEMENT ---
echo "✅  Recherche de fichiers mp3 dans $BASE_DIR\n"
compteur=1
# Tri par ordre alphabétique des Titres
find "$BASE_DIR" -type f -iname "*.mp3" -print0 | sort -z | while IFS= read -r -d '' fichier; do
 file=$(basename "$fichier")
    # -------------    si fichier n'est pas un  mp3, passer au fichier suivant  ------------------
    if [[ "$file" == *.mp3 ]]; then
        # suppression l'extension .mp3 pour faciliter le travail
        temp="${file%.mp3}"
        echo "---------------------------------------------------------------------------------------------------------"
        echo "ℹ️ $compteur -  Traitement de $temp"
    else
        echo "❌ $fichier n'est pas un MP3."
        continue
    fi

    # 1. Extraction des métadonnées actuelles
    JSON=$(ffprobe -v quiet -print_format json -show_format "$fichier")
    
    # 2. Récupération des tags (Artiste, Titre, Album, Ane)
    ARTISTE=$(echo "$JSON" | jq -r '.format.tags.artist // empty')
    TITRE=$(echo "$JSON" | jq -r '.format.tags.title // empty')
    ALBUM=$(echo "$JSON" | jq -r '.format.tags.album // empty')
    ANNEE=$(echo "$JSON" | jq -r '.format.tags.date // .format.tags.year // empty' | grep -oE '[0-9]{4}' | head -n 1)
    FOLKLORE=$(echo "$JSON" | jq -r '.format.tags.genre // empty')
    
    if [ -z "$ARTISTE" ] || [[ -z "$TITRE" ]]; then
        echo "Pas d'exfiltration car pas de nom d'artiste ou de titre"
        continue
    fi
      if [ -n "$FORCE_ANNEE" ]; then ANNEE="$FORCE_ANNEE"; fi
echo " -------------------------   Traitement de $ARTISTE - $TITRE - $ANNEE ----------------------------------------"
    # 4. Affichage du résultat
    printf "$compteur🎵 %s - %s [Album: %s] - %s - %02d:%02d \n" "$ARTISTE" "$TITRE" "${ALBUM:-Inconnu}" "${ANNEE:-0000}" "$min" "$sec"

# Expurger les "--"
SANS_TIRET_ARTISTE="${ARTISTE//-/ }"
SANS_TIRET_TITRE="${TITRE//-/ }"
SANS_TIRET_ANNEE="${ANNEE//-/ }"
    if [ "$UPDATE_MODE" = true ]; then
        mv "$fichier" "$BASE_DIR/$SANS_TIRET_ARTISTE - $SANS_TIRET_TITRE - $SANS_TIRET_ANNEE.mp3"
        echo "Nouveau nom du fichier: $SANS_TIRET_ARTISTE - $SANS_TIRET_TITRE - $SANS_TIRET_ANNEE.mp3"
    else
        echo "Ancien nom: $fichier"
        echo "Debug nouveau nom du fichier: $BASE_DIR/$SANS_TIRET_ARTISTE - $SANS_TIRET_TITRE - $SANS_TIRET_ANNEE.mp3"
    fi
# Incrémentation du compteur
((compteur++))
    # réinitialisation des variables
    annee=""; file=""; titre=""; artiste="";file="";temp="";annee="";temp=""
done
        echo "---------------------------------------------------------------------------------------------------------"
   if [ "$UPDATE_MODE" = false ]; then  echo "⚠️  Mode lecture seule (utilisez -U pour appliquer les changements)"; fi


