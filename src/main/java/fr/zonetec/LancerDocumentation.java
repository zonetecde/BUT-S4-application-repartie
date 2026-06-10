package fr.zonetec;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class LancerDocumentation {
    /**
     * Lance et enregistre le service RMI de documentation.
     *
     * @param args args[0]=URL du proxy HTTP
     */
    public static void main(String[] args) throws RemoteException {
        String proxyUrl = args.length > 0 ? args[0] : "http://localhost:8081";
        Documentation documentation = new Documentation(proxyUrl);

        try {
            ServiceDocumentation rd = (ServiceDocumentation) UnicastRemoteObject.exportObject(documentation, 0);

            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
            reg.rebind("documentation", rd);

            System.out.println("Service Documentation enregistre dans l'annuaire RMI");
        } catch (RemoteException e) {
            System.out.println("Un probleme est survenu lors de l'inscription du service Documentation dans l'annuaire : " + e);
        }
    }
}
