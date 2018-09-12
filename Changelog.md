# Changelog

## 1.10.0

### Added

* Logarithmic colormap for renderer and matrix plot.

* Logarithmic scales on LinePlot and TimePlot.

* Configuration of colormap scale through the popup menu for renderer and MatrixPlot.

* Ability to set the plot layout for a particular scan with the 'plot_layout parameter.'

* EpicsRegisterArray.setSizeToValidElements for setting array size to value given by .NORD field.

* Configuration of Channel range and alarms in StripChart.

* Ability to force rescan of FileSource (source.rescan()).

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

  
### Changed

* StripChart type 'Device' can be set with InlineDevice configuration string.

* Builtin 'run' function returns value set by 'set_return' (if called by the script).

* bsget include module, offset and timeout as parameters.

* Improved load time (removed inspect and ctypes from startup.py).

* __name__ set to '__main__' instead of '__builtin__' (as Jython does under Java Scripting API).

* StripChart plot panel made public - so can be added to plugins.

* Configuration of sign bit for process variables.

* DeviceConfig handles Jython classes and dictionary based config - 
  so Config classes can be declared in scripts.

* MjpegSource can be monitored.

* Improvements to Plugin interface.

* 'keep' replaced 'accumulate' keyword.

* Possibility to add/remove devices to the pool without initializing/closing them.


### Fixed

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
