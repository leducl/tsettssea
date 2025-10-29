package app.cinematch.ui.swing;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Panneau d’accueil (« Home ») de l’application CineMatch.
 *
 * <p>Ce composant Swing affiche un en-tête stylisé et trois cartes-boutons
 * permettant de naviguer vers différentes fonctionnalités (Swipe, Liste, Film similaire, Chat).
 * Il applique un fond en dégradé et un style « néon » aux cartes.</p>
 *
 * <p>L’instance expose un callback de navigation via {@link #onNavigate(Consumer)},
 * qui reçoit un identifiant logique de destination (ex. {@code "t2"}, {@code "t3"}, {@code "t1"}, {@code "chat"}).</p>
 *
 * <p>Exemple d’intégration :
 * <pre>{@code
 * HomePanel home = new HomePanel(mainFrame);
 * home.onNavigate(route -> mainFrame.showCard(route));
 * }</pre>
 *
 * @see app.cinematch.ui.swing.MainFrame
 */
public class HomePanel extends JPanel {

    /**
     * Callback de navigation (reçoit un identifiant de destination).
     * Peut être configuré via {@link #onNavigate(Consumer)}.
     */
    private Consumer<String> navigator;

    /** Couleur néon rose principale. */
    private static final Color NEON_PINK = new Color(255, 64, 160);
    /** Variante plus sombre du néon rose. */
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    /** Couleur du texte au survol. */
    private static final Color HOVER_PINK_TEXT = new Color(255, 210, 230);
    /** Couleur de fond par défaut des cartes. */
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    /** Couleur de fond des cartes au survol. */
    private static final Color HOVER_CARD_BG = new Color(50, 40, 60);
    /** Couleur haute du dégradé de fond. */
    private static final Color BG_TOP = new Color(18, 18, 24);
    /** Couleur basse du dégradé de fond. */
    private static final Color BG_BOTTOM = new Color(35, 20, 40);

    /**
     * Construit le panneau d’accueil et installe la mise en page, l’en-tête, les cartes
     * et les actions de navigation.
     *
     * @param frame la fenêtre principale appelante (non stockée ; utile pour le contexte d’intégration)
     */
    public HomePanel(MainFrame frame) {
        setPreferredSize(new Dimension(1080, 1920));
        setMinimumSize(new Dimension(1080, 1920));
        setMaximumSize(new Dimension(1080, 1920));
        setLayout(new BorderLayout());
        setOpaque(false);

        // --- Header ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(56, 24, 8, 24));

        JLabel title = new JLabel(
                "<html><div style='text-align:center; white-space:nowrap;'>"
                        + "<span style='color:#FFFFFF;'>CineMatch 🎬</span>&nbsp;"
                        + "<span style='color:rgb(255,64,160); text-shadow:0 0 14px rgba(255,64,160,0.65);'>Deluxe</span>"
                        + "</div></html>",
                SwingConstants.CENTER
        );
        title.setFont(pickFont(new String[]{"Segoe UI Emoji","Segoe UI","Dialog"}, Font.BOLD, 56));

        JLabel subtitle = new JLabel("Trouvez votre prochain film en un swipe.", SwingConstants.CENTER);
        subtitle.setFont(pickFont(new String[]{"Segoe UI","Dialog"}, Font.PLAIN, 22));
        subtitle.setForeground(new Color(220, 220, 220));
        subtitle.setBorder(new EmptyBorder(12, 0, 0, 0));

        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // --- Colonne centrale ---
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(new EmptyBorder(24, 32, 48, 32));

        JButton swipeBtn = makeNeonCard("⚡  Swipe", Size.LARGE, true);
        swipeBtn.addActionListener(e -> navigate("t2"));
        swipeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(swipeBtn);

        column.add(Box.createVerticalStrut(28));

        JPanel row = new JPanel(new GridLayout(1, 2, 28, 0));
        row.setOpaque(false);
        JButton listBtn = makeNeonCard("❤️  Ma liste", Size.SMALL, false);
        listBtn.addActionListener(e -> navigate("t3"));
        JButton similarBtn = makeNeonCard("🎞️  Film similaire", Size.SMALL, false);
        similarBtn.addActionListener(e -> navigate("t1"));
        JButton chatBtn = makeNeonCard("💬  Parler à l'IA", Size.SMALL, false);
        chatBtn.addActionListener(e -> navigate("chat"));
        row.add(listBtn);
        row.add(similarBtn);
        row.add(chatBtn);
        column.add(row);

        add(column, BorderLayout.CENTER);
    }

    /**
     * Tailles disponibles pour les cartes-boutons.
     */
    private enum Size { LARGE, SMALL }

    /**
     * Crée un bouton « carte » avec style néon (bordures composées, couleurs de survol, typographie).
     *
     * @param text      libellé du bouton
     * @param size      taille de la carte ({@link Size#LARGE} ou {@link Size#SMALL})
     * @param fullWidth si vrai, la carte occupe toute la largeur disponible
     * @return un bouton stylisé prêt à l’emploi
     */
    private JButton makeNeonCard(String text, Size size, boolean fullWidth) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);

        if (size == Size.LARGE) {
            btn.setFont(pickFont(new String[]{"Segoe UI Emoji","Segoe UI","Dialog"}, Font.BOLD, 40));
            btn.setPreferredSize(new Dimension(0, 360));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        } else {
            btn.setFont(pickFont(new String[]{"Segoe UI Emoji","Segoe UI","Dialog"}, Font.BOLD, 28));
            btn.setPreferredSize(new Dimension(0, 220));
            if (fullWidth) btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        }

        LineBorder outer = new LineBorder(NEON_PINK_DARK, 3, true);
        LineBorder inner = new LineBorder(NEON_PINK, 2, true);
        int padV = (size == Size.LARGE) ? 24 : 18;
        int padH = (size == Size.LARGE) ? 28 : 22;
        EmptyBorder pad = new EmptyBorder(padV, padH, padV, padH);
        btn.setBorder(new CompoundBorder(outer, new CompoundBorder(inner, pad)));

        btn.setBackground(BASE_CARD_BG);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(HOVER_CARD_BG);
                btn.setForeground(HOVER_PINK_TEXT);
                btn.setBorder(new CompoundBorder(
                        new LineBorder(NEON_PINK, 3, true),
                        new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BASE_CARD_BG);
                btn.setForeground(Color.WHITE);
                btn.setBorder(new CompoundBorder(outer, new CompoundBorder(inner, pad)));
            }
        });

        if (fullWidth) {
            Dimension pref = btn.getPreferredSize();
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        return btn;
    }

    /**
     * Sélectionne la première police disponible parmi une liste de familles candidates.
     * À défaut, retourne une police {@code Dialog}.
     *
     * @param families noms de familles de polices à tester dans l’ordre de préférence
     * @param style    style de la police (ex. {@link Font#BOLD})
     * @param size     taille en points
     * @return la police sélectionnée
     */
    private static Font pickFont(String[] families, int style, int size) {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        for (String f : families) {
            if (available.contains(f)) return new Font(f, style, size);
        }
        return new Font("Dialog", style, size);
    }

    /**
     * Peint le fond du panneau avec un dégradé vertical.
     *
     * @param g le contexte graphique
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    /**
     * Configure le callback de navigation appelé par {@link #navigate(String)}.
     *
     * @param nav consommateur recevant l’identifiant de la destination (ex. {@code "t2"})
     */
    public void onNavigate(Consumer<String> nav) { this.navigator = nav; }

    /**
     * Déclenche la navigation vers une destination logique identifiée par {@code id}.
     * Si aucun callback n’est configuré, l’appel est sans effet.
     *
     * @param id identifiant de la destination (ex. {@code "chat"})
     */
    private void navigate(String id) {
        if (navigator != null) navigator.accept(id);
    }
}
