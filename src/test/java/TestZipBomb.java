import ml.rektsky.zipbomb.ZipBombGenerator;
import sun.misc.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestZipBomb {

    public static void main(String[] args) throws Throwable {
        // Lazy to do Junit, forgive me
        OutputStream outputStream = Files.newOutputStream(Paths.get("zipbomb.zip"));

        ZipBombGenerator zipBombGenerator = new ZipBombGenerator.Builder(index -> "T-" + index + ".class", 100000, Integer.MAX_VALUE - 2)
                .fakeEntryName("FakedEntryName")
                .build();
        Map<ZipEntry, byte[]> files = new HashMap<>();
        FileInputStream fileInputStream = new FileInputStream("Main.class");
        files.put(new ZipEntry("Main.class"), IOUtils.readNBytes(fileInputStream, Integer.MAX_VALUE));
        byte[] bytes = zipBombGenerator.create(files);
        outputStream.write(bytes);
        outputStream.close();

        JarFile jarFile = new JarFile("zipbomb.zip");
        Enumeration<JarEntry> entries = jarFile.entries();
        int fileCount = 0;
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            fileCount++;
        }


        ZipInputStream input = new ZipInputStream(Files.newInputStream(Paths.get("zipbomb.zip")));
        ZipEntry entry = input.getNextEntry();
        int index = 0;
        while (entry != null) {
            index++;
            if (!entry.getName().equals("FakedEntryName") && index == 2) {
                throw new AssertionError("Entry name isn't hidden from ZipInputStream. Expected: \"FakedEntryName\", but got \"" + entry.getName() + "\"");
            }
            if (index == 3) break;
            entry = input.getNextEntry();
        }

        if (fileCount != 100001) {
            throw new AssertionError("Files count is not same as requested files count  (Expected: 100001  but got  " + fileCount);
        }
        URLClassLoader loader = new URLClassLoader(new URL[] {new File("zipbomb.zip").toURI().toURL()});
        loader.loadClass("Main").getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        System.out.println("ZipBomb test has passed!");

    }

}
