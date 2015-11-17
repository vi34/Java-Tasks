package ru.ifmo.ctddev.shatrov.bank;

public class AccountImpl implements Account {

    private String id;
    private int amount;

    public AccountImpl(String id) {
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
