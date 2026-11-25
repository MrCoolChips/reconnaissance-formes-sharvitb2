package classifier;

/**
 * Règle de départage des égalités de votes entre classes parmi les k plus proches voisins.
 */
public enum RegleEgalite {
    /** La classe ex-aequo possédant le voisin le plus proche l'emporte. */
    PLUS_PROCHE_VOISIN,
    /** La classe ex-aequo dont la distance moyenne de ses voisins est la plus faible l'emporte. */
    DISTANCE_MOYENNE
}
