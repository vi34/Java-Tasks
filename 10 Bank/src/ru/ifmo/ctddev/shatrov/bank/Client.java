package ru.ifmo.ctddev.shatrov.bank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static void main(String[] args) {
        if (args == null || args.length != 5 || args[0] == null || args[1] == null || args[2] == null || args[3] == null || args[4] == null) {
            System.err.println("Incorrect arguments\nUsage: name surname passport_number account_number changes");
            return;
        }
        Bank bank;
        try {
            Registry registry = LocateRegistry.getRegistry(null, Server.PORT);
            bank = (Bank) registry.lookup("Bank");
        } catch (NotBoundException e) {
            System.err.println(e.getMessage());
            return;
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
            return;
        }

        try {
            Person person = bank.getRemotePerson(args[2]);
            //Person person = bank.getLocalPerson(args[2]);
            if (person == null) {
                if (!bank.createPerson(args[0], args[1], args[2])) {
                    System.out.println("Can't create person");
                    return;
                }
                person =  bank.getRemotePerson(args[2]);
                //person = bank.getLocalPerson(args[2]);
            } else if (!person.getName().equals(args[0]) && person.getSurname().equals(args[1])) {
                System.out.println("Passport number is already registered for other person");
                return;
            }

            person.setAmount(args[3], Integer.parseInt(args[4]) + person.getMoney(args[3]));
            //bank.updatePerson((LocalPerson)person);
            System.out.println(("Name: " + person.getName() + "\nSurname: " + person.getSurname() + "\nPassport number: " + person.getPassportNumber() + "\nAccount: " + args[3] + "\nMoney: " + person.getMoney(args[3])));
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
