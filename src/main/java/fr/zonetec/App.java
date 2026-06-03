package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.SQLException;

public class App {
    /**
     * Lance et enregistre le service RMI des restaurants.
     *
     * @param args arguments de la ligne de commande
     * @throws RemoteException si une erreur de communication RMI se produit
     */
    public static void main(String[] args) throws RemoteException {
        try {
            System.out.println("Connexion à la base de données");
            Connection connection = ConnectionBuilder.createConnection();
            InitDataBase.creerTables(connection);
            InitDataBase.creerEntreesRestaurant(connection);
            InitDataBase.creerEntreesPlat(connection);
        } catch (SQLException e) {
            System.err.println("Erreur, impossible de se connecter ou d'initialiser la base de données");
            return;
        }
        
        // Service RMI du Restaurant
        Restaurant lc = new Restaurant();

        try {
            ServiceRestaurant rd = (ServiceRestaurant) UnicastRemoteObject.exportObject(lc, 0);

            Registry reg = LocateRegistry.getRegistry("localhost");
            reg.rebind("restaurant", rd);
        } catch (RemoteException e) {
            System.out.println("Machine distante non trouvé, annuaire non lancé ou nom de service déjà utilisé.");
        }

        // Service RMI du Fetch
        Fetch fetch = new Fetch();

        try {
            ServiceFetch rd = (ServiceFetch) UnicastRemoteObject.exportObject(fetch, 0);

            Registry reg = LocateRegistry.getRegistry("localhost");
            reg.rebind("fetch", rd);
        } catch (RemoteException e) {
            System.out.println("Machine distante non trouvé, annuaire non lancé ou nom de service déjà utilisé.");
        }
    }
}