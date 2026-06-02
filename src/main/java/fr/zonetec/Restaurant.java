package fr.zonetec;

import java.sql.*;
import java.util.ArrayList;

public class Restaurant implements ServiceRestaurant {
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
     * @param nom nom du client
     * @param prenom prénom du client
     * @param nombreConvives nombre de convives
     * @param telephone numéro de téléphone du client
     * @return réponse JSON
     */
    public Reponse reserverTable(
            String nomRestaurant,
            String nom,
            String prenom,
            int nombreConvives,
            String telephone) {
        try {
            Connection conn = ConnectionBuilder.createConnection();

            PreparedStatement st = conn.prepareStatement("SELECT * FROM Restaurant WHERE nom = ?");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                int idRes = rs.getInt("idRestaurant");
                st = conn.prepareStatement("INSERT INTO Reservation (idRestaurant, nomClient, prenomClient, nbConvives, numTel) VALUES (?,?,?,?,?)");
                st.setInt(1, idRes);
                st.setString(2, nom);
                st.setString(3, prenom);
                st.setInt(4, nombreConvives);
                st.setString(5, telephone);
                int rowsAffected = st.executeUpdate();
                if (rowsAffected > 0) {
                    return new Reponse(true, "La réservation a été ajoutée", true);
                }
            }
            return new Reponse(false, "Le Restaurant n'existe pas", null);

        } catch (SQLException e) {
        System.out.println("Erreur lors de la connexion à la BD");
        }
        return new Reponse(false, "Erreur lors de la connexion à la BD", null);
    }

    public Restaurant() {
    }
}
