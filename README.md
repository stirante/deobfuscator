## Deobfuscator
Simple apk deobfuscation based on logging.

### Building
    git clone https://github.com/stirante/deobfuscator.git
    cd deobfuscator
    ./gradlew fatJar
(on Windows, use `gradlew.bat` instead of `./gradlew`)

### Usage
    cd build/libs/
    java -Xmx1024M -jar deobfuscator-all-*.jar path/to/file.apk
    
##### Heavily depends on JADX by Skylot
