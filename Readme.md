# Overview

PShell is a scripting environment for experiments developed at PSI.

The latest stable build (pshell-<version>-fat.jar) can be download from [here](https://github.com/paulscherrerinstitute/pshell/releases).



# Requirements

The only requirement for running PShell is Java: 

 * Version from 2.0 onwards require Java 21 or superior. 
 * Version from 1.15 to 1.* require Java 11 or superior. 
 * Versions up to 1.14 require Java 8 or superior. 

The JDK is needed if using dynamic plugins (compiled on-the-fly). 

Netbeans is required for visually editing graphical plugins.

 * Version from 2.0 onwards require Netbeans 25 or superior. 
 * Version from 1.15 to 1.*  require Netbeans 11.3 or superior. 
 * Versions up to 1.14 require Netbeans 8 or superior. 

 __Note__: Version 1.x of this project is available on the v1 branch. Version 2.0 introduces major architectural changes and is not backward compatible.


# Building

The JAR file pshell-<version>-fat.jar can be built executing:
 ```
 ./gradlew :pshell:build
 ```  

After executing the build command, the file pshell-<version>-fat.jar is located in the folder  ./pshell/build/libs. 



# Launching

Launch the application typing:
 ```
 java -jar pshell-<version>-fat.jar <startup options...>
 ```  
 
Or else just  double-click the jar file, if the  system has an automatic java application launcher. 

 __Note__: Check the help contents for the other startup options.


# Utilities


Starting with version 2, PShell os organized in sub-projects, with indifidual build of RPMs and fat jars so that utilieitew can be installed intependently.


| Utility          | Description |
|------------------|-------------|
| ArchiverViewer   | Archiver data retrieval and plotting using DaqBuf service.| 
| CSM              | CamServer Manager - configuration and monitoring of CamServer.| 
| DataViewer       | HDF5 and text file browser.| 
| FDA              | Port of FDA applicatin to PShell app framework.| 
| Plotter          | PShell plotting server - used by PyScan and Datahub.| 
| ScreenPanel      | CamServer image client - image rendering, data saving, metrics and metadata displaying.| 
| StripChart       | History plots of EPICS channels, devices and streams (BSREAD, CamServer).| 
| Console          | The PShell command line interface and server.| 
| Workbench        | The PShell IDE.| 

 __Note__: PShell is an aggegator project, pshell-\<version\>-fat.jar can launch any of the utilities.


# Help

[PShell Help Contents](./workbench/src/main/assembly/help/) are also available in the application, menu "Help".



