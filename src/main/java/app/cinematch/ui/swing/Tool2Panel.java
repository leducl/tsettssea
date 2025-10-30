package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Panneau « Swipe » permettant d’afficher des recommandations aléatoires,
 * d’indiquer une préférence (envie / pas intéressé / déjà vu) et de générer
 * une description courte — SANS animations.
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *   <li>Proposition aléatoire d’un film via {@link MovieRecommenderService#recommendRandom()}</li>
 *   <li>Marquage « envie », « pas intéressé » ou « déjà vu »</li>
 *   <li>Génération asynchrone d’une description via {@link SwingWorker}</li>
 * </ul>
 *
 * <p>Conçu pour ne pas bloquer l’EDT, avec annulation des workers si nécessaire.</p>
 */
public final class Tool2Panel extends JPanel {

    /** Service de recommandation. */
    private final MovieRecommenderService service;
    /** Callback de navigation (ex. {@code "home"}). */
    private final Consumer<String> navigator;

    /** Zone HTML affichant la description. */
    private final JEditorPane descPane = new JEditorPane("text/html", "");
    /** Worker asynchrone pour la génération de description. */
    private SwingWorker<String, Void> descWorker;

    /** Titre recommandé. */
    private final JLabel title = new JLabel("—", SwingConstants.CENTER);
    /** Raison de la recommandation. */
    private final JLabel reason = new JLabel("—", SwingConstants.CENTER);
    /** Plateforme de visionnage (si disponible). */
    private final JLabel platform = new JLabel("—", SwingConstants.CENTER);

    /** Bouton « je veux voir » (envie). */
    private final JButton likeBtn = new JButton("Je veux voir");
    /** Bouton « pas intéressé ». */
    private final JButton nopeBtn = new JButton("Pas intéressé");
    /** Bouton « déjà vu ». */
    private final JButton seenBtn = new JButton("Déjà vu");
    /** Bouton retour. */
    private final JButton backBtn = new JButton("Retour");

    /** Recommandation courante. */
    private Recommendation current;

    // --- Thème ---
    private static final Color NEON_PINK = new Color(255, 64, 160);
    private static final Color NEON_PINK_DARK = new Color(200, 30, 120);
    private static final Color BASE_CARD_BG = new Color(30, 30, 40);
    private static final Color BG_TOP = new Color(18, 18, 24);
    private static final Color BG_BOTTOM = new Color(35, 20, 40);

    /**
     * Construit le panneau.
     *
     * @param service   service de recommandation (non {@code null})
     * @param navigator callback de navigation (non {@code null})
     * @throws NullPointerException si un argument est {@code null}
     */
    public Tool2Panel(final MovieRecommenderService service,
                      final Consumer<String> navigator) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 20, 20));

        // --- Barre du haut : retour ---
        final JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        final JPanel leftTop = new JPanel();
        leftTop.setOpaque(false);
        styleBackOutlined(backBtn);
        backBtn.addActionListener(e -> this.navigator.accept("home"));
        leftTop.add(backBtn);
        topBar.add(leftTop, BorderLayout.WEST);
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
        final JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        final JPanel midActions = new JPanel();
        midActions.setOpaque(false);
        styleNeonPrimary(likeBtn);
        styleNeonPrimary(nopeBtn);
        midActions.add(likeBtn);
        midActions.add(nopeBtn);
        footer.add(midActions, BorderLayout.CENTER);

        final JPanel bottomBar = new JPanel();
        bottomBar.setOpaque(false);
        styleNeonButton(seenBtn);
        bottomBar.add(seenBtn);
        footer.add(bottomBar, BorderLayout.SOUTH);

        add(footer, BorderLayout.SOUTH);

        // --- Actions ---
        likeBtn.addActionListener(e -> onLike());
        nopeBtn.addActionListener(e -> onNope());
        seenBtn.addActionListener(e -> onSeen());

        // Première proposition
        proposeNext();
    }

    /** Propose la prochaine recommandation et lance la génération de description. */
    private void proposeNext() {
        title.setText("...");
        reason.setText("...");
        platform.setText("L'ia travaille...");
        setBusy(true);
        if (descWorker != null && !descWorker.isDone()) {
            descWorker.cancel(true);
        }
        setDescHtml("<i>Génération de la proposition…</i>");
        new SwingWorker<Recommendation, Void>() {
            @Override
            protected Recommendation doInBackground() {
                Recommendation rec;
                int guard = 0;
                do {
                    rec = service.recommendRandom();
                    guard++;
                } while (JsonStorage.getByStatus("envie").contains(rec.title()) && guard < 6);
                return rec;
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

    /** Lance la génération asynchrone de la description pour la reco courante. */
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

    /** Marque « envie » puis passe directement à la suivante. */
    private void onLike() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "envie");
        proposeNext();
    }

    /** Marque « pas intéressé » puis passe directement à la suivante. */
    private void onNope() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "pas_interesse");
        proposeNext();
    }

    /** Marque « déjà vu » puis passe directement à la suivante. */
    private void onSeen() {
        if (current == null) {
            return;
        }
        service.mark(current.title(), "deja_vu");
        proposeNext();
    }

    /** Active/désactive les contrôles pendant les tâches asynchrones. */
    private void setBusy(final boolean busy) {
        likeBtn.setEnabled(!busy);
        nopeBtn.setEnabled(!busy);
        seenBtn.setEnabled(!busy);
    }

    /** Définit le contenu HTML de la zone de description. */
    private void setDescHtml(final String htmlInner) {
        final String ls = System.lineSeparator();
        final String html =
                "<html>" + ls
                        + "  <body style=\"margin:0;padding:0;\">" + ls
                        + "    <div style=\"display:flex;align-items:center;justify-content:center;min-height:240px;\">" + ls
                        + "      <div style=\"text-align:center;font-size:18px;line-height:1.5;border:2px solid rgba(255,255,255,0.35);"
                        + "border-radius:12px;padding:20px 40px;max-width:84%;background-color:rgba(255,255,255,0.05);"
                        + "box-shadow:0 0 15px rgba(0,0,0,0.35);color:#f5f5f5;\">" + ls
                        + htmlInner + ls
                        + "      </div>" + ls
                        + "    </div>" + ls
                        + "  </body>" + ls
                        + "</html>";
        descPane.setText(html);
        descPane.setCaretPosition(0);
    }

    /** Remplace les sauts de ligne par des <br/>. */
    private static String htmlCenterBig(final String text) {
        return text.replace("\n", "<br/>");
    }

    /** Échappe minimalement le HTML. */
    private static String htmlEscape(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Style bouton néon. */
    private void styleNeonButton(final JButton b) {
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(BASE_CARD_BG);
        b.setOpaque(true);
        final EmptyBorder pad = new EmptyBorder(10, 16, 10, 16);
        b.setBorder(new CompoundBorder(
                new LineBorder(NEON_PINK_DARK, 2, true),
                new CompoundBorder(new LineBorder(NEON_PINK, 1, true), pad)
        ));
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    /** Style bouton néon primaire (CTA). */
    private void styleNeonPrimary(final JButton b) {
        styleNeonButton(b);
        b.setFont(new Font("Segoe UI", Font.BOLD, 18));
        b.setPreferredSize(new Dimension(220, 56));
    }

    /** Style bouton retour « outlined » avec effet au survol. */
    private void styleBackOutlined(final JButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(new Color(220, 220, 220));

        final EmptyBorder pad = new EmptyBorder(6, 12, 6, 12);
        b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Effet au survol
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 2, true), pad));
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent e) {
                b.setForeground(new Color(220, 220, 220));
                b.setBorder(new CompoundBorder(new LineBorder(Color.WHITE, 1, true), pad));
            }
        });
    }

    /** Style labels d’information. */
    private void styleInfoLabel(final JLabel l) {
        l.setForeground(new Color(235, 235, 235));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setHorizontalAlignment(SwingConstants.CENTER);
    }




    /** Dégradé de fond. */
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
