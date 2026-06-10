package fr.zonetec;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Interface RMI pour la gestion des points géographiques personnalisés
 */
public interface ServicePoint extends ServiceDocumentation {
    ArrayList<Point> recupererTousLesPoints() throws RemoteException;
    Point ajouterPoint(double coordonneesX, double coordonneesY, String emoji, String titre, String description) throws RemoteException;
}
