# ZipBomb Generator in Java

This library add all entries you want it to add, and make it a zipbomb + semi-invalid zip file as a (very) basic 
protection layer to protect your software from being cracked.

## Features
#### **`ZipInputStream` File Name Faker**
> Fakes the file name from ZipInputStream. There are some deobfuscator uses this, and I'm talking to
> you - you are probably one of them who use this to code deobfuscation transformers.
> 
> It also hides file name from these following archive managers:
>  - 7-zip
> 
> But tt doesn't work from these following archive managers:
>  - `unzip` Linux command
>  - WinRAR

#### **ZipBomb**
> It crashes most reversing tools. There are some tested tools:
>  - JBytemod
>  - JBytemod Remastered
>  - Recaf
>  - Bytecode Viewer
>  - Threadtear
> 
> Tools that's tested and working:
>  - None

#### **Semi Corrupted Zip File**
> Crashes some archive manager. There are all tested archive managers:
>  - `zip` Linux Command, tested with `zip -T`, results in `zip error: Nothing to do! (zipbomb.zip)`
>  - Ark (KDE's default archive manager) - crashes
>  - Gnome's Archive Manager - crashes
>  - Window's File Explorer - crashes
> 
> Archive Managers that's tested and doesn't work:
>  - WinRAR
>  - 7-zip, but the file name faker works
>  - `unzip` Linux Command



## Use cases
This is obviously for obfuscation and educational purposes only, and I'm dead serious, using this
to do bad stuff is actually very stupid, and you shouldn't be doing it under any condition.

Here are some use cases:
1. Learn how the code works
2. Protect your Java programs

## Usage
1. Clone this repository, and `mvn clean package install`
2. Add this to `pom.xml`:
```xml
<dependency>
    <groupId>ml.rektsky</groupId>
    <artifactId>zipbomb-generator-java</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency> 
```
3. Example Usage:
```java

public class Main {
    public static void main(String[] args) {
        OutputStream outputStream = Files.newOutputStream(Paths.get("zipbomb.zip"));
        // Create an instance with custom configuration
        ZipBombGenerator zipBombGenerator = new ZipBombGenerator.Builder(index -> "T-" + index + ".class", (short) (Short.MAX_VALUE - 1), Integer.MAX_VALUE - 2)
                .fakeEntryName("FakedEntryName")
                .build();

        Map<ZipEntry, byte[]> files = new HashMap<>();
        // Add a file to the zip file
        // You can also read all entries from a zip file using ZipInputStream
        FileInputStream fileInputStream = new FileInputStream("Main.class");
        files.put(new ZipEntry("Main.class"), IOUtils.readNBytes(fileInputStream, Integer.MAX_VALUE));

        // Zipbomb it, it may take a while to run
        byte[] bytes = zipBombGenerator.create(files);

        // Write the zipbomb output to the file
        outputStream.write(bytes);
        outputStream.close();
    }
    
}
```