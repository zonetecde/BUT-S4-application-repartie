package fr.zonetec;

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

            // On stock les coordonnées dans un tableau de tableaux de doubles (un tableau pour chaque restaurant, avec les coordonnées X et Y)
            ArrayList<Double[]> tabCord = new ArrayList<>();

            PreparedStatement st = conn.prepareStatement("SELECT * FROM Restaurant");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
               Double[] coupleCord = new Double[2];
               coupleCord[0] = rs.getDouble("coordonneesX");
               coupleCord[1] = rs.getDouble("coordonneesY");
               tabCord.add(coupleCord);
            }
            return new Reponse(true, "La liste a été transmise", tabCord) ;
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
     * @param nom nom du client
     * @param prenom prénom du client
     * @param nombreConvives nombre de convives
     * @param telephone numéro de téléphone du client
     * @return réponse JSON
     */
    public Reponse reserverTable(String nomRestaurant, int idTable, String nom, String prenom, int nombreConvives, String telephone) {
        try {
            PreparedStatement st = conn.prepareStatement("SELECT idRestaurant FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }
            int idRest = rs.getInt("idRestaurant");
            // Création de la réservation
            st = conn.prepareStatement(
                    "INSERT INTO Reservation " +
                            "(idRestaurant, nomClient, prenomClient, idTable, reservee, nbConvives, numTel) " +
                            "VALUES (?, ?, ?, ?, 1, ?, ?)");
            st.setInt(1, idRest);
            st.setString(2, nom);
            st.setString(3, prenom);
            st.setInt(4, idTable);
            st.setInt(5, nombreConvives);
            st.setString(6, telephone);
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
                return new Reponse(false, "Erreur lors de la réservation", null);
            } catch (SQLException ex) {
                return new Reponse(false, "Erreur lors du rollback", null);
            }
        }
    }

    public Reponse recupererTablesRestaurant (String nomRestaurant) {
        //on initalise la connexion
        try {
            //on verifie l'existence du restaurant
            PreparedStatement st = conn.prepareStatement("SELECT idRestaurant FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return new Reponse(false, "Le restaurant n'existe pas", null);
            }
            //on verifier la disponibilité des tables
            st = conn.prepareStatement(
                    "SELECT t.idTable, t.nbPlaces " +
                            "FROM Table_Resto t " +
                            "JOIN Restaurant r ON t.idRestaurant = r.idRestaurant " +
                            "WHERE r.nom = ? AND t.reservee = 0 FOR UPDATE"
            );
            st.setString(1, nomRestaurant);
            rs = st.executeQuery();
            if (!rs.next()) {
                conn.rollback();
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
                System.out.println(e);
                return new Reponse(false, "Erreur lors de l'appel à la BD", null);
            } catch (SQLException ex) {
                return new Reponse(false, "Erreur lors du rollback", null);
            }
        }
    }



    public Restaurant() {
        try {
            this.conn = ConnectionBuilder.createConnection();
            this.conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
