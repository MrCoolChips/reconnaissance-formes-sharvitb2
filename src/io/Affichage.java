package io;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utilitaires d'affichage console : titres de sections et tableaux alignés en colonnes.
 */
public final class Affichage {

    private Affichage() {
    }

    /**
     * Affiche un titre de section encadré.
     *
     * @param titre texte du titre
     */
    public static void titre(String titre) {
        String barre = "=".repeat(Math.max(20, titre.length() + 4));
        System.out.println();
        System.out.println(barre);
        System.out.println("  " + titre);
        System.out.println(barre);
    }

    /**
     * Affiche un sous-titre.
     *
     * @param texte texte du sous-titre
     */
    public static void sousTitre(String texte) {
        System.out.println();
        System.out.println("--- " + texte + " ---");
    }

    /**
     * Formate un taux dans [0,1] en pourcentage avec deux décimales.
     *
     * @param taux valeur dans [0,1]
     * @return ex. « 93.06% »
     */
    public static String pct(double taux) {
        return String.format(Locale.ROOT, "%.2f%%", 100.0 * taux);
    }

    /**
     * Formate un réel avec 4 décimales.
     *
     * @param v valeur
     * @return chaîne formatée
     */
    public static String f4(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    /**
     * Construit un tableau texte aligné (colonne 0 à gauche, autres colonnes à droite).
     *
     * @param entete noms des colonnes
     * @param lignes lignes de cellules
     * @return le tableau sous forme de texte
     */
    public static String tableau(List<String> entete, List<List<String>> lignes) {
        int nc = entete.size();
        int[] largeur = new int[nc];
        List<List<String>> toutes = new ArrayList<>();
        toutes.add(entete);
        toutes.addAll(lignes);
        for (List<String> l : toutes) {
            for (int j = 0; j < nc && j < l.size(); j++) {
                largeur[j] = Math.max(largeur[j], l.get(j).length());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < toutes.size(); r++) {
            List<String> l = toutes.get(r);
            for (int j = 0; j < nc; j++) {
                String c = j < l.size() ? l.get(j) : "";
                String fmt = (j == 0) ? "%-" + largeur[j] + "s" : "%" + largeur[j] + "s";
                sb.append(String.format(fmt, c));
                sb.append(j + 1 < nc ? " | " : "");
            }
            sb.append('\n');
            if (r == 0) {
                for (int j = 0; j < nc; j++) {
                    sb.append("-".repeat(largeur[j])).append(j + 1 < nc ? "-+-" : "");
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Affiche directement un tableau aligné.
     *
     * @param entete noms des colonnes
     * @param lignes lignes de cellules
     */
    public static void afficherTableau(List<String> entete, List<List<String>> lignes) {
        System.out.print(tableau(entete, lignes));
    }
}
