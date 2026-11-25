package evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 * Protocoles de découpage des données (étape 3.a).
 *
 * <p>Avec seulement 12 échantillons par classe, un découpage unique apprentissage/test
 * (« hold-out ») donne une estimation très variable et gaspille des données. On propose :</p>
 * <ul>
 *   <li><b>Leave-One-Out (LOOCV)</b> : n plis, chaque échantillon est testé une fois avec
 *       les n−1 autres en apprentissage. Estimateur quasi sans biais, déterministe, et peu
 *       coûteux pour les k-PPV (pas d'apprentissage proprement dit).</li>
 *   <li><b>K plis stratifiés</b> : chaque pli conserve la proportion de chaque classe (avec
 *       K = 12, chaque pli contient exactement un échantillon de chaque classe). Permet
 *       d'étudier l'effet de la taille de l'apprentissage (K = 2 ≈ hold-out 50/50).</li>
 * </ul>
 */
public final class Decoupage {

    private Decoupage() {
    }

    /**
     * Validation croisée « leave-one-out ».
     *
     * @param n nombre d'échantillons
     * @return n plis (test = {i}, apprentissage = tous les autres)
     */
    public static List<Pli> leaveOneOut(int n) {
        List<Pli> plis = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int[] app = new int[n - 1];
            for (int j = 0, p = 0; j < n; j++) {
                if (j != i) {
                    app[p++] = j;
                }
            }
            plis.add(new Pli(app, new int[]{i}));
        }
        return plis;
    }

    /**
     * Validation croisée à K plis stratifiés : les indices de chaque classe sont mélangés
     * puis distribués à tour de rôle dans les K plis (le décalage est cumulé d'une classe à
     * l'autre pour équilibrer la taille des plis lorsque K ne divise pas l'effectif).
     *
     * @param etiquettes étiquette de chaque échantillon
     * @param k          nombre de plis (2 ≤ K ≤ effectif minimal d'une classe conseillé)
     * @param graine     graine du mélange aléatoire
     * @return les K plis
     */
    public static List<Pli> kPlisStratifies(int[] etiquettes, int k, long graine) {
        if (k < 2 || k > etiquettes.length) {
            throw new IllegalArgumentException("K invalide : " + k);
        }
        TreeMap<Integer, List<Integer>> parClasse = new TreeMap<>();
        for (int i = 0; i < etiquettes.length; i++) {
            parClasse.computeIfAbsent(etiquettes[i], c -> new ArrayList<>()).add(i);
        }
        Random alea = new Random(graine);
        List<List<Integer>> tests = new ArrayList<>();
        for (int f = 0; f < k; f++) {
            tests.add(new ArrayList<>());
        }
        int decalage = 0;
        for (List<Integer> indices : parClasse.values()) {
            Collections.shuffle(indices, alea);
            for (int j = 0; j < indices.size(); j++) {
                tests.get((decalage + j) % k).add(indices.get(j));
            }
            decalage += indices.size();
        }
        List<Pli> plis = new ArrayList<>(k);
        for (int f = 0; f < k; f++) {
            boolean[] estTest = new boolean[etiquettes.length];
            for (int i : tests.get(f)) {
                estTest[i] = true;
            }
            int[] test = tests.get(f).stream().mapToInt(Integer::intValue).sorted().toArray();
            int[] app = new int[etiquettes.length - test.length];
            for (int i = 0, p = 0; i < etiquettes.length; i++) {
                if (!estTest[i]) {
                    app[p++] = i;
                }
            }
            plis.add(new Pli(app, test));
        }
        return plis;
    }
}
