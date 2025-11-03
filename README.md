# CineMatch 🎬 Deluxe

## Objectif du projet

CineMatch est une application de recommandations de films avec interface Swing au look néon. Elle combine un service de suggestion propulsé par un modèle Ollama et une interface multi-outils permettant de trouver une œuvre similaire à un film aimé, de découvrir des pépites aléatoires, de gérer sa liste personnelle, de consulter l’historique et d’échanger avec un agent conversationnel spécialisé cinéma.

[App.java](src/main/java/app/cinematch/App.java) 
/
[MainFrame.java](src/main/java/app/cinematch/ui/swing/MainFrame.java)

**Image de la page d’accueil de l’application.**
![page_principale.png](images/page_principale.png)

## Architecture logicielle

```
src/main/java/app/cinematch
├── App.java                 # Point d’entrée : initialisation L&F, services et UI
├── MovieRecommenderService  # Orchestration des prompts Ollama + persistance JSON
├── api/OllamaClient.java    # Client HTTP LangChain4j vers l’instance Ollama
├── agent/                  
│   ├── ChatAgent.java       # Agent conversationnel + mémoire longue durée
│   ├── ConversationMemory.java / Memory.java / Profile.java
├── model/…                 # Records pour les messages et recommandations
├── ui/swing/               # Panneaux Swing (Home, outils, historique, chat)
└── util/JsonStorage.java   # Stockage local des films marqués en JSON
```

- **App** initialise FlatLaf, configure le client Ollama (URL et modèle via variables d’environnement) et instancie la fenêtre principale.
[App.java](src/main/java/app/cinematch/App.java)
- **MovieRecommenderService** encapsule la génération de recommandations (à partir d’un film aimé ou aléatoires), les descriptions et l’enregistrement des statuts via un point d’injection persistant.
[MovieRecommenderService.java](src/main/java/app/cinematch/MovieRecommenderService.java)
- **ChatAgent** orchestre le dialogue avec le LLM pour le panneau de chat, s’appuyant sur une mémoire conversationnelle légère et un profil métier (expert cinéma).
[ChatAgent.java](src/main/java/app/cinematch/agent/ChatAgent.java)
- **Interface Swing** repose sur un `CardLayout` : `Tool1Panel` (film similaire + description), `Tool2Panel` (mode « swipe »), `Tool3Panel` (liste personnelle), `Tool4Panel` (chat IA) et `HistoryPanel` (journal des interactions), retiré de l’interface finale car jugé non essentiel à l’usage principal.
[MainFrame.java](src/main/java/app/cinematch/ui/swing/MainFrame.java)
- **Utilitaires** : `JsonStorage` gère un fichier JSON persistant et `ImageLoader` centralise le chargement des visuels pour l’interface.
[JsonStorage.java](src/main/java/app/cinematch/util/JsonStorage.java)

## Installation et exécution

### Prérequis

- Java 17+
- Maven 3.9+
- Une instance [Ollama](https://ollama.ai/) accessible (locale ou distante)

### Étapes

1. Cloner le dépôt puis l'ouvrir avec l'IDE de son choix :
   ```bash
   git clone <url-du-depot>
   cd <nom-du-dossier>
   ```
2. Sur un autre cmd, installer ollama et télécharger le modèle (`qwen2.5:7b-instruct`) :
   ```bash
   ollama pull qwen2.5:7b-instruct
   ```
3. Lancer le server ollama local dans ce même cmd (`qwen2.5:7b-instruct`) :
   ```bash
   ollama serve
   ```
4. Sur le projet clôné précédemment, lancer la compilation et l’exécution :
   ```bash
   mvn clean package
   mvn exec:java -Dexec.mainClass=app.cinematch.App
   ```
5. (Optionnel) Générer les rapports qualité :
   ```bash
   mvn verify
   mvn site
   ```

**💡 Remarques :**

Assurez-vous que le serveur Ollama tourne en arrière-plan avant de lancer l’application (ollama serve doit rester ouvert).

Les commandes Maven utilisent les plugins déclarés dans le `pom.xml` (Checkstyle, SpotBugs, JaCoCo) pour vérifier la qualité du code et produire les rapports HTML.
[pom.xml](pom.xml)

## Modèle Ollama

L’application interroge par défaut le modèle **`qwen2.5:7b-instruct`**, configurable via la variable d’environnement `OLLAMA_MODEL`. Toutes les requêtes transitent par `OllamaClient`, basé sur LangChain4j pour la gestion des prompts et du streaming.
[App.java](src/main/java/app/cinematch/App.java) /
[OllamaClient.java](src/main/java/app/cinematch/api/OllamaClient.java)

## Répartition des tâches

**Commun**
- Mise en place de la **version initiale (V1)** du projet.
- Configuration des **dépendances Maven** et de l’environnement de développement.
- Création et paramétrage du **dépôt GitHub** pour le travail collaboratif.
- Installation d’**Ollama** et du **modèle de langage choisi (Gwen)**.
- Développement des **trois outils principaux** :
    - 🎲 Découverte aléatoire de films.
    - 🎬 Suggestion de films similaires à un titre donné.
    - 🕓 Consultation de l’historique et des avis enregistrés.

**Léo**
- IA & Agent : ajout de l’agent conversationnel (`feature/AgentMemory`) et de la **mémoire de conversation** (`feature/ConversationMemory`).
- UX Chat : **refonte visuelle de `Tool4Panel`** et **barre de chargement** pendant la réflexion de l’IA (`feature/LoadingBar`).
- Maintenance : retrait de l’API TMDB ; adaptations des tests à la nouvelle UI du chat.

**Axel**
- Qualité & Tests : mise en place des **outils de qualité** (JaCoCo, SpotBugs, Checkstyle) et **tests JUnit** à large couverture :
    - UI : suites Swing robustes (EDT-safe, headless) pour `Tool1/2/3/4Panel`, `History`, `Home`, `MainFrame`.
- Robustesse : nombreuses **corrections SpotBugs** (copies défensives, non-sérialisation de `SwingWorker`, formats portables).
- CI/Repo : ajustements de workflows/permissions et intégration continue orientée tests/qualité.

**Simon**
- UI/UX : **améliorations visuelles** (accueil, Tool2/Tool3, description, swipe buttons) et **corrections Tool2**.
- Documentation : **Javadocs** sur `api`, `agent`, `model`, `uiSwing`, `util`.
- Qualité : corrections ciblées SpotBugs (dont `ChatAgent`), **coordination & merges** réguliers des PRs.

## 🧪 Tests et qualité logicielle

La mise en place de tests approfondie permettant de garantir sa stabilité, sa robustesse et la conformité aux bonnes pratiques de développement.

### 🔹 Méthodologie
- Les tests unitaires ont été rédigés selon le **format BDD GIVEN / WHEN / THEN**, facilitant la lisibilité et la compréhension du comportement attendu.
- L’ensemble des tests a été implémenté avec **JUnit 5** et **Mockito** pour le mock des dépendances et la simulation des réponses du modèle Ollama.
- L’exécution et le suivi de la couverture sont assurés par **JaCoCo**, intégré au cycle Maven.

### 🔹 Couverture
- La couverture globale dépasse **90 %** sur l’ensemble du projet.
- Plusieurs modules atteignent **100 %** de couverture :
    - `MovieRecommenderService`
    - `OllamaClient`
    - `JsonStorage`
    - `ImageLoader`
- Des tests Swing spécifiques (EDT-safe, headless) ont été ajoutés pour valider la stabilité de l’interface utilisateur (`Tool1Panel` à `Tool4Panel`, `History`, `Home`, `MainFrame`).

### 🔹 Outils de qualité
- **JaCoCo** : mesure de couverture de code.
- **Checkstyle** : respect des conventions de code Java.
- **SpotBugs** : détection statique d’erreurs potentielles.

Ces outils garantissent un code maintenable, conforme aux standards et testable à long terme.
