package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceDocumentation extends Remote {
    /**
     * Retourne la documentation HTML du service lui-même.
     */
    String chargerDocumentation() throws RemoteException;

    default Reponse chargerDocumentation(String service) throws RemoteException {
        return new Reponse(false, "Aucune documentation disponible pour ce service", null);
    }
}
