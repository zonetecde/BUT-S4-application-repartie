package fr.zonetec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class Crous implements ServiceCrous {
    public Crous() {
    }

    /**
     * Charge la documentation HTML du service CROUS.
     *
     * @return documentation HTML du service
     */
    public String chargerDocumentation() {
        return """
                <section>
                    <h2>Service CROUS</h2>
                    <p>
                        Le service CROUS sert à afficher les restaurants universitaires et leurs menus. Le site demande
                        d'abord les restaurants pour une ville, par exemple Nancy. Le service interroge l'API CROUS,
                        cherche la région qui correspond à cette ville, puis récupère les restaurants de cette région.
                    </p>
                    <p>
                        Quand l'utilisateur clique sur un restaurant CROUS, le proxy demande ensuite au service de charger
                        le menu. La réponse de l'API contient beaucoup d'objets imbriqués. Le service transforme donc ces
                        données en un texte simple, avec la date et les plats classés par repas et par catégorie.
                    </p>
                    <p>
                        Le service CROUS est donc un traducteur entre l'API externe et notre application. Il cache les détails
                        compliqués de l'API et renvoie des données plus faciles à afficher dans le navigateur.
                    </p>
                    <img src="static/schema-service-crous.png" alt="Schéma du service CROUS" />
                </section>
                """;
    }

    /**
     * Récupère la liste des restaurants du CROUS pour une ville donnée.
     *
     * @param ville nom de la ville à rechercher (ex: "Nancy")
     * @return réponse JSON contenant la liste des restaurants
     */
    public Reponse recupererRestaurants(String ville) {
        try {
            // 1. On récupère toutes les régions
            String regionsJson = HttpClientUtils.fetchUrl("https://api.croustillant.menu/v1/regions");
            JSONObject regionsResponse = new JSONObject(regionsJson);
            JSONArray regions = regionsResponse.getJSONArray("data");

            // 2. On cherche la région qui contient le nom de la ville
            int codeRegion = -1;
            for (int i = 0; i < regions.length(); i++) {
                // la réponse est de la forme { data: [ { code: 1, libelle: "Metz/Nancy" }, ... ] }
                JSONObject region = regions.getJSONObject(i);
                if (region.getString("libelle").toLowerCase().contains(ville.toLowerCase())) {
                    // Une fois trouvé on recup le code et on break
                    codeRegion = region.getInt("code");
                    break;
                }
            }

            if (codeRegion == -1) {
                return new Reponse(false, "Région non trouvée pour la ville : " + ville, null);
            }

            // 3. On récupère les restaurants de la région
            String restosJson = HttpClientUtils.fetchUrl("https://api.croustillant.menu/v1/regions/" + codeRegion + "/restaurants");
            JSONObject restosResponse = new JSONObject(restosJson);
            JSONArray restosData = restosResponse.getJSONArray("data");

            // 4. On transforme en liste pour le front
            ArrayList<HashMap<String, Object>> restaurants = new ArrayList<>();
            for (int i = 0; i < restosData.length(); i++) {
                JSONObject resto = restosData.getJSONObject(i);
                HashMap<String, Object> r = new HashMap<>();
                r.put("code", resto.getInt("code"));
                r.put("nom", resto.getString("nom"));
                r.put("adresse", resto.getString("adresse"));
                r.put("latitude", resto.getDouble("latitude"));
                r.put("longitude", resto.getDouble("longitude"));
                r.put("horaires", resto.optString("horaires", ""));
                restaurants.add(r);
            }

            // On retourne la liste des restaurants
            return new Reponse(true, "Liste des restaurants récupérée", restaurants);
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors de la récupération des restaurants : " + e.getMessage(), null);
        }
    }

    /**
     * Récupère le menu du jour d'un restaurant du CROUS.
     *
     * @param idRestaurant code du restaurant CROUS
     * @return réponse JSON contenant le menu
     */
    public Reponse chargerMenu(int idRestaurant) {
        try {
            String menuJson = HttpClientUtils.fetchUrl("https://api.croustillant.menu/v1/restaurants/" + idRestaurant + "/menu");
            JSONObject menuResponse = new JSONObject(menuJson);
            JSONArray jours = menuResponse.getJSONArray("data");

            if (jours.length() == 0) {
                return new Reponse(false, "Aucun menu disponible", null);
            }

            // La réponse de l'api est dans plein d'objet imbriqué
            // du coup au lieu d'envoyer ça au front on formatte ça 
            // ici dans un grand string direct à afficher
            JSONObject jour = jours.getJSONObject(0);
            String date = jour.getString("date");
            JSONArray repas = jour.getJSONArray("repas");

            // On construit le menu sous forme de string lisible
            StringBuilder menuStr = new StringBuilder();
            for (int i = 0; i < repas.length(); i++) {
                JSONObject r = repas.getJSONObject(i);

                // Récup le type du repas
                String typeRepas = r.getString("type").equals("matin") ? "Matin" : "Soir";
                menuStr.append(typeRepas).append("\n");

                // Récup les catégories du repas
                JSONArray categories = r.getJSONArray("categories");
                for (int j = 0; j < categories.length(); j++) {
                    JSONObject cat = categories.getJSONObject(j);
                    menuStr.append("  ").append(cat.getString("libelle")).append(" :\n");

                    // Récup les plats de la catégorie
                    JSONArray plats = cat.getJSONArray("plats");
                    for (int k = 0; k < plats.length(); k++) {
                        JSONObject plat = plats.getJSONObject(k);
                        menuStr.append("    - ").append(plat.getString("libelle")).append("\n");
                    }
                }
            }

            // On retourne un objet avec la date et le menu formaté
            HashMap<String, String> result = new HashMap<>();
            result.put("date", date);
            result.put("menu", menuStr.toString());

            return new Reponse(true, "Menu récupéré", result);
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors du chargement du menu : " + e.getMessage(), null);
        }
    }
}
