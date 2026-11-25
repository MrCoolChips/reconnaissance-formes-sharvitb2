package experiment;

import classifier.ConfigKnn;
import classifier.Prediction;
import classifier.VoteMajoritaire;
import evaluation.EvaluationEngine;
import evaluation.MatriceConfusion;
import evaluation.ResultatEvaluation;
import io.Affichage;
import model.Dataset;
import model.Echantillon;
import model.Methode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Expérience 2.c : vote majoritaire des cinq méthodes (classifieurs de base k-PPV avec la
 * configuration retenue pour chaque méthode), sur les 6 premières classes.
 *
 * <p>Deux variantes sont comparées : vote simple (égalités départagées par la proximité du
 * plus proche voisin) et vote pondéré (égalités départagées d'abord par les taux de
 * reconnaissance individuels estimés sur l'apprentissage). On mesure aussi la fréquence des
 * situations d'égalité et le nombre d'échantillons « sauvés » par la fusion.</p>
 */
public final class ExperienceVote {

    private ExperienceVote() {
    }

    /**
     * Exécute l'expérience.
     *
     * @param ctx contexte
     */
    static void executer(Contexte ctx) {
        Dataset d = ctx.classes(6);
        Map<Methode, ConfigKnn> cfg = ctx.meilleuresConfigs();
        Affichage.titre("2.c  Vote majoritaire (5 methodes) - 6 classes, LOOCV");
        System.out.println("Classifieurs de base : " + cfg);

        List<List<Object>> resume = new ArrayList<>();
        for (boolean pond : new boolean[]{false, true}) {
            String nom = pond ? "VotePondere" : "VoteSimple";
            ResultatEvaluation res = EvaluationEngine.leaveOneOut(nom, () -> new VoteMajoritaire(cfg, pond), d);
            MatriceConfusion mc = res.matrice();
            Affichage.sousTitre(nom);
            System.out.print(mc.formater());
            ctx.csv().ecrire("vote_" + nom + "_confusion_6classes.csv", mc.enteteCsv(), mc.lignesCsv());
            ctx.csv().ecrire("vote_" + nom + "_metriques_6classes.csv", MatriceConfusion.enteteMetriques(),
                    mc.lignesMetriques());
            resume.add(List.of(nom, mc.tauxReconnaissance(), mc.tauxConfusion(), mc.precisionMacro(),
                    mc.rappelMacro(), mc.f1Macro(), res.dureeMs()));
        }
        ctx.csv().ecrire("vote_resume_6classes.csv",
                List.of("approche", "T_reco", "T_conf", "P_macro", "R_macro", "F1_macro", "duree_ms"), resume);

        // Poids (taux LOO individuels) appris sur toute la base 6 classes, pour information.
        VoteMajoritaire v = new VoteMajoritaire(cfg, true);
        v.entrainer(d.echantillons());
        Affichage.sousTitre("Poids w_j (taux LOO individuels sur les 72 echantillons)");
        v.poids().forEach((m, w) -> System.out.println("  " + m + " : " + Affichage.pct(w)));

        for (int nb : new int[]{6, 12, 18}) {
            analyserAccords(ctx, ctx.classes(nb), cfg);
        }

        Affichage.sousTitre("Influence du protocole de decoupage (vote pondere)");
        List<List<Object>> prot = Protocoles.comparer("VotePondere", () -> new VoteMajoritaire(cfg, true), d,
                ctx.graine());
        Protocoles.afficher(prot);
        ctx.csv().ecrire("vote_protocoles_6classes.csv", Protocoles.entete(), prot);
    }

    /**
     * Analyse, en leave-one-out, la répartition des votes : nombre de méthodes correctes par
     * échantillon, fréquence des égalités et gain de la fusion par rapport à chaque méthode.
     */
    private static void analyserAccords(Contexte ctx, Dataset d, Map<Methode, ConfigKnn> cfg) {
        int n = d.taille();
        int[] histogramme = new int[cfg.size() + 1];
        int egalites = 0;
        int egalitesBienResolues = 0;
        int sauves = 0;   // vote correct alors qu'au moins 3 méthodes sur 5 se trompent
        int perdus = 0;   // vote faux alors qu'au moins une méthode avait raison
        List<List<Object>> details = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Echantillon> app = new ArrayList<>(d.echantillons());
            Echantillon x = app.remove(i);
            VoteMajoritaire v = new VoteMajoritaire(cfg, true);
            v.entrainer(app);
            Map<Methode, Prediction> ind = v.predictionsIndividuelles(x);
            Prediction fin = v.predire(x);
            int bons = 0;
            Map<Integer, Integer> compte = new java.util.TreeMap<>();
            StringBuilder votes = new StringBuilder();
            for (Map.Entry<Methode, Prediction> e : ind.entrySet()) {
                int c = e.getValue().classe();
                compte.merge(c, 1, Integer::sum);
                if (c == x.classe()) {
                    bons++;
                }
                votes.append(e.getKey()).append('=').append(c).append(' ');
            }
            histogramme[bons]++;
            int max = compte.values().stream().max(Integer::compare).orElse(0);
            boolean egalite = compte.values().stream().filter(c -> c == max).count() > 1;
            if (egalite) {
                egalites++;
                if (fin.classe() == x.classe()) {
                    egalitesBienResolues++;
                }
            }
            boolean correct = fin.classe() == x.classe();
            if (correct && 2 * bons < cfg.size()) {
                sauves++;
            }
            if (!correct && bons > 0) {
                perdus++;
            }
            details.add(List.of(x.identifiant(), x.classe(), votes.toString().trim(), fin.classe(),
                    egalite ? "oui" : "non", correct ? 1 : 0));
        }
        int nb = d.nombreClasses();
        ctx.csv().ecrire("vote_details_votes_" + nb + "classes.csv",
                List.of("echantillon", "classe_reelle", "votes", "decision", "egalite", "correct"), details);
        Affichage.sousTitre("Vote pondere, " + nb + " classes : nombre de methodes correctes par echantillon (LOOCV)");
        for (int b = 0; b < histogramme.length; b++) {
            System.out.println(String.format(Locale.ROOT, "  %d/%d methodes correctes : %3d echantillons",
                    b, cfg.size(), histogramme[b]));
        }
        System.out.println("  Situations d'egalite : " + egalites + " (dont " + egalitesBienResolues
                + " correctement resolues)");
        System.out.println("  Vote correct malgre une minorite de methodes correctes : " + sauves
                + " ; vote faux alors qu'au moins une methode avait raison : " + perdus);
    }
}
