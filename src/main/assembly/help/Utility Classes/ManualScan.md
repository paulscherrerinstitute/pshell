# ManualScan

Utility class to implement a scan manually. The user implements the data acquisition loop but
benefits from the standard functionalities of a normal scan function (plotting and persistence).


The pattern is: 

```
    scan = ManualScan([positioner 1,...], [sensor 1,...], ...) 
    scan.start() 

    for ... (DAQ loop)

        ...    
        scan.append ([positioner setpoint 1,...], [positioner readback 1,...], [sensor 1,...])

    scan.end()

```

Constructor arguments:

 * writables(list of Writable): Positioners set
 * readables(list of Readable): Sensors set
 * start(list of float, optional): start positions of writables.
 * end (list of float, optional): final positions of writables.
 * steps (list of int, optional):  number of scan steps for each writable
 * relative = if true, start and end positions are relative to current at start of the scan.


__Note__: The purpose __start__, __end__ and __steps__  is only for setting the plot ranges in the beginning 
of the scan. They are not mandatory for 1D plotting, but they are for the 2D Matrix plots as the range and number
of points is needed beforehand.

