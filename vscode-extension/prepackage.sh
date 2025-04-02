cd ..
mvn package -Drascal.compile.skip -Drascal.tutor.skip -DskipTests
cp bird-core/target/bird-core-*.jar vscode-extension/assets/jars/bird-core.jar
cp bird-ide/target/bird-ide-*.jar vscode-extension/assets/jars/bird-ide.jar
cp bird-core/target/lib/typepal.jar vscode-extension/assets/jars/typepal.jar
