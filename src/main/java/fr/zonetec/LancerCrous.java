package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class LancerCrous {
    /**
     * Lance et enregistre le service RMI du CROUS.
     */
    public static void main(String[] args) throws RemoteException {
        // Service RMI du Crous
        Crous crous = new Crous();

        try {
            ServiceCrous rd = (ServiceCrous) UnicastRemoteObject.exportObject(crous, 0);

            // récupère l'annuaire RMI
            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
            reg.rebind("crous", rd);

            System.out.println("Service CROUS enregistré dans l'annuaire RMI");
        } catch (RemoteException e) {
            System.out.println("Un problème est survenu lors de l'inscription du service CROUS dans l'annuaire : " + e);
        }
    }
}
