# ScanResult

The scan builtin functions returns a __ScanResult__ object packaging all the acquired data.
__ScanResult__ is subscriptable, each element being a __ScanRecord___ element. Therefore the following
code is valid:
```
    scan_result = lscan(positioner, sensor, start, end, steps)
    print len(scan_result) 
    print scan_result[0]
```

Methods:

 * int getSize(): Returns the number of __ScanRecord__ elements in the scan.
 * List getWritables(): Returns positioners in the scan.
 * List getReadables(): Returns sensors in the scan.
 * List getRecords(): returns the list of __ScanRecord__ elements.
 * List getSetpoints(obj id): returns a list of all setpoints of the positioner given by __id__ (index, name, or device reference)
 * List getPositions(obj id): returns a list of all readbacks of the positioner given by __id__ (index, name, or device reference)
 * List getReadable(obj id): returns a list of all readouts of the sensor given by __id__ (index, name, or device reference)
 * int getIndex(): index of this scan in the execution context.
 * String getRoot(): the data persistence root for the current scan.
 * String getGroup(): the scan group insioide the root (relative path).
 * String getPath(): the full path name for the scan: root|path
 * int getDimensions(): number of dimensions of the scan
 * int getErrorCode(): a optional scan error code, scan type dependent. 0 means no error.
 * String print(String separator = "\t"): returns a table of the scan results: the column names are "Time", "Index", positioners' names and sensors' names.
    

