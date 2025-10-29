package app.cinematch.ui.swing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BDD pour HomePanel (Given/When/Then en commentaires).
 * - Navigation via onNavigate() et clics
 * - Effets hover (mouseEntered / mouseExited)
 * - Rendu (paintComponent) sans exception
 * Aucun JFrame n'est instancié (compatible headless).
 */
class HomePanelTest {

    @BeforeAll
    static void headlessOn() {
        // GIVEN — exécution headless
        System.setProperty("java.awt.headless", "true");
    }

    // helper : recherche un JButton par préfixe de texte (robuste aux emoji/espaces)
    private static JButton findButtonStartsWith(Container root, String prefix) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b) {
                String t = b.getText();
                if (t != null && t.startsWith(prefix)) return b;
            }
            if (c instanceof Container child) {
                JButton found = findButtonStartsWith(child, prefix);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    @DisplayName("Navigation: chaque bouton émet l'ID attendu")
    void givenHomePanel_whenClickButtons_thenCorrectIdsEmitted() {
        // GIVEN — panel + collecteur
        HomePanel panel = new HomePanel(null);
        final String[] last = new String[1];
        panel.onNavigate(id -> last[0] = id);

        JButton swipe   = findButtonStartsWith(panel, "⚡  Swipe");
        JButton list    = findButtonStartsWith(panel, "❤️  Ma liste");
        JButton similar = findButtonStartsWith(panel, "🎞️  Film similaire");
        JButton chat    = findButtonStartsWith(panel, "💬  Parler à l'IA");

        assertNotNull(swipe);
        assertNotNull(list);
        assertNotNull(similar);
        assertNotNull(chat);

        // WHEN — clics successifs
        swipe.doClick();   // THEN
        assertEquals("t2", last[0]);
        list.doClick();    // THEN
        assertEquals("t3", last[0]);
        similar.doClick(); // THEN
        assertEquals("t1", last[0]);
        chat.doClick();    // THEN
        assertEquals("chat", last[0]);
    }

    @Test
    @DisplayName("Hover: mouseEntered/Exited modifient puis rétablissent le style")
    void givenHomePanel_whenHoverLargeButton_thenStyleChangesAndResets() {
        // GIVEN
        HomePanel panel = new HomePanel(null);
        JButton swipe = findButtonStartsWith(panel, "⚡  Swipe");
        assertNotNull(swipe);

        Color bg0 = swipe.getBackground();
        Color fg0 = swipe.getForeground();
        var border0 = swipe.getBorder();

        // WHEN — on appelle directement les MouseListener
        long now = System.currentTimeMillis();
        MouseEvent enter = new MouseEvent(swipe, MouseEvent.MOUSE_ENTERED, now, 0, 5, 5, 0, false);
        for (var l : swipe.getMouseListeners()) l.mouseEntered(enter);

        // THEN — état hover
        assertNotEquals(bg0, swipe.getBackground());
        assertNotEquals(fg0, swipe.getForeground());
        assertNotEquals(border0, swipe.getBorder());

        // WHEN — sortie
        MouseEvent exit = new MouseEvent(swipe, MouseEvent.MOUSE_EXITED, now + 10, 0, 5, 5, 0, false);
        for (var l : swipe.getMouseListeners()) l.mouseExited(exit);

        // THEN — retour à l'état initial
        assertEquals(bg0, swipe.getBackground());
        assertEquals(fg0, swipe.getForeground());
        assertEquals(border0.getClass(), swipe.getBorder().getClass());
    }

    @Test
    @DisplayName("Rendu: paintComponent applique un gradient sans exception")
    void givenHomePanel_whenPaintComponent_thenNoExceptionAndImageFilled() {
        // GIVEN
        HomePanel panel = new HomePanel(null);
        panel.setSize(400, 300);
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // WHEN — on appelle directement paintComponent (le code qui dessine le gradient)
        assertDoesNotThrow(() -> panel.paintComponent(g2));
        g2.dispose();

        // THEN — l'image contient des pixels opaques
        int[] data = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        boolean hasOpaque = false;
        for (int argb : data) {
            if ((argb >>> 24) != 0x00) { hasOpaque = true; break; }
        }
        assertTrue(hasOpaque, "Le gradient doit remplir l'image (pixels non transparents)");
    }
}
