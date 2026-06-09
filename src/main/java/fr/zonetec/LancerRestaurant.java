package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.SQLException;

public class LancerRestaurant {
    /**
     * Lance et enregistre le service RMI des restaurants.
     */
    public static void main(String[] args) throws RemoteException {
        try {
            System.out.println("Connexion à la base de données");
            Connection connection = ConnectionBuilder.createConnection();
            InitDataBase.creerTables(connection);
            InitDataBase.creerEntreesRestaurant(connection);
            InitDataBase.creerEntreesPlat(connection);
            InitDataBase.creerEntreesTable(connection);
        } catch (SQLException e) {
            System.err.println("Erreur, impossible de se connecter ou d'initialiser la base de données");
            return;
        }
        
        // Service RMI du Restaurant
        Restaurant lc = new Restaurant();

        try {
            ServiceRestaurant rd = (ServiceRestaurant) UnicastRemoteObject.exportObject(lc, 0);

            // récupère l'annuaire RMI
            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
            reg.rebind("restaurant", rd);

            System.out.println("Service Restaurant enregistré dans l'annuaire RMI");
        } catch (RemoteException e) {
            System.out.println("Un problème est survenue lors de l'inscription du service dans l'annuaire : " + e);
        }

        // Service RMI des Points géographiques
        Points points = new Points();

        try {
            ServicePoint sp = (ServicePoint) UnicastRemoteObject.exportObject(points, 0);

            Registry reg = LocateRegistry.getRegistry("localhost");
            reg.rebind("point", sp);
        } catch (RemoteException e) {
            System.out.println("Un problème est survenue lors de l'inscription du service des points dans l'annuaire : " + e);
        }
    }
}
