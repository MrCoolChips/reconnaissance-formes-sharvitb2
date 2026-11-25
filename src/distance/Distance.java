package distance;

/**
 * Fonction de distance (dissimilarité) entre deux vecteurs caractéristiques de même dimension.
 * La « proximité » utilisée par les k-plus-proches-voisins est définie par cette fonction.
 */
public interface Distance {

    /**
     * @param x premier vecteur
     * @param y second vecteur (même dimension que x)
     * @return la distance d(x, y) ≥ 0
     */
    double calculer(double[] x, double[] y);

    /** @return un nom court de la distance (ex. « L2 »). */
    String nom();
}
