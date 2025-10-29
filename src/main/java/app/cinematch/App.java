package app.cinematch;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.ChatAgent;
import app.cinematch.agent.Memory;
import app.cinematch.agent.Profile;
import app.cinematch.api.OllamaClient;
import app.cinematch.ui.swing.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Point d'entrée de l'application CineMatch.
 * <p>Initialise le look & feel, les services (Ollama, agent, reco)
 * puis lance l'interface graphique principale.</p>
 */
public class App {

    /**
     * Méthode main : démarrage de l'application.
     *
     * @param args arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::startUi);
    }

    /**
     * Initialise le L&F, les services et lance la fenêtre principale.
     */
    private static void startUi() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            // On journalise et on continue avec le L&F par défaut.
            ex.printStackTrace();
        }

        // Variables d'environnement
        String ollamaUrl = System.getenv()
                .getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String ollamaModel = System.getenv()
                .getOrDefault("OLLAMA_MODEL", "qwen2.5:7b-instruct");

        // Service de recommandation
        MovieRecommenderService recommender =
                new MovieRecommenderService(ollamaUrl, ollamaModel);

        // Agent IA avec profil et mémoire
        OllamaClient ollamaClient = new OllamaClient(ollamaUrl, ollamaModel);
        Profile profile = Profile.defaultCinemaExpert();
        Memory memory = new Memory();
        ChatAgent agent = new ChatAgent(ollamaClient, profile, memory);

        // Lancement de l'interface principale
        new MainFrame(recommender, agent).setVisible(true);
    }
}
