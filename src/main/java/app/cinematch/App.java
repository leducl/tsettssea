package app.cinematch;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.ChatAgent;
import app.cinematch.agent.Memory;
import app.cinematch.agent.Profile;
import app.cinematch.api.OllamaClient;
import app.cinematch.agent.langchain.LangChain4jAgentBridge;
import app.cinematch.ui.swing.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::startUi);
    }

    private static void startUi() {
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception ex) { ex.printStackTrace(); }

        // Variables d'environnement
        String ollamaUrl   = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String ollamaModel = System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5:7b-instruct");

        // Services
        MovieRecommenderService recommender = new MovieRecommenderService(ollamaUrl, ollamaModel);
        Profile profile = Profile.defaultCinemaExpert(); // <-- créer AVANT usage
        LangChain4jAgentBridge bridge = new LangChain4jAgentBridge(ollamaUrl, ollamaModel, profile, recommender);

        // Client Ollama (le client REST maison, utile pour d'autres usages)
        OllamaClient ollamaClient = new OllamaClient(ollamaUrl, ollamaModel);

        // ChatAgent qui délègue ses réponses au bridge LangChain4j (tools wishlist)
        ChatAgent agent = new ChatAgent(ollamaClient, profile, new Memory(), bridge::ask);

        // UI principale
        new MainFrame(recommender, agent).setVisible(true);
    }
}
