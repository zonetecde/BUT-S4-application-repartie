package fr.zonetec;

import java.rmi.RemoteException;

public interface ServiceFetch extends ServiceDocumentation {
    Reponse fetch(String url) throws RemoteException;
}
