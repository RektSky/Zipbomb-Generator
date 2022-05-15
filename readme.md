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