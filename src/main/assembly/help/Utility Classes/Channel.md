# Channel


This class encapsulates a Epics CA connection. __caput__ and __caget__ functions create and destroy a connection 
for each access and therefore __Channel__ objects are more efficient to be used inside loops.
__Channel__ objects can be used in scan functions as positioners or sensors.

For permanent global objects, the use of classes __ch.psi.epics.Channel*__ is preferable as they manage  
reconnection. 


Methods:
 * Constructor: `__init__`(name, type = None, size = None, callback=None, alias = None, monitored=None)
    - name(str): value to be written
    - type(str, optional): type of PV, defaults 's'. 

        Scalar values: 'b', 'i', 'l', 'd', 's'. 

        Array: values: '[b', '[i,', '[l', '[d', '[s'.
    - size(int, optional): the size of the channel
    - callback(function, optional): The monitor callback.
    - alias (str, optional): Name to be used in plots and datasets (if different to the channel name).
    - monitored (bool, optional): If set to true enables the channel monitor.
 * get_channel_name(): Return the name of the channel.
 * get_size(): Return the size of the channel. 
 * set_size(size): Set the size of the channel.
 * is_connected(): Return True if channel is connected.
 * is_monitored():Return True if channel is monitored.
 * set_monitored(value): Set a channel monitor to trigger the callback function defined in the constructor.
 * put(value, timeout=None):Write to channel and wait value change. In the case of a timeout throws a TimeoutException.
    - value(obj): value to be written
    - timeout(float, optional): timeout in seconds. If none waits forever.
 * putq(value): Write to channel and don't wait.
 * get(): Get channel value.
 * wait_for_value(value, timeout=None, comparator=None): Wait channel to reach a value, using a given comparator. In the case of a timeout throws a TimeoutException.
    - value(obj): value to be verified.                
    - timeout(float, optional): timeout in seconds. If None waits forever.
    - comparator (java.util.Comparator, optional). If None, uses Object.equals.
 * write(value): Writable interface, calls __put__.
 * read(): : Readable interface, calls __get__.
 * close(): Close the channel.


