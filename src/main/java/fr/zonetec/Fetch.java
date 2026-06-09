package fr.zonetec;

import java.io.IOException;

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
            String body = HttpClientUtils.fetchUrl(url);
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