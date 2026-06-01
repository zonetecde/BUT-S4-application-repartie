package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class App {
    /**
     * Lance et enregistre le service RMI des restaurants.
     *
     * @param args arguments de la ligne de commande
     * @throws RemoteException si une erreur de communication RMI se produit
     */
    public static void main(String[] args) throws RemoteException {
        Restaurant lc = new Restaurant();

        try {
            ServiceRestaurant rd = (ServiceRestaurant) UnicastRemoteObject.exportObject(lc, 0);

            Registry reg = LocateRegistry.getRegistry("localhost");
            reg.rebind("restaurant", rd);
        } catch (RemoteException e) {
            System.out.println("Machine distante non trouvé, annuaire non lancé ou nom de service déjà utilisé.");
        }
    }
}