package ru.ifmo.ctddev.shatrov.implementor;


import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public abstract class Test3 extends Test2 {

    Test3(int a, boolean b) throws IOException, RuntimeException {
        super(a);
    }

    abstract void secFunc(int a);
    public abstract void thirdf() throws RuntimeException, IOException;

}
