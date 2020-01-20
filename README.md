# cpsolver-itc2019
ITC2019: Using UniTime Course Timetabling solver for the [ITC 2019](https://www.itc2019.org) problem

## Compilation
Use Maven to compile the project:

```
mvn dependency:copy-dependencies -DoutputDirectory=target
mvn package
```

The first command will download all the dependencies and place them in the target folder. The second command will compile the project and place the resultant cpsolver-itc2019-1.0-SNAPSHOT.jar in the target folder.

## Running
Run the solver using the following command:

```
java -jar target/cpsolver-itc2019-1.0-SNAPSHOT.jar [configuration] [input file] [output dir]
```

Where
- **configuration** is the solver configration file (for instance, see [default.cfg](../master/configuration/default.cfg) for the default configuration file)
- **input file** is the ITC 2019 competition instance file (e.g., wbg-fal10.xml)
- **output** is the output folder where the solution file (and other files) will be stored

For example:

```
java -jar target/cpsolver-itc2019-1.0-SNAPSHOT.jar \
  configuration/default.cfg \
  instances/wbg-fal10.xml \
  ./output
```
