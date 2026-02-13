# Key Cylinder Roles Mapper
This project is meant to compare CSV and Excel documents of key-cylinder permissions while supporting Excel documents containing role mappings.

The implementation is mainly focussed on my personal needs in the small municipality Rethwisch in northern Germany but can be extended if requested.

## Getting Started

### Building Sources
The project uses Maven and Project Lombok. First, make sure to install Project Lombok into your IDE, then most IDEs should be able to import the Maven project with a glance.

On the shell the sources of this project can be built using `mvn clean install`. To execute that command Project Lombok does not need to be installed.

### Execution
This project's Command Line Interface (CLI) comes with a built-in help. In your IDE you only need to start the class `de.larssh.keycylinderroles.mapper.cli.KeyCylinderRolesMapperCli`.

As long as Maven is installed on your machine and you executed `mvn install` before, you can also execute the following shell command:

```
mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=de.lars-sh.keycylinderroles.mapper:key-cylinder-roles-mapper:LATEST -DmainClass=de.larssh.keycylinderroles.mapper.cli.KeyCylinderRolesMapperCli
```
