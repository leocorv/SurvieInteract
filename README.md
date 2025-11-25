# SurvieInteract
Mod Minecraft Fabric 1.21.9

## Description
SurvieInteract est un mod Fabric destiné à servir de base pour un système d’interactions Twitch → Minecraft.  
Pour le moment, le dépôt contient uniquement la structure du mod Fabric (côté serveur et côté client), prête à recevoir les fonctionnalités futures.

Le mod est construit pour être compatible avec **Fabric Loader** et **Fabric API** en version **1.21.9**.

## État actuel du projet
Le repository contient :

- configuration Gradle Fabric (1.21.9)
- code source du mod (server + client split)
- fichiers mixins (`survieinteract.mixins.json`)
- `fabric.mod.json`
- structure propre pour ajouter la logique serveur et client

Le mod ne contient **pas encore** :
- la communication avec Twitch
- le broker externe
- les effets gameplay
- les effets visuels client

Ces éléments seront ajoutés progressivement.

## Objectif du mod
Servir de base pour :

- réception d’événements externes (via socket ou autre)
- déclenchement d’actions côté serveur
- envoi de packets vers le client pour des effets visuels
- système de consent utilisateur (opt-in / opt-out)

## Compilation
Prérequis :
- JDK 21+
- Gradle intégré via Fabric Loom

Build :
./gradlew build


Le jar généré se trouve dans `build/libs`.

## Licence
All Rights Reserved.

