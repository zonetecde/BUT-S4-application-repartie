package fr.zonetec;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe permettant de lancer le Proxy HTTP qui va exposer une API pour récupérer les différentes données du service RMI.
 */
public class ProxyServeur {
    /**
     * Lance le proxy HTTP et récupère les services RMI.
     *
     * @param args args[0]=port HTTP,
     *             args[1]=hôte RMI Restaurant, args[2]=port RMI Restaurant,
     *             args[3]=hôte RMI Crous,     args[4]=port RMI Crous,
     *             args[5]=hôte RMI Fetch,     args[6]=port RMI Fetch,
     *             args[7]=hôte RMI PointGeo,  args[8]=port RMI PointGeo
     * @throws IOException si le serveur HTTP ne peut pas démarrer
     * @throws NotBoundException si un service RMI n'est pas enregistré
     */
    public static void main(String[] args) throws IOException, NotBoundException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;

        // IP et Port où est exposé le service RMI Restaurant.
        // Par défaut on lance le proxy sur le même ordinateur que le service RMI,
        // donc localhost:1099
        String restaurantHost = args.length > 1 ? args[1] : "localhost";
        int restaurantPort = args.length > 2 ? Integer.parseInt(args[2]) : 1099;

        // IP et Port où est exposé le service RMI Crous
        String crousHost = args.length > 3 ? args[3] : "localhost";
        int crousPort = args.length > 4 ? Integer.parseInt(args[4]) : 1099;

        // IP et Port où est exposé le service RMI Fetch
        String fetchHost = args.length > 5 ? args[5] : "localhost";
        int fetchPort = args.length > 6 ? Integer.parseInt(args[6]) : 1099;

        // IP et Port où est exposé le service RMI PointGeo
        String pointGeoHost = args.length > 7 ? args[7] : "localhost";
        int pointGeoPort = args.length > 8 ? Integer.parseInt(args[8]) : 1099;

        // Récupère l'annuaire RMI et le service Restaurant
        Registry regRestaurant = LocateRegistry.getRegistry(restaurantHost, restaurantPort);
        ServiceRestaurant serviceRestaurant = (ServiceRestaurant) regRestaurant.lookup("restaurant");

        // Récupère l'annuaire RMI du service Crous
        Registry regCrous = LocateRegistry.getRegistry(crousHost, crousPort);
        ServiceCrous serviceCrous = (ServiceCrous) regCrous.lookup("crous");

        // Récupère l'annuaire RMI du service Fetch
        Registry regFetch = LocateRegistry.getRegistry(fetchHost, fetchPort);
        ServiceFetch serviceFetch = (ServiceFetch) regFetch.lookup("fetch");

        // Récupère l'annuaire RMI du service PointGeo
        Registry regPointGeo = LocateRegistry.getRegistry(pointGeoHost, pointGeoPort);
        ServicePoint servicePointGeo = (ServicePoint) regPointGeo.lookup("pointgeo");

        // Créer et configure le serveur HTTP
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Crée l'endpoint pour récupérer les coordonnées des restaurants
        server.createContext("/api/restaurants/coordonnees", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            // Vérifie que la méthode HTTP est GET
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            sendJson(exchange, 200, serviceRestaurant.recupererCoordonneesRestaurantsNancy().toJson());
        });

        server.createContext("/api/restaurants/reserver", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            // Vérifie que la méthode HTTP est POST
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Vérifie les paramètres de la requête
            Map<String, String> params = getQueryParams(exchange);
            if (!params.containsKey("idTable")) {
                sendJson(exchange, 400, new Reponse(false, "Paramètre idTable manquant", null).toJson());
                return;
            }

            if (!params.containsKey("nombreConvives")) {
                sendJson(exchange, 400, new Reponse(false, "Paramètre nombreConvives manquant", null).toJson());
                return;
            }

            if (!params.containsKey("dateHeure")) {
                sendJson(exchange, 400, new Reponse(false, "Paramètre dateHeure manquant", null).toJson());
                return;
            }

            int idTable;
            int nombreConvives;
            try {
                idTable = Integer.parseInt(params.get("idTable"));
                nombreConvives = Integer.parseInt(params.get("nombreConvives"));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, new Reponse(false, "Paramètre idTable ou nombreConvives invalide", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            Reponse response = serviceRestaurant.reserverTable(
                    params.get("nomRestaurant"),
                    idTable,
                    params.get("dateHeure"),
                    params.get("nom"),
                    params.get("prenom"),
                    nombreConvives,
                    params.get("telephone")
            );
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour récupérer les tables disponibles d'un restaurant
        server.createContext("/api/restaurants/tables", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            // Vérifie que la méthode HTTP est GET
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            Map<String, String> params = getQueryParams(exchange);
            Reponse response = serviceRestaurant.recupererTablesRestaurant(params.get("nomRestaurant"));
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour libérer les tables d'un restaurant
        server.createContext("/api/restaurants/tables/liberer", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            Reponse response = serviceRestaurant.libererTablesRestaurant(getQueryParams(exchange).get("nomRestaurant"));
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour récupérer les réservations d'un restaurant
        server.createContext("/api/restaurants/reservations", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405,
                        new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            Reponse response = serviceRestaurant.recupererReservations();

            sendJson(
                    exchange,
                    response.isSuccess() ? 200 : 400,
                    response.toJson()
            );
        });

        // Crée l'endpoint pour fetch une URL avec le service RMI de Fetch
        server.createContext("/api/fetch", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            Reponse response = serviceFetch.fetch(getQueryParams(exchange).get("url"));
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour récupérer les restaurants du CROUS d'une ville
        server.createContext("/api/crous/restaurants", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI Crous
            Reponse response = serviceCrous.recupererRestaurants(getQueryParams(exchange).get("ville"));
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour récupérer le menu d'un restaurant CROUS
        server.createContext("/api/crous/menu", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Vérifie que l'id du restaurant est présent
            Map<String, String> params = getQueryParams(exchange);
            if (!params.containsKey("idRestaurant")) {
                sendJson(exchange, 400, new Reponse(false, "Paramètre idRestaurant manquant", null).toJson());
                return;
            }

            int idRestaurant;
            try {
                idRestaurant = Integer.parseInt(params.get("idRestaurant"));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, new Reponse(false, "Paramètre idRestaurant invalide", null).toJson());
                return;
            }

            // Appel la méthode du service RMI Crous
            Reponse response = serviceCrous.chargerMenu(idRestaurant);
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour récupérer tous les points géographiques
        server.createContext("/api/points/list", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            try {
                sendJson(exchange, 200, new Reponse(true, "Points récupérés", servicePointGeo.recupererTousLesPoints()).toJson());
            } catch (Exception e) {
                sendJson(exchange, 500, new Reponse(false, "Erreur : " + e.getMessage(), null).toJson());
            }
        });

        // Crée l'endpoint pour ajouter un point géographique
        server.createContext("/api/points/add", exchange -> {
            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            Map<String, String> params = getQueryParams(exchange);
            double x = Double.parseDouble(params.getOrDefault("x", "0"));
            double y = Double.parseDouble(params.getOrDefault("y", "0"));
            String emoji = params.getOrDefault("emoji", "📍");
            String titre = params.getOrDefault("titre", "");
            String description = params.getOrDefault("description", "");

            try {
                Point p = servicePointGeo.ajouterPoint(x, y, emoji, titre, description);
                sendJson(exchange, p != null ? 200 : 400,
                        new Reponse(p != null, p != null ? "Point ajouté" : "Erreur lors de l'ajout", p).toJson());
            } catch (Exception e) {
                sendJson(exchange, 500, new Reponse(false, "Erreur : " + e.getMessage(), null).toJson());
            }
        });

        server.start();

        System.out.println("Proxy HTTP lancé sur http://localhost:" + port);
    }

    /**
     * Répond aux requêtes CORS preflight.
     *
     * @param exchange échange HTTP courant
     * @return true si la requête a été traitée
     * @throws IOException si l'écriture de la réponse échoue
     */
    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            return false;
        }

        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return true;
    }

    /**
     * Envoie une réponse JSON HTTP.
     *
     * @param exchange échange HTTP courant
     * @param statusCode code de statut HTTP
     * @param json contenu JSON à renvoyer
     * @throws IOException si l'écriture de la réponse échoue
     */
    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(exchange);

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Ajoute les headers CORS communs aux réponses du proxy.
     *
     * @param exchange échange HTTP courant
     */
    private static void addCorsHeaders(HttpExchange exchange) {
        // Pour autoriser votre site web à appeler le proxy.
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * Récupère les paramètres de query string.
     *
     * @param exchange échange HTTP courant
     * @return paramètres de query string décodés
     */
    private static Map<String, String> getQueryParams(HttpExchange exchange) {
        // On stock les paramètres dans un dictionnaire : paramètre => valeur
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return params;
        }

        // On split la query string en paramètres individuels
        for (String param : query.split("&")) {
            // On split chaque paramètre en clé et valeur (séparés par un =)
            String[] parts = param.split("=", 2); // limit à 2 au cas où la valeur contient un =
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8); // on a besoin de décoder les paramètres car ils sont encodés dans l'URL
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }

        return params;
    }
}
