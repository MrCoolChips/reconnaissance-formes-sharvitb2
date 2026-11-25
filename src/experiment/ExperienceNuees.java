package experiment;

import classifier.Normaliseur;
import classifier.NueesDynamiques;
import classifier.ResultatPartition;
import classifier.TypeNormalisation;
import evaluation.AffectationHongroise;
import evaluation.MatriceConfusion;
import io.Affichage;
import model.Dataset;
import model.Methode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Expérience 2.b : nuées dynamiques (k-means), non supervisées.
 *
 * <p>Pour chaque méthode M (et pour la concaténation des cinq descripteurs), on partitionne
 * la base en k = C groupes (C = nombre de classes), avec plusieurs initialisations, puis on
 * associe les groupes aux classes par l'algorithme hongrois pour obtenir une matrice de
 * confusion et un taux de reconnaissance. Il n'y a pas de découpage apprentissage/test :
 * l'algorithme n'utilise jamais les étiquettes, qui servent uniquement à l'évaluation.</p>
 */
public final class ExperienceNuees {

    /** Nombre maximal d'itérations. */
    static final int ITERATIONS_MAX = 100;
    /** Nombre d'initialisations (multi-start). */
    static final int INITIALISATIONS = 30;

    private ExperienceNuees() {
    }

    /** Représentation à partitionner : une méthode ou la concaténation de toutes. */
    private record Representation(String nom, Methode[] methodes) {
    }

    /**
     * Exécute l'expérience.
     *
     * @param ctx contexte
     */
    static void executer(Contexte ctx) {
        Affichage.titre("2.b  Nuees dynamiques (k-means), k = nombre de classes, "
                + INITIALISATIONS + " initialisations");
        List<Representation> reps = new ArrayList<>();
        for (Methode m : Methode.values()) {
            reps.add(new Representation(m.name(), new Methode[]{m}));
        }
        reps.add(new Representation("CONCAT", Methode.values()));

        List<List<Object>> csv = new ArrayList<>();
        for (int nb : new int[]{6, 12, 18}) {
            Dataset d = ctx.classes(nb);
            Affichage.sousTitre(nb + " classes (" + d.taille() + " echantillons)");
            List<List<String>> table = new ArrayList<>();
            for (Representation rep : reps) {
                for (TypeNormalisation t : new TypeNormalisation[]{TypeNormalisation.AUCUNE, TypeNormalisation.ZSCORE}) {
                    for (NueesDynamiques.Initialisation init : NueesDynamiques.Initialisation.values()) {
                        long debut = System.nanoTime();
                        double[][] x = preparer(d, rep, t);
                        NueesDynamiques km = new NueesDynamiques(nb, ITERATIONS_MAX, INITIALISATIONS, init,
                                ctx.graine());
                        ResultatPartition p = km.partitionner(x);
                        int[] classeDuGroupe = AffectationHongroise.associerGroupesAuxClasses(
                                p.affectations(), nb, d.etiquettes(), d.classes());
                        MatriceConfusion mc = new MatriceConfusion(d.classes());
                        int[] y = d.etiquettes();
                        for (int i = 0; i < y.length; i++) {
                            mc.ajouter(y[i], classeDuGroupe[p.affectations()[i]]);
                        }
                        double purete = AffectationHongroise.purete(p.affectations(), nb, y, d.classes());
                        double ms = (System.nanoTime() - debut) / 1e6;
                        csv.add(List.of(nb, rep.nom(), t.name(), init.name(), p.critereJ(), p.iterations(),
                                mc.tauxReconnaissance(), purete, mc.f1Macro(), ms));
                        table.add(List.of(rep.nom(), t.name().toLowerCase(), init.name().toLowerCase(),
                                String.format(Locale.ROOT, "%.4g", p.critereJ()), Integer.toString(p.iterations()),
                                Affichage.pct(mc.tauxReconnaissance()), Affichage.pct(purete),
                                Affichage.f4(mc.f1Macro()), String.format(Locale.ROOT, "%.0f", ms)));
                        if (nb == 6 && init == NueesDynamiques.Initialisation.KMEANS_PLUS_PLUS) {
                            String suffixe = rep.nom() + "_" + t.name().toLowerCase() + "_6classes.csv";
                            ctx.csv().ecrire("kmeans_" + "confusion_" + suffixe, mc.enteteCsv(), mc.lignesCsv());
                            ctx.csv().ecrire("kmeans_" + "metriques_" + suffixe,
                                    MatriceConfusion.enteteMetriques(), mc.lignesMetriques());
                            if (t == TypeNormalisation.AUCUNE && rep.methodes().length == 1
                                    && rep.methodes()[0] == Methode.values()[0]) {
                                System.out.println("Exemple : matrice de confusion " + rep.nom()
                                        + " (sans normalisation, k-means++), groupes -> classes "
                                        + Arrays.toString(classeDuGroupe));
                                System.out.print(mc.formater());
                            }
                        }
                    }
                }
            }
            Affichage.afficherTableau(List.of("representation", "norm.", "init.", "J", "iter",
                    "T_reco(hongrois)", "purete", "F1_macro", "ms"), table);
        }
        ctx.csv().ecrire("kmeans_resultats.csv", List.of("nb_classes", "representation", "normalisation",
                "initialisation", "J", "iterations", "T_reco_hongrois", "purete", "F1_macro", "duree_ms"), csv);
    }

    /** Construit la matrice des données (concaténation éventuelle puis normalisation). */
    private static double[][] preparer(Dataset d, Representation rep, TypeNormalisation t) {
        if (rep.methodes().length == 1) {
            double[][] x = d.matrice(rep.methodes()[0]);
            Normaliseur n = Normaliseur.ajuster(t, Arrays.asList(x));
            return Arrays.stream(x).map(n::appliquer).toArray(double[][]::new);
        }
        // Concaténation : chaque bloc est normalisé séparément puis divisé par sqrt(d_bloc)
        // (en z-score) pour que chaque méthode pèse autant dans la distance euclidienne.
        int n = d.taille();
        double[][] r = new double[n][0];
        for (Methode m : rep.methodes()) {
            double[][] x = d.matrice(m);
            Normaliseur nz = Normaliseur.ajuster(t, Arrays.asList(x));
            double poids = t == TypeNormalisation.AUCUNE ? 1.0 : 1.0 / Math.sqrt(x[0].length);
            for (int i = 0; i < n; i++) {
                double[] v = nz.appliquer(x[i]);
                double[] c = Arrays.copyOf(r[i], r[i].length + v.length);
                for (int j = 0; j < v.length; j++) {
                    c[r[i].length + j] = v[j] * poids;
                }
                r[i] = c;
            }
        }
        return r;
    }
}
