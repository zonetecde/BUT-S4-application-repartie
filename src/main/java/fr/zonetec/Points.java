package fr.zonetec;

import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;

/**
 * Implémentation du service RMI pour la gestion des points géographiques
 */
public class Points implements ServicePoint {

    /**
     * Récupère tous les points géographiques de la base de données
     */
    @Override
    public ArrayList<Point> recupererTousLesPoints() throws RemoteException {
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
