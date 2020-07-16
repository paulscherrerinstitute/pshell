# Overview

PShell is a scripting environment for experiments developed at PSI.

The latest stable build (pshell-<version>-fat.jar) can be download from [here](https://github.com/paulscherrerinstitute/pshell/releases).



# Requirements

The only requirement for running PShell is Java: 

 * Version from 1.15 onward require Java 11 or superior. 
 * Versions up to 1.14 require Java 8 or superior. 

The JDK is needed if using dynamic plugins (compiled on-the-fly). 

Netbeans is required for visually editing graphical plugins.

 * Version from 1.15 onward require Netbeans 11.3 or superior. 
 * Versions up to 1.14 require Netbeans 8 or superior. 



# Building

The JAR file pshell-<version>-fat.jar can be built executing:
 ```
 ./gradlew build
 ```  

After executing the build command, the file pshell-<version>-fat.jar is located in the folder  ./build/libs. 



# Launching

Launch the application typing:
 ```
 java -jar pshell-<version>-fat.jar <startup options...>
 ```  
 
Or else just  double-click the jar file, if the  system has an automatic java application launcher. 

 __Note__: The optional parameter  __-home=[path]__ sets the home folder - the default folder for configuration, context, plugins and data. If this parameter is omitted, it is set to __./home__. In the first run the default folder structure is created. Individual paths can be set in {home}/config/setup.properties.

 __Note__: Check the help contents for the other startup options.



# Help

[PShell Help Contents](./src/main/assembly/help/) are also available in the application, menu "Help".



