# Device Model

__Device__ is a notion in PShell representing a real-world hardware or subsystem. A Device encapsulates
a set of values (state) and a set of methods (behavior) in order to simplify the statements of the scripts.
For example it is more readable a command such as "motor.move(5.0)" than writing directly to EPICS channels.

Data acquisition scripts can be oriented to devices or not. 

There are 3 ways to execute scans:
 - Using built-in scan functions with  sensors (implementing __Readable__ interface) and positioners 
   (implementing __Writable__ interface) objects. Many devices types implement these interfaces and 
   can be directly used in scan commands. PSeudo-device objects can be equally created and used, 
   providing they implement __Readable__ or __Writable__ interface.
 - Using __ManualScan__ class. Scan loop is user defined, but benefiting from automatic data storage and plotting.
   Device or pseudo-device objects are not necessary, but may be used. 
   Direct network access such as caget and caput can be used to retrieve data using no device classes.
 - Custom: the acquisition loop create directly the datasets and plot. 
   Device or pseudo-device objects are not necessary, but may be used. 

An acquisition script can be created entirely based on direct EPICS channel access (or other direct
protocol) but the use of devices provide benefits: 
 - Built-in tools for device testing and monitoring.
 - Script readability and compactness.
 - Code reuse.

All devices  implement Device interface. The simplest way to create a new device type is to extends 
__DeviceBase__ class - or else another abstract class within ch.psi.pshell.device package such as __MotorBase__.

The DeviceBase class makes few assumptions on the implementations and allow creation of Device im
multiple communication model. The class has many features that can be disregarded by implementations if 
not relevant to the case.

DevicesBase features:

 - A __name__, provided in the constructor. All devices declared in the device pool must have a unique name, which will
   be a reserved word in the script interpretor.
 - A __state__ variable and "waitState" and "waitStateNot" methods. When the devices is created the state is "Invalid".
 - A __initialize__ method. Implementation can override "doInitialize". Real device access is forbidden in the device 
   constructor , but it is allowed in "doInitialize". The device state is  "Initializing" during the initialization.
   Then, if initialization is successful, the state will be set to "Ready" or another normal state defined by the 
   implementation. In case of exception on the initialization the state will be set to "Invalid". If the device timeouts 
   the state could be set to "Offline". If it has internal problems than it could be set to "Fault" .
 - __Configuration persistence__. Devices can have a persisted configuration structure which can be 
   visually edited in the workbench. Some devices have a persisted configuration other not - devices may have a static
   configuration set in the constructor.
 - __Simulation__ flag. When in simulation there no access to the real hardware. It us up to the implementations
   to manage the simulation behaviour. 
 - A internal __cache__ object that represent the device contents. The cache is returned with the __take__ method.
   The cache has an associated __age__ which is the time since it was last updated. __waitValue__ and __waitValueNot__
   methods wait for a change in the cache.
 - __Monitored__ flag. This flag indicates that the cache (device contents) are updated asynchronously. Some devices may
   disregard this flag, when it is not possible to configure this behaviour. For EPICS devices this flag implies
   in the creation of monitors for the underlying channels.
 - __Readonly__ flag. A write-once flag indicating if the device is read only.
 - __update__ method. This method is blocking, it access the real hardware to update the internal cache.
   Implementation is done overriding "doUpdate". 
 - __request__ method returns the current cache and triggers a new update in the background (multiple calls to request
   are filtered).
 - A __polling__ mechanism. __setPolling__ sets a polling interval, which is the interval for a background task to
   call __update__. If the polling value is negative it is in the "foreground" mode and __update__ is only called
   if the device has listeners.
 - A __ready__ flag, which meaning is specific to the implementation. The methods "isReady" and "waitReady(timeout)" 
   are provided. 
 - __Hierarchical organization__: devices can be hierarchically organized in 2 kinds of relation:

    - Parent/Child: Any device can be assigned to a parent. The state of the child will be dependent on the parent's:
      if a parent goes offline, or is disposed, so do all its children. Example: a __ModbusTCP__ controller is the parent
      of all IO devices under it. 
      
    - Composite/Component: Devices can assemble more complex devices, even if they have different nature or have different
      parents or communication means. Example: a __MotorGroup__ is a composite of multiple motors.
 - Message-passing through __listeners__: The __addListener__  method allows receiving Device events through a 
   __DeviceListener__ interface:

    - __onStateChanged__ is called on device state changes
    - __onValueChanged__ is called on device cache changes
    - __onValueChanging__ is called before a new value is written to the device. If the listener throws an exception the 
      writing is aborted.
    - __onReadbackChanged__. If the Device is instance of __ReadbackDevice__ and the listener is instance of 
      __ReadbackDeviceListener__ then this callback is also called when the cache of the readback is changed.

The __DevicePool__ lists the global devices, which must have unique names. These devices will be accessible from scripts and 
from the shell: a global variables will be created for each device in the pool. The device pool editor allows edition of 
device name, constructor parameters, polling, monitoring, readonly and simulation parameters. 
Devices can be added to the device pool programmatically with the __add_device__ built-in function.

Some types of devices are supported out of the box:
 
 - EPICS.
 - Serial devices (COM ports, TCP connection).
 - Modbus.
 - Imaging sources (cameras, detectors, MJPEG...).
    

The model is extensible though  and new families of devices may be added.

<br>


# Device Pool Editor
 - The device pool editing table is accessed with menu item Devices-Definition. The columns are:
    - __Enabled__: if unchecked the device is not instantiated.
    - __Name__: a unique device name.
    - __Class__: the complete class name of the device. 
      The value can be directly typed or selected in the drop-down combo containing the known classes.
    - __Parameters__: space separated constructor parameters. 
      String parameters can be delimited by double quotes ("). They must in the case they contain spaces.
      Double-clicking this field shows a button to open the parameters editor dialog, on which the 
      existing constructor signatures are listed. The one matching the typed parameters is highlighted.
      The parameter types are inferred from the available constructors. If conflicting signatures exist,
      the types may be enforced:
      - String type is enforced with double quotes (") delimiters.
      - Device type is enforced with '<' and '>' delimiters: <DEVICE_NAME>.
      - Numeric types can be enforced with the pattern: #VALUE:TYPE, where TYPE can be int, byte, short, 
        long, float, double or boolean.
    - __Polling__: if defined sets a polling interval for updating the device (in milliseconds).
    - __Monitor__: if checked marks the device as monitored (asynchronously updated).
    - __Readonly__: if checked marks the device as readonly (write operations are blocked).
    - __Simulation__: if checked instantiates the device in simulation mode.


# Abstract and Generic Devices - ch.psi.pshell.device

The ch.psi.pshell.device contains many interfaces and abstract device classes to simplify creation of custom device families:
 - __Register__: Device representing a raw scalar or array value
 - __ProcessVariable__: A Register with metadata: units, range, resolution, scale and offset. Metadata may be static or persisted in configuration.
 - __ControlledVariable__: A ProcessVariable with readback (implementing __ReadbackDevice__) and position checking (implementing __Positionable__)
 - __Positioner__: ControlledVariable with move commands (sync and async, absolute and relative), and using __waitReady__ to wait end of move .
 - __Motor__: Positioner with speed, speed range, jog commands, and reference method.
 - __DiscretePositioner__: __Movable__ device which position is defined by a string in a predefined set. 
   The position is "Unknown" if the device is not in a predefined position.
 - __Camera__: Base class for camera devices: 2D sensors with optional features of binning, roi, gain, exposure, iterations, trigger mode and more.

This package contains also ready-to-use device classes:
 - __Averager__: Performs averaging on __Register__ devices.
 - __MotorGroupBase__: Composite device which controls a set of motors in simultaneous move.
 - __MotorGroupDiscretePositioner__: DiscretePositioner having, for each position name, a set of predefined positions to be applied to a MotorGroup.
 - __RegisterCache__: Method 'read' returns the cache of another register (help to implement scans based on cached values). 
 - __Slit__: Device managing 2 motors through a MotorGroup providing registers to directly set center and size. 
 - __DummyMotor__, __DummyPositioner__, __DummyRegister__: simulated devices.

<br>


# EPICS Devices - ch.psi.pshell.epics

 - Scalar register classes wrapping EPICS PVs, constructed with channel name and, optionally, precision:

    - __ChannelByte__
    - __ChannelDouble__
    - __ChannelFloat__
    - __ChannelInteger__
    - __ChannelShort__
    - __ChannelString__
 
 - Array register classes wrapping EPICS PVs, constructed with channel name and, optionally, precision and size:

    - __ChannelByteArray__
    - __ChannelDoubleArray__
    - __ChannelFloatArray__
    - __ChannelIntegerArray__
    - __ChannelShortArray__

 - Implementations of Generic Devices in EPICS
    - __ProcessVariable__: ProcessVariable implementation, constructed with channel name.
    - __ControlledVariable__: ControlledVariable implementation, constructed with setpoint and readback channel names.
    - __Positioner__: Positioner implementation, constructed with setpoint and readback channel names.
    - __Motor__: wraps EPICS motor records.
    - __DiscretePositioner__: wraps EPICS mbbi and mbbo records.
    - __BinaryPositioner__: wraps EPICS bi and bo records.
    - __AreaDetector__: wraps  EPICS AreaDetector, implementing __Camera__.
    - __Manipulator__: DiscretePositioner having each position selected by an individual channel (integer) and a single readback (string).
    - __Slit__: Slit implementation constructed with the motor names only (EPICS motors internally created).


<br>


# Serial Devices - ch.psi.pshell.serial

The class __SerialDeviceBase__ is an abstract class for serial communications, both in mode half duplex (synchronous)and full duplex (asynchronous). 
Contains method for sending messages and waiting answers, both string and byte array oriented.
Some methods can manage simple protocols, having as parameters the header, trailer and/or number of bytes to wait for.

Implementations:

 - __SerialPortDevice__: Communication through a PC serial port. The port name and parameters are defined in the device configuration.

    On Linux the user must have RW access to the port (/dev/ttyS*) and RW access to /var/lock.
       
 - __TcpDevice__: Communication through a TCP connection. The server address and port are constructor parameters.
 - __UdpDevice__: Communication through a TCP connection. The server address and port are constructor parameters.

<br>


# Modbus Devices - ch.psi.pshell.modbus

 - Implementation of connections to Modbus master devices:

    - __ModbusSerial__: The port name and parameters are defined in the device configuration.
    - __ModbusTCP__: The server address is the constructor parameter.
    - __ModbusUDP__: The server address is the constructor parameter.

- Scalar register classes wrapping single IO in a master, constructed with the master name (parent) and the index:

    - __AnalogInput__
    - __AnalogOutput__
    - __DigitalInput__
    - __DigitalOutput__
 
- Array register classes wrapping sequence of IOs in a master, constructed with the master name (parent), index and size:

    - __AnalogInputArray__
    - __AnalogOutputArray__
    - __DigitalInputArray__
    - __DigitalOutputArray__

<br>


# Imaging Devices - ch.psi.pshell.imaging

PShell supports imaging processing and rendering. Images are generated by a special type of device: __Source__.
Declared sources can be rendered from the workbench and can be referenced from scripts. The supported image sources are:

 - __CameraSource__: Connects to a Camera device, such as an EPICS area detector.
 - __FileSource__: Polls a file, pushing a new frame every time the file changes.
 - __MjpegSource__: Receive frames from a mjpeg server.
 - __RegisterMatrixSource__: Connects to a __ReadableMatrix__, pushing a frame every time the register contents change.

<br>


# Detectors  - ch.psi.pshell.detector

The PSI ZMQ-based detector streaming is supported.


<br>


# Creating New Device Classes

New devices, or family of devices, can be created in different ways:

 - As an extension: in a new Java project packaged in a a jar file. The jar must be put in the extensions folder. 
   All jar files in the extensions folders are included in the class path.
   Classes within the jar extending __DeviceBase__ can be referenced in the Device Pool configuration window. 
   If the device implementation requires native libraries, they may be put in the extensions folders too, 
   as this folder is added to the path.
 - As a plugin: in a Java file added to the plugins folder, having a class extending __DeviceBase__. 
   This class will be dynamically compiled and can be referenced in the Device Pool configuration window.
 - As a Python class extending __DeviceBase__: a new Python device class can be defined in "local.py" script and then 
   added to the Device Pool with the built-in function __add_device__ (see example in Tutorial, file "devices").

<br>


# Inline devices

Besides global and local devices, scan functions can use inline devices. 
These are declared as URL strings within the __readables__ or __writables__ parameters.
The scan function instantiate an internal device base on the URL and close it in the end of scan. 
Inline devices can be also used in the __create_averager__ function.        


The format of device URL string is:

```
PROTOCOL_NAME://DEVICE_NAME
```

If the device has options they are declared as:

```
PROTOCOL_NAME://DEVICE_NAME?OPTION_1=VAL_1&...&OPTION_N=VAL_N
```

The supported protocols are:

- ca: Creates a device based in an EPICS channel. Options:

    - type: enforce the channel type. Scalar types: 'b', 'i', 'l', 'd', 's'. Waveform type: '[b', '[i,', '[l', '[d', '[s'.
    - size: determines the size of a waveform.
    - precision: number of precision digits.
    - timestamped: if true then the value carry the IOC timestamp, otherwise the PC timestamp.
    - blocking: if true (default) writes and waits until processing finishes. Otherwise does not wait.


- bs: Creates a device from a BSREAD channel. 
  All inline BSREAD values in a scan provide data from the same pulse id. Options:

    - width: if width and height are defined then creates a matrix register.
    - height: if width and height are defined then creates a matrix register.
    - waveform: if set to true creates a waveform devices (otherwise a scalar).
    - size: sets the array size for waveform device.
    - modulo: set the stream sub-sampling - modulo applied to pulse-id to determine the readout.
    - offset: stream offset in pulses.


- cs: creates a device from a stream image server. Device name set with the server URL.
    Options:
      - channel (str): name of the field of the image stream.

- Generic options:
    - monitored: creates a monitored device.
    - polling: sets a polling interval.
    - simulated: creates a simulated device.
    - samples: if defined creates an Averager of the device value, with the iven number of samples. 
      If samples is negative,creates an Async Averager, (__read__ averages past values instead of sampling new ones).
    - interval: if __samples__ is defined, gives the Averager sampling interval in milliseconds.
      If equals to -1, the Averager is not time-based: samples every change event.

Examples:
```
lscan("ca://CHANNEL_SETPOINT?blocking=False", ["ca://CHANNEL_READBACK_1?type=i", "ca://CHANNEL_READBACK_2?size=5:precision=0"], 0.0, 10.0, 10, latency=0.1)
mscan("bs://CHANNEL_1", ["bs://CHANNEL_1", "bs://Float64Scalar?modulo=10", "bs://Float64Waveform?size=2048"], -1, 3.0)
tscan (create_averager("bs://CHANNEL_1", 3, interval = 0.1), 10, 0.5)
```



