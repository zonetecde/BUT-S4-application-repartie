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
        return """
                <section>
                    <h2>Documentation du système</h2>
                    <p>
                        Ce système est une application répartie utilisant RMI pour connecter
                        plusieurs services : Restaurant, CROUS, Fetch et PointGeo.
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
        return HttpClientUtils.fetchUrl(proxyUrl + "/api/services/documentation/restaurant");
    }

    /**
     * Charge la documentation du service CROUS via le proxy HTTP.
     *
     * @return documentation HTML du service CROUS
     * @throws IOException si l'appel HTTP échoue
     * @throws InterruptedException si l'appel HTTP est interrompu
     */
    public String chargerDocumentationCrous() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrl(proxyUrl + "/api/services/documentation/crous");
    }

    /**
     * Charge la documentation du service Fetch via le proxy HTTP.
     *
     * @return documentation HTML du service Fetch
     * @throws IOException si l'appel HTTP échoue
     * @throws InterruptedException si l'appel HTTP est interrompu
     */
    public String chargerDocumentationFetch() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrl(proxyUrl + "/api/services/documentation/fetch");
    }

    /**
     * Charge la documentation du service PointGeo via le proxy HTTP.
     *
     * @return documentation HTML du service PointGeo
     * @throws IOException si l'appel HTTP échoue
     * @throws InterruptedException si l'appel HTTP est interrompu
     */
    public String chargerDocumentationPointGeo() throws IOException, InterruptedException {
        return HttpClientUtils.fetchUrl(proxyUrl + "/api/services/documentation/pointgeo");
    }
}
