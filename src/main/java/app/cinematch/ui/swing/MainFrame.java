package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.ChatAgent;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Fenêtre principale de l’application <b>CineMatch 🎬 Deluxe</b>.
 *
 * <p>Cette fenêtre gère la navigation entre différents écrans (cartes) via un
 * {@link CardLayout}. Les vues suivantes sont enregistrées :</p>
 * <ul>
 *   <li>{@code "home"} — écran d’accueil ({@link HomePanel})</li>
 *   <li>{@code "t1"} — outil « Film similaire » ({@link Tool1Panel})</li>
 *   <li>{@code "t2"} — outil « Swipe » ({@link Tool2Panel})</li>
 *   <li>{@code "t3"} — outil « Ma liste » ({@link Tool3Panel})</li>
 *   <li>{@code "chat"} — chat IA ({@link Tool4Panel})</li>
 *   <li>{@code "hist"} — historique ({@link HistoryPanel})</li>
 * </ul>
 *
 * <p>Le constructeur principal accepte un {@link ChatAgent} optionnel ; si
 * {@code agent} est {@code null}, le panneau de chat est instancié avec un
 * <i>fallback</i> fonctionnel qui retourne un message d’indisponibilité.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * MovieRecommenderService service = new MovieRecommenderService();
 * ChatAgent agent = new ChatAgent(ollamaClient, Profile.defaultCinemaExpert(), null);
 * MainFrame frame = new MainFrame(service, agent);
 * frame.setVisible(true);
 * }</pre>
 *
 * @see HomePanel
 * @see Tool1Panel
 * @see Tool2Panel
 * @see Tool3Panel
 * @see Tool4Panel
 * @see HistoryPanel
 */
public class MainFrame extends JFrame {

    /** Gestionnaire de cartes permettant de basculer entre les écrans. */
    private final CardLayout cards = new CardLayout();

    /** Conteneur principal qui héberge les différentes vues (cartes). */
    private final JPanel container = new JPanel(cards);

    /** Service principal de recommandation de films, partagé entre les panneaux. */
    private final MovieRecommenderService service;

    /**
     * Construit la fenêtre principale et enregistre toutes les vues.
     *
     * <p>Si {@code agent} est {@code null}, le panneau de chat ({@link Tool4Panel})
     * est créé avec une fonction de réponse par défaut indiquant l’indisponibilité
     * du chat IA.</p>
     *
     * @param service le service de recommandation injecté et partagé
     * @param agent   l’agent de conversation IA (peut être {@code null})
     */
    public MainFrame(final MovieRecommenderService service, final ChatAgent agent) {
        super("CineMatch 🎬 Deluxe");
        this.service = service;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Écrans principaux
        HomePanel home = new HomePanel(this);
        Tool1Panel t1 = new Tool1Panel(service, this::showCard);
        Tool2Panel t2 = new Tool2Panel(service, this::showCard);
        Tool3Panel t3 = new Tool3Panel(service, this::showCard);

        // agent peut être null → fallback fonctionnel
        Tool4Panel chat = (agent != null)
                ? new Tool4Panel(agent, this::showCard)
                : new Tool4Panel(
                q -> "Le chat IA est indisponible pour le moment.",
                this::showCard
        );

        // Historique
        HistoryPanel hist = new HistoryPanel(service, this);

        // Ajouter les vues
        container.add(home, "home");
        container.add(t1, "t1");
        container.add(t2, "t2");
        container.add(t3, "t3");
        container.add(chat, "chat");
        container.add(hist, "hist");

        setContentPane(container);

        // Navigation depuis HomePanel
        home.onNavigate(id -> cards.show(container, id));
    }

    /**
     * Constructeur de compatibilité : délègue au constructeur principal
     * en passant {@code null} pour l’agent IA.
     *
     * @param service le service de recommandation injecté et partagé
     */
    public MainFrame(final MovieRecommenderService service) {
        this(service, (ChatAgent) null);
    }

    /**
     * Affiche la carte identifiée par {@code id}.
     *
     * @param id identifiant logique de la vue à afficher
     *           (ex. {@code "home"}, {@code "t1"}, {@code "t2"}, {@code "t3"}, {@code "chat"}, {@code "hist"})
     */
    public void showCard(final String id) {
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), id);
    }

    /**
     * Retourne le service de recommandation partagé par l’interface.
     *
     * @return l’instance de {@link MovieRecommenderService}
     */
    public MovieRecommenderService getService() {
        return service;
    }
}
