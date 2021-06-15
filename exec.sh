mvn compile package -pl '!native' && $JAVA_HOME/bin/java \
    -cp launcher/target/launcher-20.3.0.jar \
    -Dtruffle.class.path.append=language/target/simplelanguage.jar \
    com.oracle.truffle.sl.launcher.SLMain examples/println.cal
