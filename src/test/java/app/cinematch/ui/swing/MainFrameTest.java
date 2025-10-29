//package app.cinematch.ui.swing;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import javax.swing.*;
//import java.awt.*;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Modifier;
//import java.util.Arrays;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.junit.jupiter.api.Assumptions.assumeFalse;
//
///**
// * Tests BDD (Given/When/Then) pour MainFrame.
// * - Création via réflexion (supporte constructeur 2 args, 1 arg ou 0 arg)
// * - showCard() ne lève pas d'exception
// * - HomePanel.onNavigate() peut piloter showCard()
// *
// * Les tests JFrame sont ignorés en headless.
// */
//class MainFrameTest {
//
//    /** Crée une instance de MainFrame, quelle que soit la signature disponible. */
//    private static MainFrame newMainFrameReflective() throws Exception {
//        Constructor<?>[] ctors = MainFrame.class.getConstructors();
//
//        // 1) constructeur à 2 paramètres
//        for (Constructor<?> c : ctors) {
//            if (Modifier.isPublic(c.getModifiers()) && c.getParameterCount() == 2) {
//                return (MainFrame) c.newInstance(new Object[]{null, null});
//            }
//        }
//        // 2) constructeur à 1 paramètre
//        for (Constructor<?> c : ctors) {
//            if (Modifier.isPublic(c.getModifiers()) && c.getParameterCount() == 1) {
//                return (MainFrame) c.newInstance(new Object[]{null});
//            }
//        }
//        // 3) constructeur 0 param
//        try {
//            Constructor<MainFrame> noArg = MainFrame.class.getDeclaredConstructor();
//            if (!noArg.canAccess(null)) noArg.setAccessible(true);
//            return noArg.newInstance();
//        } catch (NoSuchMethodException ignored) { }
//        throw new IllegalStateException("Aucun constructeur public exploitable trouvé pour MainFrame");
//    }
//
//    @Test
//    @DisplayName("Construction: container + CardLayout présents")
//    void givenMainFrame_whenConstructed_thenContainerAndLayoutPresent() throws Exception {
//        // GIVEN
//        assumeFalse(GraphicsEnvironment.isHeadless(), "Test ignoré en headless (JFrame interdit)");
//
//        // WHEN
//        final MainFrame frame = newMainFrameReflective();
//
//        // THEN
//        assertNotNull(frame.getContentPane(), "contentPane ne doit pas être null");
//        assertTrue(frame.getContentPane().getLayout() instanceof CardLayout,
//                "Layout attendu: CardLayout");
//        assertEquals(JFrame.EXIT_ON_CLOSE, frame.getDefaultCloseOperation(),
//                "DefaultCloseOperation doit être EXIT_ON_CLOSE");
//        assertTrue(frame.getWidth() > 0 && frame.getHeight() > 0,
//                "La fenêtre doit avoir une taille > 0");
//    }
//
//    @Test
//    @DisplayName("showCard() n'échoue pas pour les clés courantes")
//    void givenMainFrame_whenShowCard_thenNoException() throws Exception {
//        // GIVEN
//        assumeFalse(GraphicsEnvironment.isHeadless(), "Test ignoré en headless");
//        final MainFrame frame = newMainFrameReflective();
//
//        // WHEN / THEN
//        assertDoesNotThrow(() -> frame.showCard("home"));
//        assertDoesNotThrow(() -> frame.showCard("t1"));
//        assertDoesNotThrow(() -> frame.showCard("t2"));
//        assertDoesNotThrow(() -> frame.showCard("t3"));
//        assertDoesNotThrow(() -> frame.showCard("chat"));
//        assertDoesNotThrow(() -> frame.showCard("hist"));
//    }
//
//    @Test
//    @DisplayName("HomePanel → onNavigate pilote bien showCard()")
//    void givenHomePanel_whenEmitNavigate_thenMainFrameShowsCard() throws Exception {
//        // GIVEN
//        assumeFalse(GraphicsEnvironment.isHeadless(), "Test ignoré en headless");
//        final MainFrame frame = newMainFrameReflective();
//        final JPanel container = (JPanel) frame.getContentPane();
//
//        // Stream API -> home est effectivement final
//        final HomePanel home = Arrays.stream(container.getComponents())
//                .filter(HomePanel.class::isInstance)
//                .map(HomePanel.class::cast)
//                .findFirst()
//                .orElseThrow(() -> new AssertionError("HomePanel doit être présent dans le container"));
//
//        // WHEN — relier navigation → showCard
//        home.onNavigate(frame::showCard);
//
//        // THEN — ne doit pas lever (on pose juste un nouveau consumer)
//        // (frame et home sont finals, donc OK pour la lambda)
//        assertDoesNotThrow(() -> home.onNavigate(id -> frame.showCard("t2")));
//    }
//}
