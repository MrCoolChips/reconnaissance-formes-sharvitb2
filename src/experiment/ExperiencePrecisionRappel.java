package experiment;

import classifier.ConfigKnn;
import classifier.VoteMajoritaire;
import evaluation.CourbePR;
import evaluation.EvaluationEngine;
import evaluation.MoteurRecherche;
import evaluation.PrecisionRappel;
import evaluation.ResultatEvaluation;
import io.Affichage;
import model.Dataset;
import model.Methode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Expérience 3.c : courbes précision/rappel avec K = 12 et AUC pour les approches 2.a
 * (k-PPV, chaque méthode) et 2.c (vote majoritaire).
 *
 * <p>Deux constructions sont produites :</p>
 * <ul>
 *   <li><b>recherche</b> : chaque forme est une requête, la base (requête incluse, d'où
 *       N_u = 12 éléments pertinents) est triée par distance (k-PPV) ou par nombre de votes
 *       des 5 méthodes (vote majoritaire) ;</li>
 *   <li><b>scores</b> : pour chaque classe, les échantillons sont triés par le score de
 *       confiance de cette classe obtenu en leave-one-out.</li>
 * </ul>
 */
public final class ExperiencePrecisionRappel {

    /** Nombre de points de la courbe (taille d'une classe). */
    static final int K = 12;

    private ExperiencePrecisionRappel() {
    }

    /**
     * Exécute l'expérience (courbes détaillées pour 6 classes, AUC pour 6, 12 et 18 classes).
     *
     * @param ctx contexte
     */
    static void executer(Contexte ctx) {
        Map<Methode, ConfigKnn> cfg = ctx.meilleuresConfigs();
        Affichage.titre("3.c  Courbes precision / rappel (K = " + K + ") et AUC");
        List<List<Object>> points = new ArrayList<>();
        List<List<Object>> aucs = new ArrayList<>();
        for (int nb : new int[]{6, 12, 18}) {
            Dataset d = ctx.classes(nb);
            List<CourbePR> rech = new ArrayList<>();
            List<CourbePR> sc = new ArrayList<>();
            int[] y = d.etiquettes();
            int nU = d.effectifClasse(d.classes()[0]);

            for (Methode m : Methode.values()) {
                ConfigKnn c = cfg.get(m);
                rech.add(PrecisionRappel.depuisClassements("kPPV-" + m,
                        MoteurRecherche.classementsKnn(d, m, c, true), y, K, nU));
                ResultatEvaluation res = EvaluationEngine.leaveOneOut("kPPV-" + m, () -> c.creer(m), d);
                sc.add(PrecisionRappel.depuisScores("kPPV-" + m, res.scores(d.classes()), y, d.classes(), K, nU));
            }
            rech.add(PrecisionRappel.depuisClassements("Vote",
                    MoteurRecherche.classementsVote(d, cfg, K, true), y, K, nU));
            ResultatEvaluation rv = EvaluationEngine.leaveOneOut("Vote", () -> new VoteMajoritaire(cfg, true), d);
            sc.add(PrecisionRappel.depuisScores("Vote", rv.scores(d.classes()), y, d.classes(), K, nU));

            for (CourbePR c : rech) {
                ajouter(points, aucs, nb, "recherche", c);
            }
            for (CourbePR c : sc) {
                ajouter(points, aucs, nb, "scores", c);
            }
            if (nb == 6) {
                afficherCourbes("Mode recherche - 6 classes : precision P_i (rappel R_i)", rech);
                afficherCourbes("Mode scores de classe (LOOCV) - 6 classes : precision P_i (rappel R_i)", sc);
            }
        }
        ctx.csv().ecrire("pr_courbes.csv",
                List.of("nb_classes", "mode", "approche", "i", "precision", "rappel"), points);
        ctx.csv().ecrire("pr_auc.csv", List.of("nb_classes", "mode", "approche", "AUC", "P_at_K"), aucs);

        Affichage.sousTitre("AUC (K = " + K + ")");
        List<List<String>> t = new ArrayList<>();
        for (String mode : new String[]{"recherche", "scores"}) {
            List<String> noms = aucs.stream().filter(l -> l.get(1).equals(mode)).map(l -> (String) l.get(2))
                    .distinct().toList();
            for (String nom : noms) {
                List<String> l = new ArrayList<>(List.of(mode, nom));
                for (int nb : new int[]{6, 12, 18}) {
                    aucs.stream().filter(a -> a.get(0).equals(nb) && a.get(1).equals(mode) && a.get(2).equals(nom))
                            .findFirst().ifPresent(a -> l.add(Affichage.f4((Double) a.get(3))));
                }
                t.add(l);
            }
        }
        Affichage.afficherTableau(List.of("mode", "approche", "AUC 6cl", "AUC 12cl", "AUC 18cl"), t);
    }

    private static void ajouter(List<List<Object>> points, List<List<Object>> aucs, int nb, String mode, CourbePR c) {
        for (int i = 0; i < c.taille(); i++) {
            points.add(List.of(nb, mode, c.nom(), i + 1, c.precision()[i], c.rappel()[i]));
        }
        aucs.add(List.of(nb, mode, c.nom(), c.auc(), c.precision()[c.taille() - 1]));
    }

    private static void afficherCourbes(String titre, List<CourbePR> courbes) {
        Affichage.sousTitre(titre);
        List<String> entete = new ArrayList<>(List.of("i"));
        courbes.forEach(c -> entete.add(c.nom()));
        List<List<String>> lignes = new ArrayList<>();
        for (int i = 0; i < K; i++) {
            List<String> l = new ArrayList<>(List.of(Integer.toString(i + 1)));
            for (CourbePR c : courbes) {
                l.add(String.format(Locale.ROOT, "%.3f (%.3f)", c.precision()[i], c.rappel()[i]));
            }
            lignes.add(l);
        }
        List<String> a = new ArrayList<>(List.of("AUC"));
        courbes.forEach(c -> a.add(Affichage.f4(c.auc())));
        lignes.add(a);
        Affichage.afficherTableau(entete, lignes);
    }
}
