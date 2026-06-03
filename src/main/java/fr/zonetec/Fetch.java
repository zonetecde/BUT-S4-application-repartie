package fr.zonetec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Classe utilitaire permettant de récupérer des données JSON depuis une URL.
 */
public class Fetch implements ServiceFetch {
    /**
     * Fetch une URL avec le proxy de l'IUT 
     * et retourne la réponse dans un objet Reponse.
     *
     * @param url URL à fetch
     * @return réponse JSON
     */
    public Reponse fetch(String url) {
        try {
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10));

            // Met le proxy de l'IUT (http://www-cache:3128)
            clientBuilder.proxy(
                    ProxySelector.of(new InetSocketAddress("www-cache", 3128))
            );
            
            HttpClient client = clientBuilder.build();

            // Construis la requête HTTP GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // Récupère la réponse de la requête
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();
            String body = response.body();

            // Si le code de statut n'est pas dans les 200 c'est que c'est un echec
            if (statusCode < 200 || statusCode >= 300) {
                return new Reponse(false, "Erreur HTTP : " + statusCode, null);
            }

            // Retourne la réponse dans un objet Reponse
            return new Reponse(true, null, body);
        } catch (IllegalArgumentException e) {
            return new Reponse(false, "URL invalide : " + url, null);
        } catch (IOException e) {
            return new Reponse(false, "Erreur réseau : " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Reponse(false, "Requête interrompue.", null);
        } catch (Exception e) {
            return new Reponse(false, "Erreur inconnue : " + e.getMessage(), null);
        }
    }
}