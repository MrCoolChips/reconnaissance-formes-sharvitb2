package classifier;

import model.Echantillon;
import model.Methode;

import java.util.Arrays;
import java.util.List;

/**
 * Classifieur des k plus proches voisins (approche 2.a, supervisée) sur une méthode M donnée.
 *
 * <p>Étant donné l'ensemble d'apprentissage D = {x_1, ..., x_n} étiqueté, un point de test x
 * reçoit l'étiquette la plus fréquente parmi ses k plus proches échantillons de D au sens de
 * la distance choisie (Minkowski L_p). Pour k = 1 on retrouve la règle du plus proche voisin.</p>
 *
 * <p><b>Égalités.</b> Si plusieurs classes obtiennent le même nombre de votes, la règle
 * {@link RegleEgalite} s'applique : classe du plus proche voisin, ou classe dont les voisins
 * ont la distance moyenne la plus faible.</p>
 *
 * <p><b>Score de confiance.</b> Pour chaque classe c on calcule</p>
 * <pre>
 *   s_c = v_c / k + (1 / (2k)) · exp(−δ_c / d̄)
 * </pre>
 * <p>où v_c est le nombre de voisins de classe c, δ_c la distance de x à la classe c (distance
 * au plus proche échantillon de c, ou distance moyenne des voisins de c selon la règle
 * d'égalité) et d̄ la distance moyenne de x à tout l'apprentissage. Le second terme est
 * strictement inférieur à 1/(2k) : il ne peut jamais inverser un écart d'au moins un vote, et
 * il départage exactement les ex-aequo selon la règle choisie. La classe prédite est donc
 * toujours argmax_c s_c. Les scores sont ensuite normalisés (somme = 1). Ce score continu
 * permet de classer les échantillons pour les courbes précision/rappel, même pour k = 1.</p>
 */
public final class KPlusProchesVoisins implements Classifieur {

    private final Methode methode;
    private final ConfigKnn config;

    /** Normaliseur estimé sur l'apprentissage. */
    private Normaliseur normaliseur;
    /** Vecteurs d'apprentissage normalisés. */
    private double[][] base;
    /** Étiquettes des vecteurs d'apprentissage. */
    private int[] etiquettes;
    /** Classes présentes dans l'apprentissage (triées). */
    private int[] classes;

    /**
     * @param methode méthode de description utilisée
     * @param config  paramètres (k, distance, normalisation, règle d'égalité)
     */
    public KPlusProchesVoisins(Methode methode, ConfigKnn config) {
        this.methode = methode;
        this.config = config;
    }

    @Override
    public void entrainer(List<Echantillon> apprentissage) {
        List<double[]> brut = apprentissage.stream().map(e -> e.vecteur(methode)).toList();
        normaliseur = Normaliseur.ajuster(config.normalisation(), brut);
        base = brut.stream().map(normaliseur::appliquer).toArray(double[][]::new);
        etiquettes = apprentissage.stream().mapToInt(Echantillon::classe).toArray();
        classes = Arrays.stream(etiquettes).distinct().sorted().toArray();
    }

    @Override
    public Prediction predire(Echantillon x) {
        verifierEntraine();
        return decider(normaliseur.appliquer(x.vecteur(methode)), -1);
    }

    /**
     * Estime le taux de reconnaissance du classifieur sur son propre ensemble
     * d'apprentissage par « leave-one-out » interne : chaque x_i est classé à l'aide de
     * D \ {x_i}. Ce taux sert de poids (fiabilité de la méthode) dans le vote majoritaire,
     * sans jamais utiliser les échantillons de test.
     *
     * @return le taux de reconnaissance leave-one-out sur l'apprentissage
     */
    public double tauxLeaveOneOutInterne() {
        verifierEntraine();
        int ok = 0;
        for (int i = 0; i < base.length; i++) {
            if (decider(base[i], i).classe() == etiquettes[i]) {
                ok++;
            }
        }
        return (double) ok / base.length;
    }

    /**
     * Règle de décision des k-PPV.
     *
     * @param v      vecteur de test déjà normalisé
     * @param exclu  indice d'apprentissage à ignorer (−1 si aucun) : utile au leave-one-out interne
     * @return la prédiction
     */
    private Prediction decider(double[] v, int exclu) {
        int n = base.length;
        int m = exclu >= 0 ? n - 1 : n;
        if (m == 0) {
            throw new IllegalStateException("Ensemble d'apprentissage vide");
        }
        // 1) Distances de v à tous les échantillons d'apprentissage.
        double[] d = new double[n];
        Integer[] ordre = new Integer[m];
        double somme = 0;
        for (int i = 0, p = 0; i < n; i++) {
            if (i == exclu) {
                continue;
            }
            d[i] = config.distance().calculer(v, base[i]);
            somme += d[i];
            ordre[p++] = i;
        }
        // 2) Tri par distance croissante (indice croissant en cas d'égalité : déterministe).
        Arrays.sort(ordre, (a, b) -> {
            int c = Double.compare(d[a], d[b]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        int kEff = Math.min(config.k(), m);
        int nc = classes.length;
        double dMoy = somme / m;
        if (!(dMoy > 0)) {
            dMoy = 1.0;
        }

        // 3) Votes des k plus proches voisins et distances par classe.
        int[] votes = new int[nc];
        double[] sommeDistVoisins = new double[nc];
        double[] dMin = new double[nc];
        Arrays.fill(dMin, Double.POSITIVE_INFINITY);
        for (int r = 0; r < m; r++) {
            int i = ordre[r];
            int c = Arrays.binarySearch(classes, etiquettes[i]);
            if (r < kEff) {
                votes[c]++;
                sommeDistVoisins[c] += d[i];
            }
            if (d[i] < dMin[c]) {
                dMin[c] = d[i];
            }
        }

        // 4) Scores s_c = v_c/k + exp(−δ_c/d̄)/(2k) ; la prédiction est argmax s_c.
        double[] s = new double[nc];
        int meilleure = 0;
        for (int c = 0; c < nc; c++) {
            double delta;
            if (config.regle() == RegleEgalite.DISTANCE_MOYENNE && votes[c] > 0) {
                delta = sommeDistVoisins[c] / votes[c];
            } else {
                delta = dMin[c]; // classe absente de l'apprentissage : exp(−∞) = 0
            }
            s[c] = (double) votes[c] / kEff + Math.exp(-delta / dMoy) / (2.0 * kEff);
            if (s[c] > s[meilleure]) {
                meilleure = c;
            }
        }
        double total = 0;
        for (double x : s) {
            total += x;
        }
        for (int c = 0; c < nc; c++) {
            s[c] /= total;
        }
        double proximite = d[ordre[0]] / dMoy;
        return new Prediction(classes[meilleure], classes.clone(), s, proximite);
    }

    private void verifierEntraine() {
        if (base == null) {
            throw new IllegalStateException("Classifieur non entraine");
        }
    }

    /** @return la méthode de description utilisée. */
    public Methode methode() {
        return methode;
    }

    /** @return la configuration (k, distance, normalisation, règle). */
    public ConfigKnn config() {
        return config;
    }

    @Override
    public String nom() {
        return "kPPV[" + methode + "," + config.description() + "]";
    }
}
