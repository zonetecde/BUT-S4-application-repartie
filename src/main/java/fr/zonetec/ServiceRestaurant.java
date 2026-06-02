package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ServiceRestaurant extends Remote {
    Reponse recupererCoordonneesRestaurantsNancy() throws RemoteException;

    Reponse reserverTable(
            String nomRestaurant,
            String nom,
            String prenom,
            int nombreConvives,
            String telephone) throws RemoteException;
}
