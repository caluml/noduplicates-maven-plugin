## Plugin to stop a build if there are artifacts with multiple versions

### Usage
First add to your local Maven repository<br>
```mvn clean install```

Then add to your pom.xml
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.caluml</groupId>
            <artifactId>noduplicates-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>checkduplicates</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <scope>compile</scope>
            </configuration>
        </plugin>
    </plugins>
</build>
```