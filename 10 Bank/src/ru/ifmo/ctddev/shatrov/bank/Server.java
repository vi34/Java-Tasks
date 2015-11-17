package ru.ifmo.ctddev.shatrov.bank;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by vi34 on 20/05/15.
 */
public class Server {
    private static Registry registry = null;
    public static final int PORT = 8888;

    public static void main(String[] args) {
        try {
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
        }
        Bank bank = new BankImpl();
        try {
            bank = (Bank) UnicastRemoteObject.exportObject(bank, PORT);
            registry.bind("Bank", bank);
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
        } catch (AlreadyBoundException e) {
            System.err.println(e.getMessage());
        }
    }
}
