package evaluation;

import classifier.Classifieur;
import classifier.Prediction;
import model.Dataset;

import java.util.List;
import java.util.function.Supplier;

/**
 * Moteur d'évaluation : exécute un protocole de validation (liste de plis) pour un
 * classifieur supervisé et cumule la matrice de confusion.
 *
 * <p>Pour chaque pli, un <b>nouveau</b> classifieur est créé par la fabrique, entraîné sur
 * les seuls indices d'apprentissage du pli, puis appliqué aux échantillons de test. Les
 * étiquettes de test ne sont lues que pour remplir la matrice de confusion.</p>
 */
public final class EvaluationEngine {

    private EvaluationEngine() {
    }

    /**
     * Évalue un classifieur selon un protocole.
     *
     * @param nom      nom de l'expérience
     * @param fabrique fabrique de classifieurs non entraînés
     * @param donnees  base de formes
     * @param plis     protocole (LOOCV, K plis stratifiés…)
     * @return la matrice de confusion, les prédictions et la durée d'exécution
     */
    public static ResultatEvaluation evaluer(String nom, Supplier<? extends Classifieur> fabrique,
                                             Dataset donnees, List<Pli> plis) {
        long debut = System.nanoTime();
        int[] vraies = donnees.etiquettes();
        MatriceConfusion mc = new MatriceConfusion(donnees.classes());
        Prediction[] predictions = new Prediction[donnees.taille()];
        double[] tauxParPli = new double[plis.size()];
        for (int f = 0; f < plis.size(); f++) {
            Pli pli = plis.get(f);
            Classifieur c = fabrique.get();
            c.entrainer(donnees.sousListe(pli.apprentissage()));
            int bons = 0;
            for (int i : pli.test()) {
                Prediction p = c.predire(donnees.get(i));
                predictions[i] = p;
                mc.ajouter(vraies[i], p.classe());
                if (p.classe() == vraies[i]) {
                    bons++;
                }
            }
            tauxParPli[f] = pli.test().length == 0 ? 0 : (double) bons / pli.test().length;
        }
        double ms = (System.nanoTime() - debut) / 1e6;
        return new ResultatEvaluation(nom, mc, predictions, vraies, tauxParPli, ms);
    }

    /**
     * Raccourci : évaluation par leave-one-out.
     *
     * @param nom      nom de l'expérience
     * @param fabrique fabrique de classifieurs
     * @param donnees  base de formes
     * @return le résultat d'évaluation
     */
    public static ResultatEvaluation leaveOneOut(String nom, Supplier<? extends Classifieur> fabrique,
                                                 Dataset donnees) {
        return evaluer(nom, fabrique, donnees, Decoupage.leaveOneOut(donnees.taille()));
    }
}
