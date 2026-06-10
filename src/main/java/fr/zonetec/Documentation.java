package fr.zonetec;

import java.io.IOException;

public class Documentation implements ServiceDocumentation {
    private final String proxyUrl;

    /**
     * Crée un client de documentation.
     *
     * @param proxyUrl URL du proxy HTTP
     */
    public Documentation(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    /**
     * Retourne la documentation générale du système.
     */
    @Override
    public String chargerDocumentation() {
        System.out.println("[LOG] Appel de chargerDocumentation()");
        return """
                <section>
                    <h2 class="text-xl font-bold mb-3">Service Documentation</h2>
                    <p class="text-sm mb-2">
                        Le service Documentation est le <strong>point d'entrée centralisé</strong> pour consulter la
                        documentation de tous les autres services de l'application. Il joue le rôle d'intermédiaire
                        entre le navigateur et les services RMI.
                    </p>
                    <p class="text-sm mb-2">
                        Quand l'utilisateur clique sur un bouton dans le panneau de documentation, le navigateur
                        appelle le proxy HTTP sur <code>/api/documentation/{service}</code>. Le proxy transmet alors
                        la demande au service Documentation via RMI avec le nom du service demandé.
                    </p>
                    <p class="text-sm mb-2">
                        Le service Documentation reçoit ce nom et fait à son tour une requête HTTP vers le proxy
                        sur l'endpoint interne <code>/api/services/documentation/{service}</code>. Cet endpoint
                        appelle directement la méthode <code>chargerDocumentation()</code> du service RMI concerné
                        (Restaurant, CROUS, Fetch ou PointGeo), qui retourne sa propre documentation HTML.
                    </p>
                    <p class="text-sm mb-2">
                        Pour sa propre documentation (celle que vous lisez), le service ne fait pas d'appel HTTP :
                        il retourne directement ce texte, évitant ainsi une boucle infinie.
                    </p>
                </section>
                """;
    }

    /**
     * Charge la documentation d'un service donné via le proxy HTTP.
     *
     * @param service nom du service (restaurant, crous, fetch, pointgeo)
     * @return documentation HTML du service demandé
     */
    @Override
    public Reponse chargerDocumentation(String service) {
        System.out.println("[LOG] Appel de chargerDocumentation() avec les paramètres : service=" + service);
        try {
            String html;
            switch (service.toLowerCase()) {
                case "restaurant":
                    html = chargerDocumentationRestaurant();
                    break;
                case "crous":
                    html = chargerDocumentationCrous();
                    break;
                case "fetch":
                    html = chargerDocumentationFetch();
                    break;
                case "pointgeo":
                    html = chargerDocumentationPointGeo();
                    break;
                case "documentation":
                    html = chargerDocumentation();
                    break;
                default:
                    html = null;
                    break;
            }

            if (html == null) {
                return new Reponse(false, "Service inconnu : " + service, null);
            }

            return new Reponse(true, "Documentation chargee", html);
        } catch (IOException e) {
            return new Reponse(false, "Erreur reseau : " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Reponse(false, "Requete interrompue.", null);
        }
    }

    public String chargerDocumentationRestaurant() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrlLocal(proxyUrl + "/api/services/documentation/restaurant");
    }

    /**
     * Charge la documentation du service CROUS via le proxy HTTP.
     *
     * @return documentation HTML du service CROUS
     * @throws IOException si l'appel HTTP échoue
     * @throws InterruptedException si l'appel HTTP est interrompu
     */
    public String chargerDocumentationCrous() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrlLocal(proxyUrl + "/api/services/documentation/crous");
    }

    /**
     * Charge la documentation du service Fetch via le proxy HTTP.
     *
     * @return documentation HTML du service Fetch
     * @throws IOException si l'appel HTTP échoue
     * @throws InterruptedException si l'appel HTTP est interrompu
     */
    public String chargerDocumentationFetch() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrlLocal(proxyUrl + "/api/services/documentation/fetch");
    }

    /**
     * Charge la documentation du service PointGeo via le proxy HTTP.
     *
     * @return documentation HTML du service PointGeo
     * @throws IOException si l'appel HTTP échoue
     * @throws InterruptedException si l'appel HTTP est interrompu
     */
    public String chargerDocumentationPointGeo() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrlLocal(proxyUrl + "/api/services/documentation/pointgeo");
    }
}
