# Scan Editor

The Scan Editor is a tool to visually build and execute scan commands. 
A new Scan Editor window is created with menu "File - New Scan Editor".
It can be saved and and loaded with the standards "Open" and "Save" buttons in the toolbar. 
After the editor is configured, its execution can be triggered with the 
'Run' button, and aborted with the 'Abort' button.

As the controls are edited, the corresponding shell commands to perform the scan is displayed in the 
"Shell Command" text box, in the bottom of the panel. This command can be copied and
executed in the shell, or in a script. 

The edition starts by selecting the type of scan in the "Scan Type" combo box:

- __Linear__: one or multiple positioners move together linearly (having the same number of steps).
- __Multidimensional__: each positioner corresponds to one scan dimension. The first one is the slower changing dimension.
- __Vector__: one or multiple positioners follow the positions given by the "Vector" table.
- __Continuous__: linear scan, with the positioner executing a continuous move, and sampling the sensors on the fly. 
  The positioner must have a speed attribute (e.g. motor devices).
- __Time series__: sampling sensors on fixed time intervals.
- __Change event series__: sampling sensors when one of the triggers change. 
    - If no trigger is defined, and sensors contain stream devices, uses the stream as trigger.
    - If no trigger is defined,  and sensors does not contains stream devices, set all sensors as triggers.
    - If "Synchronous sampling" is not set, only cached values are sampled (no blocking read).  

Linear, Multidimensional and Continuous scans have the notion of scan range 
for each positioner (start, stop, steps). 
For the other scan types these fields are disregarded.
If scan ranges are used, the check box "Use number of steps" toggles the Positioners table
between configuring step size or number of steps for the positioners.

Devices types:

  - __Device__: uses device from the global device pool. Name field contains the device name only.
  - __Channel__: creates an inline device: EPICS channel, with the channel name given by the name field.  
  - __Stream__: creates an inline device: BSREAD channel, with the channel name given by the name field.  
  - __CamServer__: creates an inline device: reads data from a stream image server. 
    Name field contains the server locator string, or the stream URL.



If options are present, the channel name field follows the following format:
  - channel_name?option1=value2&option1=value2&...

For information in inline device option values, see __Devices__, section __Inline devices__.