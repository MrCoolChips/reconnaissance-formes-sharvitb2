package evaluation;

/**
 * Un pli (« fold ») d'un protocole de validation : indices d'apprentissage et de test,
 * disjoints, dans la base.
 *
 * @param apprentissage indices des échantillons d'apprentissage
 * @param test          indices des échantillons de test
 */
public record Pli(int[] apprentissage, int[] test) {
}
