package ru.ifmo.ctddev.shatrov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;

/**
 * Created by vi34 on 04.03.15.
 */
public class MyTester {
    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        try {
            Class c = Class.forName("ru.ifmo.ctddev.shatrov.implementor.Test1");
            implementor.implement(c, new File("./src"));
        } catch (ImplerException e) {
            System.err.println("Impler exception");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
