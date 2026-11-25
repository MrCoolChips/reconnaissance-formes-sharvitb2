package classifier;

import distance.Distance;
import model.Methode;

/**
 * Jeu de paramètres d'un classifieur k-PPV.
 *
 * @param k             nombre de voisins
 * @param distance      fonction de distance (L_1, L_2, …)
 * @param normalisation normalisation des composantes estimée sur l'apprentissage
 * @param regle         règle de départage des égalités de votes
 */
public record ConfigKnn(int k, Distance distance, TypeNormalisation normalisation, RegleEgalite regle) {

    public ConfigKnn {
        if (k < 1) {
            throw new IllegalArgumentException("k doit etre >= 1");
        }
    }

    /**
     * @param m méthode de description sur laquelle travaille le classifieur
     * @return un nouveau classifieur k-PPV (non entraîné) utilisant cette configuration
     */
    public KPlusProchesVoisins creer(Methode m) {
        return new KPlusProchesVoisins(m, this);
    }

    /** @return une description courte, ex. « k=3,L2,ZSCORE ». */
    public String description() {
        return "k=" + k + "," + distance.nom() + "," + normalisation;
    }

    @Override
    public String toString() {
        return description();
    }
}
