package io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Écriture de tableaux de résultats au format CSV (séparateur virgule, point décimal),
 * directement exploitables pour tracer les figures du rapport (tableur, Python, gnuplot…).
 */
public final class CsvWriter {

    /** Dossier de sortie (créé si nécessaire). */
    private final Path dossier;

    /**
     * @param dossier dossier de sortie des fichiers CSV
     * @throws IOException si le dossier ne peut pas être créé
     */
    public CsvWriter(Path dossier) throws IOException {
        this.dossier = dossier;
        Files.createDirectories(dossier);
    }

    /** @return le dossier de sortie. */
    public Path dossier() {
        return dossier;
    }

    /**
     * Écrit un fichier CSV.
     *
     * @param nomFichier nom du fichier (ex. « knn_grille.csv »)
     * @param entete     noms des colonnes
     * @param lignes     lignes de valeurs (Double formatés avec 6 décimales)
     * @return le chemin du fichier écrit
     */
    public Path ecrire(String nomFichier, List<String> entete, List<? extends List<?>> lignes) {
        StringBuilder sb = new StringBuilder();
        sb.append(ligne(entete)).append('\n');
        for (List<?> l : lignes) {
            sb.append(ligne(l)).append('\n');
        }
        return ecrireTexte(nomFichier, sb.toString());
    }

    /**
     * Écrit un fichier texte brut dans le dossier de sortie.
     *
     * @param nomFichier nom du fichier
     * @param contenu    contenu du fichier
     * @return le chemin du fichier écrit
     */
    public Path ecrireTexte(String nomFichier, String contenu) {
        Path p = dossier.resolve(nomFichier);
        try {
            Files.writeString(p, contenu, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'ecrire " + p + " : " + e.getMessage(), e);
        }
        return p;
    }

    /** Assemble une ligne CSV à partir de valeurs quelconques. */
    private static String ligne(List<?> valeurs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valeurs.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(formater(valeurs.get(i)));
        }
        return sb.toString();
    }

    /**
     * Formate une valeur : réels avec 6 décimales (Locale.ROOT pour le point décimal),
     * chaînes protégées par des guillemets si elles contiennent un séparateur.
     */
    static String formater(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof Double || o instanceof Float) {
            return String.format(Locale.ROOT, "%.6f", ((Number) o).doubleValue());
        }
        String s = o.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
