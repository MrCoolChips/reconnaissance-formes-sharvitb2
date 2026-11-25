package model;

import java.util.Locale;
import java.util.Optional;

/**
 * Énumération des cinq méthodes de description (signatures) calculées sur les formes
 * de la base SharvitB2. Chaque méthode M produit, pour une image SxxNyyy.pgm, un fichier
 * SxxNyyy.MET contenant un vecteur caractéristique de dimension fixe.
 *
 * <ul>
 *   <li>ART : transformée angulaire radiale (MPEG-7) ;</li>
 *   <li>ZRK : moments de Zernike ;</li>
 *   <li>GFD : descripteur de Fourier générique (transformée polaire discrète) ;</li>
 *   <li>YNG : signature structurelle de Yang (squelette) ;</li>
 *   <li>SA  : signature angulaire (distance barycentre-contour + FFT).</li>
 * </ul>
 */
public enum Methode {
    ART("art", "Transformee angulaire radiale (MPEG-7)"),
    ZRK("zrk", "Moments de Zernike"),
    GFD("gfd", "Descripteur de Fourier generique"),
    YNG("yng", "Signature structurelle de Yang"),
    SA("sa", "Signature angulaire (barycentre-contour + FFT)");

    /** Extension (en minuscules) des fichiers de signature de cette méthode. */
    private final String extension;
    /** Description courte de la méthode, utilisée dans les affichages. */
    private final String description;

    Methode(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    /** @return l'extension de fichier associée (ex. « art »). */
    public String extension() {
        return extension;
    }

    /** @return une description lisible de la méthode. */
    public String description() {
        return description;
    }

    /**
     * Retrouve la méthode correspondant à une extension de fichier (insensible à la casse).
     *
     * @param ext extension sans le point (ex. « ZRK », « gfd »)
     * @return la méthode correspondante, ou {@link Optional#empty()} si l'extension est inconnue
     */
    public static Optional<Methode> depuisExtension(String ext) {
        String e = ext.toLowerCase(Locale.ROOT);
        for (Methode m : values()) {
            if (m.extension.equals(e)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
