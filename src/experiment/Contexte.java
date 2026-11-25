package experiment;

import classifier.ConfigKnn;
import io.CsvWriter;
import model.Dataset;
import model.Methode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Contexte partagé par les expériences : base complète, écriture des résultats, graine
 * aléatoire et configurations k-PPV retenues pour chaque méthode (sélectionnées une seule
 * fois sur les 6 premières classes puis réutilisées par le vote, les courbes P/R et
 * l'étude 6/12/18 classes).
 */
public final class Contexte {

    private final Dataset complet;
    private final CsvWriter csv;
    private final long graine;
    private EnumMap<Methode, ConfigKnn> meilleures;

    /**
     * @param complet base complète (18 classes)
     * @param csv     écrivain CSV du dossier de résultats
     * @param graine  graine aléatoire globale
     */
    public Contexte(Dataset complet, CsvWriter csv, long graine) {
        this.complet = complet;
        this.csv = csv;
        this.graine = graine;
    }

    /** @return la base complète. */
    public Dataset complet() {
        return complet;
    }

    /**
     * @param nb nombre de classes
     * @return la base restreinte aux nb premières classes
     */
    public Dataset classes(int nb) {
        return complet.premieresClasses(nb);
    }

    /** @return l'écrivain CSV. */
    public CsvWriter csv() {
        return csv;
    }

    /** @return la graine aléatoire. */
    public long graine() {
        return graine;
    }

    /**
     * Configurations k-PPV retenues par méthode ; calculées silencieusement (grille
     * leave-one-out sur 6 classes) si l'expérience k-PPV n'a pas encore été exécutée.
     *
     * @return configuration par méthode
     */
    public Map<Methode, ConfigKnn> meilleuresConfigs() {
        if (meilleures == null) {
            meilleures = new EnumMap<>(ExperienceKnn.selectionner(ExperienceKnn.grille(classes(6))));
        }
        return Collections.unmodifiableMap(meilleures);
    }

    /**
     * Enregistre les configurations retenues.
     *
     * @param configs configuration par méthode
     */
    void definirMeilleuresConfigs(Map<Methode, ConfigKnn> configs) {
        this.meilleures = new EnumMap<>(configs);
    }
}
