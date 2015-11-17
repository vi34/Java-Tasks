package ru.ifmo.ctddev.shatrov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author vi34
 * @version 1.0
 */
public class Implementor implements JarImpler {

    /**
     * Check given args and call {@link #implement(Class, File)} or {@link #implementJar(Class, File)}
     *
     * @param args command line arguments. Needed argument is full name of class which should be implemented.
     */
    public static void main(String[] args) {
        if (args != null && args.length != 0 && args[0] != null) {
            if (args.length == 1) {
                try {
                    new Implementor().implement(Class.forName(args[0]), new File("."));
                } catch (ClassNotFoundException e) {
                    System.err.println("Couldn't open class " + args[0]);
                } catch (ImplerException e) {
                    e.printStackTrace();
                }
                return;
            } else if (args.length == 3 && args[1] != null && args[2] != null && args[0].equals("-jar")) {
                try {
                    new Implementor().implementJar(Class.forName(args[1]), new File(args[2]));
                } catch (ClassNotFoundException e) {
                    System.err.println("Couldn't open class " + args[0]);
                } catch (ImplerException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        System.err.println("Wrong arguments. Usage: <full.class.name> or  -jar <full.class.name> <file.jar>");

    }

    /**
     * Implement specified class or interface {@code aClass}. Creates new directory {@code root} if it isn't exists.
     *
     * @param aClass class to create implementation for.
     * @param root   root directory
     * @throws ImplerException when implementation cannot be generated.
     */

    @Override
    public void implement(Class<?> aClass, File root) throws ImplerException {
        fileImplement(aClass, root);
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>class</tt>.
     * <p>
     * Generated class full name is same as full name of the type token with <tt>Impl</tt> suffix
     * added. </p>
     *
     * @param aClass  class to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException when implementation cannot be generated
     */
    @Override
    public void implementJar(Class<?> aClass, File jarFile) throws ImplerException {
        if (!jarFile.exists()) {
            if (!jarFile.mkdirs()) {
                throw new ImplerException("Cannot create output file");
            }
        }
        File javaFile = fileImplement(aClass, jarFile);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if ((compiler.run(null, null, null, javaFile.getAbsolutePath())) != 0) {
            throw new ImplerException("Error while compilation");
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        String className = javaFile.getAbsolutePath();
        className = className.substring(0, className.lastIndexOf(".java")) + ".class";
        File classFile = new File(className);
        try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFile.getAbsolutePath() + File.separator +
                aClass.getSimpleName() + "Impl.jar"), manifest);
             InputStream inputStream = new BufferedInputStream(new FileInputStream(classFile))) {
            String packageName = aClass.getPackage().getName();
            String name = packageName.replaceAll("\\.", File.separator) + File.separator + jarFile.getAbsolutePath() + File.separator +
                    aClass.getSimpleName() + "Impl.class";
            JarEntry entry = new JarEntry(name);
            entry.setTime(System.currentTimeMillis());
            target.putNextEntry(entry);
            int count;
            byte[] buffer = new byte[1024];
            while ((count = inputStream.read(buffer)) >= 0) {
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } catch (IOException e) {
            throw new ImplerException("Error while writing to jar file");
        }
    }

    /**
     * Returns default value for a given class
     *
     * @param clazz class we get returned value for
     * @return default value of {@code clazz}
     */
    private static Object getDefaultValue(Class clazz) {
        if (clazz.equals(boolean.class)) {
            return false;
        } else if (clazz.equals(void.class)) {
            return "";
        } else if (Object.class.isAssignableFrom(clazz)) {
            return null;
        } else {
            return 0;
        }
    }

    /**
     * Same as {@link #implement(Class, File)} but also returns file with implementation.
     *
     * @param aClass class to create implementation for.
     * @param root   root directory
     * @return file with implementation of {@code aClass}
     * @throws ImplerException when implementation cannot be generated
     */
    private File fileImplement(Class<?> aClass, File root) throws ImplerException {
        File outFile = null;
        if (aClass != null && root != null) {
            boolean check = false;
            for (Constructor c : aClass.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(c.getModifiers())) {
                    check = true;
                    break;
                }
            }
            if (aClass.getConstructors().length > 0) {
                check = true;
            }
            if (Modifier.isFinal(aClass.getModifiers())) {
                check = false;
            }
            if (!check && !aClass.isInterface()) {
                throw new ImplerException();
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile = makeFile(root, aClass)), "UTF-8")) {
                writer.write("package " + aClass.getPackage().getName() + ";\r\n");
                if (aClass.isInterface()) {
                    writer.write("public class " + aClass.getSimpleName() + "Impl implements " + aClass.getSimpleName() + " {\r\n");
                    for (Method m : aClass.getMethods()) {
                        if (Modifier.isAbstract(m.getModifiers())) {
                            writeMethod(m, writer);
                        }
                    }
                    writer.write("\r\n}");
                } else {
                    writer.write("public class " + aClass.getSimpleName() + "Impl extends " + aClass.getSimpleName() + " {\r\n");
                    for (Constructor constructor : aClass.getDeclaredConstructors()) {
                        int modifier = constructor.getModifiers();
                        if (Modifier.isPrivate(modifier)) {
                            continue;
                        }
                        if (Modifier.isTransient(modifier)) {
                            modifier -= Modifier.TRANSIENT;
                        }
                        writer.write(Modifier.toString(modifier) + " " + aClass.getSimpleName() + "Impl(");
                        String args = "";
                        Class[] parameters = constructor.getParameterTypes();
                        if (parameters.length > 0) {
                            args = parameters[0].getTypeName() + " a0";
                        }
                        for (int i = 1; i < parameters.length; ++i) {
                            args += ", " + parameters[i].getTypeName() + " a" + i;
                        }
                        writer.write(args + ")");

                        Class[] exceptions = constructor.getExceptionTypes();
                        if (exceptions.length > 0) {
                            writer.write(" throws ");
                            writer.write(exceptions[0].getTypeName());
                        }
                        for (int i = 1; i < exceptions.length; ++i) {
                            writer.write(", " + exceptions[i].getTypeName());
                        }
                        writer.write("{\r\n super(");
                        if (parameters.length > 0) {
                            writer.write("a0");
                        }
                        for (int i = 1; i < parameters.length; ++i) {
                            writer.write(", a" + i);
                        }
                        writer.write(");\r\n}\r\n");
                    }

                    for (Method m : aClass.getDeclaredMethods()) {
                        if (Modifier.isAbstract(m.getModifiers()) && !Modifier.isPublic(m.getModifiers())) {
                            writeMethod(m, writer);
                        }
                    }
                    for (Method m : aClass.getMethods()) {
                        if (Modifier.isAbstract(m.getModifiers())) {
                            writeMethod(m, writer);
                        }
                    }

                    while (Modifier.isAbstract(aClass.getModifiers())) {
                        aClass = aClass.getSuperclass();
                        for (Method m : aClass.getDeclaredMethods()) {
                            if (Modifier.isAbstract(m.getModifiers()) && !Modifier.isPublic(m.getModifiers())) {
                                writeMethod(m, writer);
                            }
                        }
                    }
                    writer.write("\r\n}");
                }
            } catch (FileNotFoundException e) {
                System.err.println("Can't create output file");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Error writing to file");
            }
        } else {
            System.err.println("Wrong arguments");
        }
        return outFile;

    }

    /**
     * Writes implementation of given abstract method to file of implementation which is being generated.
     * <p>Uses {@link #getDefaultValue(Class)} to get returned value for method</p>
     *
     * @param m      is a specified method
     * @param writer used to write given method to file
     * @throws IOException when error during writing happens
     * @see java.io.Writer
     * @see java.lang.reflect.Method
     */
    private void writeMethod(Method m, Writer writer) throws IOException {
        int modifier = m.getModifiers();
        modifier -= Modifier.ABSTRACT;
        if (Modifier.isTransient(modifier)) {
            modifier -= Modifier.TRANSIENT;
        }
        writer.write(Modifier.toString(modifier) + " " + m.getReturnType().getTypeName() + " " + m.getName());
        writer.write("(");
        Class[] parameters = m.getParameterTypes();
        if (parameters.length > 0) {
            writer.write(parameters[0].getTypeName() + " a0");
        }
        for (int i = 1; i < parameters.length; ++i) {
            writer.write(", " + parameters[i].getTypeName() + " a" + i);
        }
        writer.write(")");
        writer.write(" {\r\nreturn " + getDefaultValue(m.getReturnType()) + ";\r\n}\r\n");
    }

    /**
     * Creates a file for implementation in a root directory.
     * <p>Method creates <tt>.jar</tt> file for implementation with <tt>Impl</tt> suffix.
     * Also creates needed subdirectories in root directory</p>
     *
     * @param root  root directory
     * @param clazz class to create source file for.
     * @return created file
     */
    private File makeFile(File root, Class<?> clazz) {
        String path = clazz.getCanonicalName().replace(".", File.separator) + "Impl.java";
        int k = path.lastIndexOf(File.separator);
        root = new File(root, path.substring(0, path.lastIndexOf(File.separator)));
        root.mkdirs();
        path = path.substring(k + 1);
        return new File(root, path);
    }


}
