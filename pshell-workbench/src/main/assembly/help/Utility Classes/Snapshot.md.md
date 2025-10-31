# Snapshot

The Snapshot class implements saving and restoring the state of a list of devices implementing ReadableWritable.
The state can be kept in memory or saved to files

Constructor arguments:

 * devices(list of ReadableWritable): Snapshot device set.
 * name(optional, list of String): Name for this snapshot. Determines the persistence folder.
   If ommited uses "default".


Methods:
  * void clear(): clear the state.
  * List<Exception> take(mode=Mode.PARALLEL): read state from devices.
    Returns a list of the exceptions - empty in case of success.
  * List<Exception> restore(mode=Mode.PARALLEL): write state values to devices.
    Returns a list of the exceptions - empty in case of success.
  * String save(): save current state to a file. Returns timestamp.
  * void load(timestamp=None): load state a file with a given timestamp. If None, loads the latest.
  * void del(): delete the persistence folder (all snapshots saved for the given name).

Attributes:
  * String name: snapshot name.
  * List devices: device list.
  * List state: device values.
  * boolean taken: true if state has values.
  * Path path: snapshot persistence folder.


Take and Restore Modes:
  * Mode.PARALLEL: all the devices are accessed in parallel, returning all errors.
  * Mode.SERIES: all the devices are accessed sequentially, returning all errors.
  * Mode.STOP_ON_ERROR: all the devices are access sequentially, but stops in the first error.