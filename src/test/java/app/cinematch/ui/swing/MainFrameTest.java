package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.ChatAgent;
import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MainFrameTest {

    @FunctionalInterface interface EDT<T> { T run() throws Exception; }
    private static <T> T onEDT(EDT<T> task) throws Exception {
        final Object[] out = new Object[1];
        final Exception[] err = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try { out[0] = task.run(); } catch (Exception e) { err[0] = e; }
        });
        if (err[0] != null) throw err[0];
        @SuppressWarnings("unchecked") T t = (T) out[0];
        return t;
    }
    private static Component visibleCard(Container content) {
        for (Component c : content.getComponents()) if (c.isVisible()) return c;
        return null;
    }

    @Test
    @DisplayName("GIVEN service WHEN new MainFrame(service) THEN CardLayout + navigation OK")
    void construct_with_service_only_and_navigate() throws Exception {
        // Skip si pas d’affichage → évite HeadlessException
        assumeFalse(GraphicsEnvironment.isHeadless(), "Headless: test UI sauté.");

        // GIVEN: utilise un des constructeurs existants (2 args)
        MovieRecommenderService service = new MovieRecommenderService(
                "http://localhost:11434",   // endpoint bidon
                "dummy-model"               // modèle bidon
        );

        // WHEN
        MainFrame frame = onEDT(() -> new MainFrame(service));

        try {
            // THEN
            assertTrue(frame.getContentPane().getLayout() instanceof CardLayout);
            assertEquals(6, frame.getContentPane().getComponentCount(), "home,t1,t2,t3,chat,hist attendus");

            String[] ids = {"home","t1","t2","t3","chat","hist"};
            for (String id : ids) {
                onEDT(() -> { frame.showCard(id); return null; });
                assertNotNull(onEDT(() -> visibleCard((Container) frame.getContentPane())),
                        "Après showCard(" + id + "), une carte doit être visible");
            }
        } finally {
            onEDT(() -> { frame.dispose(); return null; });
        }
    }

    @Test
    @DisplayName("GIVEN service + agent=null WHEN new MainFrame(service,null) THEN fallback chat OK")
    void construct_with_service_and_null_agent() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Headless: test UI sauté.");

        // GIVEN: même constructeur 2 args
        MovieRecommenderService service = new MovieRecommenderService(
                "http://localhost:11434",
                "dummy-model"
        );
        ChatAgent agent = null; // on teste le fallback

        // WHEN
        MainFrame frame = onEDT(() -> new MainFrame(service, agent));

        try {
            // THEN: on peut naviguer vers chat & hist
            onEDT(() -> { frame.showCard("chat"); return null; });
            assertNotNull(onEDT(() -> visibleCard((Container) frame.getContentPane())));
            onEDT(() -> { frame.showCard("hist"); return null; });
            assertNotNull(onEDT(() -> visibleCard((Container) frame.getContentPane())));
        } finally {
            onEDT(() -> { frame.dispose(); return null; });
        }
    }
}
