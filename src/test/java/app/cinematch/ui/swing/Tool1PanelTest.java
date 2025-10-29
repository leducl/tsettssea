package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.mockito.Mockito.*;

class Tool1PanelTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    /* ---------- Helpers ---------- */

    private static void noThrow(Runnable r, String messageIfThrows) {
        try {
            r.run();
        } catch (Throwable t) {
            Assertions.fail(messageIfThrows, t);
        }
    }

    private static void waitUntil(Duration timeout, String failMsg, BooleanSupplier cond) {
        Instant end = Instant.now().plus(timeout);
        while (Instant.now().isBefore(end)) {
            if (cond.getAsBoolean()) return;
            try { Thread.sleep(15); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        Assertions.fail(failMsg);
    }

    private static JButton findButton(Container root, String exactText) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && exactText.equals(b.getText())) return b;
            if (c instanceof Container cc) {
                JButton r = findButton(cc, exactText);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JTextField findTextField(Container root) {
        if (root instanceof JTextField tf) return tf;
        for (Component c : root.getComponents()) {
            if (c instanceof Container cc) {
                JTextField tf = findTextField(cc);
                if (tf != null) return tf;
            }
        }
        return null;
    }

    private static JEditorPane findEditorPane(Container root) {
        if (root instanceof JEditorPane ep) return ep;
        for (Component c : root.getComponents()) {
            if (c instanceof Container cc) {
                JEditorPane ep = findEditorPane(cc);
                if (ep != null) return ep;
            }
        }
        return null;
    }

    private static boolean panelIsShowingText(Container root, String text) {
        if (root instanceof JLabel l && Objects.equals(text, l.getText())) return true;
        for (Component c : root.getComponents()) {
            if (c instanceof Container cc) {
                if (panelIsShowingText(cc, text)) return true;
            }
        }
        return false;
    }

    /** Lecture thread-safe du HTML (évite IndexOutOfBoundsException). */
    private static String safeGetHtml(JEditorPane ep) {
        AtomicReference<String> out = new AtomicReference<>("");
        try {
            SwingUtilities.invokeAndWait(() -> out.set(ep.getText()));
        } catch (Exception ignored) {
            out.set("");
        }
        return out.get();
    }

    /** Instancie Recommendation dynamiquement selon sa déclaration record. */
    private static Recommendation newRecommendationDynamic(String title, String reason, String platform) {
        try {
            Class<Recommendation> recClass = Recommendation.class;
            if (!recClass.isRecord()) {
                return recClass.getDeclaredConstructor(String.class, String.class, String.class)
                        .newInstance(title, reason, platform);
            }
            RecordComponent[] comps = recClass.getRecordComponents();
            Object[] args = new Object[comps.length];
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < comps.length; i++) {
                idx.put(comps[i].getName(), i);
                args[i] = defaultValueFor(comps[i].getType());
            }
            if (idx.containsKey("title"))    args[idx.get("title")]    = title;
            if (idx.containsKey("reason"))   args[idx.get("reason")]   = reason;
            if (idx.containsKey("platform")) args[idx.get("platform")] = platform;

            Class<?>[] types = new Class<?>[comps.length];
            for (int i = 0; i < comps.length; i++) types[i] = comps[i].getType();
            Constructor<Recommendation> ctor = recClass.getDeclaredConstructor(types);
            if (!ctor.canAccess(null)) ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'instancier Recommendation", e);
        }
    }

    private static Object defaultValueFor(Class<?> t) {
        if (!t.isPrimitive()) {
            if (t == String.class) return "";
            return null;
        }
        if (t == boolean.class) return false;
        if (t == byte.class) return (byte) 0;
        if (t == short.class) return (short) 0;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == float.class) return 0f;
        if (t == double.class) return 0d;
        if (t == char.class) return '\0';
        return null;
    }

    /* ---------- Tests ---------- */

    @Test
    @DisplayName("Given input vide, When Proposer, Then no-op (pas d'appel service)")
    void emptyInput_noCall() {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        MainFrame frame = mock(MainFrame.class);
        Tool1Panel panel = new Tool1Panel(service, frame);

        JTextField input = findTextField(panel);
        JButton propose = findButton(panel, "Proposer");
        Assertions.assertNotNull(input);
        Assertions.assertNotNull(propose);

        input.setText("   ");
        noThrow(propose::doClick, "Proposer ne doit pas jeter avec input vide");
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Given film aimé, When Proposer, Then labels et description (HTML échappé)")
    void propose_updates_labels_and_description() {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        MainFrame frame = mock(MainFrame.class);
        Tool1Panel panel = new Tool1Panel(service, frame);

        JTextField input = findTextField(panel);
        JButton propose = findButton(panel, "Proposer");
        JEditorPane ep = findEditorPane(panel);

        Recommendation rec = newRecommendationDynamic("Matrix", "Parce que vous aimez Inception", "Netflix");
        when(service.recommendFromLike("Inception")).thenReturn(rec);
        when(service.generateDescription("Matrix")).thenReturn("Super <film> & plus");

        input.setText("Inception");
        noThrow(propose::doClick, "Proposer ne doit pas jeter");

        waitUntil(Duration.ofSeconds(2), "title non mis à jour",
                () -> panelIsShowingText(panel, "Matrix"));
        Assertions.assertTrue(panelIsShowingText(panel, "Parce que vous aimez Inception"));
        Assertions.assertTrue(panelIsShowingText(panel, "Netflix"));

        waitUntil(Duration.ofSeconds(2), "description non générée",
                () -> {
                    String html = safeGetHtml(ep);
                    return html.contains("&lt;film&gt;") && html.contains("&amp;");
                });
        Assertions.assertTrue(safeGetHtml(ep).contains("<html>"));
    }

    @Test
    @DisplayName("Régénérer description remplace le contenu")
    void regenerate_description_replaces_text() {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        MainFrame frame = mock(MainFrame.class);
        Tool1Panel panel = new Tool1Panel(service, frame);

        JTextField input = findTextField(panel);
        JButton propose = findButton(panel, "Proposer");
        JButton regen = findButton(panel, "Régénérer description");
        JEditorPane ep = findEditorPane(panel);

        Recommendation rec = newRecommendationDynamic("Y", "r", "pf");
        when(service.recommendFromLike("X")).thenReturn(rec);
        when(service.generateDescription("Y")).thenReturn("desc-1");

        input.setText("X");
        noThrow(propose::doClick, "Proposer ne doit pas jeter");

        waitUntil(Duration.ofSeconds(2), "desc-1 non visible",
                () -> safeGetHtml(ep).contains("desc-1"));

        when(service.generateDescription("Y")).thenReturn("desc-2");
        noThrow(regen::doClick, "Régénérer description ne doit pas jeter");

        waitUntil(Duration.ofSeconds(2), "desc-2 non visible",
                () -> safeGetHtml(ep).contains("desc-2"));
    }

    @Test
    @DisplayName("Ajouter à ma liste: service.mark appelé (tolérant headless/non-headless)")
    void add_to_wishlist_marks_and_dialog() throws Exception {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        MainFrame frame = mock(MainFrame.class);
        Tool1Panel panel = new Tool1Panel(service, frame);

        JTextField input = findTextField(panel);
        JButton propose = findButton(panel, "Proposer");
        JButton add = findButton(panel, "Ajouter à ma liste");

        Recommendation rec = newRecommendationDynamic("B", "r", "pf");
        when(service.recommendFromLike("A")).thenReturn(rec);
        when(service.generateDescription("B")).thenReturn("ok");

        input.setText("A");
        noThrow(propose::doClick, "Proposer ne doit pas jeter");

        // 🔧 ATTENDRE que 'current' soit défini (le titre 'B' affiché) AVANT de cliquer sur 'Ajouter'
        waitUntil(Duration.ofSeconds(2), "title 'B' non visible (current pas encore défini)",
                () -> panelIsShowingText(panel, "B"));

        Thread t = new Thread(add::doClick, "add-wishlist");
        t.start();

        waitUntil(Duration.ofSeconds(2), "mark non appelé",
                () -> {
                    try {
                        verify(service, atLeastOnce()).mark("B", "envie");
                        return true;
                    } catch (AssertionError e) {
                        return false;
                    }
                });

        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeAndWait(() -> {
                for (Window w : Window.getWindows()) {
                    if (w.isShowing() && w instanceof Dialog d) {
                        d.dispose();
                    }
                }
            });
        }
        t.join(1000);
        verify(service, atLeastOnce()).mark("B", "envie");
    }

    @Test
    @DisplayName("Hover sur bouton stylé: styles changent puis reviennent")
    void hover_changes_then_resets() {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        Tool1Panel panel = new Tool1Panel(service, mock(MainFrame.class));
        JButton propose = findButton(panel, "Proposer");
        Assertions.assertNotNull(propose);

        Color bg0 = propose.getBackground();
        Color fg0 = propose.getForeground();
        var border0 = propose.getBorder();

        MouseEvent enter = new MouseEvent(propose, MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : propose.getMouseListeners()) l.mouseEntered(enter);

        Assertions.assertNotEquals(bg0, propose.getBackground());
        Assertions.assertNotEquals(fg0, propose.getForeground());
        Assertions.assertNotEquals(border0, propose.getBorder());

        MouseEvent exit = new MouseEvent(propose, MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : propose.getMouseListeners()) l.mouseExited(exit);

        Assertions.assertEquals(bg0, propose.getBackground());
        Assertions.assertEquals(fg0, propose.getForeground());
        Assertions.assertEquals(border0.getClass(), propose.getBorder().getClass());
    }

    @Test
    @DisplayName("paintComponent: gradient sans exception et pixels opaques")
    void paint_component_gradient_ok() {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        Tool1Panel panel = new Tool1Panel(service, mock(MainFrame.class));
        panel.setSize(320, 200);

        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        noThrow(() -> panel.paintComponent(g2), "paintComponent ne doit pas jeter");
        g2.dispose();

        int[] data = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        boolean hasOpaque = false;
        for (int argb : data) {
            if ((argb >>> 24) != 0x00) { hasOpaque = true; break; }
        }
        Assertions.assertTrue(hasOpaque, "Le gradient doit remplir l'image");
    }

    @Test
    @DisplayName("startDescriptionForCurrent: no-op si current == null")
    void start_desc_noop_when_current_null() {
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        Tool1Panel panel = new Tool1Panel(service, mock(MainFrame.class));

        JButton regen = findButton(panel, "Régénérer description");
        noThrow(regen::doClick, "Régénérer description ne doit pas jeter si current == null");
        verifyNoInteractions(service);
    }
}
