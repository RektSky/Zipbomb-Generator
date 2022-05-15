import ml.rektsky.zipbomb.ZipBombGenerator;
import sun.misc.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.*;
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

        ZipBombGenerator zipBombGenerator = new ZipBombGenerator.Builder(index -> "T-" + index + ".class", (short) (Short.MAX_VALUE - 1), Integer.MAX_VALUE - 2)
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
            System.out.println(jarEntry.getName());
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            fileCount++;
        }


        ZipInputStream input = new ZipInputStream(Files.newInputStream(Paths.get("zipbomb.zip")));
        ZipEntry entry = input.getNextEntry();
        while (entry != null) {
            if (!entry.getName().equals("FakedEntryName")) {
                throw new AssertionError("Entry name isn't hidden from ZipInputStream. Expected: \"FakedEntryName\", but got \"" + entry.getName() + "\"");
            }
            entry = input.getNextEntry();
        }

        if (fileCount != Short.MAX_VALUE) {
            throw new AssertionError("Files count is not same as requested files count  (Expected: " + Short.MAX_VALUE + "  but got  " + fileCount);
        }

    }

}
