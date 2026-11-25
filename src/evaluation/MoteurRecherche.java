package evaluation;

import classifier.ConfigKnn;
import classifier.Normaliseur;
import model.Dataset;
import model.Methode;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Classement des candidats pour une requête (recherche par le contenu), utilisé pour les
 * courbes précision/rappel en mode recherche.
 *
 * <p>La normalisation éventuelle des composantes est estimée sur toute la base consultée :
 * elle n'utilise aucune étiquette (paramètres non supervisés), il n'y a donc pas de fuite
 * d'information sur la classe de la requête.</p>
 */
public final class MoteurRecherche {

    private MoteurRecherche() {
    }

    /**
     * Classement k-PPV : candidats triés par distance croissante à la requête dans
     * l'espace de la méthode m (même distance et normalisation que le classifieur 2.a).
     *
     * @param d              base consultée
     * @param m              méthode de description
     * @param cfg            configuration (distance, normalisation)
     * @param inclureRequete si vrai, la requête est placée en tête (distance nulle à elle-même)
     * @return classements[q] = indices triés par pertinence décroissante
     */
    public static int[][] classementsKnn(Dataset d, Methode m, ConfigKnn cfg, boolean inclureRequete) {
        double[][] dist = matriceDistances(d, m, cfg);
        int n = d.taille();
        int[][] r = new int[n][];
        for (int q = 0; q < n; q++) {
            int[] autres = trierParDistance(dist[q], q);
            r[q] = inclureRequete ? prefixer(q, autres) : autres;
        }
        return r;
    }

    /**
     * Classement par vote majoritaire : chaque méthode M_j vote pour ses K' plus proches
     * candidats (K' = K, ou K−1 si la requête occupe déjà la première place). Les candidats
     * sont triés par nombre de votes décroissant ; à égalité de votes, par somme des rangs
     * croissante (comptage de Borda), puis par indice.
     *
     * @param d              base consultée
     * @param configs        configuration de chaque méthode votante
     * @param k              taille K des listes de votes
     * @param inclureRequete si vrai, la requête est placée en tête
     * @return classements[q] = indices triés par pertinence décroissante
     */
    public static int[][] classementsVote(Dataset d, Map<Methode, ConfigKnn> configs, int k, boolean inclureRequete) {
        int n = d.taille();
        EnumMap<Methode, int[][]> rangs = new EnumMap<>(Methode.class);
        for (Map.Entry<Methode, ConfigKnn> e : configs.entrySet()) {
            double[][] dist = matriceDistances(d, e.getKey(), e.getValue());
            int[][] rg = new int[n][n];
            for (int q = 0; q < n; q++) {
                int[] ordre = trierParDistance(dist[q], q);
                for (int pos = 0; pos < ordre.length; pos++) {
                    rg[q][ordre[pos]] = pos;  // rang (0 = plus proche) de chaque candidat
                }
            }
            rangs.put(e.getKey(), rg);
        }
        int kVote = inclureRequete ? k - 1 : k;
        int[][] r = new int[n][];
        for (int q = 0; q < n; q++) {
            int[] votes = new int[n];
            long[] borda = new long[n];
            for (int[][] rg : rangs.values()) {
                for (int j = 0; j < n; j++) {
                    if (j == q) {
                        continue;
                    }
                    if (rg[q][j] < kVote) {
                        votes[j]++;
                    }
                    borda[j] += rg[q][j];
                }
            }
            Integer[] cand = new Integer[n - 1];
            for (int j = 0, p = 0; j < n; j++) {
                if (j != q) {
                    cand[p++] = j;
                }
            }
            Arrays.sort(cand, (a, b) -> {
                if (votes[a] != votes[b]) {
                    return Integer.compare(votes[b], votes[a]);
                }
                if (borda[a] != borda[b]) {
                    return Long.compare(borda[a], borda[b]);
                }
                return Integer.compare(a, b);
            });
            int[] autres = Arrays.stream(cand).mapToInt(Integer::intValue).toArray();
            r[q] = inclureRequete ? prefixer(q, autres) : autres;
        }
        return r;
    }

    /** Matrice n × n des distances entre échantillons (vecteurs normalisés sur la base). */
    private static double[][] matriceDistances(Dataset d, Methode m, ConfigKnn cfg) {
        int n = d.taille();
        List<double[]> brut = Arrays.asList(d.matrice(m));
        Normaliseur norm = Normaliseur.ajuster(cfg.normalisation(), brut);
        double[][] x = brut.stream().map(norm::appliquer).toArray(double[][]::new);
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double v = cfg.distance().calculer(x[i], x[j]);
                dist[i][j] = v;
                dist[j][i] = v;
            }
        }
        return dist;
    }

    /** Indices j ≠ q triés par distance croissante (indice croissant à égalité). */
    private static int[] trierParDistance(double[] dq, int q) {
        int n = dq.length;
        Integer[] o = new Integer[n - 1];
        for (int j = 0, p = 0; j < n; j++) {
            if (j != q) {
                o[p++] = j;
            }
        }
        Arrays.sort(o, (a, b) -> {
            int c = Double.compare(dq[a], dq[b]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        return Arrays.stream(o).mapToInt(Integer::intValue).toArray();
    }

    private static int[] prefixer(int q, int[] autres) {
        int[] r = new int[autres.length + 1];
        r[0] = q;
        System.arraycopy(autres, 0, r, 1, autres.length);
        return r;
    }
}
