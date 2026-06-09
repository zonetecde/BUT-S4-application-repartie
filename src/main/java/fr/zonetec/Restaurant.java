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
            dateHeure = dateHeure.replace('T', ' '); // datetime local retourne : 2026-06-10T19:00
            if (!dateHeure.contains(":00")) {
                dateHeure += ":00";
            }
            Timestamp dateReservation = Timestamp.valueOf(dateHeure);
            Timestamp finReservation = new Timestamp(dateReservation.getTime() + 7200000); // On considère que la réservation dure 2h
            PreparedStatement st = conn.prepareStatement("SELECT idRestaurant FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }
            int idRest = rs.getInt("idRestaurant");

            // Vérifie que la table existe et qu'elle a assez de places
            st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Reservation " +
                            "WHERE idRestaurant = ? AND idTable = ? " +
                            "AND dateRes < ? AND dateRes + 2/24 > ?"
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
        } catch (IllegalArgumentException e) {
            return new Reponse(false, "Date de réservation invalide", null);
        } catch (SQLException e) {
            try {
                conn.rollback();
                return new Reponse(false, "Erreur lors de la réservation", null);
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
    public Reponse recupererTablesRestaurant(String nomRestaurant, String dateHeure, int nombreConvives) throws RemoteException {
        try {
            dateHeure = dateHeure.replace('T', ' ');
            if (!dateHeure.contains(":00")) {
                dateHeure += ":00";
            }
            Timestamp dateReservation = Timestamp.valueOf(dateHeure);
            Timestamp finReservation = new Timestamp(dateReservation.getTime() + 7200000); // 2 heures de réservation

            PreparedStatement st = conn.prepareStatement("SELECT idRestaurant FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }
            int idRest = rs.getInt("idRestaurant");

            st = conn.prepareStatement(
                    "SELECT t.idTable, t.nbPlaces " +
                            "FROM Table_Resto t " +
                            "WHERE t.idRestaurant = ? AND t.nbPlaces >= ? " +
                            "AND t.idTable NOT IN (" +
                            "    SELECT idTable FROM Reservation " +
                            "    WHERE idRestaurant = ? " +
                            "      AND dateRes < ? " +
                            "      AND dateRes + 2/24 > ? " +
                            ")"
            );
            st.setInt(1, idRest);
            st.setInt(2, nombreConvives);
            st.setInt(3, idRest);
            st.setTimestamp(4, finReservation);
            st.setTimestamp(5, dateReservation);
            rs = st.executeQuery();
            if (!rs.next()) {
                return new Reponse(false, "Aucune table disponible pour ce créneau", null);
            }
            HashMap<Integer, Integer> tables = new HashMap<>();
            tables.put(rs.getInt(1), rs.getInt(2));
            while (rs.next()) {
                tables.put(rs.getInt(1), rs.getInt(2));
            }
            return new Reponse(true, "", tables);
        } catch (IllegalArgumentException e) {
            return new Reponse(false, "Date de réservation invalide", null);
        } catch (SQLException e) {
            try {
                conn.rollback();
                if (e.getErrorCode() == 54) {
                    return new Reponse(false, "Veuillez patienter, quelqu'un est déjà en train de réserver une table", null);
                }
                System.out.println(e);
                return new Reponse(false, "Erreur lors de l'appel à la BD", null);
            } catch (SQLException ex) {
                return new Reponse(false, "Erreur lors du rollback", null);
            }
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
