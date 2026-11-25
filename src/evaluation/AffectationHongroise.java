package evaluation;

import java.util.Arrays;

/**
 * Algorithme hongrois (Kuhn–Munkres) en O(n³) : affectation de coût minimal dans une
 * matrice carrée. Utilisé pour associer les groupes découverts par les nuées dynamiques
 * (non supervisé, groupes numérotés arbitrairement) aux classes réelles, en maximisant le
 * nombre total d'échantillons « bien placés » ; on peut ensuite construire une matrice de
 * confusion et un taux de reconnaissance comme pour une méthode supervisée.
 */
public final class AffectationHongroise {

    private AffectationHongroise() {
    }

    /**
     * Résout le problème d'affectation : minimise Σ_i cout[i][affectation[i]].
     *
     * @param cout matrice de coûts n × m avec n ≤ m
     * @return affectation[i] = colonne associée à la ligne i (colonnes toutes distinctes)
     */
    public static int[] minimiser(double[][] cout) {
        int n = cout.length;
        int m = cout[0].length;
        if (n > m) {
            throw new IllegalArgumentException("Il faut n <= m");
        }
        // Implémentation par potentiels (indices 1..n / 1..m, 0 = colonne fictive).
        double[] u = new double[n + 1];
        double[] v = new double[m + 1];
        int[] p = new int[m + 1];      // p[j] = ligne affectée à la colonne j
        int[] way = new int[m + 1];
        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[m + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            boolean[] used = new boolean[m + 1];
            do {
                used[j0] = true;
                int i0 = p[j0];
                int j1 = 0;
                double delta = Double.POSITIVE_INFINITY;
                for (int j = 1; j <= m; j++) {
                    if (!used[j]) {
                        double cur = cout[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }
                for (int j = 0; j <= m; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }
        int[] affectation = new int[n];
        for (int j = 1; j <= m; j++) {
            if (p[j] != 0) {
                affectation[p[j] - 1] = j - 1;
            }
        }
        return affectation;
    }

    /**
     * Associe chaque groupe à une classe en maximisant le recouvrement total
     * Σ_g |D_g ∩ classe(g)| (coût = −recouvrement).
     *
     * @param groupes   groupe (0..k−1) de chaque échantillon
     * @param nbGroupes nombre de groupes k (k ≤ C)
     * @param vraies    étiquette réelle de chaque échantillon
     * @param classes   étiquettes des C classes (triées)
     * @return classeDuGroupe[g] = étiquette de classe associée au groupe g
     */
    public static int[] associerGroupesAuxClasses(int[] groupes, int nbGroupes, int[] vraies, int[] classes) {
        int[][] recouvrement = tableContingence(groupes, nbGroupes, vraies, classes);
        double[][] cout = new double[nbGroupes][classes.length];
        for (int g = 0; g < nbGroupes; g++) {
            for (int c = 0; c < classes.length; c++) {
                cout[g][c] = -recouvrement[g][c];
            }
        }
        int[] a = minimiser(cout);
        int[] r = new int[nbGroupes];
        for (int g = 0; g < nbGroupes; g++) {
            r[g] = classes[a[g]];
        }
        return r;
    }

    /**
     * Table de contingence groupes × classes.
     *
     * @param groupes   groupe de chaque échantillon
     * @param nbGroupes nombre de groupes
     * @param vraies    étiquette réelle de chaque échantillon
     * @param classes   étiquettes des classes (triées)
     * @return t[g][c] = nombre d'échantillons de la classe classes[c] dans le groupe g
     */
    public static int[][] tableContingence(int[] groupes, int nbGroupes, int[] vraies, int[] classes) {
        int[][] t = new int[nbGroupes][classes.length];
        for (int i = 0; i < groupes.length; i++) {
            t[groupes[i]][Arrays.binarySearch(classes, vraies[i])]++;
        }
        return t;
    }

    /**
     * Pureté de la partition : (1/N) Σ_g max_c |D_g ∩ c| (chaque groupe compté pour sa
     * classe majoritaire, sans contrainte de bijection, d'où pureté ≥ taux après hongrois).
     *
     * @param groupes   groupe de chaque échantillon
     * @param nbGroupes nombre de groupes
     * @param vraies    étiquettes réelles
     * @param classes   étiquettes des classes
     * @return la pureté dans [0,1]
     */
    public static double purete(int[] groupes, int nbGroupes, int[] vraies, int[] classes) {
        int[][] t = tableContingence(groupes, nbGroupes, vraies, classes);
        int s = 0;
        for (int[] l : t) {
            s += Arrays.stream(l).max().orElse(0);
        }
        return (double) s / groupes.length;
    }
}
