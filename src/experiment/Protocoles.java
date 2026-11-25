package experiment;

import classifier.Classifieur;
import evaluation.Decoupage;
import evaluation.EvaluationEngine;
import evaluation.ResultatEvaluation;
import io.Affichage;
import model.Dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Comparaison des protocoles de découpage (étape 4.a) : leave-one-out et K plis stratifiés
 * (K = 12, 6, 4, 3, 2) répétés plusieurs fois avec des mélanges différents. Montre
 * l'influence de la taille de l'ensemble d'apprentissage et la variance de l'estimation.
 */
public final class Protocoles {

    /** Valeurs de K étudiées. */
    static final int[] VALEURS_K = {12, 6, 4, 3, 2};
    /** Nombre de répétitions des validations croisées à K plis. */
    static final int REPETITIONS = 10;

    private Protocoles() {
    }

    /**
     * Évalue un classifieur sous les différents protocoles.
     *
     * @param approche nom de l'approche
     * @param fabrique fabrique de classifieurs
     * @param d        base
     * @param graine   graine de base des mélanges
     * @return lignes CSV : approche, protocole, n_app, T_reco moyen, écart-type, min, max, durée
     */
    static List<List<Object>> comparer(String approche, Supplier<? extends Classifieur> fabrique,
                                       Dataset d, long graine) {
        List<List<Object>> lignes = new ArrayList<>();
        ResultatEvaluation loo = EvaluationEngine.leaveOneOut(approche, fabrique, d);
        lignes.add(List.of(approche, "LOOCV", d.taille() - 1, loo.tauxReconnaissance(), 0.0,
                loo.tauxReconnaissance(), loo.tauxReconnaissance(), loo.dureeMs()));
        int[] y = d.etiquettes();
        for (int k : VALEURS_K) {
            double[] taux = new double[REPETITIONS];
            double duree = 0;
            for (int r = 0; r < REPETITIONS; r++) {
                ResultatEvaluation res = EvaluationEngine.evaluer(approche, fabrique, d,
                        Decoupage.kPlisStratifies(y, k, graine + 1000L * k + r));
                taux[r] = res.tauxReconnaissance();
                duree += res.dureeMs();
            }
            double moy = 0;
            double min = 1;
            double max = 0;
            for (double t : taux) {
                moy += t;
                min = Math.min(min, t);
                max = Math.max(max, t);
            }
            moy /= REPETITIONS;
            double v = 0;
            for (double t : taux) {
                v += (t - moy) * (t - moy);
            }
            double et = Math.sqrt(v / (REPETITIONS - 1));
            int nApp = d.taille() - d.taille() / k;
            lignes.add(List.of(approche, k + "-plis strat. x" + REPETITIONS, nApp, moy, et, min, max,
                    duree / REPETITIONS));
        }
        return lignes;
    }

    /** En-tête CSV des lignes produites par {@link #comparer}. */
    static List<String> entete() {
        return List.of("approche", "protocole", "n_apprentissage", "T_reco_moyen", "ecart_type",
                "T_reco_min", "T_reco_max", "duree_ms_moyenne");
    }

    /**
     * Affiche les lignes de comparaison sous forme de tableau.
     *
     * @param lignes lignes produites par {@link #comparer}
     */
    static void afficher(List<List<Object>> lignes) {
        List<List<String>> t = new ArrayList<>();
        for (List<Object> l : lignes) {
            t.add(List.of(l.get(0).toString(), l.get(1).toString(), l.get(2).toString(),
                    Affichage.pct((Double) l.get(3)), Affichage.pct((Double) l.get(4)),
                    Affichage.pct((Double) l.get(5)) + " - " + Affichage.pct((Double) l.get(6)),
                    String.format(java.util.Locale.ROOT, "%.1f", (Double) l.get(7))));
        }
        Affichage.afficherTableau(List.of("approche", "protocole", "n_app", "T_reco", "ecart-type",
                "min - max", "ms"), t);
    }
}
