# Changelog

## 1.19.0

### Added
 
* Improve MatrixPlotRenderer to better mimic MatrixPlotJFree.

### Changed

* Update to Jython 2.7.3

### Fixed

* Fix first initialization of DummyMotor.


## 1.18.0

### Added
 
* Built-in XScan panels from FDA (XScan variables and code snippets share same global interpreter).

* __xscan__ function in scripting API

* __create_link__ function in scripting API

* Automatically retrieve and save metadata for EPICS channel devices.

* Configuration of visibility of scan panel, output panel, data browser and queue browser.

* Option to open data files in Document Tab.

* Dynamic change of LAF.

* Built-in support to TIFF files.

* Configuration of sub-folder and visible file types in data panel. Support opening generic file types.

* Viridis colormap

* _data_utils__: simple direct creation of TIFF and HDF5 files.


### Changed

* Save preferences as JSON and not bin. 

* Change default home folder to ~/pshell/home (not current folder).

* Update JHDF5 to 19.04.1: support to Apple M1 processor.

### Fixed

* Support to Nasshorn in Java>11


## 1.17.0

### Added
 
* Configuration of file permissions (data, scripts, config and logs).

* Terminal tab.

* Packages (modules containing factorized scripts, devices and plugins).

* MasterPositioner (interpolation of slave motors).

* New scan arguments: monitors, diags, snaps and meta.

* UUID to data file attributes.

* ProviderHDF5 gives access to JHDF5 Writer object.

* Support HDF5 soft links

* Support stack of images on AreaDetector

* Template for processor plugins.

* Option -tbar to add toolbars to detached panels.

* Ability to pause/resume scans and queues.

* Pause/resume control commands.

* Python proxy.

* jeputils.import_py for transparently calling CPython functions.

* Ability to use JEP as scripting engine (-type=cpy): can run directly CPython (and NumPy/SciPy).

### Changed

* Futures for writting/moving/stopping devices run on private threads.

* QueueProcessor extends PanelProcessor so that it can be loaded as a plugin.

* API improvements.

* Web UI layout improvements: data file download, script edition and plots.

* Use jep.SharedInterpreter (instead of jep.Jep) not to run CPython in sub-interpreter.

### Fixed

* Workaround to .class files permissions bug (Jython 2.7.2).

* Web UI plotting now is complete, based on offscreen rendering on server.


## 1.16.0

### Added
 
* Session management.

* SciCat integration.

* expand_path (shortcut to get_context().setup.expandPath) function to scripting API.

* Options to disable logs file in data output.

* {dseq} tag: unique daily index of the run.

* Notification task list.

* Increased number of imported Java builtin classes.

* Automatic size adjustment of Epics arrays (waveforms) according to NORD.

* ImageSource.getTrigger to trigger monitor scans

* Override value-less command line arguments -<arg> with -<arg>=false.

### Changed

* Data Setup dialog includes all configuration for data generation and transfer.

* Split startup.py to overcome limitation of 10kb. 

* Channel objects are now Device and can be added to device pool.

### Fixed

* JEP module reloading with Python3.

* fill_null behaviour in streams.
  
* Many small fixes.


## 1.15.0

### Added
 
* IntelliJ project

* __before_region__ callback in RegionScan.

* Preference to Disable offscreen buffers on plots - better drawing quality on Mac. 

* ScanResult and can be indexed by int (list of records) or device (list of device values for that scan).

* ScanRecord can be indexed also by device.

* __size__ startup option to set the size of main dialog if GUI state is not persisted.

* Script callback for data folder change: __on_change_data_path__

* Options to save scan script and output.

* Facade to ImageJ image measurements.

* Rsync utilities.

* Build option for HDF5 1.10.

### Changed

* Java 11 is required to run PShell (source and bin formats are set to Java 11).

* Jython 2.7.2 - fixing http://bugs.jython.org/issue2403 and https://bugs.jython.org/issue2846. 

* Aliases are not needed for Python devices anymore (as getName can be nor overridden)

* Alias handled by Nameable class.

* Devices avoid creating references to themselves on alias map not to block GC.

* ReadonlyRegisterArray has default getSize (returning size of cache).

* ScanRecord.values is not valid any more: should be replaced by ScanRecord.readables or ScanRecord[device].


### Fixed

* Fixed names of attribute files in LayoutFDA
  
* Channel class closes channel on finalizer.

* Reading array of Boolean type on HDF5 files.


## 1.14.0

### Added

* HistogramGenerator device.

* Command-line option to include items to class path, library path and script path.

* Data.copy method.

* 'fda' file format.

* Highlighting VB files.

* Image threshold operation.

* Saving metadata to TIF files.

* Access to left tab pane.

* ctrl+q accelerator to quit application.

* 'split keyword on scan commands to separate scan passes in multiple tables.

* RPM generation.

* Task queues.

* PanelProcessor: Panel plugins that interact with buttons and menu for execution.

* 'debug'property to exec pars.

* -blaf option as a shortcut for flat&dark laf.

* Ability to hide Main Frame's components with the -hide option.

* Methods for changing user/effective user.

* Data file configuration dialog.

* Menu to display script settings property file.

* Command line argument '-vers' to force versioning in local mode.

* Added keys(), values() and items() methods do Subsctiptable.Mapped interface (e.g: BS stream value).

* Run Next command (Menu Shell->Run Next).

* Configuration of plot fonts.
  

### Changed

* ScanEditor doesn't keep records in memory.

* Block indentation with tab/shift+tab.

* Block commenting with alt+/.

* Allow operations between Data objects if underlying buffer length is different (width and height must match).

* Gradle update.

* Console only display server messages if configured.

* Line plot pointer colors match axis color.

* Set 'File New' as a menu. 

* Reorganize Python client package.

* Button Restart disabled in offline mode.


### Fixed

* Autoscale in step plots.

* Logarithmic Axis.

* AreaDetector simulated data type.

* Text format supports flat folder layout reopening same log file.

* Protected image division by zero.

* Show alias in domain axis.

* When pasting, text tabs are translated into spaces.

* Bug generating series names when 2d data was drawn in line plots.

* Plot bugs on mscan.

* Console location menu in local mode.

* Make Averager & ArrayRegisterStats implement ReadableType so no exception if preserving types.

* Execution of processors in command line.



## 1.13.0

### Added

* Parallel device initialization (-pini option).

* StreamCameraViewer.

* Camera calibration dialog.

* Configuration of default separator on CSV format.

* MainFrame.searchIcon method.

* FlatLaf look and feel.

* Possibility to create streams using HeaderReservingMsgAllocator. 

  
### Changed

* Streams save Pulse ID as long, not double.

* Default colors on HistoryChart defined by JFreeChart.

* PipelineServer listener to track config changes.

* BSREAD 4.0.1, using JeroMQ 0.5.1-PSI: compatible again with HeaderReservingMsgAllocator.


### Fixed

* Bug not applying default colors on StripChart.

* Opening FDA files on Windows.

* Bug forking empty Callable list (parallelize and fork built-ins).



## 1.12.0

### Added

* 'invoke' method to execute script functions on the event thread.

* {seq} tag for a sequential identifier for naming data files.

* Context.Debug option - displaying stack trace of exceptions in scripts

* 2d tweak command.

* Configurable device wait interval.

* monitorByPosition option to motor config.

* EPICS Motor kickstart utility cycling modes to GO.

* Ability to reload CPython modules with JEP.

* Persistence of experiment state to diffutils (limits, restrictions, ub)

* MotorGroup.isStartingSimultaneousMove for interlocks differ synchronised and sequential moves.

  
### Changed

* BSREAD version 0.4.0 

* ZMQ version 0.5.1

* Removed reflection call from waitReady.



### Fixed

* Compatibility with JDK > 10 

* Bug pushing null image.



## 1.11.0

### Added

* Utilities script for generating reports from command statistics files (statsutils.py).

* Data queries at DataAPI (access to DataBuffer).

* Included secondary domain axis in plot API.

* Preference to display jog and homing buttons.

* Global map in Context to exchange data between plugins.

* waitState added to Plugin interface.

* Tweak buttons on MotorGroupPanel

* 'tweak'function.

* TimePlot copies text in addition to images.

* Ability to set data and script folders programmatically.

* Motor.setCurrentPosition set the current motor position to a given value.

* Option to restore default speed after Motor or MotorGroup move.

* HKL moves on diffractometer can perform simultaneous motor move 
  (proportional speed).

* ColormapAdapter: convert RGB images into luminance arrays.

  
### Changed

* BSREAD default modulo set to 1.

* Strip chart saves repeated points with different timestamps.

* Adapted to Diffcalc release 2.1


### Fixed

* Stream channel creation failures with addScalar.

* Strip chart dragging issues.

* Scienta zero supplies



## 1.10.0

### Added

* Colormap scale panel on renderer

* CSV file format.

* Option -dplt for creating plots for detached windows.

* vscan supports also generators to provide positioner positions.

* Console location on left.

* Included 'then' execution parameter: sets statement to be executed on the 
  completion of current keeping application state Busy.

* Option for setting HDF5 compression, shuffling nd layout, on data and scan builtin functions.

* Logarithmic colormap for renderer and matrix plot.

* Logarithmic scales on LinePlot and TimePlot.

* Configuration of colormap scale through the popup menu for renderer and MatrixPlot.

* Ability to set the plot layout for a particular scan with the 'plot_layout parameter.'

* EpicsRegisterArray.setSizeToValidElements for setting array size to value given by .NORD field.

* Configuration of Channel range and alarms in StripChart.

* Ability to force rescan of FileSource (source.rescan()).

* Command statistics.

* Configuring CamServer entries in Scan Editor with same syntax as StripChart.

* Selection of coordinate system in renderer status bar.

* Context.evalFileBackgroundAsync.

* Command line option to start Data Panel only (-dtpn).

* -attach option for StripChart: running multiple charts in single process.

* Command line option to start in full screen mode (-full).

* Callbacks to Panel load & unload events.

* ChannelSelector control, searching matching channel names in DataAPI, EpicsBootInfo or Pipeline server.

* Context.setInterpreterVariable.

* Series visibility menu item on TimePlot.

* Command-line option to set default plot background and grid color.

* StripChart configuration of tick label font.

* Ability to set handlers for command execution events (on_command_started/on_command_finished), 
  so that script setup/cleanup can be factorized, and script flow can be simplified, 
  avoiding try/finally blocks in order to restore defaults.
* Setting data format and layout from command line (-dfmt, -dlay options).

* "format" keyword can be used instead of "provider".

  
### Changed

* Undo can restore file state to unchanged (and remove '*' from file tab title).

* Scans save by default original date types in HDF5 (behavior was converting to double by default).

* StripChart type 'Device' can be set with InlineDevice configuration string.

* Builtin 'run' function returns value set by 'set_return' (if called by the script).

* bsget include module, offset and timeout as parameters.

* Improved load time (removed inspect and ctypes from startup.py).

* \_\_name\_\_ set to '\_\_main\_\_' instead of '\_\_builtin\_\_' (as Jython does under Java Scripting API).

* StripChart plot panel made public - so can be added to plugins.

* Configuration of sign bit for process variables.

* DeviceConfig handles Jython classes and dictionary based config - 
  so Config classes can be declared in scripts.

* MjpegSource can be monitored.

* Improvements to Plugin interface.

* 'keep' replaced 'accumulate' keyword.

* Possibility to add/remove devices to the pool without initializing/closing them.

* show_panel supports a Config parameter - opening a config window.


### Fixed

* Errors plotting data from zigzag scans.

* Bug compiling plugins in current folder.

* SFLayout.

* TcpDevice.flush() flushes the socket in addition to clearing the buffer.

* Improved flushing sub-sampled MJPEG streams.

* Message of StripChart saved data file name.

* TimePlotJFree considers size parameter in getSnapshot.

* PShell Python client.

* Correctly returning command IDs for async commands.

* StripChart and history plots supporting boolean devices.

* Only triggers monitor callbacks in PyEPICS wrapper if status is CAStatus.NORMAL.

* Stdout messages when PyEPICS channel is disconnected.

* Error messages persisting state of JTabbedPane

* Initialize speed of simulated motors is set to default.

* Device.setCache(child, value)

* Arr.remove.



## 1.9.0

### Added

* Scan editor dialog (graphical configuration of simple scans).

* __cawait__ can be called with range parameter instead of comparator.
    
* Added support to BigInteger in data persistence.

* Included 'tag' execution parameter to configure scan data root name.

* Included Python client to REST API.

* Configuration of colors and markers in StripChart.

* Offscreen plotting, so that plot commands (and __get_plot_snapshot__) work in server mode.

* Plot snapshots with arbitrary sizes. 

* Drag and drop files to Document tab.

* ReadonlyProcessVariable (analog inputs with units and calibration).

* RegisterStats to provide InlineDevice operations on waveforms (parameter 'op'). 
  URL syntax is: channel?op=OP, where OP can be mean, min, max, stdev, variance, sum.

* Averaging in InlineDevice parameters. Parameters 'samples' and 'interval' to setup
  averaging, and parameter 'op' to retrieve Averager child devices.

* New data layout: LayoutSF (id "sf") (proposal for SwissFEL common format).

* Script callbacks for start/end of scan passes: __before_pass__ and __after_pass__.

* Command line option '-extr to force extraction of startup.py and other scripting utilities.

* ReadonlyProcessVariable class: an analog input with scale,offset and unit.

* Included 'display' option to disable scan data displaying on plots, table and console. 

* Script settings: persisted  inJjava property files, ACCERSSED with __get_setting__ and __set_setting__.

  
### Changed

* EPICS channels are now by default timestamped and nullify invalid values. 
  Default behavior for invalid values can be set with Epics.setDefaultInvalidValueAction.

* StripChart persistence based on scan layouts.

* Convert.flatten and Convert.reshape handling multi-dimensional array shape conversion
  (replacing Convert.toBidimensional, Convert.toUnidimensional).

* Replaced Millisecond into FixedMillisecond in TimePlotJFree, which has better performance.

* Token {count} can have  customized format. E.g: {count}%02d.

* Monitor scan supporting multiple triggers and undefined number of elements.

* Device polling first call is immediate (timer delay=0) .

* Move scan common optional arguments to **pars (so that signatures don't get too big): 
  __title__, __before_read__,  __after_read__, __before_pass__ and __after_pass__.
 
* Ability to abort background scans: both server __evalAsync__ command  and  __run__ with async flag
  return command id. It can be aborted with abort/id  in the server.

* Included local timestamp and device timestamps in scan record.

* Included __caget__ option to retrieve also channel metadata (severity, timestamp).

* 'check_positions=False' option is used in ContinuousScan to disable following errors.
  The dataset may have less point than what was specified.

* Changed comment & indentation shortcuts not to collide with meta+shift+Q on Mac.

* Changed "accumulate" and "persist" keywords into "keep" and "save" (keeping backward compatibility).

* __show_panel__ now works in console mode. Plugin.showPanel work in detached mode.

### Fixed

* get_plot_snapshot manage names of plots with no title.

* Many GUI fixes for Mac OS (accelerators, colors, sizes...).

* Log cleanup bug fix (time to live in ms was truncated because was not a long).

* Read calls in the cache change event callback return the cache, and do not trigger new read.

* Workaround to bug in JFreeChart when appending very small values to matric plot.

* Fixed return values of simultaneous commands and persistence of simultaneous scans.

* Fixed execution context in sub-threads of background commands.

* Fixed EPICS freezing when reading  channel from a monitor callback (Updated to JCAE 2.9.6).

* Improved robustness in HDF5 writing, converting types to match the dataset.


## 1.9-SNAPSHOT

### Added

* REST API and data server provide access to data also in json, binary or bs format.

* Support to webcams: ch.psi.pshell.imaging.Webcam. Configured with camera id and resolution index. 
  If empty takes the default. Existing camera ids can be read with:
```
ch.psi.pshell.imaging.Webcam.getWebcams()
```
 Available camera resolutions can be listed with: 
```
webcam.getResolutions()
```
 

### Changed

* Updated Jython to 2.7.1.



## 1.8.0 (2017-12-05)

### Added

* __Inline Devices__: channel names can be provided directly in the scan functions 
  (and in __create_averager__ command) - and therefore devices don't have to be explicitly created
  for scans to be performed. 
  Scans can mix regular devices and channel names.
  A prefix indicates the protocol: 
    * "ca://" for  EPICS channel
    * "bs://" for beam synchronous stream.  
  
  See "Inline Devices" section in "Help Contents" - "Devices" for details and creation options.
  
  Examples:
```
av = create_averager("ca://CHANNEL_1", 3, interval = 0.1)
lscan("ca://CHANNEL_SETPOINT", ["ca://CHANNEL_READBACK_1", "ca://CHANNEL_READBACK_2"], 0.0, 10.0, 10, latency=0.1)
mscan("bs://CHANNEL_1", ["bs://CHANNEL_1", "bs://Float64Scalar?modulo=10", "bs://Float64Waveform?size=2048"], -1, 3.0)
```


* Email/SMS notification on script execution failure. 

  Configuration: edit {config}/mail.properties    and set "Notification Level" parameter in the global configuration dialog. 
  Notifications can also be manually sent by scripts: 
```
notify(subject, text, attachments = None, to=None)
```

* Register.setSettlingCondition: wait for an event after a writing operation to a register.
```
positioner.setSettlingCondition(ChannelSettlingCondition("MOVING_FLAG_CHANNEL_NAME", 0))
```
  This waits MOVING_FLAG_CHANNEL_NAME channel get to 0 before exiting a register write operation. 

* Included ScanResult.getData, returning the scan data as a multi-dimensional array.

* Exception dialog has "Details" button to show stack trace.

* Included "{sysuser}" tag for configuring path names: it is replaced by system user name.


### Changed

* All options currently given by set_exec_pars can be typed  inline in the scan command.  
  In this case they apply only to the current scan, and not for the following commands 
  in the script:
```
lscan (positioner, sensor, 0.0, 10.0, 10, persist=False)
```

* Displaying preferences can also be input inline. The preference parameters are in lowercase .
  Also options given by  setup_plotting (which is now deprecated) can be used inline:
```
lscan (positioner, [s1,s2,s3,s4], 0.0, 10.0, 10, plots_list = [s1,s3], line_plots = [s1], domain_axis = s2, range="auto")
```

* Auto-completion works also for Python objects.

* Miscellaneous documentation tweaks and fixes.
