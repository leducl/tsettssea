package app.cinematch.ui.swing;

import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Comparator;
import java.util.List;

/**
 * Panneau Swing affichant l’historique des interactions de l’utilisateur
 * avec l’agent conversationnel ou le système de recommandation.
 *
 * <p>Ce panneau présente une table listant les films enregistrés dans la mémoire
 * persistante (via {@link JsonStorage}), triés par date décroissante.
 * Il offre également un bouton pour rafraîchir les données et un bouton
 * pour revenir à l’écran d’accueil.</p>
 *
 * <p>Ce composant est conçu pour être intégré à une {@link MainFrame}
 * et interagit indirectement avec le service de recommandation
 * {@code MovieRecommenderService}.</p>
 *
 * <p>Structure visuelle :</p>
 * <ul>
 *     <li>Barre supérieure : titre + bouton retour</li>
 *     <li>Zone centrale : table d’historique</li>
 *     <li>Barre inférieure : bouton de rafraîchissement</li>
 * </ul>
 *
 * <p>Exemple d’intégration :
 * <pre>{@code
 * MainFrame frame = new MainFrame();
 * MovieRecommenderService service = new MovieRecommenderService();
 * frame.setContentPane(new HistoryPanel(service, frame));
 * }</pre>
 *
 * @see app.cinematch.model.HistoryEntry
 * @see app.cinematch.util.JsonStorage
 * @see app.cinematch.ui.swing.MainFrame
 */
public class HistoryPanel extends JPanel {

    /** Table principale affichant la liste des entrées d’historique. */
    private final JTable table = new JTable();

    /** Bouton permettant de recharger l’historique depuis le stockage JSON. */
    private final JButton refresh = new JButton("↻ Rafraîchir");

    /** Bouton permettant de revenir au menu principal. */
    private final JButton backBtn = new JButton("⬅ Retour au menu");

    /**
     * Crée un nouveau panneau d’historique lié au service de recommandation.
     *
     * @param service le service de recommandation utilisé (actuellement non utilisé directement)
     * @param parent  la fenêtre principale dans laquelle afficher ce panneau
     */
    public HistoryPanel(final app.cinematch.MovieRecommenderService service, final MainFrame parent) {
        setLayout(new BorderLayout(10, 10));

        // --- Barre du haut ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(backBtn, BorderLayout.WEST);

        JLabel title = new JLabel("Historique des actions", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        topBar.add(title, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        // --- Table centrale ---
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Barre du bas ---
        JPanel bottom = new JPanel();
        bottom.add(refresh);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions des boutons ---
        backBtn.addActionListener(e -> parent.showCard("home"));
        refresh.addActionListener(e -> loadHistory());

        // --- Chargement initial des données ---
        loadHistory();
    }

    /**
     * Recharge la table d’historique en récupérant toutes les entrées
     * stockées dans {@link JsonStorage}, puis les trie par date décroissante.
     *
     * <p>Chaque entrée est ensuite ajoutée dans un {@link DefaultTableModel}
     * pour mise à jour de la {@link JTable} principale.</p>
     */
    private void loadHistory() {
        List<HistoryEntry> all = JsonStorage.loadAll();
        all.sort(Comparator.comparing(HistoryEntry::dateTimeIso).reversed());

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Titre", "Statut", "Date"}, 0);
        for (HistoryEntry e : all) {
            model.addRow(new Object[]{e.title(), e.status(), e.dateTimeIso()});
        }

        table.setModel(model);
    }
}
