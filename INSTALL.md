```
cd ..
git clone https://github.com/streamblocks/streamblocks-tycho.git
cd streamblocks-tycho
export JAVA_HOME=<path to GraalVM>
export PATH=$PATH:$JAVA_HOME/bin
mvn -DskipTests install
cd ../streamblocks-graalvm
gu install native-image
```