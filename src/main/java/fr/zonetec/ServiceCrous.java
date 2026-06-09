package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceCrous extends Remote {
    Reponse recupererRestaurants(String ville) throws RemoteException;

    Reponse chargerMenu(int idRestaurant) throws RemoteException;
}
