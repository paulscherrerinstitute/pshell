# Overview

PShell is a scripting environment for experiments developed at PSI.



# Installation

The only requirement for running PShell is Java 8 JRE. 

 * Java 8 JDK is needed if using dynamic plugins (compiled on-the-fly). 
 * Netbeans 8 is required for visually editing graphical plugins.

 1. Build the JAR file pshell-<version>-fat.jar or download the latest stable build from [here](https://github.com/paulscherrerinstitute/pshell/releases).

    The project can be built executing:
 ```
 ./gradlew build
 ```  
 
 2. Type:
 ```
 java -jar pshell-<version>-fat.jar <startup options...>
 ```  
 
 Or else just  double-click the jar file, if the  system has an automatic java application launcher. 

 __Note__: The optional parameter  __-home=[path]__ sets the home folder - the default folder for configuration, context, plugins and data. If this parameter is omitted, it is set to __./home__. In the first run the default folder structure is created. Individual paths can be set in {home}/config/setup.properties.

 __Note__: Check the help contents for the other startup options.



# Help

[PShell Help Contents](./src/main/assembly/help/) are also available in the application, menu "Help".



