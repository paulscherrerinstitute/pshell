# FAQ



## Configuration

 * How to start execution choosing the config file __config.properties__ from the command line?

    - Copy setup.properties to a new file, edit the new setup entry "configFile" to the
      desired config file, and start the application with -setp pointing to the new setup file.

 * What it the URL of the web interface?

    - Check in "Help-Setup" menu, item "Server URL". If field is empty, then the server is not enabled.

---
## Devices

 * Can I add a pseudo-device, or a device instantiated in a script,  to the device pool and view it
   in the __Devices__ panel?

    - Yes, by calling:
        ```
        add_device(s1, force=True)
        ```
      This will add the device to the global device pool. The device name will be set to a global variable, it 
      will be highlighted when editing scripts, and  it will be visible in __Devices__ panel.
      The name of a device in the device pool must be unique. 
      If the name already exists and __force__ is false, then an exception will be thrown. If __force__
      is true then the former device will be disposed.


---
## Scripting

 * How to detect in a script if  running in local mode (non-exclusive), and if the server is enabled?

    - With:
        ```
        get_context().localMode
        get_context().serverEnabled
        ```

 * How to define script setup / cleanup callbacks? This can simplify script flow, so that try/finally
   blocks are not always needed to restore initial state.

    - Enable "Command Execution Events" in the configuration dialog.
    - Define in local.py the handlers for command execution start and/or finish:
        ```
        def on_command_started(info):
            ...

        def on_command_finished(info):
            ...
        ```
      The 'info' parameter is a CommandInfo object (see section "Utility Classes").

---
## Scans

 * I am having out of memory exceptions during my scans. What can I do?

    - PShell can have out of memory exceptions when scanning big arrays because scan data is kept 
      in memory. Possible solutions:
        1. Increase java heap size, using the java -Xmx option in the startup command.
           E.g. use "java -Xmx6G -jar pshell*.jar" to set the heap size to 6G.
        2. The default behaviour is to keep in memory all scan records, which are returned
           in the end of the scan. This can be disabled, for the running script, with:
        ```
        set_exec_pars(keep = False)
        ```
        3. In the workbench the scan data is sent to the plot window and also to the table in the 
           "Scripts" tab, consuming memory. Plotting and table displaying can be disabled for the
            running script with:
        ```
        set_preference(Preference.PLOT_DISABLED,True)
        set_preference(Preference.TABLE_DISABLED,True)
        ```
    - Optionally the options above may be provided inline in the scan command. In this case they are
      valid only for the running scan, and not for the following. For example: 
        ```
        lscan (pos, sensor, start, end, steps, keep = False, plot_disabled = True, table_disabled = True)
        ```
        

 * How can I disable data persistence when executing test scripts?

    - Include the following command before the scan.
        ```
        set_exec_pars(persist = False)
        ```
    - Include  the option inline in the scan command:
        ```
        scan (pos, sensor, start, end, steps, persist = False)
        ```

 * What parameters are received by the scan callback functions __before_read__ and __after_read__?

    - __before_read__ receives 2 optional parameters:
        1. __position__: a list of the writable positions for the current scan step.
        2. __scan__: a reference to the current scan object, which can be used to retrieve scan state information.
    - __after_read__ receives 2 optional parameters:
        1. __record__: the current scan record (see Utility Classes/ScanRecord)
        2. __scan__: a reference to the current scan object, which can be used to retrieve scan state information.

 * My positioner must verify the completion of the command by checking a certain channel. How can it be done?
    - Many syntaxes are possible, the most compact: 
        ```
        positioner = ControlledVariable("positioner", "SETPOINT_CHANNEL_NAME", "READBACK_CHANNEL_NAME")        
        positioner.initialize()
        positioner.setSettlingCondition(ChannelSettlingCondition("MOVING_FLAG_CHANNEL_NAME", 0))
        positioner.settlingCondition.latency = 100          #some delay before waiting for settling condition
        ```
    - More explicitly, using SettlingCondition:
        ```
        positioner = ControlledVariable("positioner", "SETPOINT_CHANNEL_NAME", "READBACK_CHANNEL_NAME")        
        positioner.initialize()
        class MySettlingCondition(SettlingCondition):
            def doWait(self):
                    time.sleep(0.1)
                    cawait('MOVING_FLAG_CHANNEL_NAME', 0)    
        positioner.setSettlingCondition(MySettlingCondition())            
        ```
    - Using pseudo-devices, e.g:
        ```
        class Positioner(Writable):
            def write(self, value):
                caput ("SETPOINT_CHANNEL_NAME", value)
                time.sleep(0.1)
                cawait('MOVING_FLAG_CHANNEL_NAME', 0)    
        positioner = Positioner()
        ```
        
 * My positioner don't have readback values in different scales than setpoints. 
   How can I make the scan not to wait the readback reach the setpoint.
    - ControlledVariable and Positioner devices can have the  in-position band checking disabled during 
      scans  setting the resolution to infinity
        ```
        positioner.config.resolution = float('inf') 
        ```   

 * Scans assume types and shapes of arrays are known previously, so tables can be created before the scab begins.  
   If the types or shapes of readables can only br defined upon the first sampling then 
   the scan data won't be correctly saved. How this can be solved?

    - This scan should do a lazy creation of tables. 
      It can be set as the default option in the "Data Setup" window, tab "Scans".      
      Or it can be set for a specific scan including the option 'lazy' in the scan command:
        ```
        scan (pos, sensor, start, end, steps, lazy = True)
        ```

---
## Versioning

 * Is it possible to add another folder to the repo 
   (in addition to the standard ones: __config__, __devices__, __plugins__ and __scripts__)?

    - Yes, if it is located under the {home} folder:

        1. Edit {home}/.gitignore and add a line to remove the desired folder from the ignore list: 
        ```
        !/my_folder_name
        ```
        2. In next restart the new folder items can be seen from the Repository Changes dialog.
        3. The new folder can then be added to the repo by executing in the console:
        ```
        c.getVersioningManager().add("my_folder_name")
        ```

 * How often should I call __cleanup_repository()__?

    - This call permorms a "git gc", which removes unneeded objects from the repo. 
      The repository use is not expected not to be intensive, so it could be called just once an year 
      (or no called at all). For more information see: https://git-scm.com/docs/git-gc .

 