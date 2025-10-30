# CineMatch 🎬 Deluxe

## Objectif du projet

CineMatch est une application de recommandations de films avec interface Swing au look néon. Elle combine un service de suggestion propulsé par un modèle Ollama et une interface multi-outils permettant de trouver une œuvre similaire à un film aimé, de découvrir des pépites aléatoires, de gérer sa liste personnelle, de consulter l’historique et d’échanger avec un agent conversationnel spécialisé cinéma.【F:src/main/java/app/cinematch/App.java†L15-L58】【F:src/main/java/app/cinematch/ui/swing/MainFrame.java†L12-L101】

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

- **App** initialise FlatLaf, configure le client Ollama (URL et modèle via variables d’environnement) et instancie la fenêtre principale.【F:src/main/java/app/cinematch/App.java†L32-L52】
- **MovieRecommenderService** encapsule la génération de recommandations (à partir d’un film aimé ou aléatoires), les descriptions et l’enregistrement des statuts via un point d’injection persistant.【F:src/main/java/app/cinematch/MovieRecommenderService.java†L14-L128】
- **ChatAgent** orchestre le dialogue avec le LLM pour le panneau de chat, s’appuyant sur une mémoire conversationnelle légère et un profil métier (expert cinéma).【F:src/main/java/app/cinematch/agent/ChatAgent.java†L27-L132】
- **Interface Swing** repose sur un `CardLayout` : `Tool1Panel` (film similaire + description), `Tool2Panel` (mode « swipe »), `Tool3Panel` (liste personnelle), `Tool4Panel` (chat IA) et `HistoryPanel` (journal des interactions).【F:src/main/java/app/cinematch/ui/swing/MainFrame.java†L12-L78】
- **Utilitaires** : `JsonStorage` gère un fichier JSON persistant et `ImageLoader` centralise le chargement des visuels pour l’interface.【F:src/main/java/app/cinematch/util/JsonStorage.java†L20-L104】【F:src/main/java/app/cinematch/util/ImageLoader.java†L15-L76】

## Installation et exécution

### Prérequis

- Java 17+
- Maven 3.9+
- Une instance [Ollama](https://ollama.ai/) accessible (locale ou distante)

### Étapes

1. Cloner le dépôt puis se placer à la racine :
   ```bash
   git clone <url-du-depot>
   cd tsettssea
   ```
2. Exporter les variables d’environnement si besoin (valeurs par défaut : `http://localhost:11434` et `qwen2.5:7b-instruct`) :
   ```bash
   export OLLAMA_BASE_URL=http://localhost:11434
   export OLLAMA_MODEL=qwen2.5:7b-instruct
   ```
3. Lancer la compilation et l’exécution :
   ```bash
   mvn clean package
   mvn exec:java -Dexec.mainClass=app.cinematch.App
   ```
4. (Optionnel) Générer les rapports qualité :
   ```bash
   mvn verify
   mvn site
   ```

Les commandes Maven utilisent les plugins déclarés dans le `pom.xml` (Checkstyle, SpotBugs, JaCoCo) pour vérifier la qualité du code et produire les rapports HTML.【F:pom.xml†L55-L208】

## Modèle Ollama

L’application interroge par défaut le modèle **`qwen2.5:7b-instruct`**, configurable via la variable d’environnement `OLLAMA_MODEL`. Toutes les requêtes transitent par `OllamaClient`, basé sur LangChain4j pour la gestion des prompts et du streaming.【F:src/main/java/app/cinematch/App.java†L38-L54】【F:src/main/java/app/cinematch/api/OllamaClient.java†L24-L87】

## Répartition des tâches

- **Intégration IA & prompts** : conception des prompts JSON stricts, implémentation du `MovieRecommenderService` et du `ChatAgent`, maintien des profils conversationnels et de la mémoire de session.【F:src/main/java/app/cinematch/MovieRecommenderService.java†L52-L128】【F:src/main/java/app/cinematch/agent/ChatAgent.java†L27-L132】
- **Interface utilisateur** : design néon, navigation `CardLayout` et interactions asynchrones (`SwingWorker`) pour les panneaux `Tool1Panel` à `Tool4Panel`, ainsi que la gestion de l’historique et des visuels via `ImageLoader`.【F:src/main/java/app/cinematch/ui/swing/Tool1Panel.java†L24-L188】【F:src/main/java/app/cinematch/ui/swing/MainFrame.java†L12-L101】【F:src/main/java/app/cinematch/util/ImageLoader.java†L15-L76】
- **Infrastructure & qualité** : configuration Maven (FlatLaf, LangChain4j, Jackson), pipeline de build (`exec-maven-plugin`, `maven-jar-plugin`), outils de qualité (`checkstyle`, `spotbugs`, `jacoco`) et persistance locale (`JsonStorage`).【F:pom.xml†L11-L162】【F:src/main/java/app/cinematch/util/JsonStorage.java†L20-L104】

Chaque sous-ensemble peut évoluer indépendamment : l’IA (prompts et client) expose une API textuelle, tandis que l’interface consomme exclusivement les objets `Recommendation` et les callbacks fournis par le service, facilitant les itérations futures.
