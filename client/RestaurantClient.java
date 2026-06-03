import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import fr.zonetec.Reponse;
import fr.zonetec.ServiceFetch;
import fr.zonetec.ServiceRestaurant;

public class RestaurantClient {
    /**
     * Appelle les méthodes du service RMI des restaurants à des fins de test.
     *
     * @param args adresse IP et port de l'annuaire RMI
     * @throws RemoteException si une erreur RMI se produit
     * @throws NotBoundException si le service restaurant n'est pas enregistré
     */
    public static void main(String[] args) throws RemoteException, NotBoundException {
        Registry reg = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));

        // Récupère le premier serice de restaurant
        ServiceRestaurant service = (ServiceRestaurant) reg.lookup("restaurant");

        Reponse coordonnees = service.recupererCoordonneesRestaurantsNancy();
        System.out.println(coordonnees.toJson());

        Reponse reservation = service.reserverTable("Grand Café Foy", "Dupont", "Jean", 4, "0600000000");
        System.out.println(reservation.toJson());

        // Récupère le deuxième service pour fetch une API nécessitant le proxy de l'IUT
        ServiceFetch serviceFetch = (ServiceFetch)reg.lookup("fetch");
        Reponse fetch = serviceFetch.fetch("https://carto.g-ny.eu/data/cifs/cifs_waze_v2.json");
        System.out.println(fetch.toJson());
    }
}
