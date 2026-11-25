package evaluation;

/**
 * Courbe précision/rappel moyenne en K points (i = 1..K) et son aire sous la courbe.
 *
 * @param nom       nom de l'approche
 * @param precision precision[i−1] = moyenne des Precision_i
 * @param rappel    rappel[i−1] = moyenne des Recall_i
 * @param auc       aire sous la courbe (méthode des trapèzes)
 */
public record CourbePR(String nom, double[] precision, double[] rappel, double auc) {

    /** @return le nombre de points K. */
    public int taille() {
        return precision.length;
    }
}
