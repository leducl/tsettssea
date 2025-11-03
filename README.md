# CineMatch 🎬 Deluxe

## Aperçu

CineMatch est une application Java/Swing de recommandations cinéma qui marie une interface néon responsive et un agent IA piloté par Ollama. L’utilisateur peut explorer des pépites aléatoires, demander une recommandation liée à un film aimé, gérer sa liste personnelle, consulter l’historique et dialoguer avec un assistant conversationnel spécialisé.

![Aperçu de l’accueil](images/page_principale.png)

## Fonctionnalités clés

- **Recommandations intelligentes** : génération d’idées à partir d’un film apprécié ou via un mode découverte aléatoire, avec enrichissement automatique (raison, plateforme, année).【F:src/main/java/app/cinematch/MovieRecommenderService.java†L55-L124】【F:src/main/java/app/cinematch/ui/swing/Tool1Panel.java†L38-L136】
- **Mode swipe** : interface inspirée du « tinder de films » pour accepter/refuser les propositions et enregistrer un statut en un clic.【F:src/main/java/app/cinematch/ui/swing/Tool2Panel.java†L44-L178】
- **Gestion de wishlist et d’historique** : stockage persistant des films vus, à voir ou ignorés, consultable depuis l’outil « Ma liste » et l’onglet Historique.【F:src/main/java/app/cinematch/ui/swing/Tool3Panel.java†L36-L169】【F:src/main/java/app/cinematch/ui/swing/HistoryPanel.java†L33-L145】
- **Chat IA outillé** : le panneau de discussion s’appuie sur LangChain4j pour orchestrer des outils (ajout multiple, génération de description, statistiques) tout en conservant une mémoire glissante de la conversation.【F:src/main/java/app/cinematch/ui/swing/Tool4Panel.java†L40-L179】【F:src/main/java/app/cinematch/agent/langchain/LangChain4jAgentBridge.java†L19-L91】
- **Persistance JSON** : toutes les actions utilisateur sont sérialisées dans `src/main/resources/storage.json`, avec surcharge possible via `-Dcinematch.storage` et mode silencieux pour les tests.【F:src/main/java/app/cinematch/util/JsonStorage.java†L21-L109】

## Architecture logicielle

```
src/main/java/app/cinematch
├── App.java                     # Point d’entrée : FlatLaf + injection des services
├── MovieRecommenderService.java # Prompts Ollama + persistance
├── agent/
│   ├── ChatAgent.java          # Orchestration locale + mémoire courte
│   ├── ConversationMemory.java / Memory.java / Profile.java
│   ├── langchain/
│   │   ├── CineAssistant.java  # Interface LangChain4j avec règles métiers
│   │   └── LangChain4jAgentBridge.java # Pont tools + heuristiques client
│   └── tools/
│       ├── WishlistTools.java  # CRUD wishlist / statuts
│       ├── LibraryTools.java   # Accès JsonStorage depuis l’agent
│       ├── ViewingTools.java   # Génération descriptions / next-to-watch
│       ├── MaintenanceTools.java / BulkTools.java / MultiActionTools.java
├── api/OllamaClient.java       # Client HTTP pour le modèle Ollama
├── model/…                     # Records (Recommendation, HistoryEntry, …)
├── ui/swing/                   # Fenêtres/panneaux (Home, Tool1-4, History)
└── util/JsonStorage.java       # Persistance JSON thread-safe
```

Les tests unitaires reflètent cette organisation sous `src/test/java/app/cinematch`, avec des suites dédiées pour les outils de l’agent, l’UI Swing (mode headless) et les utilitaires.【F:src/test/java/app/cinematch/agent/tools/WishlistToolsTest.java†L14-L131】【F:src/test/java/app/cinematch/ui/swing/Tool2PanelTest.java†L28-L164】【F:src/test/java/app/cinematch/util/JsonStorageTest.java†L19-L138】

## Agent IA & LangChain4j

- `App` instancie un `LangChain4jAgentBridge` configuré sur Ollama (`OLLAMA_BASE_URL`, `OLLAMA_MODEL`) et l’injecte dans `ChatAgent` via un délégué fonctionnel.【F:src/main/java/app/cinematch/App.java†L23-L36】
- Le bridge expose un contrat `CineAssistant` doté d’un prompt système contraignant l’usage des outils et la formulation des réponses.【F:src/main/java/app/cinematch/agent/langchain/CineAssistant.java†L5-L47】
- Des outils LangChain4j spécialisés traduisent les intentions en appels métier : ajout/suppression en masse, modifications de statut, statistiques, recommandations à regarder ensuite, etc.【F:src/main/java/app/cinematch/agent/tools/MultiActionTools.java†L18-L137】
- Un pré-traitement côté client gère les commandes d’ajout multiple avant délégation au LLM, garantissant robustesse même hors connexion modèle.【F:src/main/java/app/cinematch/agent/langchain/LangChain4jAgentBridge.java†L93-L140】

## Persistance & données

- Le stockage par défaut (`storage.json`) contient l’historique des titres et statuts.
- Propriétés système utiles :
  - `-Dcinematch.storage=/chemin/perso.json`
  - `-Dcinematch.storage.silent=true` (désactive les logs d’erreur en tests)
- Les records `HistoryEntry`, `Recommendation` et les utilitaires `JsonStorage`/`ImageLoader` centralisent la sérialisation et le rendu des visuels.【F:src/main/java/app/cinematch/model/HistoryEntry.java†L3-L19】【F:src/main/java/app/cinematch/util/ImageLoader.java†L15-L78】

## Installation & exécution

### Prérequis

- Java 17+
- Maven 3.9+
- [Ollama](https://ollama.ai/) avec un modèle conversationnel compatible (par défaut `qwen2.5:7b-instruct`).

### Étapes

1. **Cloner le dépôt** :
   ```bash
   git clone <url-du-depot>
   cd tsettssea
   ```
2. **Préparer Ollama** (dans un terminal séparé) :
   ```bash
   ollama pull qwen2.5:7b-instruct
   ollama serve
   ```
3. **Configurer les variables (optionnel)** :
   ```bash
   export OLLAMA_BASE_URL="http://localhost:11434"
   export OLLAMA_MODEL="qwen2.5:7b-instruct"
   ```
4. **Compiler et lancer l’application** :
   ```bash
   mvn clean package
   mvn exec:java -Dexec.mainClass=app.cinematch.App
   ```

## Tests & qualité logicielle

- `mvn test` : exécution des tests JUnit 5/Mockito (UI Swing headless, services, outils IA).【F:src/test/java/app/cinematch/MovieRecommenderServiceTest.java†L26-L189】
- `mvn verify` : pipeline qualité complet (JaCoCo, SpotBugs, Checkstyle) avec rapports dans `target/site`.
- `mvn site` : génération du site Maven (rapport HTML JaCoCo, SpotBugs, Checkstyle).

Les tests suivent une approche BDD (Given/When/Then) et atteignent une couverture élevée (>90 %) sur les modules critiques, notamment le service de recommandation, les outils de l’agent et la persistance JSON.【F:src/test/java/app/cinematch/agent/langchain/LangChain4jAgentBridgeTest.java†L20-L134】【F:src/test/java/app/cinematch/util/JsonStorageTest.java†L19-L138】

## Ressources

- Capture d’écran : `images/page_principale.png` (accueil).
- Fichier de persistance initial : `src/main/resources/storage.json` (peut être nettoyé avant déploiement final).

## Contributions principales

- **App & Services** : intégration FlatLaf, configuration Ollama/LLM, service de recommandation résilient.【F:src/main/java/app/cinematch/App.java†L17-L37】【F:src/main/java/app/cinematch/MovieRecommenderService.java†L25-L161】
- **Agent IA** : ChatAgent avec mémoire glissante, pont LangChain4j et suite d’outils spécialisés.【F:src/main/java/app/cinematch/agent/ChatAgent.java†L11-L76】【F:src/main/java/app/cinematch/agent/langchain/LangChain4jAgentBridge.java†L19-L140】
- **Interface Swing** : panneaux thématiques (Home, Tool1–Tool4, Historique) orchestrés par `MainFrame` et ses tests headless.【F:src/main/java/app/cinematch/ui/swing/MainFrame.java†L14-L108】【F:src/test/java/app/cinematch/ui/swing/MainFrameTest.java†L26-L140】
- **Qualité & tests** : suites unitaires et outillage Maven (JaCoCo, SpotBugs, Checkstyle) pour garantir robustesse et maintenabilité.【F:pom.xml†L54-L151】【F:src/test/java/app/cinematch/agent/tools/BulkToolsTest.java†L15-L104】
