package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Panneau Swing permettant de proposer un film similaire à partir d’un film aimé
 * et de générer une description courte, le tout avec un style d’interface « néon ».
 *
 * <p>Composants principaux :</p>
 * <ul>
 *   <li>Champ texte pour le film aimé</li>
 *   <li>Bouton de proposition</li>
 *   <li>Labels titre/raison/plateforme</li>
 *   <li>Zone HTML pour la description</li>
 *   <li>Boutons « Ajouter à ma liste » et « Régénérer description »</li>
 * </ul>
 *
 * <p>La génération de recommandations et de descriptions est réalisée de manière
 * asynchrone via {@link SwingWorker} pour ne pas bloquer l’EDT.</p>
 *
 * <p>Conforme Checkstyle (imports, indentation, javadoc, lignes courtes) et
 * évite les flags SpotBugs courants (annulation de workers, NPE, etc.).</p>
 */
public final class Tool1Panel extends JPanel {

    /** UID de sérialisation standard. */
    private static final long serialVersionUID = 1L;

    // --- Thème ---

    /** Couleur néon rose principale. */
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

    // --- Dépendances et état ---

    /** Service de recommandation injecté. */
    private transient MovieRecommenderService service;
    /** Callback de navigation (ex. {@code "home"}). */
    private transient Consumer<String> navigator;
    /** Recommandation courante affichée. */
    private transient Recommendation current;

    // --- UI : champs et boutons ---

    /** Champ de saisie du film aimé. */
    private final JTextField input = new JTextField();
    /** Bouton pour lancer la proposition. */
    private final JButton propose = new JButton("Proposer");
    /** Label du titre recommandé. */
    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    /** Label de la raison de recommandation. */
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    /** Label de la plateforme de visionnage. */
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);
    /** Zone d’affichage HTML pour la description. */
    private final JEditorPane descPane = new JEditorPane("text/html", "");

    /** Worker asynchrone pour la génération de description. */
    private transient SwingWorker<String, Void> descWorker;
    /** Bouton d’ajout à la liste d’envies. */
    private final JButton addWishlist = new JButton("Ajouter à ma liste");
    /** Bouton de régénération de la description. */
    private final JButton descBtn = new JButton("Régénérer description");
    /** Bouton retour. */
    private final JButton backBtn = new JButton("Retour");

    /**
     * Constructeur principal : installe l’UI, les styles et les actions.
     *
     * @param service   service de recommandation (non {@code null})
     * @param navigator callback de navigation (ex. {@code "home"}) (non {@code null})
     * @throws NullPointerException si {@code service} ou {@code navigator} est {@code null}
     */
    public Tool1Panel(final MovieRecommenderService service,
                      final Consumer<String> navigator) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        // --- Barre du haut : retour + champ + proposer ---
        final JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setOpaque(false);

        final JPanel left = new JPanel();
        left.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> this.navigator.accept("home"));
        left.add(backBtn);
        topBar.add(left, BorderLayout.WEST);

        final JPanel topInput = new JPanel(new BorderLayout(8, 8));
        topInput.setOpaque(false);
        final JLabel lbl = new JLabel("Film aimé : ");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 15f));
        topInput.add(lbl, BorderLayout.WEST);

        styleTextField(input);
        topInput.add(input, BorderLayout.CENTER);

        styleNeonButton(propose);
        topInput.add(propose, BorderLayout.EAST);
        topBar.add(topInput, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        // --- Centre : titre + description + infos ---
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Color.WHITE);

        final JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(title, BorderLayout.NORTH);

        descPane.setEditable(false);
        descPane.setOpaque(false);
        descPane.setBorder(new EmptyBorder(10, 24, 10, 24));

        final JScrollPane descScroll = new JScrollPane(descPane);
        descScroll.setBorder(null);
        descScroll.getViewport().setOpaque(false);
        descScroll.setOpaque(false);
        center.add(descScroll, BorderLayout.CENTER);

        final JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setOpaque(false);
        styleInfoLabel(reason);
        styleInfoLabel(platform);
        info.add(reason);
        info.add(platform);
        center.add(info, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // --- Bas : actions ---
        final JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        styleNeonButton(addWishlist);
        styleNeonButton(descBtn);
        bottom.add(addWishlist);
        bottom.add(descBtn);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions ---
        propose.addActionListener(e -> onPropose());
        addWishlist.addActionListener(e -> onAdd());
        descBtn.addActionListener(e -> startDescriptionForCurrent());
    }

    /**
     * Constructeur de compatibilité acceptant un {@link MainFrame} parent
     * (utilisé pour la navigation).
     *
     * @param service service de recommandation (non {@code null})
     * @param parent  frame parent, utilisé pour la navigation (non {@code null})
     * @throws NullPointerException si {@code service} ou {@code parent} est {@code null}
     */
    public Tool1Panel(final MovieRecommenderService service,
                      final app.cinematch.ui.swing.MainFrame parent) {
        this(service,
                Objects.requireNonNull(parent, "parent must not be null")::showCard);
    }

    /**
     * Libère proprement les ressources asynchrones lors du retrait du composant.
     * Annule le worker de description si encore actif.
     */
    @Override
    public void removeNotify() {
        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }
        super.removeNotify();
    }

    /**
     * Déclenche une proposition de film à partir du champ « Film aimé ».
     * Gère l’état « busy », annule les tâches précédentes si nécessaire,
     * et exécute l’appel à {@link MovieRecommenderService#recommendFromLike(String)}
     * dans un {@link SwingWorker}.
     */
    private void onPropose() {
        final String liked = input.getText().trim();
        if (liked.isEmpty()) {
            return;
        }

        setBusy(true);

        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }

        setDescHtml("<i>Recherche d’un film similaire…</i>");

        new SwingWorker<Recommendation, Void>() {
            @Override
            protected Recommendation doInBackground() {
                return service.recommendFromLike(liked);
            }

            @Override
            protected void done() {
                try {
                    current = get();
                    title.setText(current.title());
                    reason.setText(current.reason());
                    platform.setText(current.platform());
                    startDescriptionForCurrent();
                } catch (final Exception ex) {
                    title.setText("Erreur: " + ex.getMessage());
                    setDescHtml("<i>Description indisponible.</i>");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * Démarre la génération asynchrone de la description pour la recommandation
     * courante (si présente) via {@link MovieRecommenderService#generateDescription(String)}.
     * Annule le worker précédent si nécessaire et protège contre les courses d’états.
     */
    private void startDescriptionForCurrent() {
        if (current == null) {
            return;
        }

        final String titleAtStart = current.title();
        setDescHtml("<i>Génération de la description…</i>");

        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }

        descWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return service.generateDescription(titleAtStart);
            }

            @Override
            protected void done() {
                if (current == null || !current.title().equals(titleAtStart)) {
                    return;
                }
                try {
                    if (!isCancelled()) {
                        final String txt = get();
                        setDescHtml(htmlCenterBig(htmlEscape(txt)));
                    }
                } catch (final Exception ex) {
                    if (!isCancelled()) {
                        setDescHtml("<i>Description indisponible.</i>");
                    }
                }
            }
        };

        descWorker.execute();
    }

    /**
     * Ajoute le film courant à la liste d’envies (statut {@code "envie"})
     * via {@link MovieRecommenderService#mark(String, String)}, puis affiche
     * une boîte de dialogue de confirmation.
     */
    private void onAdd() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "envie");
        JOptionPane.showMessageDialog(this, "Ajouté à la liste 'envie'.");
    }

    /**
     * Active ou désactive les composants interactifs pendant les tâches asynchrones.
     *
     * @param busy {@code true} pour désactiver temporairement l’UI, {@code false} sinon
     */
    private void setBusy(final boolean busy) {
        propose.setEnabled(!busy);
        addWishlist.setEnabled(!busy);
        descBtn.setEnabled(!busy);
        input.setEnabled(!busy);
        backBtn.setEnabled(!busy);
    }

    /**
     * Définit le contenu HTML de la zone de description (sans {@code String.format}).
     * Ajoute un conteneur centré et stylisé autour du contenu fourni.
     *
     * @param htmlInner contenu HTML interne (déjà échappé si nécessaire)
     */
    private void setDescHtml(final String htmlInner) {
        final String ls = System.lineSeparator();

        final String part1 = "<html>" + ls
                + "  <body style=\"margin:0;padding:0;\">" + ls
                + "    <div style=\"display:flex;align-items:center;justify-content:"
                + "center;min-height:240px;\">" + ls
                + "      <div style=\"text-align:center;font-size:18px;line-height:1.5;"
                + "border:2px solid rgba(255,255,255,0.35);border-radius:12px;padding:"
                + "20px 40px;max-width:84%;background-color:rgba(255,255,255,0.05);"
                + "box-shadow:0 0 15px rgba(0,0,0,0.35);color:#f5f5f5;\">" + ls;

        final String part2 = htmlInner + ls
                + "      </div>" + ls
                + "    </div>" + ls
                + "  </body>" + ls
                + "</html>";

        descPane.setText(part1 + part2);
        descPane.setCaretPosition(0);
    }

    /**
     * Centre et agrandit visuellement le texte en remplaçant les sauts de ligne
     * par des balises {@code <br/>} pour l’affichage HTML.
     *
     * @param text texte source
     * @return texte prêt pour l’injection HTML
     */
    private static String htmlCenterBig(final String text) {
        return text.replace("\n", "<br/>");
    }

    /**
     * Échappe minimalement le HTML pour éviter une interprétation des caractères
     * spéciaux les plus courants.
     *
     * @param s texte source (peut être {@code null})
     * @return texte échappé (jamais {@code null})
     */
    private static String htmlEscape(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Applique un style « néon » aux boutons d’action (couleurs, bordures composées,
     * effet de survol).
     *
     * @param b bouton à styliser
     */
    private void styleNeonButton(final JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);

        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setBackground(HOVER_CARD_BG);
                b.setForeground(HOVER_PINK_TXT);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 2, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)));
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setBackground(BASE_CARD_BG);
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK_DARK, 2, true),
                        new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)));
            }
        });

        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
     * Applique le style du champ texte (couleurs, bordure et police).
     *
     * @param tf champ à styliser
     */
    private void styleTextField(final JTextField tf) {
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBackground(new Color(25, 25, 32));
        tf.setBorder(new CompoundBorder(
                new LineBorder(new Color(120, 120, 140), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    /**
     * Applique le style des labels d’information (couleur, police, centrage).
     *
     * @param l label à styliser
     */
    private void styleInfoLabel(final JLabel l) {
        l.setForeground(new Color(235, 235, 235));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setHorizontalAlignment(SwingConstants.CENTER);
    }

    /**
     * Dessine le fond en dégradé vertical du panneau.
     *
     * @param g contexte graphique Swing
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            final int w = getWidth();
            final int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }
}
