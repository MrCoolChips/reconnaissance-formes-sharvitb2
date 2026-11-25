package evaluation;

import java.util.Arrays;

/**
 * Calcul des courbes précision/rappel avec K points et de l'AUC (étape 3.c).
 *
 * <p><b>Mode « recherche » (requête/candidats).</b> Chaque échantillon q de la base est
 * pris comme requête ; les échantillons sont classés par similarité décroissante avec q.
 * Si n_i est le nombre d'éléments de la classe de q parmi les i premiers candidats :</p>
 * <pre>
 *   Precision_i^u = n_i / i        Recall_i^u = n_i / N_u        (i = 1..K, K = 12)
 * </pre>
 * <p>où N_u est le nombre d'éléments pertinents (12 = taille d'une classe lorsque la
 * requête fait partie de la base consultée). Les points sont moyennés sur toutes les
 * requêtes (les classes étant équilibrées, c'est aussi la moyenne des moyennes par classe).</p>
 *
 * <p><b>Mode « scores de classe ».</b> Pour chaque classe u, les échantillons de test sont
 * classés par score de confiance décroissant s_u(x) produit par le classifieur (k-PPV ou
 * vote) en leave-one-out ; on applique les mêmes formules puis on moyenne sur les classes.</p>
 *
 * <p><b>AUC.</b> Règle des trapèzes sur les points (R_i, P_i), en partant du point
 * (0, P_1) : AUC = Σ_{i=1..K} (R_i − R_{i−1}) (P_i + P_{i−1}) / 2 avec R_0 = 0, P_0 = P_1.
 * La valeur maximale (classement parfait, N_u = K) vaut 1.</p>
 */
public final class PrecisionRappel {

    private PrecisionRappel() {
    }

    /**
     * Courbe P/R en mode recherche.
     *
     * @param nom         nom de l'approche
     * @param classements classements[q] = indices des candidats triés par pertinence décroissante
     * @param etiquettes  étiquette de chaque échantillon
     * @param k           nombre de points K
     * @param nU          nombre d'éléments pertinents N_u par requête
     * @return la courbe moyenne et son AUC
     */
    public static CourbePR depuisClassements(String nom, int[][] classements, int[] etiquettes, int k, int nU) {
        double[] p = new double[k];
        double[] r = new double[k];
        int nq = classements.length;
        for (int q = 0; q < nq; q++) {
            int ni = 0;
            for (int i = 0; i < k; i++) {
                if (i < classements[q].length && etiquettes[classements[q][i]] == etiquettes[q]) {
                    ni++;
                }
                p[i] += (double) ni / (i + 1);
                r[i] += (double) ni / nU;
            }
        }
        for (int i = 0; i < k; i++) {
            p[i] /= nq;
            r[i] /= nq;
        }
        return new CourbePR(nom, p, r, auc(p, r));
    }

    /**
     * Courbe P/R à partir des scores de confiance par classe.
     *
     * @param nom     nom de l'approche
     * @param scores  scores[i][c] : score de la classe classes[c] pour l'échantillon i
     * @param vraies  étiquette réelle de chaque échantillon
     * @param classes étiquettes des classes (colonnes de scores)
     * @param k       nombre de points K
     * @param nU      nombre d'éléments pertinents par classe N_u
     * @return la courbe moyenne sur les classes et son AUC
     */
    public static CourbePR depuisScores(String nom, double[][] scores, int[] vraies, int[] classes, int k, int nU) {
        int n = scores.length;
        double[] p = new double[k];
        double[] r = new double[k];
        for (int u = 0; u < classes.length; u++) {
            final int cu = u;
            Integer[] ordre = new Integer[n];
            for (int i = 0; i < n; i++) {
                ordre[i] = i;
            }
            Arrays.sort(ordre, (a, b) -> {
                int c = Double.compare(scores[b][cu], scores[a][cu]);
                return c != 0 ? c : Integer.compare(a, b);
            });
            int ni = 0;
            for (int i = 0; i < k; i++) {
                if (i < n && vraies[ordre[i]] == classes[u]) {
                    ni++;
                }
                p[i] += (double) ni / (i + 1);
                r[i] += (double) ni / nU;
            }
        }
        for (int i = 0; i < k; i++) {
            p[i] /= classes.length;
            r[i] /= classes.length;
        }
        return new CourbePR(nom, p, r, auc(p, r));
    }

    /**
     * Aire sous la courbe précision/rappel par la règle des trapèzes.
     *
     * @param p précisions P_1..P_K
     * @param r rappels R_1..R_K (croissants)
     * @return l'AUC
     */
    public static double auc(double[] p, double[] r) {
        double aire = 0;
        double rPrec = 0;
        double pPrec = p.length > 0 ? p[0] : 0;
        for (int i = 0; i < p.length; i++) {
            aire += (r[i] - rPrec) * (p[i] + pPrec) / 2.0;
            rPrec = r[i];
            pPrec = p[i];
        }
        return aire;
    }
}
