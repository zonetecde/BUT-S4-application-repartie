package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class LancerPointGeo {
    /**
     * Lance et enregistre le service RMI des points géographiques.
     */
    public static void main(String[] args) throws RemoteException {
        System.setProperty("java.rmi.server.codebase", "file:target/restaurant-rmi-server.jar");

        Points service = new Points();

        try {
            ServicePoint rd = (ServicePoint) UnicastRemoteObject.exportObject(service, 0);

            Registry reg = LocateRegistry.getRegistry(1099);
            reg.rebind("pointgeo", rd);

            System.out.println("Service PointGeo enregistré dans l'annuaire RMI");
        } catch (RemoteException e) {
            System.out.println("Un problème est survenu lors de l'inscription du service PointGeo : " + e);
        }
    }
}
