#!/bin/bash

#----------------------------------------------------------------------------------
# Prerequis
sudo apt update
sudo apt install openssh-client
#----------------------------------------------------------------------------------
ssh-keygen -V
# Générer la clé SSH
ssh-keygen -t rsa -b 4096 -C "gen@selfmicro.com"
# Choisir l’emplacement et le mot de passe
Enter file in which to save the key (/home/utilisateur/.ssh/id_rsa):
/home/enjoy/.ssh/gen.selfmicro.com
# Vérifier la création des fichiers
~/.ssh/gen.selfmicro.com      → clé privée (à ne jamais partager)
~/.ssh/gen.selfmicro.com.pub  → clé publique (à copier sur le serveur distant)

# Copier la clé publique sur un serveur distant (activer acces du serveur sshd par mot de passe)
SH_OPTIONS="-o IdentitiesOnly=yes"
ssh-copy-id -i ~/.ssh/gen.selfmicro.com -p 2523 enjoy@192.168.1.80
# si succes de la copie - acces mainetant avec la cle
ssh -i ~/.ssh/gen.selfmicro.com -p 2523 enjoy@192.168.1.80
# Ne pas oublier de desactiver acces du serveur sshd par mot de passe
# Pour une syntaxe + facile, parametres de connexions dasn ~.ssh/config
Host gen
  HostName enjoy.selfmicro.com
  IdentityFile ~/.ssh/gen.selfmicro.com
  User enjoy
  Port 2523
  ServerAliveInterval 60
  IdentitiesOnly yes
# l'acces peut de faire maintenant avec la commande:
ssh gen
