package classifier;

import java.util.Arrays;
import java.util.Random;

/**
 * Nuées dynamiques / k-means (approche 2.b, non supervisée) : partitionnement par
 * minimisation de l'erreur quadratique (« squared-error partitioning »).
 *
 * <p>Pour une partition D_1, …, D_k avec n_i éléments dans D_i :</p>
 * <pre>
 *   m_i = (1 / n_i) Σ_{x ∈ D_i} x                    (centre de gravité)
 *   J   = Σ_{i=1..k} Σ_{x ∈ D_i} || x − m_i ||²       (critère à minimiser)
 * </pre>
 * <p>Algorithme (étapes 1 à 4 du cours, sans l'étape 5 de fusion/scission) :</p>
 * <ol>
 *   <li>choisir k centres initiaux (aléatoires ou k-means++) ;</li>
 *   <li>affecter chaque point au centre le plus proche (nouvelle partition) ;</li>
 *   <li>recalculer les centres comme centres de gravité des groupes ;</li>
 *   <li>répéter 2–3 jusqu'à stabilité des affectations ou un nombre maximal d'itérations.</li>
 * </ol>
 * <p>L'algorithme ne converge que vers un minimum local de J : il est relancé avec
 * plusieurs initialisations (« multi-start ») et l'on garde la partition de J minimal.</p>
 */
public final class NueesDynamiques {

    /** Stratégie de choix des centres initiaux. */
    public enum Initialisation {
        /** k points distincts tirés uniformément. */
        ALEATOIRE,
        /** k-means++ : tirage d'un nouveau centre avec une probabilité ∝ D(x)². */
        KMEANS_PLUS_PLUS
    }

    private final int k;
    private final int iterationsMax;
    private final int nbInitialisations;
    private final Initialisation initialisation;
    private final long graine;

    /**
     * @param k                 nombre de groupes (ici le nombre de classes)
     * @param iterationsMax     nombre maximal d'itérations par exécution
     * @param nbInitialisations nombre d'initialisations différentes (multi-start)
     * @param initialisation    stratégie d'initialisation
     * @param graine            graine du générateur aléatoire (reproductibilité)
     */
    public NueesDynamiques(int k, int iterationsMax, int nbInitialisations,
                           Initialisation initialisation, long graine) {
        if (k < 1 || iterationsMax < 1 || nbInitialisations < 1) {
            throw new IllegalArgumentException("Parametres invalides");
        }
        this.k = k;
        this.iterationsMax = iterationsMax;
        this.nbInitialisations = nbInitialisations;
        this.initialisation = initialisation;
        this.graine = graine;
    }

    /**
     * Partitionne les données en k groupes ; retourne la meilleure des exécutions (J minimal).
     *
     * @param x matrice n × d des données (déjà normalisées si souhaité)
     * @return la meilleure partition trouvée
     */
    public ResultatPartition partitionner(double[][] x) {
        if (x.length < k) {
            throw new IllegalArgumentException("Moins de points (" + x.length + ") que de groupes (" + k + ")");
        }
        Random alea = new Random(graine);
        ResultatPartition meilleur = null;
        for (int s = 0; s < nbInitialisations; s++) {
            ResultatPartition r = executer(x, alea, s);
            if (meilleur == null || r.critereJ() < meilleur.critereJ()) {
                meilleur = r;
            }
        }
        return meilleur;
    }

    /** Une exécution complète de l'algorithme à partir d'une initialisation. */
    private ResultatPartition executer(double[][] x, Random alea, int numero) {
        int n = x.length;
        int d = x[0].length;
        double[][] centres = initialiser(x, alea);
        int[] aff = new int[n];
        Arrays.fill(aff, -1);
        int it = 0;
        while (it < iterationsMax) {
            it++;
            // Étape 2 : affectation au centre le plus proche.
            boolean change = false;
            for (int i = 0; i < n; i++) {
                int g = plusProcheCentre(x[i], centres);
                if (g != aff[i]) {
                    aff[i] = g;
                    change = true;
                }
            }
            if (!change) {
                break; // les appartenances sont stables : minimum local atteint
            }
            // Étape 3 : recalcul des centres de gravité m_i = (1/n_i) Σ x.
            double[][] somme = new double[k][d];
            int[] effectif = new int[k];
            for (int i = 0; i < n; i++) {
                effectif[aff[i]]++;
                for (int j = 0; j < d; j++) {
                    somme[aff[i]][j] += x[i][j];
                }
            }
            for (int g = 0; g < k; g++) {
                if (effectif[g] == 0) {
                    // Groupe vide : on le réinitialise sur le point le plus mal représenté.
                    int loin = pointLePlusEloigne(x, centres, aff, effectif);
                    effectif[aff[loin]]--;
                    for (int j = 0; j < d; j++) {
                        somme[aff[loin]][j] -= x[loin][j];
                    }
                    aff[loin] = g;
                    effectif[g] = 1;
                    somme[g] = x[loin].clone();
                }
            }
            for (int g = 0; g < k; g++) {
                for (int j = 0; j < d; j++) {
                    centres[g][j] = somme[g][j] / effectif[g];
                }
            }
        }
        return new ResultatPartition(aff, centres, critere(x, centres, aff), it, numero);
    }

    /** Étape 1 : choix des centres initiaux selon la stratégie retenue. */
    private double[][] initialiser(double[][] x, Random alea) {
        int n = x.length;
        double[][] c = new double[k][];
        if (initialisation == Initialisation.ALEATOIRE) {
            int[] perm = permutation(n, alea);
            for (int g = 0; g < k; g++) {
                c[g] = x[perm[g]].clone();
            }
            return c;
        }
        // k-means++ : premier centre uniforme, puis tirage proportionnel à D(x)².
        c[0] = x[alea.nextInt(n)].clone();
        double[] d2 = new double[n];
        Arrays.fill(d2, Double.POSITIVE_INFINITY);
        for (int g = 1; g < k; g++) {
            double total = 0;
            for (int i = 0; i < n; i++) {
                d2[i] = Math.min(d2[i], distanceCarree(x[i], c[g - 1]));
                total += d2[i];
            }
            int choisi;
            if (total <= 0) {
                choisi = alea.nextInt(n);
            } else {
                double u = alea.nextDouble() * total;
                choisi = n - 1;
                for (int i = 0; i < n; i++) {
                    u -= d2[i];
                    if (u <= 0) {
                        choisi = i;
                        break;
                    }
                }
            }
            c[g] = x[choisi].clone();
        }
        return c;
    }

    private static int[] permutation(int n, Random alea) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = alea.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        return p;
    }

    private static int plusProcheCentre(double[] x, double[][] centres) {
        int meilleur = 0;
        double dMin = Double.POSITIVE_INFINITY;
        for (int g = 0; g < centres.length; g++) {
            double dd = distanceCarree(x, centres[g]);
            if (dd < dMin) {
                dMin = dd;
                meilleur = g;
            }
        }
        return meilleur;
    }

    /**
     * Point le plus éloigné de son centre, choisi parmi les groupes de plus d'un élément
     * (pour ne pas vider un autre groupe en le déplaçant).
     */
    private static int pointLePlusEloigne(double[][] x, double[][] centres, int[] aff, int[] effectif) {
        int loin = 0;
        double max = -1;
        for (int i = 0; i < x.length; i++) {
            if (effectif[aff[i]] <= 1) {
                continue;
            }
            double dd = distanceCarree(x[i], centres[aff[i]]);
            if (dd > max) {
                max = dd;
                loin = i;
            }
        }
        return loin;
    }

    /**
     * Critère d'erreur quadratique J = Σ_i Σ_{x∈D_i} ||x − m_i||².
     *
     * @param x       données
     * @param centres centres m_i
     * @param aff     affectations
     * @return la valeur de J
     */
    public static double critere(double[][] x, double[][] centres, int[] aff) {
        double j = 0;
        for (int i = 0; i < x.length; i++) {
            j += distanceCarree(x[i], centres[aff[i]]);
        }
        return j;
    }

    /** Carré de la distance euclidienne ||a − b||². */
    static double distanceCarree(double[] a, double[] b) {
        double s = 0;
        for (int j = 0; j < a.length; j++) {
            double e = a[j] - b[j];
            s += e * e;
        }
        return s;
    }
}
