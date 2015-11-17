package ru.ifmo.ctddev.shatrov.bank;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    public Person getRemotePerson(String passport)
            throws RemoteException;

    public LocalPerson getLocalPerson(String passport)
        throws RemoteException;

    public boolean createPerson(String name, String surname, String passportNumber)
            throws RemoteException;

    public void updatePerson(LocalPerson person) throws RemoteException;
}
