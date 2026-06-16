# BUT S4 — Application Répartie

**Groupe :** Guerin Léa, Dumont Nathan, Brissinger Erwann, STASZEWSKI Rayane

---

## Présentation du projet

Ce projet est une **application répartie** composée de plusieurs services Java RMI interconnectés, d'un proxy HTTP exposant une API REST, et d'un frontend web TypeScript avec une carte interactive Leaflet. Le tout permet de visualiser et d'interagir avec différents types de données géolocalisées sur la ville de Nancy.

---

## Documentation technique

La documentation est disponible sur le site internet, et en markdown ici :
[DOCUMENTATION.md](DOCUMENTATION.md)

## Services proposés

| Service           | Nom RMI         | Rôle                                                                                                                                                      |
| ----------------- | --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Restaurant**    | `restaurant`    | Gestion des restaurants (coordonnées, tables, réservations) avec verrous SQL pour éviter les conflits de réservation. Connecté à une base Oracle.         |
| **CROUS**         | `crous`         | Récupération des restaurants universitaires CROUS et de leurs menus via l'API CROUS externe.                                                              |
| **Fetch**         | `fetch`         | Proxy d'appels HTTP externes (utilise le proxy IUT). Permet de contourner les restrictions CORS et centralise les requêtes sortantes.                     |
| **PointGeo**      | `pointgeo`      | Gestion des points personnalisés ajoutés par les utilisateurs sur la carte (coordonnées, emoji, titre, description). Stockés en base Oracle.              |
| **Documentation** | `documentation` | Point d'entrée centralisé pour consulter la documentation HTML de chaque service. Joue le rôle d'intermédiaire entre le frontend et les services RMI.     |
| **Proxy HTTP**    | —               | Serveur HTTP Java exposant une API REST (`/api/*`) qui traduit les requêtes HTTP en appels RMI vers les services. Point d'entrée unique pour le frontend. |

### Frontend web

Une carte interactive Leaflet affichant :

-   **Stations Vélib'** de Nancy (API Cyclocity en direct)
-   **Incidents Waze** (récupérés via le service Fetch)
-   **Restaurants** avec réservation de table
-   **Restaurants CROUS** avec affichage des menus
-   **Points personnalisés** ajoutables par l'utilisateur
-   Panneau d'administration et filtres d'affichage

---

## Où trouver les ressources

### Documentation

| Fichier                                              | Contenu                                                                                                                          |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| [`docs/deploiement.md`](docs/deploiement.md)         | Guide complet de déploiement des services Java RMI et du proxy HTTP (prérequis, compilation Maven, lancement sur chaque machine) |
| [`docs/deploiement_web.md`](docs/deploiement_web.md) | Compilation et ouverture du frontend TypeScript                                                                                  |

### Code source

| Dossier                     | Description                                                                                              |
| --------------------------- | -------------------------------------------------------------------------------------------------------- |
| `src/main/java/fr/zonetec/` | Code source Java : services RMI, proxy HTTP, classes utilitaires                                         |
| `web/`                      | Frontend TypeScript : carte Leaflet, modules par service (velibs, incidents, restaurants, crous, points) |
| `client/`                   | Client Java de test pour le service Restaurant                                                           |

### Fichiers générés

| Fichier                            | Description                                                |
| ---------------------------------- | ---------------------------------------------------------- |
| `target/restaurant-rmi-server.jar` | JAR exécutable généré par `mvn clean package`              |
| `web/dist/index.js`                | Bundle TypeScript généré par `npm run build` (via esbuild) |
