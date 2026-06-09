package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Interface RMI pour la gestion des points géographiques personnalisés
 */
public interface ServicePoint extends Remote {
    ArrayList<Point> recupererTousLesPoints() throws RemoteException;
    Point ajouterPoint(double coordonneesX, double coordonneesY, String emoji, String titre, String description) throws RemoteException;
    boolean supprimerPoint(int idPoint) throws RemoteException;
    Point recupererPoint(int idPoint) throws RemoteException;
}
