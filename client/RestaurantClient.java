import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import fr.zonetec.Reponse;
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

        ServiceRestaurant service = (ServiceRestaurant) reg.lookup("restaurant");

        Reponse coordonnees = service.recupererCoordonneesRestaurantsNancy();
        System.out.println(coordonnees.toJson());

        Reponse reservation = service.reserverTable("Restaurant test", "Dupont", "Jean", 4, "0600000000");
        System.out.println(reservation.toJson());
    }
}
