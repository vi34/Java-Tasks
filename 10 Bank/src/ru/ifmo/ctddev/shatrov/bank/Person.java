package ru.ifmo.ctddev.shatrov.bank;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public interface Person {

    public String getName() throws Exception;

    public String getSurname() throws Exception;

    public String getPassportNumber() throws Exception;

    public ConcurrentHashMap<String, Account> getAccounts() throws Exception;

    public int setAmount(String accountId, int amount) throws Exception;

    public int getMoney(String key) throws Exception;
}
