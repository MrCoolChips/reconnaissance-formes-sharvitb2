package model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Un échantillon (une forme) de la base SharvitB2 : son étiquette de classe (1..18),
 * son numéro dans la classe (1..12) et ses cinq vecteurs caractéristiques, un par
 * méthode de description {@link Methode}.
 *
 * <p>La classe est immuable : les vecteurs sont copiés à la construction et ne sont
 * jamais modifiés par la suite (les classifieurs travaillent sur des copies normalisées).</p>
 */
public final class Echantillon {

    /** Étiquette de classe (vérité terrain), Sxx → xx. */
    private final int classe;
    /** Numéro de l'échantillon dans sa classe, Nyyy → yyy. */
    private final int numero;
    /** Vecteurs caractéristiques indexés par méthode. */
    private final Map<Methode, double[]> vecteurs;

    /**
     * @param classe   étiquette de classe
     * @param numero   numéro de l'échantillon dans la classe
     * @param vecteurs vecteurs caractéristiques par méthode (copiés)
     */
    public Echantillon(int classe, int numero, Map<Methode, double[]> vecteurs) {
        this.classe = classe;
        this.numero = numero;
        EnumMap<Methode, double[]> copie = new EnumMap<>(Methode.class);
        vecteurs.forEach((m, v) -> copie.put(m, v.clone()));
        this.vecteurs = Collections.unmodifiableMap(copie);
    }

    /** @return l'étiquette de classe (vérité terrain). */
    public int classe() {
        return classe;
    }

    /** @return le numéro de l'échantillon dans sa classe. */
    public int numero() {
        return numero;
    }

    /**
     * Retourne le vecteur caractéristique d'une méthode. Le tableau retourné ne doit
     * pas être modifié par l'appelant (il est partagé pour éviter des copies inutiles).
     *
     * @param m méthode de description
     * @return le vecteur x ∈ R^d de la méthode m
     */
    public double[] vecteur(Methode m) {
        double[] v = vecteurs.get(m);
        if (v == null) {
            throw new IllegalStateException("Methode " + m + " absente pour " + identifiant());
        }
        return v;
    }

    /**
     * Concatène les vecteurs des méthodes demandées (dans l'ordre fourni) en un seul
     * vecteur ; utilisé par exemple pour un k-means sur l'ensemble des descripteurs.
     *
     * @param methodes méthodes à concaténer
     * @return le vecteur concaténé
     */
    public double[] concatenation(Methode... methodes) {
        int d = 0;
        for (Methode m : methodes) {
            d += vecteur(m).length;
        }
        double[] r = new double[d];
        int pos = 0;
        for (Methode m : methodes) {
            double[] v = vecteur(m);
            System.arraycopy(v, 0, r, pos, v.length);
            pos += v.length;
        }
        return r;
    }

    /** @return l'identifiant au format de la base, ex. « S01N007 ». */
    public String identifiant() {
        return String.format("S%02dN%03d", classe, numero);
    }

    @Override
    public String toString() {
        return identifiant();
    }
}
