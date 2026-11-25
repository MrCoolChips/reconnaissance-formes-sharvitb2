package distance;

import java.util.Locale;

/**
 * Métrique de Minkowski (norme L_p) pour des vecteurs de dimension d :
 *
 * <pre>
 *   L_p(x, y) = ( Σ_{i=1..d} |x_i − y_i|^p )^(1/p)
 * </pre>
 *
 * <p>Cas particuliers :</p>
 * <ul>
 *   <li>p = 1 : distance de Manhattan (city block), L_1(x, y) = Σ |x_i − y_i| ;</li>
 *   <li>p = 2 : distance euclidienne, L_2(x, y) = ( Σ |x_i − y_i|² )^(1/2) ;</li>
 *   <li>p = +∞ : distance de Tchebychev, L_∞(x, y) = max_i |x_i − y_i| (limite de L_p).</li>
 * </ul>
 */
public final class Minkowski implements Distance {

    /** Ordre p de la norme (p ≥ 1 pour obtenir une métrique). */
    private final double p;

    /**
     * @param p ordre de la norme, p ≥ 1 (ou {@link Double#POSITIVE_INFINITY})
     */
    public Minkowski(double p) {
        if (!(p >= 1.0)) {
            throw new IllegalArgumentException("p doit etre >= 1 (p = " + p + ")");
        }
        this.p = p;
    }

    /** @return la distance de Manhattan L_1. */
    public static Minkowski manhattan() {
        return new Minkowski(1.0);
    }

    /** @return la distance euclidienne L_2. */
    public static Minkowski euclidienne() {
        return new Minkowski(2.0);
    }

    /** @return la distance de Tchebychev L_∞. */
    public static Minkowski tchebychev() {
        return new Minkowski(Double.POSITIVE_INFINITY);
    }

    /** @return l'ordre p. */
    public double p() {
        return p;
    }

    @Override
    public double calculer(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Dimensions differentes : " + x.length + " / " + y.length);
        }
        if (p == 1.0) {                       // L_1 : somme des écarts absolus
            double s = 0;
            for (int i = 0; i < x.length; i++) {
                s += Math.abs(x[i] - y[i]);
            }
            return s;
        }
        if (p == 2.0) {                       // L_2 : racine de la somme des carrés
            double s = 0;
            for (int i = 0; i < x.length; i++) {
                double e = x[i] - y[i];
                s += e * e;
            }
            return Math.sqrt(s);
        }
        if (Double.isInfinite(p)) {           // L_∞ : plus grand écart absolu
            double m = 0;
            for (int i = 0; i < x.length; i++) {
                m = Math.max(m, Math.abs(x[i] - y[i]));
            }
            return m;
        }
        double s = 0;                         // cas général L_p
        for (int i = 0; i < x.length; i++) {
            s += Math.pow(Math.abs(x[i] - y[i]), p);
        }
        return Math.pow(s, 1.0 / p);
    }

    @Override
    public String nom() {
        if (Double.isInfinite(p)) {
            return "Linf";
        }
        if (p == Math.rint(p)) {
            return "L" + (int) p;
        }
        return String.format(Locale.ROOT, "L%.2f", p);
    }

    @Override
    public String toString() {
        return nom();
    }
}
