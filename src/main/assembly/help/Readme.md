# Overview

PShell is a scripting environment for experiments developed at PSI.

<br>


# Installation

The only requirement for running PShell is Java 8 JRE. 
Java 8 JDK is needed if using dynamic plugins (compiled on-the-fly). 
Netbeans 8 is required for visually editing graphical plugins.

 1. Download or build the JAR file pshell-*.jar

 2. Type:
 ```
 java -jar pshell-*.jar [startup options...]
 ```  
 
 Or else just  double-click the jar file, if the  system has an automatic java application launcher. 


 __Note__: The optional parameter  __-home=[path]__ sets the home folder - the default folder for configuration, 
context, plugins and data. If this parameter is omitted, it is set to __./home__. 
In the first run the default folder structure is created. Individual paths can be set in {home}/config/setup.properties.

 __Note__: Check other command-line parameters in the session __Startup Options__.


