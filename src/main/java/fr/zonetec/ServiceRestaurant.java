package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceRestaurant extends Remote {
    Reponse recupererCoordonneesRestaurantsNancy() throws RemoteException;

    Reponse reserverTable(
            String nomRestaurant,
            String nom,
            String prenom,
            int nombreConvives,
            String telephone) throws RemoteException;
}
