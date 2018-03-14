# ScanRecord

The ScanRecord class contains the data acquired for one point during a scan. 


Methods:

  * int getIndex(): the sequential index of the present record within the scan. 
  * int getPass(): the number of the current scan pass.
  * int getIndexInPass(): the sequential index of the present record within the current scan pass. 
  * long getTimestamp(): the system time in milliseconds after sampling the record sensor. 
  * Number[] getSetpoints(): the setpoint for each positioner.
  * Number[] getPositions (): the readback for each positioner.
  * Object[] getValues(): the readout of each sensor, which can be a scalar or an array.
  * void invalidate():  when called from __after_read__ callback, flags the scan to re-sample current scan record.
  * void cancel():  when called from __after_read__ callback, flags this record to be discarded.
  * String print(String separator):  print contents to a string, which columns are time, index, positioner values and sensor values.
