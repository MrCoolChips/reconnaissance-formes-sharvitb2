package classifier;

import model.Echantillon;

import java.util.List;

/**
 * Classifieur supervisé : apprentissage sur un ensemble étiqueté puis prédiction.
 * Une instance est entraînée une fois par pli du protocole de validation.
 */
public interface Classifieur {

    /**
     * Apprentissage à partir d'échantillons étiquetés.
     *
     * @param apprentissage ensemble d'apprentissage D = {x_1, ..., x_n}
     */
    void entrainer(List<Echantillon> apprentissage);

    /**
     * Classe un échantillon de test.
     *
     * @param x échantillon à classer (son étiquette n'est jamais lue)
     * @return la prédiction (classe estimée + scores de confiance)
     */
    Prediction predire(Echantillon x);

    /** @return un nom descriptif du classifieur et de ses paramètres. */
    String nom();
}
