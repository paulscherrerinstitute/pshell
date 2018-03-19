# Changelog


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

* Averaging in URLDevice parameters. Parameters 'samples' and 'interval' to setup
  averaging, and parameter 'op' to retrieve Averager child devices.

* RegisterStats to provide URLDevice operations on waveforms (parameter 'op').

* New data layout: LayoutSF (proposal for SwissFEL common format).

* Script callbacks for start/end of scan passes: __before_pass__ and __after_pass__.


### Changed

* Convert.flatten and Convert.reshape handling multi-dimensional array shape conversion
  (replacing Convert.toBidimensional, Convert.toUnidimensional).

* Replaced Millisecond into FixedMillisecond in TimePlotJFree, which has better performance.

* Token {count} can have  customized format. E.g: {count}%02d.

* Monitor scan supporting multiple triggers.

* Device polling first event is immediate (timer delay=0) .

* Move scan common optional arguments to **pars(so signatures don't get too big): 
  __title__, __before_read__ and __after_read__, __before_pass__ and __after_pass__.
 
* Ability to abort background scans: both server __evalAsync__ command  and  __run__ with async flag
  return command id. It can be aborted with abort/id  in the server.

* Included local timestamp and device timestamps in scan record.


### Fixed

* get_plot_snapshot manage names of plots with no title.

* Many GUI fixes for Mac OS (accelerators, colors, sizes...).

* Log cleanup bug fix (time to live in ms was truncated because was not a long).

* Read calls in the cache change event callback return the cache, and do not trigger new read.

* Workaround to bug in JFreeChart when appending very small values to matric plot.

* Fixed return values of simultaneous commands dependent and persistence of simultaneous scans.



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
