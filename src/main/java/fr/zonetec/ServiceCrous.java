package fr.zonetec;

import java.rmi.RemoteException;

public interface ServiceCrous extends ServiceDocumentation {
    Reponse recupererRestaurants(String ville) throws RemoteException;

    Reponse chargerMenu(int idRestaurant) throws RemoteException;
}
