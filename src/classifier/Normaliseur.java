package classifier;

import java.util.List;

/**
 * Normalisation affine composante par composante : x'_j = (x_j − a_j) / b_j.
 *
 * <p>Les paramètres (a, b) sont estimés <b>uniquement sur l'ensemble d'apprentissage</b>
 * (méthode {@link #ajuster}) puis appliqués aux échantillons de test, afin d'éviter toute
 * fuite d'information du test vers l'apprentissage.</p>
 */
public final class Normaliseur {

    /** Seuil sous lequel un écart-type (ou une étendue) est considéré comme nul. */
    private static final double EPSILON = 1e-12;

    private final TypeNormalisation type;
    /** Décalages a_j (moyennes ou minimums). */
    private final double[] a;
    /** Facteurs d'échelle b_j (écarts-types ou étendues). */
    private final double[] b;

    private Normaliseur(TypeNormalisation type, double[] a, double[] b) {
        this.type = type;
        this.a = a;
        this.b = b;
    }

    /**
     * Estime les paramètres de normalisation sur un ensemble de vecteurs.
     *
     * <ul>
     *   <li>ZSCORE : a_j = μ_j = (1/n) Σ x_j, b_j = σ_j = sqrt((1/n) Σ (x_j − μ_j)²) ;</li>
     *   <li>MINMAX : a_j = min x_j, b_j = max x_j − min x_j ;</li>
     *   <li>AUCUNE : a_j = 0, b_j = 1.</li>
     * </ul>
     * Une composante constante (b_j ≈ 0) reçoit b_j = 1 pour éviter une division par zéro.
     *
     * @param type    type de normalisation
     * @param donnees vecteurs d'apprentissage (non vide, même dimension)
     * @return le normaliseur ajusté
     */
    public static Normaliseur ajuster(TypeNormalisation type, List<double[]> donnees) {
        if (donnees.isEmpty()) {
            throw new IllegalArgumentException("Ensemble d'apprentissage vide");
        }
        int d = donnees.get(0).length;
        int n = donnees.size();
        double[] a = new double[d];
        double[] b = new double[d];
        java.util.Arrays.fill(b, 1.0);
        switch (type) {
            case AUCUNE -> {
                // rien à estimer
            }
            case ZSCORE -> {
                for (double[] x : donnees) {
                    for (int j = 0; j < d; j++) {
                        a[j] += x[j];
                    }
                }
                for (int j = 0; j < d; j++) {
                    a[j] /= n;
                }
                double[] v = new double[d];
                for (double[] x : donnees) {
                    for (int j = 0; j < d; j++) {
                        double e = x[j] - a[j];
                        v[j] += e * e;
                    }
                }
                for (int j = 0; j < d; j++) {
                    double s = Math.sqrt(v[j] / n);
                    b[j] = s > EPSILON ? s : 1.0;
                }
            }
            case MINMAX -> {
                double[] max = new double[d];
                java.util.Arrays.fill(a, Double.POSITIVE_INFINITY);
                java.util.Arrays.fill(max, Double.NEGATIVE_INFINITY);
                for (double[] x : donnees) {
                    for (int j = 0; j < d; j++) {
                        a[j] = Math.min(a[j], x[j]);
                        max[j] = Math.max(max[j], x[j]);
                    }
                }
                for (int j = 0; j < d; j++) {
                    double e = max[j] - a[j];
                    b[j] = e > EPSILON ? e : 1.0;
                }
            }
        }
        return new Normaliseur(type, a, b);
    }

    /**
     * Applique la normalisation à un vecteur (le vecteur d'origine n'est pas modifié).
     *
     * @param x vecteur brut
     * @return le vecteur normalisé (le même tableau si le type est AUCUNE)
     */
    public double[] appliquer(double[] x) {
        if (type == TypeNormalisation.AUCUNE) {
            return x;
        }
        double[] r = new double[x.length];
        for (int j = 0; j < x.length; j++) {
            r[j] = (x[j] - a[j]) / b[j];
        }
        return r;
    }

    /** @return le type de normalisation. */
    public TypeNormalisation type() {
        return type;
    }
}
