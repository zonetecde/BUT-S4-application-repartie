package fr.zonetec;

import java.rmi.RemoteException;

public interface ServiceRestaurant extends ServiceDocumentation {
    Reponse recupererCoordonneesRestaurantsNancy() throws RemoteException;

    Reponse reserverTable(
            String nomRestaurant,
            int idTable,
            String dateHeure,
            String nom,
            String prenom,
            int nombreConvives,
            String telephone) throws RemoteException;

    Reponse recupererTablesRestaurant(String nomRestaurant, String dateHeure) throws RemoteException;

    Reponse libererTablesRestaurant(String nomRestaurant, String dateHeure) throws RemoteException;

    Reponse recupererReservations() throws RemoteException;
}
