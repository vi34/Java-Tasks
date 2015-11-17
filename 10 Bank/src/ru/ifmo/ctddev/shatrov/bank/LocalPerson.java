package ru.ifmo.ctddev.shatrov.bank;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class LocalPerson implements Person, Serializable {

    private ConcurrentHashMap<String, Account> accounts;
    private String name;
    private String surname;
    private String passportNumber;

    public LocalPerson(String name, String surname, String passportNumber) {
        this.name = name;
        this.surname = surname;
        this.passportNumber = passportNumber;
        accounts = new ConcurrentHashMap<String, Account>();
    }

    public LocalPerson(Person p) {
        try {
            name = p.getName();
            surname = p.getSurname();
            passportNumber = p.getPassportNumber();
            accounts = p.getAccounts();
        } catch (Exception e) {
            name = "";
            surname = "";
            passportNumber = "";
            accounts = new ConcurrentHashMap<String, Account>();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassportNumber() {
        return passportNumber;
    }

    @Override
    public ConcurrentHashMap<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    public int setAmount(String accountId, int amount) {
        Account account = getAccount(accountId);
        account.setAmount(amount);
        accounts.put(accountId, account);
        return amount;
    }


    @Override
    public int getMoney(String accountId) {
        return getAccount(accountId).getAmount();
    }

    private Account getAccount(String accountId) {
        if (!accounts.containsKey(accountId)) {
            accounts.put(accountId, new AccountImpl(accountId));
        }
        return accounts.get(accountId);
    }
}
