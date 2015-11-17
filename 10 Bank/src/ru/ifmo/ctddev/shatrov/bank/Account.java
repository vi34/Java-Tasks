package ru.ifmo.ctddev.shatrov.bank;

import java.io.Serializable;

public interface Account extends Serializable {

    public String getId();
    public int getAmount();
    public void setAmount(int amount);
}
