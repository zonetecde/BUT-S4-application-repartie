package fr.zonetec;

import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class Restaurant implements ServiceRestaurant {
    // On utilise un set pour stocker les restaurants en cours de réservation, afin d'empêcher deux clients de resa en mm temps
    // On utilise un ConcurrentHashMap pour éviter les problèmes de concurrence
    private static Set<String> RESTAURANTS_EN_RESERVATION = ConcurrentHashMap.newKeySet();

    Connection conn ;

    /**
     * Charge la documentation HTML du service Restaurant.
     *
     * @return documentation HTML du service
     */
    public String chargerDocumentation() {
        return """
                <section>
                    <h2>Service Restaurant</h2>
                    <p>
                        Le service Restaurant gère les restaurants, les tables disponibles et les réservations. Le site
                        commence par demander la liste des restaurants avec leurs coordonnées pour les afficher sur la carte.
                    </p>
                    <p>
                        Quand l'utilisateur veut réserver, il choisit d'abord une date et une heure. Le service cherche alors
                        les tables du restaurant qui ne sont pas déjà prises sur ce créneau. Une réservation dure une heure,
                        donc une table réservée à midi est considérée occupée jusqu'à 13h.
                    </p>
                    <p>
                        Pour éviter que deux personnes réservent en même temps le même restaurant au même horaire, le service
                        prend un verrou sur le couple restaurant + date/heure. Il verrouille aussi les lignes SQL des tables
                        disponibles avec FOR UPDATE NOWAIT. Si une autre personne est déjà en train de réserver ce même créneau,
                        le service renvoie un message demandant de patienter.
                    </p>
                    <p>
                        Quand l'utilisateur valide la réservation, le service vérifie une dernière fois que la table existe,
                        que le nombre de convives est correct, et qu'aucune réservation ne chevauche ce créneau. Ensuite il
                        insère la réservation en base et libère le verrou.
                    </p>
                    <img src="static/schema-service-restaurant.png" alt="Schéma du service Restaurant" />
                    <img src="schema-service-restaurant-2.png" alt="Schéma du service Restaurant" />
                </section>
                """;
    }
    /**
     * Récupère les coordonnées de tous les restaurants de Nancy enregistrés en base.
     *
     * @return réponse JSON
     */
    public Reponse recupererCoordonneesRestaurantsNancy() {
        try {
            Connection conn = ConnectionBuilder.createConnection();

            // On stock les restaurants dans le format attendu par le site.
            ArrayList<HashMap<String, Object>> restaurants = new ArrayList<>();

            PreparedStatement st = conn.prepareStatement("SELECT * FROM Restaurant");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
               HashMap<String, Object> restaurant = new HashMap<>();
               restaurant.put("idRestaurant", rs.getInt("idRestaurant"));
               restaurant.put("nom", rs.getString("nom"));
               restaurant.put("adresse", rs.getString("adresse"));
               restaurant.put("lat", rs.getDouble("coordonneesY"));
               restaurant.put("lon", rs.getDouble("coordonneesX"));
               restaurants.add(restaurant);
            }
            return new Reponse(true, "La liste a été transmise", restaurants) ;
        } catch (SQLException e) {
            System.out.println("Erreur lors de la connexion à la BD");
        }
        return new Reponse(false, "Erreur lors de la connexion à la BD", null);
    }

    /**
     * Réserve une table dans un restaurant.
     *
     * @param nomRestaurant nom du restaurant
     * @param idTable id de la table
     * @param dateHeure date et heure de la réservation au format yyyy-mm-dd hh:mm:ss
     * @param nom nom du client
     * @param prenom prénom du client
     * @param nombreConvives nombre de convives
     * @param telephone numéro de téléphone du client
     * @return réponse JSON
     */
    public Reponse reserverTable(String nomRestaurant, int idTable, String dateHeure, String nom, String prenom, int nombreConvives, String telephone) {
        try {
            Timestamp dateReservation = Timestamp.valueOf(dateHeure);
            Timestamp finReservation = new Timestamp(dateReservation.getTime() + 3600000); // On considère que la réservation dure 1h, on ajoute donc 3600000ms à la date de début pour obtenir la date de fin
            PreparedStatement st = conn.prepareStatement("SELECT idRestaurant FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }
            int idRest = rs.getInt("idRestaurant");
            st = conn.prepareStatement(
                    "SELECT nbPlaces FROM Table_Resto " +
                            "WHERE idRestaurant = ? AND idTable = ? " +
                            "FOR UPDATE NOWAIT"
            );
            st.setInt(1, idRest);
            st.setInt(2, idTable);
            rs = st.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Cette table n'existe pas ou n'est plus disponible", null);
            }

            int nbPlaces = rs.getInt("nbPlaces");

            if (nombreConvives > nbPlaces) {
                conn.rollback();
                return new Reponse(false, "Le nombre de convives dépasse la capacité de la table", null);
            }
            if (nombreConvives <= 0) {
                conn.rollback();
                return new Reponse(false, "Le nombre de convives doit être supérieur à 0", null);
            }

            // Vérifie que la table existe et qu'elle a assez de places
            st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Reservation " +
                            "WHERE idRestaurant = ? AND idTable = ? " +
                            "AND dateRes < ? AND dateRes + 1/24 > ?"
            );
            st.setInt(1, idRest);
            st.setInt(2, idTable);
            st.setTimestamp(3, finReservation);
            st.setTimestamp(4, dateReservation);
            rs = st.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return new Reponse(false, "Une réservation existe déjà sur ce créneau", null);
            }

            // Création de la réservation
            st = conn.prepareStatement(
                    "INSERT INTO Reservation " +
                            "(idRestaurant, nomClient, prenomClient, idTable, dateRes, nbConvives, numTel) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)");
            st.setInt(1, idRest);
            st.setString(2, nom);
            st.setString(3, prenom);
            st.setInt(4, idTable);
            st.setTimestamp(5, dateReservation);
            st.setInt(6, nombreConvives);
            st.setString(7, telephone);
            int rowsAffected = st.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                return new Reponse(false, "Impossible d'ajouter la réservation", null);
            }
            conn.commit();
            RESTAURANTS_EN_RESERVATION.remove(nomRestaurant + "|" + dateHeure);
            return new Reponse(true, "La réservation a été ajoutée", true);
        } catch (SQLException e) {
        try {
            conn.rollback();

            if (e.getErrorCode() == 54) {
                return new Reponse(false, "Cette table est déjà en cours de réservation par un autre client", null);
            }
            e.printStackTrace();
            return new Reponse(false, "Erreur SQL : " + e.getMessage(), null);
        } catch (SQLException ex) {
            return new Reponse(false, "Erreur lors du rollback", null);
        }
    }
    }

    /**
     * Récupère les tables disponibles d'un restaurant et verrouille les lignes sélectionnées jusqu'à la réservation.
     *
     * @param nomRestaurant nom du restaurant
     * @param dateHeure date et heure de la réservation au format yyyy-mm-dd hh:mm:ss
     * @return réponse JSON contenant un dictionnaire idTable => nombre de places
     */
    public Reponse recupererTablesRestaurant (String nomRestaurant, String dateHeure) throws RemoteException {
        boolean verrouPris = false;
        // lock seulement le resto pr la meme date/heure
        String reservationKey = nomRestaurant + "|" + dateHeure;
        //on initalise la connexion
        try {
            Timestamp debutReservation = Timestamp.valueOf(dateHeure);
            // on dit qu'une resa dure une heure
            Timestamp finReservation = new Timestamp(debutReservation.getTime() + 3600000);

            //on verifie l'existence du restaurant
            PreparedStatement st = conn.prepareStatement("SELECT idRestaurant FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }
            int idRest = rs.getInt("idRestaurant");
            //on verifier la disponibilité des tables et que personne d'autre n'est en train de réserver dans ce restaurant
            if (!RESTAURANTS_EN_RESERVATION.add(reservationKey)) {
                conn.rollback();
                return new Reponse(false, "Veuillez patienter, une autre personne est déjà en train de réserver dans ce restaurant pour ce créneau", null);
            }
            verrouPris = true;
            st = conn.prepareStatement(
                    "SELECT t.idTable, t.nbPlaces " +
                            "FROM Table_Resto t " +
                            "WHERE t.idRestaurant = ? " +
                            "AND NOT EXISTS ( " +
                            "   SELECT 1 FROM Reservation res " +
                            "   WHERE res.idRestaurant = t.idRestaurant " +
                            "   AND res.idTable = t.idTable " +
                            "   AND res.dateRes < ? " +
                            "   AND res.dateRes + 1/24 > ? " +
                            ") " +
                            "FOR UPDATE NOWAIT"
            );
            st.setInt(1, idRest);
            st.setTimestamp(2, finReservation);
            st.setTimestamp(3, debutReservation);
            rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                RESTAURANTS_EN_RESERVATION.remove(reservationKey);
                return new Reponse(false, "Aucune table disponible", null);
            }
            HashMap<Integer, Integer> tables = new HashMap<>() ;
            tables.put(rs.getInt(1),rs.getInt(2));
            while (rs.next()) {
                tables.put(rs.getInt(1),rs.getInt(2));
            }
            return new Reponse(true, "", tables);
        }   catch (SQLException e) {
            try {
                conn.rollback();
                if (verrouPris) {
                    RESTAURANTS_EN_RESERVATION.remove(reservationKey);
                }
                // Si le code d'erreur est 54 c'est que la ligne est verrouillée par une autre transaction, donc qu'une autre personne est en train de réserver
                if (e.getErrorCode() == 54) {
                    return new Reponse(false, "Veuillez patienter, quelqu'un est déjà en train de réserver une table sur ce créneau", null);
                }
                System.out.println(e);
                return new Reponse(false, "Erreur lors de l'appel à la BD", null);
            } catch (SQLException ex) {
                return new Reponse(false, "Erreur lors du rollback", null);
            }
        } catch (IllegalArgumentException e) {
            return new Reponse(false, "Date de réservation invalide", null);
        }
    }

    /**
     * Libère le verrou de reservation d'un restaurant.
     *
     * @param nomRestaurant nom du restaurant
     * @param dateHeure date et heure de la réservation
     * @return réponse JSON confirmant la liberation
     */
    public Reponse libererTablesRestaurant(String nomRestaurant, String dateHeure) throws RemoteException {
        if (nomRestaurant != null && dateHeure != null) {
            RESTAURANTS_EN_RESERVATION.remove(nomRestaurant + "|" + dateHeure);
        }
        try {
            // le rollback va libérer les verrous pris lors de la récupération des tables, même si aucune table n'a été réservée à la fin
            conn.rollback();
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la liberation du verrou", null);
        }
        return new Reponse(true, "Verrou de reservation libéré", true);
    }

    public Reponse recupererReservations() {
        try {
            ArrayList<HashMap<String, Object>> reservations = new ArrayList<>();

            PreparedStatement st = conn.prepareStatement(
                    "SELECT r.idRestaurant, r.nom AS nomRestaurant, " +
                            "res.idReservation, res.idTable, res.dateRes, " +
                            "res.nomClient, res.prenomClient, res.nbConvives, res.numTel " +
                            "FROM Reservation res " +
                            "JOIN Restaurant r ON res.idRestaurant = r.idRestaurant " +
                            "ORDER BY res.dateRes DESC"
            );

            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                HashMap<String, Object> reservation = new HashMap<>();

                reservation.put("idRestaurant", rs.getInt("idRestaurant"));
                reservation.put("nomRestaurant", rs.getString("nomRestaurant"));
                reservation.put("idReservation", rs.getInt("idReservation"));
                reservation.put("idTable", rs.getInt("idTable"));
                reservation.put("dateRes", rs.getTimestamp("dateRes").toString());
                reservation.put("nomClient", rs.getString("nomClient"));
                reservation.put("prenomClient", rs.getString("prenomClient"));
                reservation.put("nbConvives", rs.getInt("nbConvives"));
                reservation.put("numTel", rs.getString("numTel"));

                reservations.add(reservation);
            }

            return new Reponse(true, "Réservations récupérées", reservations);
        } catch (SQLException e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur lors de la récupération des réservations : " + e.getMessage(), null);
        }
    }



    /**
     * Initialise le service restaurant avec une connexion en transaction manuelle.
     */
    public Restaurant() {
        try {
            this.conn = ConnectionBuilder.createConnection();
            this.conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
