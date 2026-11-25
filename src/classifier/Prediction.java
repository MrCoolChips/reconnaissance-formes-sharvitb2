package classifier;

import java.util.Arrays;

/**
 * Résultat de la classification d'un échantillon.
 *
 * @param classe    classe estimée
 * @param classes   étiquettes des classes connues du classifieur (triées)
 * @param scores    score de confiance continu pour chaque classe de {@code classes}
 *                  (positifs, de somme 1 : assimilable à une « probabilité »), utilisé pour
 *                  le classement précision/rappel
 * @param proximite distance normalisée au plus proche voisin (d_1 / distance moyenne) :
 *                  plus elle est faible, plus la décision est « sûre » ; sert à départager
 *                  les égalités du vote majoritaire
 */
public record Prediction(int classe, int[] classes, double[] scores, double proximite) {

    /**
     * @param c une étiquette de classe
     * @return le score de confiance associé à c (0 si la classe est inconnue)
     */
    public double score(int c) {
        int p = Arrays.binarySearch(classes, c);
        return p >= 0 ? scores[p] : 0.0;
    }
}
