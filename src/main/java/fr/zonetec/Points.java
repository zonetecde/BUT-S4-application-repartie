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
            String query = "INSERT INTO Point_Geo (coordonneesX, coordonneesY, emoji, titre, description) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setDouble(1, coordonneesX);
            stmt.setDouble(2, coordonneesY);
            stmt.setString(3, emoji);
            stmt.setString(4, titre);
            stmt.setString(5, description);
            
            stmt.executeUpdate();
            
            // Récupère l'ID généré
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int idPoint = rs.getInt(1);
                newPoint = new Point(idPoint, coordonneesX, coordonneesY, emoji, titre, description);
                System.out.println("Point ajouté avec l'ID : " + idPoint);
            }
            
            conn.close();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du point : " + e.getMessage());
        }
        
        return newPoint;
    }

    /**
     * Supprime un point par son ID
     */
    @Override
    public boolean supprimerPoint(int idPoint) throws RemoteException {
        try {
            Connection conn = ConnectionBuilder.createConnection();
            String query = "DELETE FROM Point_Geo WHERE idPoint = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, idPoint);
            
            int rowsAffected = stmt.executeUpdate();
            conn.close();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du point : " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère un point par son ID
     */
    @Override
    public Point recupererPoint(int idPoint) throws RemoteException {
        try {
            Connection conn = ConnectionBuilder.createConnection();
            String query = "SELECT * FROM Point_Geo WHERE idPoint = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, idPoint);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Point point = new Point(
                    rs.getInt("idPoint"),
                    rs.getDouble("coordonneesX"),
                    rs.getDouble("coordonneesY"),
                    rs.getString("emoji"),
                    rs.getString("titre"),
                    rs.getString("description")
                );
                conn.close();
                return point;
            }
            
            conn.close();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du point : " + e.getMessage());
        }
        
        return null;
    }
}
