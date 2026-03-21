#!/bin/bash
# créé le 20-03/2026
# syntaxe:  ./mpv-InMetada.sh  --base_dir "/srv/Musique" --dir "electro/" --annee [ANNEE]  --genre [GENRE] --album [ALBUM]"
# Analyse des options
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
ALBUM=""


while [[ $# -gt 0 ]]; do
    case "$1" in
        -U)
            UPPERCASE=true
            shift  1 # On décale d'un cran
            ;;
        --base_dir)
            BASE_DIR="$2"
            shift 2 # On décale de deux (l'option + la valeur)
            ;;
        --dir)
            SEARCH_DIR="$2"
            shift 2
            ;;
        *)
            echo "❌ Argument inconnu : $1"
            exit 1
            ;;
    esac
done

# Boucle pour lire les arguments
#while [[ $# -gt 0 ]]; do
  #case "$1" in
      #-U)
      #UPDATE_MODE="$2"
      #shift 2 # On décale de 2 pour passer à l'argument suivant
      #;;
    #--base_dir)
      #BASE_DIR="$2"
      #shift 2 # On décale de 2 pour passer à l'argument suivant
      #;;
      #--dir)
      #SEARCH_DIR="$2"
      #shift 2 # On décale de 2 pour passer à l'argument suivant
      #;;
    #--annee)
      #ANNEE="$2"
      #shift 2
      #;;
    #--genre)
      #GENRE="$2"
      #shift 2
      #;;
    #--album)
      #ALBUM="$2"
      #shift 2
      #;;
    #*)
      #echo "Argument inconnu : $1"
      #shift # On décale de 1 seulement si l'argument n'est pas reconnu
      #;;
  #esac
#done

BASE_DIR="$BASE_DIR/$SEARCH_DIR"
album="$ALBUM"
#-----------------------------------------------------------------------------------------------------------------------------------------------
# Vérification : si l'argument est vide (-z)
if [ -z "$SEARCH_DIR" ]; then
    echo "❌ Erreur : Vous devez entrer un répertoire de recherche"
    echo "syntaxe:./mpv-InMetada.sh  -U false --base_dir '/srv/Musique' --dir 'electro/' [--annee ANNEE]  [--genre GENRE] [--album ALBUM]"
    exit 1
fi
if [ -z "$ANNEE" -o -z "$GENRE" -o -z "$ALBUM" ]; then
    echo "⚠️ Options: $ANNEE $GENRE $ALBUM - Par défaut options vierges - syntaxe:./mpv-InMetada.sh  --base_dir '/srv/Musique' --dir 'electro/' --annee [ANNEE]  --genre [GENRE] --album [ALBUM]"
fi

# --- VÉRIFICATION RÉPERTOIRE ---
if [ ! -d "$BASE_DIR" ]; then
    echo "❌ Erreur : Le répertoire $BASE_DIR n'existe pas."
    exit 1
fi
echo "🚀 Début du traitement dans : $BASE_DIR"
compteur=1
# ---------------------- Recherche fichier mp3 dans $BASE_DIR  ------------------------------------------------------------------------------------
find "$BASE_DIR" -type f -iname "*.mp3" -print0 | sort -z | while IFS= read -r -d '' fichier; do
       #---------------------------------------------------------------------------------------------------------------------------------------------------------------
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
    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    # --------------      NOM du REPERTOIRE  du fichier      extraction    ALBUM -ANNEE             ---------------------------------------
    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    ALBUM_ANNEE=$(basename "$(dirname "$fichier")/")
    if [[ "$ALBUM_ANNEE" =~ ^(.*)" - "([0-9]{4})$ ]]; then
        album="${BASH_REMATCH[1]}"
        annee_dir="${BASH_REMATCH[2]}"
    fi
    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    # --------------      NOM DU FICHIER      extraction    ARTISTE -TITRE - ANNEE                    ---------------------------------------
    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    # Regex pour capturer : (Artiste) - (Titre) (Année)
    IFS='-' read -r artiste titre annee <<< "$temp"
    # Nettoyage des espaces blancs résiduels (indispensable après le split sur '-')
    artiste=$(echo "$artiste" | sed 's/[[:space:]]*$//;s/^[[:space:]]*//')
    titre=$(echo "$titre" | sed 's/[[:space:]]*$//;s/^[[:space:]]*//')
    annee=$(echo "$annee" | sed 's/[[:space:]]*$//;s/^[[:space:]]*//')
    echo "Titre: $titre"
    echo "Artiste: $artiste"
     if [ -z $annee ]; then
       if [ -n $annee_dir ]; then annee="$annee_dir";echo "Année répertoire: $annee_dir"; fi
    else
     echo "Année: $annee"
    fi
    # 2. Vérification et message de succès
    if [[ -z "$artiste" || -z "$titre" ]]; then
        echo "❌ Erreur : Extraction incomplète (Artiste ou Titre manquant)."
    fi

    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    # --------------      METADATA MP3          extraction    ARTISTE -TITRE - ANNEE - GENRE - DUREE           ---------------------
    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    # Extraction JSON
    JSON=$(ffprobe -v quiet -print_format json -show_format "$fichier")
    CUR_ARTIST=$(echo "$JSON" | jq -r '.format.tags.artist // empty')
    CUR_TITRE=$(echo "$JSON" | jq -r '.format.tags.title // empty')
    CUR_ALBUM=$(echo "$JSON" | jq -r '.format.tags.album // empty')
    CUR_ANNEE=$(echo "$JSON" | jq -r '.format.tags.date // .format.tags.year // empty' | grep -oE '[0-9]{4}')
    CUR_GENRE=$(echo "$JSON" | jq -r '.format.tags.genre // empty')
    CUR_DURATION=$(echo "$JSON" | jq -r '.format.duration // empty')
    ##---------------------------------------------------------------------------------------------------------------------------------------------------------------
    # ---------------------------------------      METADATA           Mise à jour            ----------------------------------------------------------------
    #---------------------------------------------------------------------------------------------------------------------------------------------------------------
    if [ -n "$CUR_ARTIST" ]; then artiste=$artiste;echo "🎤 Maj Artiste -> $artiste"; fi
    #if [ -n "$CUR_TITRE" ] ; then titre=$CUR_TITRE;echo "🎵 Maj Titre -> $titre"; fi
    if [ -n "$CUR_TITRE" ] ; then titre=$titre;echo "🎵 Maj Titre -> $titre"; fi
    if [ -z "$CUR_ALBUM" -a -n "$album" ]; then album=$album "⚠️  Maj Album -> $album"; fi
  #  if [ -z "$CUR_ANNEE" -a -n "$annee_dir" ]; then annee=$annee "⚠️  Maj Annèe -> $annee"; fi
    if [ -z "$CUR_ANNEE" -a -z "$annee_dir" ]; then annee=$annee;echo "⚠️  Maj Annèe -> $annee"; fi
    if [ -n "$CUR_ANNEE" ]; then annee="$CUR_ANNEE"; fi
    if [ -n "$ANNEE" ]; then annee="$ANNEE"; fi

    if [ -z "$CUR_GENRE" ] || [[ "$CUR_GENRE" == *"Unknown"* ]]; then echo "⚠️  Maj Genre -> $genre"; fi
    # Execution de la mise à jour si commande passée avec option -U
    # On "décale" les arguments pour ignorer les options traitées
# Cela permet d'utiliser $1 pour le nom du fichier après -U
shift $((OPTIND-1))
    if [ "$UPDATE_MODE" = true ]; then
        # Ici, insérez votre logique de mise à jour ID3
        id3v2 --artist "$artiste" "$fichier"
        id3v2 --song "$titre" "$fichier"
        id3v2 --album "$album" "$fichier"
        id3v2 --year "$annee" "$fichier"
        id3v2 --genre "$genre" "$fichier"
    fi
#---------------------------------------------------------------------------------------------------------------------------------------------------------------
    ((compteur++))
    if [ -n "$annee" ] || [[ -n "$album" ]]; then
        echo "✅  $filename   🎤Artiste: $artiste -- 🎵Titre: $titre 📅 $annee-- Album: $album"
    else
        echo "✅  $filename   🎤Artiste: $artiste -- 🎵Titre: $titre"
    fi
    # réinitialisation des variables
    annee=""; file=""; titre=""; artiste="";file="";temp="";annee="";temp=""
done
if [ "$UPDATE_MODE" = true ]; then
    echo "🚀 Mode Update activé !"
else
    echo "⚠️  Mode lecture seule (utilisez -U pour appliquer les changements)"
fi
echo "🏁 Traitement terminé."
# On "décale" les arguments pour ignorer les options traitées
# Cela permet d'utiliser $1 pour le nom du fichier après -U

