# Changelog

## 1.9-SNAPSHOT

### Added

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
