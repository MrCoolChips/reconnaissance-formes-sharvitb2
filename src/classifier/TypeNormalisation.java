package classifier;

/**
 * Type de normalisation appliquée composante par composante aux vecteurs caractéristiques
 * avant le calcul des distances.
 *
 * <p>La normalisation est importante pour certaines méthodes : par exemple les moments
 * de Zernike (ZRK) ont des composantes dont l'ordre de grandeur varie de 10² à 10⁴, si bien
 * qu'une distance L_p non normalisée est dominée par quelques composantes.</p>
 */
public enum TypeNormalisation {
    /** Aucune normalisation : on utilise les vecteurs bruts. */
    AUCUNE,
    /** Centrage-réduction : x'_j = (x_j − μ_j) / σ_j, μ et σ estimés sur l'apprentissage. */
    ZSCORE,
    /** Mise à l'échelle dans [0,1] : x'_j = (x_j − min_j) / (max_j − min_j). */
    MINMAX
}
