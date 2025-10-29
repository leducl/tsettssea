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

    private final Consumer<String> navigator;
    private final Function<String, String> askFn;

    private final JTextPane conversationPane = new JTextPane();
    private final JScrollPane scrollPane = new JScrollPane(conversationPane);
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Envoyer");
    private final JButton backButton = new JButton("← Retour");

    private final JLabel thinkingLabel = new JLabel("🤔 L’IA réfléchit...");
    private final JProgressBar loadingBar = new JProgressBar();

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

        // --- Haut : titre et bouton retour ---
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        styleBackButton(backButton);
        backButton.addActionListener(e -> navigator.accept("home"));

        final JLabel title = new JLabel("💬 Discussion avec l’IA", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        header.add(backButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

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

    /** Style : bouton retour */
    private void styleBackButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(Color.LIGHT_GRAY);
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
}
