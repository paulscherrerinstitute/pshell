# ScanRecord

The __ScanRecord__ class contains the data acquired for one point during a scan. 
Scans return a __ScanResult__ objects which contais a list of __ScanRecord__. 
__ScanRecord__  objcts are also send in the __after_read__ callback. 
__ScanRecord__ is subscriptable by the device, device name or index.   Therefore the following code is valid:

Therefore the following code is valid:
```
def after_read(record, scan):
    print record[0]
    print record[sensor]
    print record["positioner"]

scan_result = lscan(positioner, sensor, start, end, steps, after_read=after_read)
    print scan_result[0][0]
    print scan_result[0][sensor]
    print scan_result[0]["positioner"]
```


Methods:

  * int getIndex(): the sequential index of the present record within the scan. 
  * int getPass(): the number of the current scan pass.
  * int getIndexInPass(): the sequential index of the present record within the current scan pass.
  * long getLocalTimestamp(): the system time in milliseconds after sampling the record sensor. 
  * Long getRemoteTimestamp(): the record timestamp notified by device.
  * long getTimestamp(): returns the device timestamp, if present, otherwise the local timestamp.
  * Long[] getDeviceTimestamps(): the readable timestamps, which may differ from the record timestamp.
  * Number[] getSetpoints(): the setpoint for each positioner.
  * Number[] getPositions (): the readback for each positioner.
  * Object[] getReadables(): the readout of each sensor, which can be a scalar or an array.
  * Number getSetpoint(positioner): the setpoint for the positioner (device or device name).
  * Number getPosition (positioner): the readback for the positioner (device or device name).
  * Object getReadable(sensor): the readout of the sensor (device or device name), which can be a scalar or an array.
  * void invalidate():  when called from __after_read__ callback, flags the scan to re-sample current scan record.
  * void cancel():  when called from __after_read__ callback, flags this record to be discarded.
  * String print(String separator):  print contents to a string, which columns are time, index, positioner values and sensor values.
