package fr.zonetec;

import java.io.IOException;

/**
 * Classe utilitaire permettant de récupérer des données JSON depuis une URL.
 */
public class Fetch implements ServiceFetch {
    /**
     * Charge la documentation HTML du service Fetch.
     *
     * @return documentation HTML du service
     */
    public String chargerDocumentation() {
        return """
                <section>
                    <h2>Service Fetch</h2>
                    <p>
                        Le service Fetch sert à récupérer le contenu d'une URL pour les autres parties de l'application.
                        Le navigateur ne va pas toujours appeler directement les API externes. Il passe par le proxy HTTP,
                        puis le proxy demande au service Fetch de faire la requête. Le service Fetch utilise lui-même le proxy de l'IUT pour faire la requête, ce qui lui permet d'accéder à des ressources externes que le navigateur ne peut pas atteindre directement à cause des règles de sécurité CORS.
                    </p>
                    <p>
                        Concrètement, le proxy envoie une URL au service Fetch. Le service vérifie l'URL, fait la requête
                        avec la méthode HTTP partagée du projet, récupère le texte de la réponse, puis le renvoie dans un objet Reponse.
                        Si l'URL est mauvaise, si le réseau ne répond pas, ou si la requête est interrompue, le service renvoie
                        une erreur simple au lieu de faire planter tout le programme.
                    </p>
                    <p>
                        Ce service est utile car il centralise les appels externes. Les autres services n'ont pas besoin de
                        connaître les détails du client HTTP ou du proxy de l'IUT.
                    </p>
                    <img src="static/schema-service-fetch.png" alt="Schéma du service Fetch" />
                </section>
                """;
    }

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
