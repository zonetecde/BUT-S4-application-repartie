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
            ArrayList<Double[]> tabCord = new ArrayList<>();
            Connection conn = ConnectionBuilder.createConnection();
            PreparedStatement st = conn.prepareStatement("SELECT * FROM Restaurant");
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
               Double[] coupleCord = new Double[2];
               coupleCord[0] = rs.getDouble("coordonnesX");
               coupleCord[1] = rs.getDouble("coordonnesY");
               tabCord.add(coupleCord);
            }
            return new Reponse(true, "", tabCord) ;
        } catch (SQLException e) {
            System.out.println("Erreur lors de la connexion à la BD");
        }
        return new Reponse(false, "", false);
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
            PreparedStatement st = conn.prepareStatement("SELECT * FROM Restaurant WHERE ? = nom");
            st.setString(1, nomRestaurant);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                int idRes = rs.getInt(0);
                st = conn.prepareStatement("INSERT INTO Reservation (idRestaurant, nomClient, prenomClient, nbConvives, numTel) VALUES (?,?,?,?,?)");
                st.setInt(1,idRes);
                st.setString(2, nom);
                st.setString(3, prenom);
                st.setInt(4,nombreConvives);
                st.setString(5, telephone);
                rs = st.executeQuery();
                if (rs.next()) {
                    return new Reponse(true, "", true) ;
                }
            }
            return new Reponse(true, "Le Restaurant n'existe pas", false);

        } catch (SQLException e) {
        System.out.println("Erreur lors de la connexion à la BD");
        }
        return new Reponse(false, "Problème", false);
    }

    public Restaurant() {
    }
}
