package evaluation;

import io.Affichage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Matrice de confusion C × C et critères d'évaluation associés (étape 3.b).
 *
 * <p>Convention : la ligne i correspond à la <b>vérité terrain</b> (classe réelle), la
 * colonne j à la <b>classe estimée</b>. M[i][j] = nombre d'échantillons de la classe i
 * classés en j. Pour une classe i :</p>
 * <pre>
 *   VP_i = M[i][i]
 *   FN_i = Σ_j M[i][j] − VP_i      (échantillons de i non reconnus)
 *   FP_i = Σ_j M[j][i] − VP_i      (échantillons attribués à tort à i)
 *   R_i  = VP_i / (VP_i + FN_i)     (rappel)
 *   P_i  = VP_i / (VP_i + FP_i)     (précision)
 *   F1_i = 2 · P_i · R_i / (P_i + R_i)
 *   T_reco = Σ_i VP_i / N           T_conf = 1 − T_reco
 * </pre>
 */
public final class MatriceConfusion {

    private final int[] classes;
    private final int[][] m;

    /**
     * @param classes étiquettes des classes (triées), qui indexent lignes et colonnes
     */
    public MatriceConfusion(int[] classes) {
        this.classes = classes.clone();
        Arrays.sort(this.classes);
        this.m = new int[classes.length][classes.length];
    }

    private int position(int classe) {
        int p = Arrays.binarySearch(classes, classe);
        if (p < 0) {
            throw new IllegalArgumentException("Classe inconnue : " + classe);
        }
        return p;
    }

    /**
     * Enregistre un résultat de classification.
     *
     * @param vraie   classe réelle (vérité terrain)
     * @param estimee classe estimée par le classifieur
     */
    public void ajouter(int vraie, int estimee) {
        m[position(vraie)][position(estimee)]++;
    }

    /** @return les étiquettes des classes. */
    public int[] classes() {
        return classes.clone();
    }

    /** @return M[i][j] (i : vérité, j : estimée), en positions 0..C−1. */
    public int valeur(int i, int j) {
        return m[i][j];
    }

    /** @return le nombre total N d'échantillons testés. */
    public int total() {
        int t = 0;
        for (int[] l : m) {
            for (int v : l) {
                t += v;
            }
        }
        return t;
    }

    /** @return le nombre d'échantillons correctement classés (trace de M). */
    public int correctementClasses() {
        int t = 0;
        for (int i = 0; i < m.length; i++) {
            t += m[i][i];
        }
        return t;
    }

    /** @return le taux de reconnaissance T_reco = (bien classés) / N. */
    public double tauxReconnaissance() {
        int n = total();
        return n == 0 ? 0 : (double) correctementClasses() / n;
    }

    /** @return le taux de confusion (d'erreur) T_conf = (mal classés) / N = 1 − T_reco. */
    public double tauxConfusion() {
        int n = total();
        return n == 0 ? 0 : (double) (n - correctementClasses()) / n;
    }

    /** @return VP_i. */
    public int vraisPositifs(int i) {
        return m[i][i];
    }

    /** @return FN_i = (somme de la ligne i) − VP_i. */
    public int fauxNegatifs(int i) {
        int s = 0;
        for (int j = 0; j < m.length; j++) {
            s += m[i][j];
        }
        return s - m[i][i];
    }

    /** @return FP_i = (somme de la colonne i) − VP_i. */
    public int fauxPositifs(int i) {
        int s = 0;
        for (int[] ligne : m) {
            s += ligne[i];
        }
        return s - m[i][i];
    }

    /** @return la précision P_i = VP_i / (VP_i + FP_i) (0 si la classe n'est jamais prédite). */
    public double precision(int i) {
        int d = vraisPositifs(i) + fauxPositifs(i);
        return d == 0 ? 0 : (double) vraisPositifs(i) / d;
    }

    /** @return le rappel R_i = VP_i / (VP_i + FN_i). */
    public double rappel(int i) {
        int d = vraisPositifs(i) + fauxNegatifs(i);
        return d == 0 ? 0 : (double) vraisPositifs(i) / d;
    }

    /** @return la F-mesure F1_i = 2 P_i R_i / (P_i + R_i). */
    public double f1(int i) {
        double p = precision(i);
        double r = rappel(i);
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }

    /** @return la précision macro-moyenne (1/C) Σ P_i. */
    public double precisionMacro() {
        double s = 0;
        for (int i = 0; i < classes.length; i++) {
            s += precision(i);
        }
        return s / classes.length;
    }

    /** @return le rappel macro-moyen (1/C) Σ R_i. */
    public double rappelMacro() {
        double s = 0;
        for (int i = 0; i < classes.length; i++) {
            s += rappel(i);
        }
        return s / classes.length;
    }

    /** @return la F-mesure macro-moyenne (1/C) Σ F1_i. */
    public double f1Macro() {
        double s = 0;
        for (int i = 0; i < classes.length; i++) {
            s += f1(i);
        }
        return s / classes.length;
    }

    /** @return la matrice formatée pour la console (lignes = vérité, colonnes = estimée). */
    public String formater() {
        List<String> entete = new ArrayList<>();
        entete.add("verite\\estimee");
        for (int c : classes) {
            entete.add(String.format("S%02d", c));
        }
        entete.add("R_i");
        List<List<String>> lignes = new ArrayList<>();
        for (int i = 0; i < classes.length; i++) {
            List<String> l = new ArrayList<>();
            l.add(String.format("S%02d", classes[i]));
            for (int j = 0; j < classes.length; j++) {
                l.add(m[i][j] == 0 ? "." : Integer.toString(m[i][j]));
            }
            l.add(Affichage.f4(rappel(i)));
            lignes.add(l);
        }
        List<String> p = new ArrayList<>();
        p.add("P_i");
        for (int j = 0; j < classes.length; j++) {
            p.add(String.format(java.util.Locale.ROOT, "%.2f", precision(j)));
        }
        p.add("");
        lignes.add(p);
        return Affichage.tableau(entete, lignes)
                + "T_reco = " + Affichage.pct(tauxReconnaissance())
                + "   T_conf = " + Affichage.pct(tauxConfusion())
                + "   P_macro = " + Affichage.f4(precisionMacro())
                + "   R_macro = " + Affichage.f4(rappelMacro())
                + "   F1_macro = " + Affichage.f4(f1Macro()) + "\n";
    }

    /** @return l'en-tête CSV de la matrice. */
    public List<String> enteteCsv() {
        List<String> e = new ArrayList<>();
        e.add("verite\\estimee");
        for (int c : classes) {
            e.add(String.format("S%02d", c));
        }
        return e;
    }

    /** @return les lignes CSV de la matrice. */
    public List<List<Object>> lignesCsv() {
        List<List<Object>> r = new ArrayList<>();
        for (int i = 0; i < classes.length; i++) {
            List<Object> l = new ArrayList<>();
            l.add(String.format("S%02d", classes[i]));
            for (int j = 0; j < classes.length; j++) {
                l.add(m[i][j]);
            }
            r.add(l);
        }
        return r;
    }

    /** @return l'en-tête CSV des critères par classe. */
    public static List<String> enteteMetriques() {
        return List.of("classe", "VP", "FP", "FN", "precision", "rappel", "F1");
    }

    /** @return les critères par classe + la ligne de macro-moyenne + T_reco / T_conf. */
    public List<List<Object>> lignesMetriques() {
        List<List<Object>> r = new ArrayList<>();
        for (int i = 0; i < classes.length; i++) {
            r.add(List.of(String.format("S%02d", classes[i]), vraisPositifs(i), fauxPositifs(i),
                    fauxNegatifs(i), precision(i), rappel(i), f1(i)));
        }
        r.add(List.of("macro", "", "", "", precisionMacro(), rappelMacro(), f1Macro()));
        r.add(List.of("T_reco", "", "", "", tauxReconnaissance(), "", ""));
        r.add(List.of("T_conf", "", "", "", tauxConfusion(), "", ""));
        return r;
    }
}
