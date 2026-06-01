package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceRestaurant extends Remote {
    String inscrireServiceCentral(String urlCentralService) throws RemoteException;

    String recupererCoordonneesRestaurantsNancy() throws RemoteException;

    String reserverTable(
            String nomRestaurant,
            String nom,
            String prenom,
            int nombreConvives,
            String telephone) throws RemoteException;
}
