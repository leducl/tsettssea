package app.cinematch.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class ImageLoaderTest {

    /* ---------------- Helpers ---------------- */

    /** Crée un PNG temporaire (couleur unie) et retourne son URL file:. */
    private static URL createTempPng(int w, int h, Color color) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(color);
        g2.fillRect(0, 0, w, h);
        g2.dispose();

        File tmp = File.createTempFile("imageloader_test_", ".png");
        tmp.deleteOnExit();
        assertTrue(ImageIO.write(img, "png", tmp), "Écriture PNG échouée");
        return tmp.toURI().toURL();
    }

    /* --------------- Tests ------------------- */

    @Test
    @DisplayName("Given grande image, When contraintes 100x100, Then scale down en 100x50 (ratio conservé)")
    void givenLargeImage_whenMax100x100_thenScaledTo100x50() throws Exception {
        // Given — une image 400x200
        URL url = createTempPng(400, 200, new Color(20, 120, 240));

        // When — charge avec bornes 100x100
        ImageIcon icon = ImageLoader.loadPoster(url.toString(), 100, 100);

        // Then — non null et dimensions attendues (échelle 0.25)
        assertNotNull(icon, "Icon ne doit pas être null");
        assertEquals(100, icon.getIconWidth(), "Largeur attendue 100");
        assertEquals(50, icon.getIconHeight(), "Hauteur attendue 50");
    }

    @Test
    @DisplayName("Given petite image, When contraintes plus grandes, Then pas d'upscale (dimensions inchangées)")
    void givenSmallImage_whenBiggerBounds_thenNoUpscale() throws Exception {
        // Given — image 60x40
        URL url = createTempPng(60, 40, new Color(200, 60, 60));

        // When — bornes 100x100
        ImageIcon icon = ImageLoader.loadPoster(url.toString(), 100, 100);

        // Then — dimensions identiques à la source
        assertNotNull(icon);
        assertEquals(60, icon.getIconWidth());
        assertEquals(40, icon.getIconHeight());
    }

    @Test
    @DisplayName("Given même URL, When load deux fois, Then instance mise en cache (==)")
    void givenSameUrl_whenLoadedTwice_thenReturnsSameCachedInstance() throws Exception {
        // Given — image 120x80
        URL url = createTempPng(120, 80, new Color(30, 180, 90));

        // When — deux chargements successifs
        ImageIcon first = ImageLoader.loadPoster(url.toString(), 60, 60);
        ImageIcon second = ImageLoader.loadPoster(url.toString(), 60, 60);

        // Then — même instance (cache ConcurrentHashMap)
        assertNotNull(first);
        assertSame(first, second, "Doit renvoyer la même instance (cache)");
        // Et dimensions cohérentes : min(60/120, 60/80)=0.5 => 60x40
        assertEquals(60, first.getIconWidth());
        assertEquals(40, first.getIconHeight());
    }
    @Test
    @DisplayName("Given fichier existant non-image, When loadPoster, Then ImageIO.read renvoie null → retourne null")
    void givenExistingNonImageFile_whenLoadPoster_thenNull() throws Exception {
        // Given — on crée un fichier temporaire texte (existe, mais pas une image)
        File tmp = File.createTempFile("imageloader_not_image_", ".txt");
        tmp.deleteOnExit();
        try (var out = new java.io.FileWriter(tmp)) {
            out.write("Ceci n'est pas une image.");
        }
        URL url = tmp.toURI().toURL();

        // When — lecture via ImageIO.read(...) => devrait retourner null (pas d'IOException ici)
        ImageIcon icon = ImageLoader.loadPoster(url.toString(), 100, 100);

        // Then — la méthode gère le cas et retourne null
        assertNull(icon, "Un fichier non-image existant doit produire un ImageIcon null");
    }
    @Test
    @DisplayName("Given URL invalide, When loadPoster, Then retourne null (IOException gérée)")
    void givenInvalidUrl_whenLoadPoster_thenNull() throws Exception {
        // Given — URL file inexistante (pas de réseau)
        File phantom = new File("this_file_does_not_exist_12345.png");
        URL bad = phantom.toURI().toURL();

        // When
        ImageIcon icon = ImageLoader.loadPoster(bad.toString(), 100, 100);

        // Then
        assertNull(icon, "Doit retourner null si l'image ne peut être lue");
    }

    @Test
    @DisplayName("Given null ou blank, When loadPoster, Then null immédiat")
    void givenNullOrBlank_whenLoadPoster_thenNull() {
        // Given/When/Then
        assertNull(ImageLoader.loadPoster(null, 100, 100));
        assertNull(ImageLoader.loadPoster("   ", 100, 100));
    }
}
