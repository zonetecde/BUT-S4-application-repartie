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
     * @param args hôte RMI, port RMI et port HTTP optionnels
     * @throws IOException si le serveur HTTP ne peut pas démarrer
     * @throws NotBoundException si un service RMI n'est pas enregistré
     */
    public static void main(String[] args) throws IOException, NotBoundException {
        // IP et Port où est exposé le service RMI.
        // Par défaut on lance le proxy sur le même ordinateur que le service RMI,
        // donc localhost:1099
        String rmiHost = args.length > 0 ? args[0] : "localhost";
        int rmiPort = args.length > 1 ? Integer.parseInt(args[1]) : 1099;

        // On va exposer notre API sur le port 8081
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 8081;

        // Récupère l'annuaire RMI et les services
        Registry reg = LocateRegistry.getRegistry(rmiHost, rmiPort);
        ServiceRestaurant serviceRestaurant = (ServiceRestaurant) reg.lookup("restaurant");
        ServiceFetch serviceFetch = (ServiceFetch) reg.lookup("fetch");

        // Créer et configure le serveur HTTP
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Crée l'endpoint pour récupérer les coordonnées des restaurants
        server.createContext("/api/restaurants/coordonnees", exchange -> {
            // Vérifie que la méthode HTTP est GET
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            sendJson(exchange, 200, serviceRestaurant.recupererCoordonneesRestaurantsNancy().toJson());
        });

        server.createContext("/api/restaurants/reserver", exchange -> {
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
                    params.get("nom"),
                    params.get("prenom"),
                    nombreConvives,
                    params.get("telephone")
            );
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour récupérer les tables disponibles d'un restaurant
        server.createContext("/api/restaurants/tables", exchange -> {
            // Vérifie que la méthode HTTP est GET
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            Reponse response = serviceRestaurant.recupererTablesRestaurant(getQueryParams(exchange).get("nomRestaurant"));
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        // Crée l'endpoint pour fetch une URL avec le service RMI de Fetch
        server.createContext("/api/fetch", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, new Reponse(false, "Méthode non autorisée", null).toJson());
                return;
            }

            // Appel la méthode du service RMI et envoie la réponse au client
            Reponse response = serviceFetch.fetch(getQueryParams(exchange).get("url"));
            sendJson(exchange, response.isSuccess() ? 200 : 400, response.toJson());
        });

        server.start();

        System.out.println("Proxy HTTP lancé sur http://localhost:" + port);
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

        // Pour autoriser votre site web à appeler le proxy.
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
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
