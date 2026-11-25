package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Ensemble d'échantillons (base de formes). Les échantillons sont triés par
 * (classe, numéro) afin que tous les traitements soient déterministes.
 */
public final class Dataset {

    /** Échantillons triés par classe puis par numéro. */
    private final List<Echantillon> echantillons;

    /**
     * @param echantillons liste des échantillons (copiée puis triée)
     */
    public Dataset(List<Echantillon> echantillons) {
        List<Echantillon> copie = new ArrayList<>(echantillons);
        copie.sort(Comparator.comparingInt(Echantillon::classe).thenComparingInt(Echantillon::numero));
        this.echantillons = Collections.unmodifiableList(copie);
    }

    /** @return le nombre total d'échantillons n. */
    public int taille() {
        return echantillons.size();
    }

    /** @return l'échantillon d'indice i (0 ≤ i &lt; n). */
    public Echantillon get(int i) {
        return echantillons.get(i);
    }

    /** @return la liste (non modifiable) des échantillons. */
    public List<Echantillon> echantillons() {
        return echantillons;
    }

    /**
     * Restreint la base aux {@code nb} premières classes (S01..Snb).
     *
     * @param nb nombre de classes à conserver (6, 12 ou 18 dans le projet)
     * @return une nouvelle base ne contenant que les classes 1..nb
     */
    public Dataset premieresClasses(int nb) {
        int[] cl = classes();
        if (nb > cl.length) {
            throw new IllegalArgumentException("Seulement " + cl.length + " classes disponibles");
        }
        int limite = cl[nb - 1];
        List<Echantillon> r = new ArrayList<>();
        for (Echantillon e : echantillons) {
            if (e.classe() <= limite) {
                r.add(e);
            }
        }
        return new Dataset(r);
    }

    /** @return les étiquettes de classe distinctes, triées par ordre croissant. */
    public int[] classes() {
        return echantillons.stream().mapToInt(Echantillon::classe).distinct().sorted().toArray();
    }

    /** @return le nombre de classes C. */
    public int nombreClasses() {
        return classes().length;
    }

    /** @return le vecteur des étiquettes (vérité terrain) aligné sur les indices des échantillons. */
    public int[] etiquettes() {
        return echantillons.stream().mapToInt(Echantillon::classe).toArray();
    }

    /**
     * @param classe une étiquette de classe
     * @return le nombre d'échantillons de cette classe (N_u, 12 pour SharvitB2)
     */
    public int effectifClasse(int classe) {
        return (int) echantillons.stream().filter(e -> e.classe() == classe).count();
    }

    /**
     * Extrait la sous-liste des échantillons d'indices donnés (ensemble d'apprentissage d'un pli).
     *
     * @param indices indices dans la base
     * @return la liste correspondante
     */
    public List<Echantillon> sousListe(int[] indices) {
        List<Echantillon> r = new ArrayList<>(indices.length);
        for (int i : indices) {
            r.add(echantillons.get(i));
        }
        return r;
    }

    /**
     * @param m méthode de description
     * @return la dimension d du vecteur caractéristique de la méthode m
     */
    public int dimension(Methode m) {
        return echantillons.isEmpty() ? 0 : echantillons.get(0).vecteur(m).length;
    }

    /**
     * @param m méthode de description
     * @return la matrice n × d des vecteurs de la méthode m (lignes partagées, non copiées)
     */
    public double[][] matrice(Methode m) {
        double[][] x = new double[taille()][];
        for (int i = 0; i < taille(); i++) {
            x[i] = echantillons.get(i).vecteur(m);
        }
        return x;
    }
}
