package fr.zonetec;

import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;

/**
 * Implémentation du service RMI pour la gestion des points géographiques
 */
public class Points implements ServicePoint {
    /**
     * Charge la documentation HTML du service PointGeo.
     *
     * @return documentation HTML du service
     */
    public String chargerDocumentation() {
        System.out.println("[LOG] Appel de chargerDocumentation()");
        return """
                <section>
                    <h2 class="text-xl font-bold mb-3">Service PointGeo</h2>
                    <p class="text-sm mb-2">
                        Le service PointGeo sert à gérer les points ajoutés par les utilisateurs sur la carte. Un point
                        contient des coordonnées, un emoji, un titre et une description. Ces informations sont stockées
                        dans la base de données pour pouvoir les retrouver plus tard.
                    </p>
                    <p class="text-sm mb-2">
                        Quand le site charge la carte, le proxy appelle le service PointGeo pour récupérer tous les points.
                        Le service lit la table Point_Geo, transforme chaque ligne en objet Point, puis renvoie la liste
                        au navigateur.
                    </p>
                    <p class="text-sm mb-2">
                        Quand un utilisateur ajoute un point, le site envoie les coordonnées et le texte au proxy. Le proxy
                        appelle ensuite le service PointGeo. Le service insère le point en base, récupère l'identifiant créé,
                        puis renvoie le nouveau point pour que la carte puisse l'afficher directement.
                    </p>
                    <img src="static/schema-service-pointgeo.png" alt="Schéma du service PointGeo" class="my-3 rounded shadow-md max-w-full" />
                </section>
                """;
    }

    /**
     * Récupère tous les points géographiques de la base de données
     */
    @Override
    public ArrayList<Point> recupererTousLesPoints() throws RemoteException {
        System.out.println("[LOG] Appel de recupererTousLesPoints()");
        ArrayList<Point> points = new ArrayList<>();
        
        try {
            Connection conn = ConnectionBuilder.createConnection();
            String query = "SELECT * FROM Point_Geo";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Point point = new Point(
                    rs.getInt("idPoint"),
                    rs.getDouble("coordonneesX"),
                    rs.getDouble("coordonneesY"),
                    rs.getString("emoji"),
                    rs.getString("titre"),
                    rs.getString("description")
                );
                points.add(point);
            }
            
            conn.close();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des points : " + e.getMessage());
        }
        
        return points;
    }

    /**
     * Ajoute un nouveau point géographique dans la base de données
     */
    @Override
    public Point ajouterPoint(double coordonneesX, double coordonneesY, String emoji, String titre, String description) throws RemoteException {
        System.out.println("[LOG] Appel de ajouterPoint() avec les paramètres : coordonneesX=" + coordonneesX + ", coordonneesY=" + coordonneesY + ", emoji=" + emoji + ", titre=" + titre + ", description=" + description);
        Point newPoint = null;
        
        try {
            Connection conn = ConnectionBuilder.createConnection();
            String query = "BEGIN INSERT INTO Point_Geo (coordonneesX, coordonneesY, emoji, titre, description) VALUES (?, ?, ?, ?, ?) RETURNING idPoint INTO ?; END;";
            CallableStatement stmt = conn.prepareCall(query);
            
            stmt.setDouble(1, coordonneesX);
            stmt.setDouble(2, coordonneesY);
            stmt.setNString(3, emoji);
            stmt.setString(4, titre);
            stmt.setString(5, description);
            stmt.registerOutParameter(6, Types.INTEGER);
            
            stmt.executeUpdate();
            
            int idPoint = stmt.getInt(6);
            newPoint = new Point(idPoint, coordonneesX, coordonneesY, emoji, titre, description);
            System.out.println("Point ajouté avec l'ID : " + idPoint);
            
            conn.close();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du point : " + e.getMessage());
        }
        
        return newPoint;
    }
}
