package app.cinematch.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitaire de chargement et de mise en cache d’images distantes (affiches).
 *
 * <p>Les images sont récupérées via HTTP(S), optionnellement redimensionnées
 * de façon <em>proportionnelle</em> pour tenir dans un cadre {@code maxW}×{@code maxH},
 * puis mises en cache en mémoire pour accélérer les appels suivants.</p>
 *
 * <p><b>Thread-safety :</b> le cache est un {@link ConcurrentHashMap} pour
 * permettre des accès concurrents. Les valeurs stockées sont des {@link ImageIcon}
 * prêts à l’emploi côté Swing.</p>
 */
public class ImageLoader {

    /**
     * Cache en mémoire associant une URL à son icône redimensionnée.
     */
    private static final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();

    /**
     * Charge une image distante (affiche) depuis {@code url}, la redimensionne
     * si nécessaire pour tenir dans {@code maxW}×{@code maxH} en conservant le
     * ratio, puis la retourne sous forme d’{@link ImageIcon}. Les résultats
     * sont mis en cache en mémoire.
     *
     * <p>Comportements particuliers :</p>
     * <ul>
     *   <li>Si {@code url} est {@code null} ou vide, retourne {@code null}.</li>
     *   <li>Si l’image ne peut pas être lue ({@link IOException} ou format
     *       non pris en charge), retourne {@code null}.</li>
     *   <li>Si l’image est plus petite que le cadre, elle n’est pas agrandie.</li>
     * </ul>
     *
     * @param url  l’URL de l’image à charger
     * @param maxW largeur maximale du rendu (en pixels)
     * @param maxH hauteur maximale du rendu (en pixels)
     * @return une icône {@link ImageIcon} prête à l’affichage, ou {@code null} en cas d’échec
     */
    public static ImageIcon loadPoster(String url, int maxW, int maxH) {
        if (url == null || url.isBlank()) return null;
        if (cache.containsKey(url)) return cache.get(url);
        try {
            BufferedImage img = ImageIO.read(new URL(url));
            if (img == null) return null;

            int w = img.getWidth(), h = img.getHeight();
            double scale = Math.min((double) maxW / w, (double) maxH / h);

            Image scaled = img;
            if (scale < 1.0) {
                scaled = img.getScaledInstance(
                        (int) (w * scale),
                        (int) (h * scale),
                        Image.SCALE_SMOOTH
                );
            }

            ImageIcon icon = new ImageIcon(scaled);
            cache.put(url, icon);
            return icon;
        } catch (IOException e) {
            System.err.println("[ImageLoader] Erreur de chargement : " + e.getMessage());
            return null;
        }
    }
}
