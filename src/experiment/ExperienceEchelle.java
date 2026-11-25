package experiment;

import classifier.Classifieur;
import classifier.ConfigKnn;
import classifier.VoteMajoritaire;
import evaluation.Decoupage;
import evaluation.EvaluationEngine;
import evaluation.ResultatEvaluation;
import io.Affichage;
import model.Dataset;
import model.Methode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Expérience 4.b : passage à l'échelle. Les approches 2.a (k-PPV, chaque méthode, avec la
 * configuration retenue sur 6 classes) et 2.c (vote majoritaire simple et pondéré) sont
 * évaluées sur les 6, 12 puis 18 premières classes. On compare les taux de reconnaissance
 * globaux (LOOCV et 12 plis stratifiés) et on mesure les temps d'exécution.
 */
public final class ExperienceEchelle {

    private ExperienceEchelle() {
    }

    /**
     * Exécute l'expérience.
     *
     * @param ctx contexte
     */
    static void executer(Contexte ctx) {
        Map<Methode, ConfigKnn> cfg = ctx.meilleuresConfigs();
        Affichage.titre("4.b  Comparaison 6 / 12 / 18 classes (taux de reconnaissance globaux)");
        Map<String, Supplier<? extends Classifieur>> approches = new LinkedHashMap<>();
        for (Methode m : Methode.values()) {
            ConfigKnn c = cfg.get(m);
            approches.put("kPPV-" + m + " [" + c.description() + "]", () -> c.creer(m));
        }
        approches.put("VoteSimple", () -> new VoteMajoritaire(cfg, false));
        approches.put("VotePondere", () -> new VoteMajoritaire(cfg, true));

        int[] tailles = {6, 12, 18};
        List<List<Object>> csv = new ArrayList<>();
        List<List<String>> table = new ArrayList<>();
        for (Map.Entry<String, Supplier<? extends Classifieur>> a : approches.entrySet()) {
            List<String> l = new ArrayList<>(List.of(a.getKey()));
            List<String> temps = new ArrayList<>();
            for (int nb : tailles) {
                Dataset d = ctx.classes(nb);
                ResultatEvaluation loo = EvaluationEngine.leaveOneOut(a.getKey(), a.getValue(), d);
                ResultatEvaluation k12 = EvaluationEngine.evaluer(a.getKey(), a.getValue(), d,
                        Decoupage.kPlisStratifies(d.etiquettes(), 12, ctx.graine()));
                csv.add(List.of(a.getKey(), nb, d.taille(), loo.tauxReconnaissance(), loo.matrice().f1Macro(),
                        loo.dureeMs(), k12.tauxReconnaissance(), k12.dureeMs()));
                l.add(Affichage.pct(loo.tauxReconnaissance()) + " / " + Affichage.pct(k12.tauxReconnaissance()));
                temps.add(String.format(Locale.ROOT, "%.0f", loo.dureeMs()));
                System.out.println(String.format(Locale.ROOT, "[%s] %2d classes : T_reco LOOCV = %s (%.1f ms)",
                        a.getKey(), nb, Affichage.pct(loo.tauxReconnaissance()), loo.dureeMs()));
            }
            l.add(String.join(" / ", temps));
            table.add(l);
        }
        Affichage.sousTitre("T_reco  LOOCV / 12-plis stratifies   (temps LOOCV en ms : 6 / 12 / 18 classes)");
        Affichage.afficherTableau(List.of("approche", "6 classes", "12 classes", "18 classes", "ms LOOCV"), table);
        ctx.csv().ecrire("echelle_6_12_18.csv", List.of("approche", "nb_classes", "n", "T_reco_LOOCV",
                "F1_macro_LOOCV", "duree_ms_LOOCV", "T_reco_12plis", "duree_ms_12plis"), csv);
    }
}
