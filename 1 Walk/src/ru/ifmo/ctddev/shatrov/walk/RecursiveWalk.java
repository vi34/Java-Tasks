package ru.ifmo.ctddev.shatrov.walk;


import java.io.*;
import java.nio.file.*;

/**
 * Created by vi34 on 17.02.15.
 */
public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Incorrect args");
            return;
        }
        String input = args[0];
        String output = args[1];
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(input), "UTF-8");
             BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(output), "UTF-8")) {
                String dir;
                while ((dir = bufferedReader.readLine()) != null) {
                    try {
                        walk(new File(dir), writer);
                    } catch (IOException e) {
                        System.err.println("Error writing to file " + output);
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("Can't create output file");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("input file not found");
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void walk(File file, Writer writer) throws IOException {
        int hash = 0;
        boolean write = true;
        try {
            if (file.isFile()) {
                try (InputStream is = new FileInputStream(file)) {
                    hash = calculateHash(is);
                } catch (IOException e) {
                    System.err.println("error while reading file");
                }
            } else if (file.isDirectory()) {
                write = false;
                try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(file.getPath()))) {
                    for(Path path: stream) {
                        walk(path.toFile(), writer);
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        } finally {
            if(write) {
                writer.write(String.format("%08x", hash) + " " + file.getPath());
                writer.write("\r\n");
            }
        }
    }

    private static int calculateHash(InputStream is) throws IOException {
        int hash = 0x811c9dc5;
        int count;
        byte[] buff = new byte[1024];
        while((count = is.read(buff)) >= 0) {
            for (int i = 0; i < count; i++) {
                hash = (hash * 0x01000193) ^ (buff[i] & 0xff);
            }
        }
        return hash;
    }
}
