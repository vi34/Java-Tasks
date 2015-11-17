package ru.ifmo.ctddev.shatrov.bank;


import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class BankImpl implements Bank {
    private final ConcurrentHashMap<String, Person> persons = new ConcurrentHashMap<String, Person>();

    public BankImpl() {
    }

    @Override
    public Person getRemotePerson(String passport) throws RemoteException {
        if (!persons.containsKey(passport)) {
            return null;
        }
        return persons.get(passport);
    }

    @Override
    public LocalPerson getLocalPerson(String passport) throws RemoteException {
        if (!persons.containsKey(passport)) {
            return null;
        }
        return new LocalPerson(persons.get(passport));
    }

    public void updatePerson(LocalPerson person) {
        persons.put(person.getPassportNumber(),person);
    }

    @Override
    public boolean createPerson(String name, String surname, String passportNumber) throws RemoteException {
        try {
            if (!persons.containsKey(passportNumber)) {
                persons.put(passportNumber, new RemotePersonImpl(name, surname, passportNumber));
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            return false;
        }
    }

}
