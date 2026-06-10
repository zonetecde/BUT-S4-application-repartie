package fr.zonetec;

import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;


public class Restaurant implements ServiceRestaurant {

    Connection conn ;
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
                            "WHERE idRestaurant = ? AND idTable = ?"
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
     * @return réponse JSON contenant un dictionnaire idTable => nombre de places
     */
    public Reponse recupererTablesRestaurant(String nomRestaurant, String dateHeure) throws RemoteException {
        try {
            Timestamp debutReservation = Timestamp.valueOf(dateHeure);
            Timestamp finReservation = new Timestamp(debutReservation.getTime() + 3600000);

            PreparedStatement st = conn.prepareStatement(
                    "SELECT idRestaurant FROM Restaurant WHERE nom = ?"
            );
            st.setString(1, nomRestaurant);

            ResultSet rs = st.executeQuery();

            if (!rs.next()) {
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }

            int idRest = rs.getInt("idRestaurant");

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
                            ")"
            );

            st.setInt(1, idRest);
            st.setTimestamp(2, finReservation);
            st.setTimestamp(3, debutReservation);

            rs = st.executeQuery();

            HashMap<Integer, Integer> tables = new HashMap<>();

            while (rs.next()) {
                tables.put(rs.getInt("idTable"), rs.getInt("nbPlaces"));
            }

            return new Reponse(true, "Tables disponibles récupérées", tables);

        } catch (IllegalArgumentException e) {
            return new Reponse(false, "Date de réservation invalide", null);
        } catch (SQLException e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur lors de la récupération des tables : " + e.getMessage(), null);
        }
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
