# Plugin Management

The plugin configuration window (menu "File", "Plugins") provide controls for plugin management 
and also for the creation of dynamic Plugins. 

Plugins inherits an API providing access to PShell features and receiving events
from the application. 

There are two types of plugins:

 * Dynamic plugins are .java files compiled on-the-fly, as needed.
 * Static plugins are packaged in .jar files.


Both plugins types should be put in the plugin folder (default is "./home/plugins"). They are individually enabled 
in the plugin configuration window, tab "Enabling". The plugins that are correctly loaded are displayed in the "Loaded" tab.
If an enabled plugin is not correctly loaded then the information about the error can be found in the system logs.

Two kinds of dynamic plugins can be created in the plugin configuration window:

 * Standard: They can implement logical customizations (non-GUI related), or else freely customize any aspect of the GUI.
 * Panel: A plugin that add one panel as a tab on the document pane. It is the suggested way to implement
   a single use case, simplifying final user access to scripts. 

Newly created plugins are saved to the plugins folder and are enabled by default. 
After creation, the new plugins can be immediately be loaded the GUI:

 * In "Enabling" tab click on the plugin name and press "Load".

A plugin can be configured to be loaded at startup. 
  
 * On "Enabling" tab, set the "Enabled" checkbox.
 * Save the new plugin configurtation (button "Save").
 * Reload plugins ("Loaded" tab, button "Reload All" button).

Panel plugins should be edited with Netbeans. Every time the plugin changes, it can be dynamically updated on
PShell by clicking the "Reload All" button, or else by typing the __:reload__ control command.

<br>
In order to properly edit Panel plugins within Netbeans:
 1. Create a new Netbeans project (Java, class library), saving it anywhere.
 2. In the project properties, "Library", add pshell*.jar as a library.
 3. In the project properties, "Source", remove all Package Folders. Then add a folder pointing to the PShell
    plugin folder, entering the Label of preference (e.g. "Plugins").
 4. In the project properties, "Source", click "Includes/Excludes" , enter "*.java" in "Includes" field 
    and press "OK".
 5. Now the plugins folder can be properly viewed, and the Panel plugins edited. 
 6. The "Pallete" toolbar can be customized, including components from PShell project, such as __MotorPanel__, 
    __MotorReadoutPanel__, __ProcessVariablePanel__ and __DiscretePositionerPanel__. 
    These components cn then be dragged to the panel.
 
    Also PShell plots may be included in the toolbar: __LinePlotJFree__, __TimePlotJFree__, and __MatrixPlotJFree__.
<br>



# Plugin API
 * Properties and access methods
    - String getPluginName() 
    - File getPluginFile() 
    - boolean isStarted() 
    - State getState() 
    - Logger getLogger()
    - Context getContext()
 * Overridable callbacks
    - void onStart()
    - void onStop()
    - void onStateChange(State state, State former)
    - void onExecutedFile(String fileName, Object result)
    - void onInitialize(int runCount)
    - void onUpdatedDevices() 
    - void onStoppedDevices() 
 * Script and statement eval
    - Object eval(String str)
    - Object eval(String str, boolean background)
    - CompletableFuture<?> evalAsync(String str)
    - CompletableFuture<?> evalAsync(String str, boolean background)
    - Object run(String scriptName)
    - Object run(String scriptName, Object args) 
    - CompletableFuture<?> runAsync(String scriptName)
    - CompletableFuture<?> runAsync(String scriptName, Object args) 
    - CompletableFuture<?> runBackground(String scriptName)
    - void abort()
 * Scripting tools
    - void setGlobalVar(String name, Object val) 
    - void setGlobalsVars(HashMap<String, Object> vars)
    - void injectVars()
    - void setPreference(CommandSource source, ViewPreference name, Object value)
 * Background Task management
    - void startTask(String scriptName, int delay)
    - void startTask(String scriptName, int delay, int interval)
    - void stopTask(String scriptName)
    - void stopTask(String scriptName, boolean force)
 * Device management and access
    - Device getDevice(String name)
    - boolean addDevice(Device device) 
    - boolean removeDevice(Device device) 
    - void updateAll()
    - void stopAll()
 * MainFrame (View) access helper methods:
    - App getApp()  
    - View getView() 
    - Frame getTopLevel() 
    - DevicePanel showDevicePanel(String name)
    - DevicePanel showDevicePanel(Device device)
    - Renderer showRenderer(String name)
    - Renderer showRenderer(Source source)
    - ConfigDialog showDeviceConfigDialog(GenericDevice device, boolean readOnly)
    - void showWindow(Window window)
    - void closeWindows()
    - Window[] getWindows()
    - void showException(Exception ex)
    - void sendOutput(String str)
    - void sendError(String str)
 * Panel Plugin additional API
    - String getTitle() 
    - void startTimer(int interval) 
    - void startTimer(int interval, int delay)
    - void stopTimer()
    - void update(): Request graphical update in the event loop, filtering sequential calls.
    - protected void doUpdate(): Implement the graphical update.
    - protected void onTimer()
    - protected String getContextPath()
    - protected void setPersistedComponents(Component[] components)
    - protected Component[] getPersistedComponents()
    - protected Path getComponentsStatePath()
    - protected void clearComponentsState()
    - protected Path getWindowStatePath()
    - protected void saveWindowState()
    - protected void loadWindowState()
    - protected void saveComponentsState()
    - protected void loadComponentsState()
    - protected void setBackgroundUpdate(boolean value)
    - protected boolean getBackgroundUpdate()
<br>


When Plugins invoke script execution from the event dispatch thread, the call should be 
always asynchronous so that the interface will not freeze.

As the async methods in the API return CompletableFuture objects, it is very simple to 
define a callbacks to the successful end of execution of scripts:


```
    runAsync("MyScript", args).thenAccept((ret) -> {                
        //Called in the end of execution, if successful
        //ret holds the value set by the script with the set_return function.
    });
```


Or else having a callback to both success end failure:


```
    runAsync("MyScript", args).handle((ret, ex) -> {
        //Called in the end of execution
        if (ex != null){
            //If an exception has taken place: ex is exception thrown by the script
        } else {
            //Successful execution: ret is the value set by the script with set_return function
        }
        return null;
    });  
```

