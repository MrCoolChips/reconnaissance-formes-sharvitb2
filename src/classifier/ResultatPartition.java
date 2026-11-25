package classifier;

/**
 * Résultat d'un partitionnement par nuées dynamiques (k-means).
 *
 * @param affectations   numéro de groupe (0..k−1) de chaque échantillon
 * @param centres        centres m_i des k groupes
 * @param critereJ       erreur quadratique J = Σ_i Σ_{x∈D_i} ||x − m_i||²
 * @param iterations     nombre d'itérations effectuées par la meilleure exécution
 * @param initialisation numéro (0..) de l'initialisation ayant donné le J minimal
 */
public record ResultatPartition(int[] affectations, double[][] centres, double critereJ,
                                int iterations, int initialisation) {

    /** @return le nombre de groupes k. */
    public int nombreGroupes() {
        return centres.length;
    }
}
