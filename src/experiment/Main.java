package experiment;

import io.Affichage;
import io.CsvWriter;
import io.DatasetLoader;
import model.Dataset;
import model.Methode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Point d'entrée en ligne de commande du projet de Reconnaissance des Formes (SharvitB2).
 *
 * <pre>
 * Usage : java -cp out experiment.Main [options]
 *   --donnees DIR     dossier des signatures (défaut : exemples/=Signatures)
 *   --sortie DIR      dossier des résultats CSV (défaut : results)
 *   --graine N        graine aléatoire (défaut : 42)
 *   --experience X    tout | knn | nuees | vote | pr | echelle (défaut : tout)
 * </pre>
 *
 * <p>Toute la sortie console est également enregistrée dans {@code results/journal.txt}.</p>
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        Path donnees = dossierDonneesParDefaut();
        Path sortie = Paths.get("results");
        long graine = 42;
        String experience = "tout";
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--donnees" -> donnees = Paths.get(valeur(args, ++i, a));
                case "--sortie" -> sortie = Paths.get(valeur(args, ++i, a));
                case "--graine" -> graine = Long.parseLong(valeur(args, ++i, a));
                case "--experience" -> experience = valeur(args, ++i, a).toLowerCase(Locale.ROOT);
                case "-h", "--help", "--aide" -> {
                    usage();
                    return;
                }
                default -> {
                    System.err.println("Option inconnue : " + a);
                    usage();
                    System.exit(2);
                }
            }
        }

        // Duplication de la sortie console dans un journal.
        PrintStream console = System.out;
        ByteArrayOutputStream journal = new ByteArrayOutputStream();
        System.setOut(new PrintStream(new Tee(console, journal), true, StandardCharsets.UTF_8));
        long debut = System.nanoTime();
        try {
            CsvWriter csv = new CsvWriter(sortie);
            Dataset base = new DatasetLoader(donnees).charger();
            afficherBase(base, donnees);
            Contexte ctx = new Contexte(base, csv, graine);
            boolean tout = experience.equals("tout");
            boolean connue = false;
            if (tout || experience.equals("knn")) {
                ExperienceKnn.executer(ctx);
                connue = true;
            }
            if (tout || experience.equals("nuees")) {
                ExperienceNuees.executer(ctx);
                connue = true;
            }
            if (tout || experience.equals("vote")) {
                ExperienceVote.executer(ctx);
                connue = true;
            }
            if (tout || experience.equals("pr")) {
                ExperiencePrecisionRappel.executer(ctx);
                connue = true;
            }
            if (tout || experience.equals("echelle")) {
                ExperienceEchelle.executer(ctx);
                connue = true;
            }
            if (!connue) {
                System.err.println("Experience inconnue : " + experience);
                usage();
                System.exit(2);
            }
            System.out.println();
            System.out.println(String.format(Locale.ROOT, "Duree totale : %.1f s. Resultats CSV dans %s",
                    (System.nanoTime() - debut) / 1e9, sortie.toAbsolutePath().normalize()));
            System.out.flush();
            Files.write(sortie.resolve("journal.txt"), journal.toByteArray());
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
            System.exit(1);
        } finally {
            System.setOut(console);
        }
    }

    /** Affiche un résumé de la base chargée. */
    private static void afficherBase(Dataset base, Path donnees) {
        Affichage.titre("1.  Lecture des donnees : " + donnees.toAbsolutePath().normalize());
        System.out.println("Echantillons : " + base.taille() + "   classes : " + base.nombreClasses()
                + "   echantillons par classe : " + base.effectifClasse(base.classes()[0]));
        for (Methode m : Methode.values()) {
            System.out.println(String.format(Locale.ROOT, "  M^%-3s : dimension %3d  (%s)",
                    m, base.dimension(m), m.description()));
        }
    }

    /**
     * Dossier de données par défaut : le premier dossier existant parmi
     * {@code exemples/=Signatures}, {@code exemples} et {@code ../=SharvitB2/=Signatures}.
     */
    private static Path dossierDonneesParDefaut() {
        Path[] candidats = {
                Paths.get("exemples", "=Signatures"),
                Paths.get("exemples"),
                Paths.get("..", "=SharvitB2", "=Signatures")
        };
        for (Path p : candidats) {
            if (Files.isDirectory(p)) {
                return p;
            }
        }
        return candidats[0];
    }

    private static String valeur(String[] args, int i, String option) {
        if (i >= args.length) {
            System.err.println("Valeur manquante pour " + option);
            System.exit(2);
        }
        return args[i];
    }

    private static void usage() {
        System.out.println("""
                Usage : java -cp out experiment.Main [options]
                  --donnees DIR     dossier des signatures (defaut : exemples/=Signatures)
                  --sortie DIR      dossier des resultats CSV (defaut : results)
                  --graine N        graine aleatoire (defaut : 42)
                  --experience X    tout | knn | nuees | vote | pr | echelle (defaut : tout)
                """);
    }

    /** Flux de sortie qui recopie les octets vers deux destinations. */
    private static final class Tee extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        Tee(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int c) throws IOException {
            a.write(c);
            b.write(c);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }
    }
}
