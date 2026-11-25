package classifier;

import model.Echantillon;
import model.Methode;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Vote majoritaire (approche 2.c, supervisée) : fusion des décisions de cinq classifieurs
 * de base, un par méthode de description (M^ART, M^ZRK, M^GFD, M^YNG, M^SA).
 *
 * <p>Chaque méthode M_j est associée à un classifieur k-PPV (configuration propre à la
 * méthode). Pour un échantillon de test x, chaque M_j émet un vote pour la classe e_j(x) ;
 * on compte V_c = #{ j : e_j(x) = c } et la classe retenue est argmax_c V_c.</p>
 *
 * <p><b>Égalités</b> (ex. 2-2-1) — départage hiérarchique :</p>
 * <ol>
 *   <li>vote pondéré W_c = Σ_{j : e_j(x)=c} w_j, où w_j est le taux de reconnaissance
 *       individuel de M_j estimé par leave-one-out <i>sur l'ensemble d'apprentissage
 *       uniquement</i> (si la pondération est activée, sinon w_j = 1) ;</li>
 *   <li>si l'égalité persiste : la classe proposée par la méthode dont la distance
 *       normalisée au plus proche voisin (d_1 / d̄) est la plus faible ;</li>
 *   <li>en dernier recours : la plus petite étiquette (déterminisme).</li>
 * </ol>
 *
 * <p><b>Score de confiance</b> par classe (pour les courbes précision/rappel) :</p>
 * <pre>
 *   S_c = V_c / M + (0.9 / M) · ( Σ_j w_j · s_{j,c} ) / Σ_j w_j
 * </pre>
 * <p>où s_{j,c} est le score continu du k-PPV de la méthode j et M = 5. Les scores sont
 * normalisés pour sommer à 1. Le premier terme reflète le vote, le second (borné par 0.9/M)
 * affine le classement à nombre de votes égal.</p>
 */
public final class VoteMajoritaire implements Classifieur {

    private final Map<Methode, ConfigKnn> configs;
    private final boolean ponderation;

    private final EnumMap<Methode, KPlusProchesVoisins> bases = new EnumMap<>(Methode.class);
    private final EnumMap<Methode, Double> poids = new EnumMap<>(Methode.class);

    /**
     * @param configs     configuration k-PPV de chaque méthode participant au vote
     * @param ponderation si vrai, les égalités sont départagées par un vote pondéré par
     *                    les taux de reconnaissance individuels ; sinon tous les poids valent 1
     */
    public VoteMajoritaire(Map<Methode, ConfigKnn> configs, boolean ponderation) {
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Au moins une methode est necessaire");
        }
        this.configs = new EnumMap<>(configs);
        this.ponderation = ponderation;
    }

    @Override
    public void entrainer(List<Echantillon> apprentissage) {
        bases.clear();
        poids.clear();
        for (Map.Entry<Methode, ConfigKnn> e : configs.entrySet()) {
            KPlusProchesVoisins knn = e.getValue().creer(e.getKey());
            knn.entrainer(apprentissage);
            bases.put(e.getKey(), knn);
            // Poids w_j : fiabilité de la méthode estimée sur l'apprentissage seulement.
            poids.put(e.getKey(), ponderation ? knn.tauxLeaveOneOutInterne() : 1.0);
        }
    }

    @Override
    public Prediction predire(Echantillon x) {
        if (bases.isEmpty()) {
            throw new IllegalStateException("Classifieur non entraine");
        }
        int[] classes = null;
        int[] votes = null;
        double[] votesPonderes = null;
        double[] fusion = null;
        double[] meilleureProximite = null;
        double sommePoids = 0;

        for (Map.Entry<Methode, KPlusProchesVoisins> e : bases.entrySet()) {
            Prediction p = e.getValue().predire(x);
            if (classes == null) {
                classes = p.classes();
                votes = new int[classes.length];
                votesPonderes = new double[classes.length];
                fusion = new double[classes.length];
                meilleureProximite = new double[classes.length];
                Arrays.fill(meilleureProximite, Double.POSITIVE_INFINITY);
            }
            double w = poids.get(e.getKey());
            int c = Arrays.binarySearch(classes, p.classe());
            votes[c]++;                                   // V_c
            votesPonderes[c] += w;                        // W_c
            meilleureProximite[c] = Math.min(meilleureProximite[c], p.proximite());
            for (int i = 0; i < classes.length; i++) {
                fusion[i] += w * p.score(classes[i]);     // Σ w_j s_{j,c}
            }
            sommePoids += w;
        }

        // Décision : votes, puis vote pondéré, puis proximité, puis étiquette.
        int gagnante = 0;
        for (int c = 1; c < classes.length; c++) {
            if (meilleure(c, gagnante, votes, votesPonderes, meilleureProximite)) {
                gagnante = c;
            }
        }

        int nbMethodes = bases.size();
        double[] s = new double[classes.length];
        double total = 0;
        for (int c = 0; c < classes.length; c++) {
            double f = sommePoids > 0 ? fusion[c] / sommePoids : 0;
            s[c] = (double) votes[c] / nbMethodes + 0.9 / nbMethodes * f;
            total += s[c];
        }
        for (int c = 0; c < classes.length; c++) {
            s[c] /= total;
        }
        return new Prediction(classes[gagnante], classes.clone(), s, meilleureProximite[gagnante]);
    }

    /** Vrai si la classe a doit être préférée à la classe b selon la hiérarchie de départage. */
    private static boolean meilleure(int a, int b, int[] votes, double[] w, double[] prox) {
        if (votes[a] != votes[b]) {
            return votes[a] > votes[b];
        }
        if (Math.abs(w[a] - w[b]) > 1e-12) {
            return w[a] > w[b];
        }
        if (prox[a] != prox[b]) {
            return prox[a] < prox[b];
        }
        return false; // à égalité complète on garde la plus petite étiquette
    }

    /** @return les poids w_j calculés lors du dernier apprentissage. */
    public Map<Methode, Double> poids() {
        return Collections.unmodifiableMap(poids);
    }

    /**
     * Prédictions individuelles des classifieurs de base (pour l'analyse des désaccords).
     *
     * @param x échantillon à classer
     * @return la prédiction de chaque méthode
     */
    public Map<Methode, Prediction> predictionsIndividuelles(Echantillon x) {
        EnumMap<Methode, Prediction> r = new EnumMap<>(Methode.class);
        bases.forEach((m, k) -> r.put(m, k.predire(x)));
        return r;
    }

    @Override
    public String nom() {
        return "Vote" + (ponderation ? "Pondere" : "Simple") + configs.keySet();
    }
}
