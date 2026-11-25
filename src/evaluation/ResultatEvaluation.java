package evaluation;

import classifier.Prediction;

/**
 * Résultat de l'évaluation d'un classifieur selon un protocole de validation.
 *
 * @param nom         nom de l'expérience / du classifieur
 * @param matrice     matrice de confusion cumulée sur tous les plis
 * @param predictions prédiction obtenue pour chaque échantillon lorsqu'il était en test
 *                    (chaque échantillon est testé exactement une fois en LOOCV / K plis)
 * @param vraies      étiquettes réelles des échantillons
 * @param tauxParPli  taux de reconnaissance de chaque pli
 * @param dureeMs     durée totale (apprentissage + test) en millisecondes
 */
public record ResultatEvaluation(String nom, MatriceConfusion matrice, Prediction[] predictions,
                                 int[] vraies, double[] tauxParPli, double dureeMs) {

    /** @return le taux de reconnaissance global T_reco. */
    public double tauxReconnaissance() {
        return matrice.tauxReconnaissance();
    }

    /** @return l'écart-type des taux de reconnaissance par pli (0 pour un seul pli). */
    public double ecartTypePlis() {
        int n = tauxParPli.length;
        if (n < 2) {
            return 0;
        }
        double m = 0;
        for (double t : tauxParPli) {
            m += t;
        }
        m /= n;
        double v = 0;
        for (double t : tauxParPli) {
            v += (t - m) * (t - m);
        }
        return Math.sqrt(v / (n - 1));
    }

    /**
     * Matrice n × C des scores de confiance, alignée sur les classes données.
     *
     * @param classes étiquettes de classe (colonnes)
     * @return scores[i][c] = score de la classe classes[c] pour l'échantillon i
     */
    public double[][] scores(int[] classes) {
        double[][] s = new double[predictions.length][classes.length];
        for (int i = 0; i < predictions.length; i++) {
            for (int c = 0; c < classes.length; c++) {
                s[i][c] = predictions[i].score(classes[c]);
            }
        }
        return s;
    }
}
