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
 * Utilitaire partagé pour effectuer des requêtes HTTP GET.
 */
public class HttpClientUtils {
    // Défini le client que toutes les requêtes utiliseront
    // possède le proxy de l'IUT
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .proxy(ProxySelector.of(new InetSocketAddress("www-cache", 3128)))
            .build();

    /**
     * Effectue une requête HTTP GET et retourne le corps de la réponse,
     * SANS passer par le proxy de l'IUT (pour les URLs locales).
     *
     * @param url URL à appeler
     * @return corps de la réponse en String
     * @throws IOException si une erreur HTTP ou réseau survient
     * @throws InterruptedException si la requête est interrompue
     */
    public static String fetchUrlLocal(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Erreur HTTP " + statusCode + " pour l'URL : " + url);
        }

        return response.body();
    }

    /*
     * Effectue une requête HTTP GET et retourne le corps de la réponse.
     *
     * @param url URL à appeler
     * @return corps de la réponse en String
     * @throws IOException si une erreur HTTP ou réseau survient
     * @throws InterruptedException si la requête est interrompue
     */
    public static String fetchUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Vérifie que le code de statut HTTP est good
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Erreur HTTP " + statusCode + " pour l'URL : " + url);
        }

        return response.body();
    }
}
