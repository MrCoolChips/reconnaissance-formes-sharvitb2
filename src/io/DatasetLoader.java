package io;

import model.Dataset;
import model.Echantillon;
import model.Methode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lecture de la base SharvitB2 (étape 1 du projet).
 *
 * <p>Le chargeur parcourt récursivement un dossier racine et reconnaît tous les fichiers
 * nommés selon la convention {@code SxxNyyy.MET} où :</p>
 * <ul>
 *   <li>{@code Sxx} est l'étiquette de classe (S01..S18) ;</li>
 *   <li>{@code Nyyy} est le numéro de l'échantillon (N001..N012) ;</li>
 *   <li>{@code MET} est l'extension de la méthode : ART, ZRK, GFD, YNG ou SA.</li>
 * </ul>
 * <p>La casse est ignorée et un suffixe {@code .txt} éventuel est toléré
 * (ex. {@code s01n001.zrk.txt}). Chaque fichier contient une suite de nombres réels
 * séparés par des blancs (un par ligne dans la base fournie) ; la dimension est lue
 * dynamiquement puis vérifiée pour être identique pour tous les échantillons d'une
 * même méthode.</p>
 */
public final class DatasetLoader {

    /** Motif des noms de fichiers de signatures, insensible à la casse. */
    private static final Pattern MOTIF = Pattern.compile(
            "^s(\\d+)n(\\d+)\\.([a-z]+)(?:\\.txt)?$", Pattern.CASE_INSENSITIVE);

    /** Dossier racine contenant (éventuellement dans des sous-dossiers) les fichiers de signatures. */
    private final Path racine;

    /**
     * @param racine dossier racine des signatures (ex. {@code =SharvitB2/=Signatures})
     */
    public DatasetLoader(Path racine) {
        this.racine = racine;
    }

    /**
     * Charge l'ensemble de la base en mémoire.
     *
     * @return la base complète, triée par (classe, numéro)
     * @throws IOException si un fichier est illisible, mal formé, ou si la base est incomplète
     */
    public Dataset charger() throws IOException {
        if (!Files.isDirectory(racine)) {
            throw new IOException("Dossier de donnees introuvable : " + racine.toAbsolutePath());
        }
        // Clé "classe:numero" -> vecteurs par méthode (TreeMap pour un ordre stable).
        Map<String, EnumMap<Methode, double[]>> parEchantillon = new TreeMap<>();
        Map<String, int[]> identifiants = new TreeMap<>();

        List<Path> fichiers;
        try (Stream<Path> s = Files.walk(racine)) {
            fichiers = s.filter(Files::isRegularFile).toList();
        }
        for (Path f : fichiers) {
            Matcher mt = MOTIF.matcher(f.getFileName().toString());
            if (!mt.matches()) {
                continue; // fichiers parasites (.DS_Store, images pgm, ...) ignorés
            }
            int classe = Integer.parseInt(mt.group(1));
            int numero = Integer.parseInt(mt.group(2));
            Optional<Methode> m = Methode.depuisExtension(mt.group(3));
            if (m.isEmpty()) {
                // Tolérance aux fautes de frappe de la base (ex. « s05n011.zrkt.txt ») :
                // on accepte une extension qui commence par une extension connue.
                m = parPrefixe(mt.group(3));
                if (m.isEmpty()) {
                    continue;
                }
                System.out.println("[Avertissement] extension non standard '" + mt.group(3) + "' interpretee comme "
                        + m.get() + " : " + f.getFileName());
            }
            String cle = String.format("%02d:%03d", classe, numero);
            EnumMap<Methode, double[]> vecteurs =
                    parEchantillon.computeIfAbsent(cle, k -> new EnumMap<>(Methode.class));
            identifiants.put(cle, new int[]{classe, numero});
            if (vecteurs.put(m.get(), lireVecteur(f)) != null) {
                throw new IOException("Fichier en double pour " + cle + " / " + m.get() + " : " + f);
            }
        }
        if (parEchantillon.isEmpty()) {
            throw new IOException("Aucun fichier de signature trouve sous " + racine.toAbsolutePath());
        }

        // Vérification de complétude et de cohérence des dimensions.
        EnumMap<Methode, Integer> dimensions = new EnumMap<>(Methode.class);
        List<Echantillon> echantillons = new ArrayList<>();
        for (Map.Entry<String, EnumMap<Methode, double[]>> e : parEchantillon.entrySet()) {
            EnumMap<Methode, double[]> v = e.getValue();
            for (Methode m : Methode.values()) {
                double[] x = v.get(m);
                if (x == null) {
                    throw new IOException("Methode " + m + " manquante pour l'echantillon " + e.getKey());
                }
                Integer d = dimensions.putIfAbsent(m, x.length);
                if (d != null && d != x.length) {
                    throw new IOException("Dimension incoherente pour " + m + " (" + e.getKey()
                            + ") : " + x.length + " au lieu de " + d);
                }
            }
            int[] id = identifiants.get(e.getKey());
            echantillons.add(new Echantillon(id[0], id[1], v));
        }
        return new Dataset(echantillons);
    }

    /**
     * Recherche l'unique méthode dont l'extension est un préfixe de l'extension donnée.
     *
     * @param ext extension lue dans le nom de fichier
     * @return la méthode si elle est unique, sinon vide
     */
    private static Optional<Methode> parPrefixe(String ext) {
        String e = ext.toLowerCase(java.util.Locale.ROOT);
        Methode trouvee = null;
        for (Methode m : Methode.values()) {
            if (e.startsWith(m.extension())) {
                if (trouvee != null) {
                    return Optional.empty();
                }
                trouvee = m;
            }
        }
        return Optional.ofNullable(trouvee);
    }

    /**
     * Lit un vecteur de réels dans un fichier texte (valeurs séparées par des blancs,
     * la virgule décimale est acceptée).
     *
     * @param fichier fichier de signature
     * @return le vecteur lu
     * @throws IOException si le fichier est vide ou contient une valeur non numérique
     */
    static double[] lireVecteur(Path fichier) throws IOException {
        String contenu = Files.readString(fichier, StandardCharsets.ISO_8859_1).trim();
        if (contenu.isEmpty()) {
            throw new IOException("Fichier vide : " + fichier);
        }
        String[] jetons = contenu.split("\\s+");
        double[] v = new double[jetons.length];
        for (int i = 0; i < jetons.length; i++) {
            try {
                v[i] = Double.parseDouble(jetons[i].replace(',', '.'));
            } catch (NumberFormatException ex) {
                throw new IOException("Valeur non numerique '" + jetons[i] + "' dans " + fichier, ex);
            }
            if (!Double.isFinite(v[i])) {
                throw new IOException("Valeur non finie dans " + fichier);
            }
        }
        return v;
    }
}
