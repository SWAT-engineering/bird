cd ..
mvn package -Drascal.compile.skip -Drascal.tutor.skip -DskipTests
cp bird-core/target/bird-core-*.jar bird-extension/assets/jars/bird-core.jar
cp bird-ide/target/bird-ide-*.jar bird-extension/assets/jars/bird-ide.jar
cp bird-core/target/lib/typepal.jar bird-extension/assets/jars/typepal.jar
