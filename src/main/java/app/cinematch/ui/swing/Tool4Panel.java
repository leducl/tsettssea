package app.cinematch.ui.swing;

import app.cinematch.agent.ChatAgent;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface de discussion moderne avec l’IA (ChatAgent).
 * Style inspiré des chats modernes : bulles, transition fluide, chargement animé.
 */
public final class Tool4Panel extends JPanel {

    /** Couleurs principales. */
    private static final Color BG_TOP = new Color(18, 18, 24);
    private static final Color BG_BOTTOM = new Color(35, 20, 40);
    private static final Color PINK_ACCENT = new Color(255, 64, 160);

    // Zoom aide
    private static final double HELP_MIN = 0.9;
    private static final double HELP_MAX = 2.0;
    private static final double HELP_STEP = 0.1;
    private static final double HELP_DEFAULT = HELP_MIN; // par défaut = "A-" au max (rendu préféré)

    private final Consumer<String> navigator;
    private final Function<String, String> askFn;

    private final JTextPane conversationPane = new JTextPane();
    private final JScrollPane scrollPane = new JScrollPane(conversationPane);
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Envoyer");
    private final JButton backButton = new JButton("Retour");

    // Bouton info (aide) — FlatLaf "help" = icône "?"
    private final JButton infoButton = new JButton();

    private final JLabel thinkingLabel = new JLabel("🤔 L’IA réfléchit...");
    private final JProgressBar loadingBar = new JProgressBar();

    // Zoom de l’aide (par défaut comme "A-" au max)
    private double helpScale = HELP_DEFAULT;

    public Tool4Panel(final Function<String, String> askFunction,
                      final Consumer<String> navigationCallback) {
        this.askFn = Objects.requireNonNull(askFunction);
        this.navigator = Objects.requireNonNull(navigationCallback);
        buildUi();
    }

    public Tool4Panel(final ChatAgent agent, final Consumer<String> navigationCallback) {
        this(Objects.requireNonNull(agent)::ask,
                Objects.requireNonNull(navigationCallback));
    }

    /** Construction complète de l'interface. */
    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        // --- Haut : titre, retour, aide ---
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        styleBackButton(backButton);
        backButton.addActionListener(e -> navigator.accept("home"));

        final JLabel title = new JLabel("💬 Discussion avec l’IA", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        // Conteneur à droite pour le bouton d’aide
        final JPanel rightHeader = new JPanel();
        rightHeader.setOpaque(false);

        // Bouton "help" FlatLaf : icône "?"
        infoButton.putClientProperty("JButton.buttonType", "help");
        infoButton.setToolTipText("Aide & commandes (F1) — multi-actions, statuts et exemples");
        infoButton.setFocusable(false);
        infoButton.setPreferredSize(new Dimension(40, 40)); // plus grand
        infoButton.addActionListener(e -> showHelpDialog());
        rightHeader.add(infoButton);

        header.add(backButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(rightHeader, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Raccourci clavier F1 = Aide
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0), "showHelp");
        getActionMap().put("showHelp", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { showHelpDialog(); }
        });

        // --- Zone de conversation ---
        conversationPane.setEditable(false);
        conversationPane.setContentType("text/html");
        conversationPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        conversationPane.setBackground(new Color(25, 25, 32));
        conversationPane.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        conversationPane.setText(
                "<html><body style='font-family:Segoe UI; color:white;'>" +
                        "<p style='color:gray;'>Bienvenue dans la discussion avec l’IA 👋</p>" +
                        "</body></html>"
        );

        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 1, true));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // --- Bas : champ texte + boutons + chargement ---
        final JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setOpaque(false);

        styleInputField(inputField);
        styleNeonButton(sendButton);

        final JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.setOpaque(false);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Chargement
        thinkingLabel.setForeground(new Color(220, 220, 220));
        thinkingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        thinkingLabel.setVisible(false);

        loadingBar.setIndeterminate(true);
        loadingBar.setForeground(PINK_ACCENT);
        loadingBar.setBackground(new Color(40, 40, 50));
        loadingBar.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120), 1, true));
        loadingBar.setPreferredSize(new Dimension(200, 6));
        loadingBar.setVisible(false);

        final JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(thinkingLabel, BorderLayout.NORTH);
        loadingPanel.add(loadingBar, BorderLayout.SOUTH);

        bottom.add(inputPanel, BorderLayout.CENTER);
        bottom.add(loadingPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // Actions
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    /** Gestion de l'envoi du message + affichage IA. */
    private void sendMessage() {
        final String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        appendMessage("Vous", text, true);
        inputField.setText("");
        sendButton.setEnabled(false);

        thinkingLabel.setVisible(true);
        loadingBar.setVisible(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return askFn.apply(text);
            }

            @Override
            protected void done() {
                try {
                    appendMessage("IA", get(), false);
                } catch (Exception ex) {
                    appendMessage("Erreur", ex.getMessage(), false);
                } finally {
                    sendButton.setEnabled(true);
                    thinkingLabel.setVisible(false);
                    loadingBar.setVisible(false);
                }
            }
        }.execute();
    }

    /** Affiche un message dans la zone HTML. */
    private void appendMessage(String author, String message, boolean user) {
        String color = user ? "#ff80d0" : "#a0a0ff";
        String bubbleColor = user
                ? "rgba(255, 64, 160, 0.15)"
                : "rgba(80, 80, 120, 0.2)";
        String current = conversationPane.getText();

        // Insère la bulle HTML
        String htmlMessage = String.format(
                "<div style='margin:8px 0; padding:8px 12px; background:%s; " +
                        "border-radius:10px; color:white; max-width:85%%;'>"
                        + "<b style='color:%s;'>%s :</b><br>%s</div>",
                bubbleColor, color, author, message.replace("\n", "<br>")
        );

        // Insertion juste avant </body>
        String updated = current.replace("</body>", htmlMessage + "</body>");
        conversationPane.setText(updated);

        // Auto-scroll vers le bas
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    /** Style : champ texte */
    private void styleInputField(JTextField field) {
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBackground(new Color(25, 25, 32));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(120, 120, 140), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setToolTipText("Écris ton message ici...");
    }

    /** Style : bouton néon */
    private void styleNeonButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(50, 40, 60));
        btn.setOpaque(true);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(new CompoundBorder(
                new LineBorder(PINK_ACCENT, 2, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(PINK_ACCENT);
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(50, 40, 60));
                btn.setForeground(Color.WHITE);
            }
        });
    }

    /** Style bouton retour */
    private void styleBackButton(final JButton btn) {
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setForeground(new Color(220, 220, 220));

        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        btn.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // cliquable

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                btn.setForeground(Color.WHITE);
                btn.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                btn.setForeground(new Color(220, 220, 220));
                btn.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    /** Fond dégradé violet sombre */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    /** Méthode utilitaire exigée par les tests (compound border). */
    @SuppressWarnings("unused")
    private static CompoundBorder compound(final Color color, final int thickness,
                                           final EmptyBorder pad) {
        return new CompoundBorder(new LineBorder(color, thickness, true), pad);
    }

    /** Ouvre une fenêtre d’aide compacte avec zoom. */
    private void showHelpDialog() {
        final JDialog dlg = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Aide — Agent IA",
                Dialog.ModalityType.MODELESS
        );
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final JEditorPane pane = new JEditorPane("text/html", buildHelpHtml(helpScale));
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setEditable(false);
        pane.setOpaque(false);

        final JScrollPane scroller = new JScrollPane(pane);
        scroller.setBorder(new LineBorder(new Color(80, 80, 100), 1, true));
        scroller.setPreferredSize(new Dimension(860, 600));

        // Barre d’actions: A-, Défaut, A+, Fermer
        final JButton zoomOut = new JButton("A-");
        final JButton reset = new JButton("Défaut");
        final JButton zoomIn = new JButton("A+");
        final JButton close = new JButton("Fermer");
        styleNeonButton(zoomOut);
        styleNeonButton(reset);
        styleNeonButton(zoomIn);
        styleNeonButton(close);

        zoomOut.addActionListener(e -> {
            helpScale = Math.max(HELP_MIN, helpScale - HELP_STEP);
            pane.setText(buildHelpHtml(helpScale));
            pane.setCaretPosition(0);
        });
        reset.addActionListener(e -> {
            helpScale = HELP_DEFAULT;
            pane.setText(buildHelpHtml(helpScale));
            pane.setCaretPosition(0);
        });
        zoomIn.addActionListener(e -> {
            helpScale = Math.min(HELP_MAX, helpScale + HELP_STEP);
            pane.setText(buildHelpHtml(helpScale));
            pane.setCaretPosition(0);
        });
        close.addActionListener(e -> dlg.dispose());

        final JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.setOpaque(false);
        south.add(zoomOut);
        south.add(reset);
        south.add(zoomIn);
        south.add(close);

        final JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setOpaque(false);
        root.add(scroller, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
    /** Contenu HTML de l’aide, avec tailles dépendantes de 'scale' (sans String.format). */
    private String buildHelpHtml(double scale) {
        // Calcule des tailles en px selon l’échelle
        int BODY = (int) Math.round(16 * scale);   // base
        int H1   = (int) Math.round(26 * scale);
        int H2   = (int) Math.round(20 * scale);
        int CODE = (int) Math.round(15 * scale);
        int HINT = (int) Math.round(14 * scale);
        int PAD  = (int) Math.round(12 * scale);

        // CSS
        String css = """
      <style>
        body{font-family:'Segoe UI',sans-serif;color:#f5f5f5;background:#1b1b22;}
        h1{margin:0 0 8px;}
        h2{margin:16px 0 6px;color:#ffd2ea;}
        ul{margin:6px 0 10px 22px;}
        li{margin:6px 0;}
        code{background:#2a2833;padding:2px 8px;border-radius:8px;display:inline-block;}
        .hint{color:#ccccdd;}
        .box{background:#222231;border:1px solid #51425e;border-radius:10px;padding:{PAD}px;}
        .tags{margin:6px 0 10px 0;}
        .tag{display:inline-block;margin:2px 6px 2px 0;padding:2px 8px;border-radius:10px;background:#2a2833;border:1px solid #51425e;font-size:90%;}
      </style>
    """;

        // HTML
        String html = """
      <html><body style="font-size:{BODY}px; line-height:1.55;">
      <h1 style="font-size:{H1}px;">Que puis-je demander à l’agent IA ?</h1>

      <div class='box'>
        <h2 style="font-size:{H2}px;">Actions combinées (orchestrateur)</h2>
        <ul>
          <li><b>Plusieurs actions en une phrase</b> : <code style="font-size:{CODE}px;">Ajoute Drive à ma liste et supprime Dune de ma liste</code></li>
          <li><b>Changement de statut en lot</b> : <code style="font-size:{CODE}px;">Mets Alien, Heat, Drive en deja_vu</code></li>
          <li><b>Mix ajout/retrait</b> : <code style="font-size:{CODE}px;">Ajoute "Blade Runner 2049"; puis supprime Parasite.</code></li>
        </ul>
        <div class="tags">
          <span class="tag">Mots-clefs multi-actions : <i>et</i>, <i>puis</i>, <i>;</i>, <i>.</i></span>
          <span class="tag">Comprend titres "entre guillemets"</span>
          <span class="tag">CSV : <i>Alien, Heat, Drive</i></span>
        </div>

        <h2 style="font-size:{H2}px;">Wishlist (liste d’envie)</h2>
        <ul>
          <li><b>Ajouter</b> : <code style="font-size:{CODE}px;">Ajoute "Le Samouraï" à ma wishlist</code></li>
          <li><b>Retirer</b> : <code style="font-size:{CODE}px;">Enlève Matrix de ma wishlist</code></li>
          <li><b>Afficher</b> : <code style="font-size:{CODE}px;">Affiche ma liste d’envie</code></li>
          <li><b>Ajout multiple (instantané)</b> : <code style="font-size:{CODE}px;">Ajoute Alien, Heat, Drive à ma wishlist</code></li>
        </ul>

        <h2 style="font-size:{H2}px;">Statuts des films</h2>
        <ul>
          <li><b>Déjà vu</b> : <code style="font-size:{CODE}px;">J’ai vu Dune, marque-le déjà vu</code></li>
          <li><b>Pas intéressé</b> : <code style="font-size:{CODE}px;">Je n’aime pas Matrix</code></li>
          <li><b>Changer statut</b> : <code style="font-size:{CODE}px;">Mets Jojo Rabbit en pas_interesse</code></li>
          <li><b>En lot</b> : <code style="font-size:{CODE}px;">J'ai déja vu Alien, Heat, Drive</code></li>
        </ul>
        <div class="tags">
          <span class="tag">Statuts valides : <b>envie</b>, <b>deja_vu</b>, <b>pas_interesse</b></span>
          <span class="tag">Synonymes compris : <i>déjà vu / deja vu</i>, <i>pas intéressé</i></span>
        </div>

        <h2 style="font-size:{H2}px;">Descriptions & choix</h2>
        <ul>
          <li><b>Description courte</b> : <code style="font-size:{CODE}px;">Décris Jojo Rabbit</code></li>
          <li><b>Prochain à regarder</b> : <code style="font-size:{CODE}px;">Propose-moi le prochain film (au hasard)</code></li>
        </ul>

        <h2 style="font-size:{H2}px;">Maintenance & tri</h2>
        <ul>
          <li><b>Nettoyage</b> : <code style="font-size:{CODE}px;">Nettoie les entrées vides de ma wishlist</code></li>
          <li><b>Renommer</b> : <code style="font-size:{CODE}px;">Renomme Le seigneur des anneaux en The Lord of the Rings</code></li>
          <li><b>Tri</b> : <code style="font-size:{CODE}px;">Affiche ma wishlist triée de A à Z</code></li>
          <li><b>Stats</b> : <code style="font-size:{CODE}px;">Donne les statistiques de mes listes</code></li>
        </ul>

        <h2 style="font-size:{H2}px;">Astuces</h2>
        <ul>
          <li>Après une action, clique <b>Rafraîchir</b> dans l’onglet <i>Ma liste</i> pour voir la mise à jour.</li>
          <li>Raccourci : touche <b>F1</b> pour rouvrir cette aide.</li>
          <li>L’orchestrateur comprend les combinaisons naturelles (<i>et</i>, <i>puis</i>, <i>;</i>), les guillemets, et les listes CSV.</li>
        </ul>
      </div>

      <p class='hint' style="font-size:{HINT}px;">Tu peux enchaîner : “Recommande 3 films, ajoute le 2e à ma wishlist et décris-le.”</p>
      </body></html>
    """;

        // Remplacements
        return (css + html)
                .replace("{PAD}",  String.valueOf(PAD))
                .replace("{BODY}", String.valueOf(BODY))
                .replace("{H1}",   String.valueOf(H1))
                .replace("{H2}",   String.valueOf(H2))
                .replace("{CODE}", String.valueOf(CODE))
                .replace("{HINT}", String.valueOf(HINT));
    }

    // (Optionnel) Surcharge no-arg pour appeler buildHelpHtml() sans paramètre
    @SuppressWarnings("unused")
    private String buildHelpHtml() {
        return buildHelpHtml(helpScale);
    }
}
