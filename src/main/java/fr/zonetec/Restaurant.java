package fr.zonetec;

public class Restaurant implements ServiceRestaurant {
    /**
     * Récupère les coordonnées de tous les restaurants de Nancy enregistrés en base.
     *
     * @return réponse JSON
     */
    public String recupererCoordonneesRestaurantsNancy() {
        return "[]";
    }

    /**
     * Réserve une table dans un restaurant.
     *
     * @param nomRestaurant nom du restaurant
     * @param nom nom du client
     * @param prenom prénom du client
     * @param nombreConvives nombre de convives
     * @param telephone numéro de téléphone du client
     * @return réponse JSON
     */
    public String reserverTable(
            String nomRestaurant,
            String nom,
            String prenom,
            int nombreConvives,
            String telephone) {
        return "{}";
    }

    public Restaurant() {
    }
}
