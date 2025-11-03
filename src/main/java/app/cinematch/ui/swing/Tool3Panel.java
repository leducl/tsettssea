package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Panneau « Ma liste » affichant les films marqués en {@code "envie"} et
 * permettant d’en générer une description, d’actualiser la liste, ou de
 * retirer un élément (en le marquant {@code "pas_interesse"}).
 *
 * <p>Le panneau présente :
 * <ul>
 *   <li>Une liste défilante des titres (colonne gauche) ;</li>
 *   <li>Une zone de description HTML (colonne droite) ;</li>
 *   <li>Un bandeau d’actions (rafraîchir, décrire, retirer) et un bouton retour.</li>
 * </ul>
 *
 * <p>Le thème visuel reprend le style « néon » utilisé dans le reste de l’UI
 * (bordures composées, couleurs de survol) et un fond en dégradé vertical.</p>
 *
 * <p><b>Amélioration :</b> ajout de 3 boutons de filtre pour afficher
 * les listes par statut : {@code "envie"}, {@code "pas_interesse"}, {@code "deja_vu"}.</p>
 */
public final class Tool3Panel extends JPanel {

    /** Service de recommandation (utilisé pour générer des descriptions). */
    private final MovieRecommenderService service;
    /** Callback de navigation (ex. {@code "home"}). */
    private final Consumer<String> navigator;

    /** Modèle de données de la liste des titres. */
    private final DefaultListModel<String> model = new DefaultListModel<>();
    /** Liste des titres (gauche). */
    private final JList<String> list = new JList<>(model);
    /** Zone HTML d’affichage de la description (droite). */
    private final JEditorPane descPane = new JEditorPane("text/html", "");

    /** Bouton d’actualisation de la liste. */
    private final JButton refresh = new JButton("Rafraîchir");
    /** Bouton de génération de description pour l’élément sélectionné. */
    private final JButton describe = new JButton("Générer description");
    /** Bouton de retrait (marque « pas intéressé »). */
    private final JButton remove = new JButton("Retirer");
    /** Bouton retour. */
    private final JButton backBtn = new JButton("Retour");

    // --- Nouveaux boutons de filtre ---
    /** Bouton filtre: afficher les films marqués "envie". */
    private final JButton btnEnvie = new JButton("Envie de voir");
    /** Bouton filtre: afficher les films marqués "pas_interesse". */
    private final JButton btnNope = new JButton("Pas intéressé");
    /** Bouton filtre: afficher les films marqués "deja_vu". */
    private final JButton btnSeen = new JButton("Déjà vu");

    /** Statut actuellement affiché (filtre courant). */
    private String currentStatus = "envie";

    // --- Thème ---

    /** Couleur néon rose. */
    private static final Color NEON_PINK = new Color(255, 64, 160);
    /** Variante plus sombre du néon rose. */
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    /** Couleur de texte au survol. */
    private static final Color HOVER_PINK_TXT = new Color(255, 210, 230);
    /** Couleur de fond de carte par défaut. */
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    /** Couleur de fond de carte au survol. */
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    /** Couleur haute du dégradé d’arrière-plan. */
    private static final Color BG_TOP = new Color(18, 18, 24);
    /** Couleur basse du dégradé d’arrière-plan. */
    private static final Color BG_BOTTOM = new Color(35, 20, 40);
    /** Couleur de texte secondaire. */
    private static final Color TEXT_DIM = new Color(220, 220, 220);
    /** Taille des boutons */
    private static final java.awt.Dimension ACTION_BTN_SIZE = new java.awt.Dimension(180, 42);

    /**
     * Construit le panneau « Ma liste » et installe l’interface, les styles et les actions.
     *
     * @param service   service de recommandation (non {@code null})
     * @param navigator callback de navigation (ex. {@code "home"}) (non {@code null})
     * @throws NullPointerException si {@code service} ou {@code navigator} est {@code null}
     */
    public Tool3Panel(final MovieRecommenderService service,
                      final Consumer<String> navigator) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");

        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        // --- Barre du haut : retour + actions + filtres ---
        final JPanel topBar = new JPanel(new BorderLayout(12, 12));
        topBar.setOpaque(false);

        final JPanel leftTop = new JPanel();
        leftTop.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> this.navigator.accept("home"));
        leftTop.add(backBtn);

        final JPanel actions = new JPanel();
        actions.setOpaque(false);
        styleNeon(refresh);
        styleNeon(describe);
        styleNeon(remove);
        actions.add(refresh);
        actions.add(describe);
        actions.add(remove);

        // --- Barre de filtres (nouvelle) ---
        final JPanel filterPanel = new JPanel();
        filterPanel.setOpaque(false);
        styleNeon(btnEnvie);
        styleNeon(btnNope);
        styleNeon(btnSeen);
        filterPanel.add(btnEnvie);
        filterPanel.add(btnNope);
        filterPanel.add(btnSeen);

        final JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setOpaque(false);
        rightContainer.add(actions, BorderLayout.NORTH);
        rightContainer.add(filterPanel, BorderLayout.SOUTH);

        topBar.add(leftTop, BorderLayout.WEST);
        topBar.add(rightContainer, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // --- Liste gauche ---
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(BASE_CARD_BG);
        list.setForeground(Color.WHITE);
        list.setFixedCellHeight(36);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        list.setCellRenderer(new NeonListCellRenderer());

        final JScrollPane leftScroll = new JScrollPane(list);
        styleScrollAsCard(leftScroll, "MA LISTE");

        // --- Zone description droite ---
        descPane.setEditable(false);
        descPane.setOpaque(false);
        setDescHtml("<i>Sélectionnez un film pour voir la description.</i>");
        final JScrollPane rightScroll = new JScrollPane(descPane);
        styleScrollAsCard(rightScroll, "DESCRIPTION");

        // --- Split horizontal ---
        final JSplitPane split =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.36);
        add(split, BorderLayout.CENTER);

        // --- Actions ---
        refresh.addActionListener(e -> loadByStatus(currentStatus));
        describe.addActionListener(e -> generateForSelection());
        remove.addActionListener(e -> removeSelection());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setDescHtml("<i>Génération possible avec le bouton ci-dessus.</i>");
            }
        });

        // --- Filtres (nouveau) ---
        btnEnvie.addActionListener(e -> loadByStatus("envie"));
        btnNope.addActionListener(e -> loadByStatus("pas_interesse"));
        btnSeen.addActionListener(e -> loadByStatus("deja_vu"));

        // Chargement initial : garde le comportement d’origine
        loadWishlist();
    }

    /**
     * Recharge le modèle de liste à partir du stockage JSON en ne gardant que
     * les titres marqués {@code "envie"}.
     * <p>
     * Conserve la compatibilité avec l’API existante en déléguant à
     * {@link #loadByStatus(String)} avec {@code "envie"}.
     * </p>
     */
    private void loadWishlist() {
        loadByStatus("envie");
    }

    /**
     * Recharge le modèle de liste à partir du stockage JSON en filtrant par statut
     * et préserve/rétablit une sélection utile pour les actions suivantes.
     *
     * @param status statut à afficher ("envie", "pas_interesse", "deja_vu")
     */
    private void loadByStatus(final String status) {
        currentStatus = status;

        // Mémorise la sélection courante pour la restaurer si possible
        final String previouslySelected = list.getSelectedValue();

        model.clear();
        final List<String> items = JsonStorage.getByStatus(status);
        for (String t : items) {
            String cleaned = stripQuotes(t).trim();
            if (!cleaned.isEmpty()) {                 // <-- évite la case vide
                model.addElement(cleaned);
            }
        }


        // Restaure la sélection si l’élément est encore présent ; sinon, sélectionne le 1er
        if (previouslySelected != null) {
            int idx = -1;
            for (int i = 0; i < model.size(); i++) {
                if (previouslySelected.equals(model.get(i))) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                list.setSelectedIndex(idx);
            } else if (!model.isEmpty()) {
                list.setSelectedIndex(0);
            } else {
                list.clearSelection();
            }
        } else if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        } else {
            list.clearSelection();
        }

        setDescHtml("<i>Liste affichée : " + escape(status) + "</i>");
    }


    /**
     * Supprime les guillemets typographiques et ASCII du texte fourni.
     *
     * @param s texte source (peut être {@code null})
     * @return texte nettoyé (jamais {@code null})
     */
    private String stripQuotes(final String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[\"“”«»]", "");
    }

    /**
     * Lance la génération asynchrone de la description pour l’élément sélectionné.
     * Actualise l’UI en mode occupé le temps de l’opération.
     */
    private void generateForSelection() {
        final String t = list.getSelectedValue();
        if (t == null) {
            return;
        }
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return service.generateDescription(t);
            }
            @Override
            protected void done() {
                try {
                    final String txt = get();
                    setDescHtml(escape(txt).replace("\n", "<br/>"));
                } catch (final Exception ex) {
                    setDescHtml("<span style='color:#ff8ab8;'>Erreur :</span> "
                            + escape(ex.getMessage()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * Retire l’élément sélectionné de la liste en le marquant
     * {@code "pas_interesse"}, puis rafraîchit l’affichage selon le filtre courant.
     */
    private void removeSelection() {
        final String t = list.getSelectedValue();
        if (t == null) {
            return;
        }
        JsonStorage.addOrUpdate(t, "pas_interesse");
        loadByStatus(currentStatus);
        setDescHtml("<i>Retiré de la liste.</i>");
    }

    /**
     * Active/désactive les contrôles pendant les tâches asynchrones.
     *
     * @param busy {@code true} pour désactiver temporairement l’UI
     */
    private void setBusy(final boolean busy) {
        refresh.setEnabled(!busy);
        describe.setEnabled(!busy);
        remove.setEnabled(!busy);
        list.setEnabled(!busy);
        btnEnvie.setEnabled(!busy);
        btnNope.setEnabled(!busy);
        btnSeen.setEnabled(!busy);
        backBtn.setEnabled(!busy);
    }

    /**
     * Applique un style « néon » (bordures composées, couleurs de survol) aux boutons.
     *
     * @param b bouton à styliser
     */
    private void styleNeon(final JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);
        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
        ));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setBackground(HOVER_CARD_BG);
                b.setForeground(HOVER_PINK_TXT);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 2, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)
                ));
            }
            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setBackground(BASE_CARD_BG);
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK_DARK, 2, true),
                        new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
                ));
            }
        });
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        // Taille uniforme pour tous les boutons d’action
        b.setPreferredSize(ACTION_BTN_SIZE);
    }

    /**
     * Applique un style « outlined » au bouton Retour (texte pâle + contour).
     *
     * @param b bouton à styliser
     */
    private void styleBackOutlined(final JButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(TEXT_DIM);
        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }
            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setForeground(TEXT_DIM);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    /**
     * Habille un {@link JScrollPane} avec une bordure « carte » et un en-tête titre.
     *
     * @param sp    panneau de défilement à styliser
     * @param title titre affiché en tête
     */
    private void styleScrollAsCard(final JScrollPane sp, final String title) {
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 3, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 2, true),
                        new EmptyBorder(8, 8, 8, 8))
        ));
        final JLabel head = new JLabel(title, SwingConstants.CENTER);
        head.setOpaque(true);
        head.setBackground(new Color(20, 20, 28, 180));
        head.setForeground(Color.WHITE);
        head.setFont(new Font("Segoe UI", Font.BOLD, 16));
        head.setBorder(new EmptyBorder(10, 12, 10, 12));
        sp.setColumnHeaderView(head);
    }

    /**
     * Définit le HTML de la zone de description (sans {@code String.format}), avec
     * un conteneur simple et des styles minimaux.
     *
     * @param inner contenu HTML interne
     */
    private void setDescHtml(final String inner) {
        final String html =
                "<html>" + System.lineSeparator()
                        + "  <body style=\"margin:0;padding:0;font-family:'Segoe UI',sans-serif;color:#f5f5f5;\">"
                        + System.lineSeparator()
                        + "    <div style=\"padding:4px 6px; min-height:220px;\">"
                        + System.lineSeparator()
                        + "      <div style=\"font-size:15px;line-height:1.5;\">"
                        + inner
                        + "</div>" + System.lineSeparator()
                        + "    </div>" + System.lineSeparator()
                        + "  </body>" + System.lineSeparator()
                        + "</html>";
        descPane.setText(html);
        descPane.setCaretPosition(0);
    }

    /**
     * Échappe minimalement le HTML (caractères spéciaux usuels).
     *
     * @param s texte source (peut être {@code null})
     * @return texte échappé (jamais {@code null})
     */
    private static String escape(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Rendu personnalisé de la liste, appliquant le thème « néon »
     * et nettoyant les guillemets typographiques.
     */
    private static final class NeonListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(final JList<?> jl, final Object value,
                                                      final int index, final boolean isSelected,
                                                      final boolean cellHasFocus) {
            final JLabel c =
                    (JLabel) super.getListCellRendererComponent(jl, value, index, isSelected, cellHasFocus);
            final String text = c.getText() == null ? "" : c.getText().replaceAll("[\"“”«»]", "");
            c.setText(text);
            c.setOpaque(true);
            c.setBackground(isSelected ? HOVER_CARD_BG : BASE_CARD_BG);
            c.setForeground(isSelected ? HOVER_PINK_TXT : Color.WHITE);
            c.setBorder(new EmptyBorder(8, 12, 8, 12));
            c.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            return c;
        }
    }

    /**
     * Dessine le fond en dégradé vertical du panneau.
     *
     * @param g contexte graphique
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
