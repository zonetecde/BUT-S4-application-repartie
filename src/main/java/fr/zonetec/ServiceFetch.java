package fr.zonetec;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceFetch extends Remote {
    Reponse fetch(String url) throws RemoteException;
}
