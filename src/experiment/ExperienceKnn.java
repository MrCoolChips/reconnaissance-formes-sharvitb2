package experiment;

import classifier.ConfigKnn;
import classifier.RegleEgalite;
import classifier.TypeNormalisation;
import distance.Minkowski;
import evaluation.EvaluationEngine;
import evaluation.MatriceConfusion;
import evaluation.ResultatEvaluation;
import io.Affichage;
import model.Dataset;
import model.Methode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Expérience 2.a : k plus proches voisins sur les 6 premières classes, pour chacune des
 * cinq méthodes M.
 *
 * <ol>
 *   <li>Grille de paramètres évaluée en leave-one-out : k ∈ {1,3,5,7,9,11},
 *       distance ∈ {L1, L2}, normalisation ∈ {aucune, z-score}.</li>
 *   <li>Sélection, pour chaque méthode, de la configuration de meilleur T_reco
 *       (à égalité : la première rencontrée, c.-à-d. le plus petit k).</li>
 *   <li>Matrice de confusion et critères (P, R, F1) de la configuration retenue.</li>
 *   <li>Comparaison des protocoles de découpage pour cette configuration.</li>
 * </ol>
 * <p>Remarque : choisir les paramètres sur le même leave-one-out que celui qui sert à
 * l'évaluation donne une estimation légèrement optimiste ; la grille complète est fournie
 * pour juger de la sensibilité aux paramètres.</p>
 */
public final class ExperienceKnn {

    /** Valeurs de k testées. */
    static final int[] VALEURS_K = {1, 3, 5, 7, 9, 11};
    /** Distances testées. */
    static final Minkowski[] DISTANCES = {Minkowski.manhattan(), Minkowski.euclidienne()};
    /** Normalisations testées. */
    static final TypeNormalisation[] NORMALISATIONS = {TypeNormalisation.AUCUNE, TypeNormalisation.ZSCORE};

    /** Un point de la grille de paramètres. */
    record PointGrille(Methode methode, ConfigKnn config, double taux, double dureeMs) {
    }

    private ExperienceKnn() {
    }

    /**
     * Évalue toute la grille de paramètres en leave-one-out.
     *
     * @param d base
     * @return les résultats de chaque configuration pour chaque méthode
     */
    static List<PointGrille> grille(Dataset d) {
        List<PointGrille> r = new ArrayList<>();
        for (Methode m : Methode.values()) {
            for (TypeNormalisation t : NORMALISATIONS) {
                for (Minkowski dist : DISTANCES) {
                    for (int k : VALEURS_K) {
                        ConfigKnn cfg = new ConfigKnn(k, dist, t, RegleEgalite.PLUS_PROCHE_VOISIN);
                        ResultatEvaluation res = EvaluationEngine.leaveOneOut(m + "", () -> cfg.creer(m), d);
                        r.add(new PointGrille(m, cfg, res.tauxReconnaissance(), res.dureeMs()));
                    }
                }
            }
        }
        return r;
    }

    /**
     * Sélectionne la meilleure configuration de chaque méthode.
     *
     * @param grille résultats de la grille
     * @return configuration retenue par méthode
     */
    static Map<Methode, ConfigKnn> selectionner(List<PointGrille> grille) {
        EnumMap<Methode, PointGrille> best = new EnumMap<>(Methode.class);
        for (PointGrille p : grille) {
            PointGrille b = best.get(p.methode());
            if (b == null || p.taux() > b.taux() + 1e-12) {
                best.put(p.methode(), p);
            }
        }
        EnumMap<Methode, ConfigKnn> r = new EnumMap<>(Methode.class);
        best.forEach((m, p) -> r.put(m, p.config()));
        return r;
    }

    /**
     * Exécute l'expérience complète.
     *
     * @param ctx contexte d'exécution
     */
    static void executer(Contexte ctx) {
        Dataset d = ctx.classes(6);
        Affichage.titre("2.a  k plus proches voisins - 6 classes (" + d.taille() + " echantillons), LOOCV");

        // 1) Grille de paramètres.
        List<PointGrille> g = grille(d);
        List<List<Object>> csv = new ArrayList<>();
        for (PointGrille p : g) {
            csv.add(List.of(p.methode().name(), p.config().distance().nom(), p.config().normalisation().name(),
                    p.config().k(), p.taux(), 1 - p.taux(), p.dureeMs()));
        }
        ctx.csv().ecrire("knn_grille_6classes.csv",
                List.of("methode", "distance", "normalisation", "k", "T_reco", "T_conf", "duree_ms"), csv);

        for (Methode m : Methode.values()) {
            Affichage.sousTitre("Grille T_reco pour M^" + m + " (dim " + d.dimension(m) + ") : " + m.description());
            List<String> entete = new ArrayList<>(List.of("k"));
            for (TypeNormalisation t : NORMALISATIONS) {
                for (Minkowski dist : DISTANCES) {
                    entete.add(dist.nom() + "/" + t.name().toLowerCase());
                }
            }
            List<List<String>> lignes = new ArrayList<>();
            for (int k : VALEURS_K) {
                List<String> l = new ArrayList<>(List.of(Integer.toString(k)));
                for (TypeNormalisation t : NORMALISATIONS) {
                    for (Minkowski dist : DISTANCES) {
                        g.stream().filter(p -> p.methode() == m && p.config().k() == k
                                        && p.config().normalisation() == t
                                        && p.config().distance().nom().equals(dist.nom()))
                                .findFirst().ifPresent(p -> l.add(Affichage.pct(p.taux())));
                    }
                }
                lignes.add(l);
            }
            Affichage.afficherTableau(entete, lignes);
        }

        // 2) Sélection.
        Map<Methode, ConfigKnn> best = selectionner(g);
        ctx.definirMeilleuresConfigs(best);

        // 3) Détail de la configuration retenue.
        List<List<Object>> resume = new ArrayList<>();
        for (Methode m : Methode.values()) {
            ConfigKnn cfg = best.get(m);
            ResultatEvaluation res = EvaluationEngine.leaveOneOut("kPPV-" + m, () -> cfg.creer(m), d);
            MatriceConfusion mc = res.matrice();
            Affichage.sousTitre("Configuration retenue M^" + m + " : " + cfg.description());
            System.out.print(mc.formater());
            ctx.csv().ecrire("knn_" + m + "_confusion_6classes.csv", mc.enteteCsv(), mc.lignesCsv());
            ctx.csv().ecrire("knn_" + m + "_metriques_6classes.csv", MatriceConfusion.enteteMetriques(),
                    mc.lignesMetriques());
            resume.add(List.of(m.name(), cfg.k(), cfg.distance().nom(), cfg.normalisation().name(),
                    mc.tauxReconnaissance(), mc.tauxConfusion(), mc.precisionMacro(), mc.rappelMacro(),
                    mc.f1Macro(), res.dureeMs()));
        }
        ctx.csv().ecrire("knn_resume_6classes.csv", List.of("methode", "k", "distance", "normalisation",
                "T_reco", "T_conf", "P_macro", "R_macro", "F1_macro", "duree_ms"), resume);

        Affichage.sousTitre("Resume k-PPV (configurations retenues)");
        List<List<String>> t = new ArrayList<>();
        for (List<Object> l : resume) {
            t.add(List.of(l.get(0).toString(), l.get(1) + "," + l.get(2) + "," + l.get(3),
                    Affichage.pct((Double) l.get(4)), Affichage.f4((Double) l.get(6)),
                    Affichage.f4((Double) l.get(7)), Affichage.f4((Double) l.get(8))));
        }
        Affichage.afficherTableau(List.of("methode", "config", "T_reco", "P_macro", "R_macro", "F1_macro"), t);

        // 4) Protocoles.
        Affichage.sousTitre("Influence du protocole de decoupage (k-PPV, configurations retenues)");
        List<List<Object>> prot = new ArrayList<>();
        for (Methode m : Methode.values()) {
            ConfigKnn cfg = best.get(m);
            prot.addAll(Protocoles.comparer("kPPV-" + m, () -> cfg.creer(m), d, ctx.graine()));
        }
        Protocoles.afficher(prot);
        ctx.csv().ecrire("knn_protocoles_6classes.csv", Protocoles.entete(), prot);
    }
}
