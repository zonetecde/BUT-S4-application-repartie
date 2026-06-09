package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class LancerFetch {
    /**
     * Lance et enregistre le service RMI du Fetch.
     */
    public static void main(String[] args) throws RemoteException {
        // Service RMI du Fetch
        Fetch fetch = new Fetch();

        try {
            ServiceFetch rd = (ServiceFetch) UnicastRemoteObject.exportObject(fetch, 0);

            Registry reg = LocateRegistry.getRegistry("localhost");
            reg.rebind("fetch", rd);

            System.out.println("Service Fetch enregistré dans l'annuaire RMI");
        } catch (RemoteException e) {
            System.out.println("Un problème est survenu lors de l'inscription du service Fetch dans l'annuaire : " + e);
        }
    }
}
