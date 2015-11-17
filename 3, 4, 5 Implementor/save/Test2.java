package ru.ifmo.ctddev.shatrov.implementor;

import java.io.File;
import java.io.IOException;

/**
 * Created by vi34 on 03.03.15.
 */
public abstract class Test2 {
    int a1, a2;
    private boolean b1;
    protected char c = '1';
    Test2(int a) throws IOException{
        a1 = a;
    }
    private Test2(boolean b)  {}
    protected Test2(char c) {}

    private void func1(int a) {
        a++;
    }
    protected abstract boolean func2(boolean b);
    public abstract void func3(int a);
    public final void fun4(int a) {}
    abstract int ff(File f);
}
