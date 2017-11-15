###################################################################################################
#  Global definitions and built-in functions
###################################################################################################

import sys
import time
import math
import types
import inspect
import os.path
from operator import add, mul, sub, truediv
from time import sleep

from array import array
import jarray

import java.lang.Class as Class
import java.lang.Object as Object
import java.beans.PropertyChangeListener
import java.util.concurrent.Callable
import java.util.List
import java.lang.reflect.Array
import java.awt.image.BufferedImage as BufferedImage    
import java.awt.Color as Color

import org.python.core.PyArray as PyArray

import ch.psi.utils.Threading as Threading
import ch.psi.utils.State as State
import ch.psi.utils.Convert as Convert
import ch.psi.utils.Arr as Arr
import ch.psi.utils.Chrono as Chrono
import ch.psi.pshell.core.CommandSource as CommandSource
import ch.psi.pshell.core.ContextAdapter as ContextListener
import ch.psi.pshell.core.Context
import ch.psi.pshell.core.UrlDevice 
import ch.psi.pshell.data.PlotDescriptor as PlotDescriptor
import ch.psi.pshell.data.Table as Table
import ch.psi.pshell.device.Device as Device
import ch.psi.pshell.device.DeviceBase as DeviceBase
import ch.psi.pshell.device.RegisterBase as RegisterBase
import ch.psi.pshell.device.ProcessVariableBase as ProcessVariableBase
import ch.psi.pshell.device.ControlledVariableBase as ControlledVariableBase
import ch.psi.pshell.device.PositionerBase as PositionerBase
import ch.psi.pshell.device.MotorBase as MotorBase
import ch.psi.pshell.device.DiscretePositionerBase as DiscretePositionerBase
import ch.psi.pshell.device.MotorGroupBase as MotorGroupBase
import ch.psi.pshell.device.MotorGroupDiscretePositioner as MotorGroupDiscretePositioner
import ch.psi.pshell.device.ReadonlyRegisterBase as ReadonlyRegisterBase
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase as ReadonlyAsyncRegisterBase
import ch.psi.pshell.device.RegisterCache as RegisterCache
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray as ReadonlyRegisterArray
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix as ReadonlyRegisterMatrix
import ch.psi.pshell.device.DummyPositioner as DummyPositioner
import ch.psi.pshell.device.DummyMotor as DummyMotor
import ch.psi.pshell.device.DummyRegister as DummyRegister
import ch.psi.pshell.device.Timestamp as Timestamp
import ch.psi.pshell.device.Interlock as Interlock
import ch.psi.pshell.device.Readable as Readable
import ch.psi.pshell.device.Readable.ReadableArray as ReadableArray
import ch.psi.pshell.device.Readable.ReadableMatrix as ReadableMatrix
import ch.psi.pshell.device.Readable.ReadableCalibratedArray as ReadableCalibratedArray
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix as ReadableCalibratedMatrix
import ch.psi.pshell.device.ArrayCalibration as ArrayCalibration
import ch.psi.pshell.device.MatrixCalibration as MatrixCalibration
import ch.psi.pshell.device.Writable as Writable
import ch.psi.pshell.device.Writable.WritableArray as WritableArray
import ch.psi.pshell.device.Stoppable as Stoppable
import ch.psi.pshell.device.Averager as Averager
import ch.psi.pshell.device.Delta as Delta
import ch.psi.pshell.device.DeviceAdapter as DeviceListener
import ch.psi.pshell.device.ReadbackDeviceAdapter as ReadbackDeviceListener
import ch.psi.pshell.device.MotorAdapter as MotorListener
import ch.psi.pshell.device.MoveMode as MoveMode
import ch.psi.pshell.epics.Epics as Epics
import ch.psi.pshell.epics.EpicsScan as EpicsScan
import ch.psi.pshell.imaging.Source as Source
import ch.psi.pshell.imaging.SourceBase as SourceBase
import ch.psi.pshell.imaging.DirectSource as DirectSource
import ch.psi.pshell.imaging.RegisterMatrixSource as RegisterMatrixSource
import ch.psi.pshell.imaging.ImageListener as ImageListener
import ch.psi.pshell.plot.LinePlotSeries as LinePlotSeries
import ch.psi.pshell.plot.LinePlotErrorSeries as LinePlotErrorSeries
import ch.psi.pshell.plot.MatrixPlotSeries as MatrixPlotSeries
import ch.psi.pshell.scan.LineScan 
import ch.psi.pshell.scan.ContinuousScan
import ch.psi.pshell.scan.AreaScan
import ch.psi.pshell.scan.VectorScan
import ch.psi.pshell.scan.ManualScan
import ch.psi.pshell.scan.HardwareScan
import ch.psi.pshell.scan.RegionScan
import ch.psi.pshell.scan.TimeScan
import ch.psi.pshell.scan.MonitorScan 
import ch.psi.pshell.scan.BinarySearch
import ch.psi.pshell.scan.HillClimbingSearch
import ch.psi.pshell.scan.ScanResult
import ch.psi.pshell.bs.BsScan
import ch.psi.pshell.bs.Stream as Stream
import ch.psi.pshell.scripting.ViewPreference as Preference
import ch.psi.pshell.scripting.ScriptUtils as ScriptUtils


def get_context():   
    return ch.psi.pshell.core.Context.getInstance()

###################################################################################################
#Type conversion and checking
###################################################################################################


def to_array(obj, type = 'o'):   
    """Convert Python list to Java array.

    Args:
        obj(list): Original data.
        type(str): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 'd' = double, 
                              'c' = char, 'z' = boolean, 's' = String,  'o' = Object 
    Returns:
        Java array.

    """     
    if type[0] == '[':
        type = type[1:]    
    arrayType = ScriptUtils.getType("["+type)    

    if obj is None:
        return None
    if isinstance(obj,java.util.List):
        obj = obj.toArray()    
        if type != 'o':
            obj = Convert.toPrimitiveArray(obj, ScriptUtils.getType(type))
    if isinstance(obj,PyArray):                
        if type != 'o':
            if (Arr.getRank(obj)== 1) and (obj.typecode != type):
                ret = java.lang.reflect.Array.newInstance(ScriptUtils.getType(type), len(obj))            
                if type == 's':                              
                    for i in range(len(obj)): ret[i] = str(obj[i])   
                elif type == 'c':                              
                    for i in range(len(obj)): ret[i] = chr(obj[i])   
                else:      
                    for i in range(len(obj)): ret[i] = obj[i]
                obj = ret
            if type not in ['o', 's']:
                obj = Convert.toPrimitiveArray(obj)
        return obj        
    if is_list(obj):
        if type=='o' or  type== 's':
            ret = java.lang.reflect.Array.newInstance(ScriptUtils.getType(type), len(obj))
            for i in range (len(obj)):
                if is_list(obj[i]):
                    ret[i] = to_array(obj[i],type)
                elif type == 's':
                    ret[i] = str(obj[i])
                else:
                    ret[i] = obj[i]        
            return ret            

        if len(obj)>0 and is_list(obj[0]):
            if  len(obj[0])>0 and is_list(obj[0][0]):
                    ret = java.lang.reflect.Array.newInstance(arrayType,len(obj),len(obj[0]))                    
                    for i in range(len(obj)):
                        ret[i]=to_array(obj[i], type)            
                    return ret
            else:
                ret = java.lang.reflect.Array.newInstance(arrayType,len(obj))
                for i in range(len(obj)):
                    ret[i]=to_array(obj[i], type)            
                return ret     
        return jarray.array(obj,type)
    return obj

def to_list(obj):
    """Convert an object into a Python List.

    Args:
        obj(tuple or array or ArrayList): Original data.        
    Returns:
        List.

    """
    if obj is None:
        return None
    if isinstance(obj,tuple) or isinstance(obj,java.util.ArrayList) :
        return list(obj)
    #if isinstance(obj,PyArray):
    #    return obj.tolist()
    if not isinstance(obj,list):
        return [obj,]
    return obj

def is_list(obj):
    return isinstance(obj,tuple) or isinstance(obj,list) or isinstance (obj, java.util.ArrayList)

def is_string(obj):
    return (type(obj) is str) or (type(obj) is unicode)

###################################################################################################
#Standard scan commands
###################################################################################################
def on_before_scan_readout(scan, pos):
    try:
        if scan.before_read != None:
            arguments = scan.before_read.func_code.co_argcount
            if arguments == 0:
                scan.before_read()
            elif arguments==1:
                scan.before_read(pos.tolist())
            elif arguments==2:
                scan.before_read(pos.tolist(), scan)
    except AttributeError:
        pass

def on_after_scan_readout(scan, record):
    try:
        if scan.after_read != None:
            arguments = scan.after_read.func_code.co_argcount
            if  arguments == 0:
                scan.after_read()
            elif arguments==1:
                scan.after_read(record)
            elif arguments==2:
                scan.after_read(record, scan)
    except AttributeError:
        pass

class LineScan(ch.psi.pshell.scan.LineScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class ContinuousScan(ch.psi.pshell.scan.ContinuousScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class AreaScan(ch.psi.pshell.scan.AreaScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class RegionScan(ch.psi.pshell.scan.RegionScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class VectorScan(ch.psi.pshell.scan.VectorScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class TimeScan(ch.psi.pshell.scan.TimeScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class MonitorScan(ch.psi.pshell.scan.MonitorScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class ContinuousScan(ch.psi.pshell.scan.ContinuousScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class BsScan(ch.psi.pshell.bs.BsScan):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class ManualScan (ch.psi.pshell.scan.ManualScan):
    def __init__(self, writables, readables, start = None, end = None, steps = None, relative = False):
        ch.psi.pshell.scan.ManualScan.__init__(self, writables, readables, start, end, steps, relative)

    def append(self,setpoints, positions, values):
        ch.psi.pshell.scan.ManualScan.append(self, to_array(setpoints), to_array(positions), to_array(values))

class BinarySearch(ch.psi.pshell.scan.BinarySearch):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

class HillClimbingSearch(ch.psi.pshell.scan.HillClimbingSearch):
    def onBeforeReadout(self, pos):
        on_before_scan_readout(self, pos)

    def onAfterReadout(self, record):
        on_after_scan_readout(self, record)

def lscan(writables, readables, start, end, steps, latency=0.0, relative=False, passes=1, zigzag=False, before_read=None, after_read=None, title=None, **pars):
    """Line Scan: positioners change together, linearly from start to end positions.

    Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(int or float or list of float): number of scan steps (int) or step size (float).
        relative (bool, optional): if true, start and end positions are relative to 
            current at start of the scan
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writables invert direction on each pass.
        before_read (function, optional): callback on each step, before each readout. Callback may have as 
            optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. Callback may have as 
            optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    latency_ms=int(latency*1000)   
    writables=to_list(string_to_obj(writables))
    readables=to_list(string_to_obj(readables))
    start=to_list(start)
    end=to_list(end)    
    if type(steps) is float or is_list(steps):
        steps = to_list(steps)
    scan = LineScan(writables,readables, start, end , steps, relative, latency_ms, passes, zigzag)        
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def vscan(writables, readables, vector, line = False, latency=0.0, relative=False, passes=1, zigzag=False, before_read=None, after_read=None, title=None, **pars):
    """Vector Scan: positioners change following values provided in a vector.

    Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        vector(list of list of float): table of positioner values.
        line (bool, optional): if true, processs as line scan (1d)
        relative (bool, optional): if true, start and end positions are relative to current at 
            start of the scan
        latency(float, optional): settling time for each step before readout, defaults to 0.0.        
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writables invert direction on each pass.
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readables=to_list(string_to_obj(readables))
    if len(vector) == 0:
        vector.append([])
    elif (not is_list(vector[0])) and (not isinstance(vector[0],PyArray)):
        vector = [[x,] for x in vector]
    vector = to_array(vector, 'd')
    scan = VectorScan(writables,readables, vector, line, relative, latency_ms, passes, zigzag)    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def ascan(writables, readables, start, end, steps, latency=0.0, relative=False, passes=1, zigzag=False, before_read=None, after_read=None, title=None, **pars):
    """Area Scan: multi-dimentional scan, each positioner is a dimention.

    Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(list of int or list of float): number of scan steps (int) or step size (float).
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        relative (bool, optional): if true, start and end positions are relative to current at 
            start of the scan
        passes(int, optional): number of passes
        zigzag (bool, optional): if true writables invert direction on each row.
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readables=to_list(string_to_obj(readables))
    start=to_list(start)
    end=to_list(end)    
    if is_list(steps):
        steps = to_list(steps)
    scan = AreaScan(writables,readables, start, end , steps, relative, latency_ms, passes, zigzag)    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()    


def rscan(writable, readables, regions, latency=0.0, relative=False, passes=1, zigzag=False, before_read=None, after_read=None, title=None, **pars):
    """Region Scan: positioner scanned linearly, from start to end positions, in multiple regions.

    Args:
        writable(Writable): Positioner set on each step, for each region.
        readables(list of Readable): Sensors to be sampled on each step.
        regions (list of tuples (float,float, int)   or (float,float, float)): each tuple define a scan region
                                (start, stop, steps) or (start, stop, step_size)                                  
        relative (bool, optional): if true, start and end positions are relative to 
            current at start of the scan
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writable invert direction on each pass.
        before_read (function, optional): callback on each step, before each readout. Callback may have as 
            optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. Callback may have as 
            optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    start=[]
    end=[]
    steps=[]
    for region in regions:
        start.append(region[0])
        end.append(region[1])
        steps.append(region[2])
    latency_ms=int(latency*1000)
    writable=string_to_obj(writable)
    readables=to_list(string_to_obj(readables))
    start=to_list(start)
    end=to_list(end)    
    steps = to_list(steps)
    scan = RegionScan(writable,readables, start, end , steps, relative, latency_ms, passes, zigzag)    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def cscan(writables, readables, start, end, steps, latency=0.0, time=None, relative=False, passes=1, zigzag=False, before_read=None, after_read=None, title=None, **pars):
    """Continuous Scan: positioner change continuously from start to end position and readables are sampled on the fly.

    Args:
        writable(Speedable or list of Motor): A positioner with a  getSpeed method or 
                    a list of motors.
        readables(list of Readable): Sensors to be sampled on each step.
        start(float or list of float): start positions of writables.
        end(float or list of float): final positions of writabless.
        steps(int or float or list of float): number of scan steps (int) or step size (float).
        time (float, seconds): if not None then writables is Motor array and speeds are 
                    set according to time.
        relative (bool, optional): if true, start and end positions are relative to 
            current at start of the scan
        latency(float, optional): sleep time in each step before readout, defaults to 0.0.
        before_read (function, optional): callback on each step, before each readout. 
                    Callback may have as optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. 
                    Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    latency_ms=int(latency*1000)
    readables=to_list(string_to_obj(readables))
    writables=to_list(string_to_obj(writables))
    #A single Writable with fixed speed
    if time is None:
        scan = ContinuousScan(writables[0],readables, start, end , steps, relative, latency_ms, passes, zigzag)
    #A set of Writables with speed configurable
    else:        
        start=to_list(start)
        end=to_list(end)    
        if type(steps) is float or is_list(steps):
            steps = to_list(steps)
        scan = ContinuousScan(writables,readables, start, end , steps, time, relative, latency_ms, passes, zigzag)
    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def hscan(config, writable, readables, start, end, steps, passes=1, zigzag=False, before_stream=None, after_stream=None, after_read=None, title=None, **pars):
    """Hardware Scan: values sampled by external hardware and received asynchronously.

    Args:
        config(dict): Configuration of the hardware scan. The "class" key provides the implementation class.
                      Other keys are implementation specific.
        writable(Writable): A positioner appropriated to the hardware scan type.
        readables(list of Readable): Sensors appropriated to the hardware scan type.
        start(float): start positions of writable.
        end(float): final positions of writables.
        steps(int or float): number of scan steps (int) or step size (float).
        before_stream (function, optional): callback before just before starting positioner move. 
        after_stream (function, optional): callback before just after stopping positioner move. 
        after_read (function, optional): callback on each readout. 
                    Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    cls = Class.forName(config["class"])
    class HardwareScan(cls):
        def __init__(self, config, writable, readables, start, end, stepSize, passes, zigzag):
            cls.__init__(self, config, writable, readables, start, end, stepSize, passes, zigzag)
        def onBeforeStream(self, current_pass):
            if scan.before_stream != None:
                scan.before_stream(current_pass)
        def onAfterStream(self, current_pass):
            if scan.after_stream != None:
                scan.after_stream(current_pass)
        def onAfterReadout(self, record):
            on_after_scan_readout(self, record)         
               
    readables=to_list(string_to_obj(readables))
    #TODO: Calling through abstract would enForce the constructor signature, but is not working
    #scan = ch.psi.pshell.scan.HardwareScan.newScan(HardwareScan, config, writable,readables, start, end , steps, passes, zigzag)    
    scan = HardwareScan(config, writable,readables, start, end , steps, passes, zigzag)        
    scan.before_stream=before_stream
    scan.after_stream=after_stream
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def bscan(stream, records, before_read=None, after_read=None, title=None, **pars):
    """BS Scan: records all values in a beam synchronous stream.

    Args:
        stream(Stream): stream object
        records(int): number of records to store
        before_read (function, optional): callback on each step, before each readout. 
                    Callback may have as optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. 
                    Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """    
    stream=string_to_obj(stream)
    scan = BsScan(stream,int(records))    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()

def tscan(readables, points, interval, before_read=None, after_read=None, title=None, **pars):
    """Time Scan: sensors are sampled in fixed time intervals.

    Args:
        readables(list of Readable): Sensors to be sampled on each step.
        points(int): number of samples.
        interval(float): time interval between readouts. Minimum temporization is 0.001s
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """          
    interval= max(interval, 0.001)   #Minimum temporization is 1ms
    interval_ms=int(interval*1000)
    readables=to_list(string_to_obj(readables))    
    scan = TimeScan(readables, points, interval_ms)    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def mscan(trigger, readables, points, timeout = None, async = True, take_initial = False, before_read=None, after_read=None, title=None, **pars):
    """Monitor Scan: sensors are sampled when received change event of the trigger device.

    Args:
        trigger(Device): Source of the sampling triggering.
        readables(list of Readable): Sensors to be sampled on each step.
                                     If  trigger has cache and is included in readables, it is not read 
                                     for each step, but the change event value is used.
        points(int): number of samples.
        timeout(float, optional): maximum scan time in seconds. 
        async(bool, optional): if True then records are sampled and stored on event change callback. Enforce 
                               reading only cached values of sensors.
                               If False, the scan execution loop waits for trigger cache update. Do not make 
                               cache only access, but may loose change events.
        take_initial(bool, optional): if True include current values as first record (before first trigger).
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """      
    timeout_ms=int(timeout*1000) if ((timeout is not None) and (timeout>=0)) else -1
    trigger = string_to_obj(trigger)
    readables=to_list(string_to_obj(readables))    
    scan = MonitorScan(trigger, readables, points, timeout_ms, async, take_initial)
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()

def escan(name, title=None, **pars):
    """Epics Scan: execute an Epics Scan Record.

    Args:
        name(str): Name of scan record.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        ScanResult object.

    """      
    scan = EpicsScan(name)    
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()


def bsearch(writables, readable, start, end, steps, maximum = True, strategy = "Normal", latency=0.0, relative = False, before_read=None, after_read=None, title=None, **pars):
    """Binary search: searches writables in a binary search fashion to find a local maximum for the readable.

    Args:
        writables(list of Writable): Positioners set on each step.
        readable(Readable): Sensor to be sampled.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(float or list of float): resolution of search for each writable.
        maximum (bool , optional): if True (default) search maximum, otherwise minimum.
        strategy (str , optional): "Normal": starts search midway to scan range and advance in the best direction.
                                             Uses orthogonal neighborhood (4-neighborhood for 2d)
                                   "Boundary": starts search on scan range.                                              
                                   "FullNeighborhood": Uses complete neighborhood (8-neighborhood for 2d)
        
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        relative (bool, optional): if true, start and end positions are relative to current at 
            start of the scan
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see 4_pars).

    Returns:
        SearchResult object.

    """    
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readable=string_to_obj(readable)
    start=to_list(start)
    end=to_list(end)   
    steps = to_list(steps) 
    strategy = BinarySearch.Strategy.valueOf(strategy)
    scan = BinarySearch(writables,readable, start, end , steps, maximum, strategy, relative, latency_ms)    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()    

def hsearch(writables, readable, range_min,  range_max, initial_step, resolution, noise_filtering_steps = 1, maximum = True, latency=0.0, relative = False, before_read=None, after_read=None, title=None, **pars):
    """Hill Climbing search: searches writables in decreasing steps to find a local maximum for the readable.
    Args:
        writables(list of Writable): Positioners set on each step.
        readable(Readable): Sensor to be sampled.
        range_min(list of float): minimum positions of writables.
        range_max(list of float): maximum positions of writables.
        initial_step(float or list of float):initial step size for for each writable.
        resolution(float or list of float): resolution of search for each writable (minimum step size).
        noise_filtering_steps(int): number of aditional steps to filter noise
        maximum (bool , optional): if True (default) search maximum, otherwise minimum.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        relative (bool, optional): if true, range_min and range_max positions are relative to current at 
            start of the scan
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
        pars(dict, optional): options to this scan (see set_exec_pars).

    Returns:
        SearchResult object.

    """    
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readable=string_to_obj(readable)
    range_min=to_list(range_min)
    range_max=to_list(range_max)   
    initial_step = to_list(initial_step) 
    resolution = to_list(resolution) 
    scan = HillClimbingSearch(writables,readable, range_min, range_max , initial_step, resolution, noise_filtering_steps, maximum, relative, latency_ms)    
    scan.before_read=before_read
    scan.after_read=after_read
    scan.setPlotTitle(title)
    get_context().setCommandPars(scan, pars);
    scan.start()
    return scan.getResult()    

###################################################################################################
#Data plotting
###################################################################################################

def plot(data, name = None, xdata = None, ydata=None, title=None):    
    """Request one or multiple plots of user data (1d, 2d or 3d).

    Args:
        data: array or list of values. For multiple plots, array of arrays or lists of values.
        name(str or list of str, optional): plot name or list of names (if multiple plots). 
        xdata: array or list of values. For multiple plots, array of arrays or lists of values.
        ydata: array or list of values. For multiple plots, array of arrays or lists of values.
        title(str, optional): plotting window name.

    Returns:
        ArrayList of Plot objects.

    """      
    if isinstance(data, ch.psi.pshell.data.Table):
        if is_list(xdata):
            xdata = to_array(xdata, 'd')
        return get_context().plot(data,xdata,name,title)

    if isinstance(data, ch.psi.pshell.scan.ScanResult):
        return get_context().plot(data,title)

    if (name is not None) and is_list(name):
        if len(name)==0:
            name=None;
        else:
            if (data==None):
                data = []
                for n in name:
                    data.append([])
        plots = java.lang.reflect.Array.newInstance(Class.forName("ch.psi.pshell.data.PlotDescriptor"), len(data))                    
        for i in range (len(data)):         
            plotName = None if (name is None) else name[i]
            x = xdata 
            if is_list(x) and len(x)>0 and (is_list(x[i]) or isinstance(x[i] , java.util.List) or isinstance(x[i],PyArray)):
                x = x[i]                
            y = ydata
            if is_list(y) and len(y)>0 and (is_list(y[i]) or isinstance(y[i] , java.util.List) or isinstance(y[i],PyArray)):
                y = y[i]
            plots[i] =  PlotDescriptor(plotName , to_array(data[i], 'd'), to_array(x, 'd'), to_array(y, 'd'))            
        return get_context().plot(plots,title)
    else:        
        plot = PlotDescriptor(name, to_array(data, 'd'), to_array(xdata, 'd'), to_array(ydata, 'd'))
        return get_context().plot(plot,title)

def get_plots(title=None):    
    """Return all current plots in the plotting window given by 'title'.

    Args:
        title(str, optional): plotting window name.

    Returns:
        ArrayList of Plot objects.

    """

    return get_context().getPlots(title)

def get_plot_snapshots(title = None, file_type = "png", temp_path = get_context().setup.getContextPath()):
    """Returns list with file names of plots snapshots from a plotting context.

    Args:
        title(str, optional): plotting window name.
        file_type(str, optional): "png", "jpg", "bmp" or "gif"
        temp_path(str, optional): path where the files will be generated.

    Returns:
        list of strings

    """
    time.sleep(0.1) #Give some time to plot to be finished - it is not sync  with acquisition
    ret = []
    for p in get_plots(title):
        file_name = os.path.abspath(temp_path + "/" + p.getTitle() + "." + file_type)
        p.saveSnapshot(file_name , file_type)
        ret.append(file_name)
    return ret

###################################################################################################
#Data access functions
###################################################################################################

def load_data(path, index=0, shape=None):    
    """Read data from the current persistence context or from data files.

    Args:
        path(str): Path to group or dataset relative to the persistence context root.
                   If in the format 'root|path' then read from path given by 'root'.
        index(int or listr, optional): 
                if integer, data depth (used for 3D datasets returning a 2d matrix)
                If a list, specifies the full coordinate for multidimensional datasets.
        shape(list, optional): only valid if index is a list, provides the shape of the data array.
                In this case return a flattened a one-dimensional array.

    Returns:
        Data array

    """    
    if index is not None and is_list(index):
        slice = get_context().dataManager.getData(path, index, shape)
    else:
        slice = get_context().dataManager.getData(path, index)
    return slice.sliceData

def get_attributes(path):    
    """Get the attributes from the current persistence context or from data files.

    Args:
        path(str): Path to group or dataset relative to the current persistence context root.
                   If in the format 'root|path' then read from path given by 'root'.
    Returns:
        Dictionary

    """    
    return get_context().dataManager.getAttributes(path)

def save_dataset(path, data, type='d'):
    """Save data into a dataset within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        type(str, optional): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
                              'd' = double, 'c' = char, 's' = String,  'o' = Object 
                   default: 'd' (convert data to array of doubles)
        data (array or list): data to be saved
    Returns:
        Dictionary

    """    
    data = to_array(data, type)
    get_context().dataManager.setDataset(path,data)

def create_group(path):
    """Create an empty dataset within the current persistence context.

    Args:
        path(str): Path to group relative to the current persistence context root.
    Returns:
        None

    """    
    get_context().dataManager.createGroup(path)

def create_dataset(path, type, unsigned=False, dimensions=None):
    """Create an empty dataset within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        type(str): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
                              'd' = double, 'c' = char, 's' = String,  'o' = Object 
        unsigned(boolean, optional): create a dataset of unsigned type.
        dimensions(tuple of int, optional): a 0 value means variable length in that dimension.
    Returns:
        None

    """    
    get_context().dataManager.createDataset(path, ScriptUtils.getType(type), unsigned, dimensions)

def create_table(path, names, types=None, lengths=None):
    """Create an empty table (dataset of compound type) within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        names(list of strings): name of each column
        types(array of str):  'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
                              'd' = double, 'c' = char, 's' = String,  'o' = Object 
                              Note:A '[' prefix on type name indicates an array type.
        lengths(list of int): the array length for each columns(0 for scalar types).
    Returns:
        None

    """    
    type_classes = []
    if (types is not None):
        for i in range (len(types)):
            type_classes.append(ScriptUtils.getType(types[i]))
    get_context().dataManager.createDataset(path, names, type_classes, lengths)

def append_dataset(path, data, index=None, type='d', shape=None):
    """Append data to dataset.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(number or array or list): name of each column.
        index(int or list, optional): if set then add the data in a specific position in the dataset.
                If integer is the index in an array (data must be 1 order lower than dataset)
                If a list, specifies the full coordinate for multidimensional datasets.
        type(str, optional): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
                              'd' = double, 'c' = char, 's' = String,  'o' = Object 
                   default: 'd' (convert data to array of doubles)
        shape(list, optional): only valid if index is a list, provides the shape of the data array.
                In this case data must be a flattened one-dimensional array.
    Returns:
        None

    """    
    data = to_array(data, type)
    if index is None:
        get_context().dataManager.appendItem(path, data)
    else:
        if is_list(index):
            if shape is None: 
                shape = [len(index)]           
            get_context().dataManager.setItem(path, data, index, shape)
        else:
            get_context().dataManager.setItem(path, data, index)

def append_table(path, data):
    """Append data to a table (dataset of compound type) 

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(list): List of valus for each column of the table. Array types can be expressed as lists.
    Returns:
        None

    """    
    #data = to_array(data)
    if is_list(data):
        arr = java.lang.reflect.Array.newInstance(Class.forName("java.lang.Object"),len(data))
        for i in range (len(data)):
            if is_list(data[i]):
                arr[i] = to_array(data[i], 'd')
            else:
                arr[i] = data[i]        
        data=arr
    get_context().dataManager.appendItem(path, data)

def flush_data():
    """Flush all data files immediately.

    Args:
        None
    Returns:
        None
    """
    get_context().dataManager.flush()

def set_attribute(path, name, value, unsigned = False):
    """Set an attribute to a group or dataset.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        name(str): name of the atttribute
        value(Object): the attribute value
        unsigned(bool, optional):  if applies, indicate if  value is unsigned.
    Returns:
        None
    """
    if is_list(value):
        value = Convert.toStringArray(to_array(value))
    get_context().dataManager.setAttribute(path, name, value, unsigned)

def log(log):        
    """Writes a log to the system log and data context - if there is an ongoing scan or script execution.        

    Args:        
         log(str): Log string.

    Returns:
        None
    """  
    get_context().scriptingLog(str(log))
    get_context().dataManager.appendLog(str(log))

def set_exec_pars(**args):
    """  Configures the script execution parameters, overriding the system configuration.
    
    Args: 
      args(dictionary). Keys:
        name(str, optional): value of the {name} tag. Default is the running script name 
                             (or "scan" in the case of  a command line scan command.)
        type(str, optional): value of the {type} tag. Default is empty.
                             This field can be used to store data in  sub-folders of standard location.
        path(str, optional):  If defined provides the full path name for data output root (overriding config))
                             The tag {data} can be used to enter a path relative to the standard data folder.
        layout(str, optional): Overrides default data layout.
        depth_dim(int, optional): dimension of 2d-matrixes in 3d datasets.
        persist(bool, optional): Overrides the configuration option to auto save scan data.
        flush(bool, optional): Overrides the configuration option to flush file on each record.
        accumulate(bool, optional): Overrides the configuration option to release scan records. 
                                    If false disable accumulation of scan records to scan result.
        preserve(bool, optional): Overrides the configuration option to preserve device types. 
                                  If false all values are converted to double.
        open(bool, optional): If true opens data output root (instead of only doing in the first data access call)
                              If false closes output root, if open.
        reset(bool, optional): If true reset the scan counter - the {count} tag and set the timestamp to now.
        group(str, optional): Overrides default layout group name for scans
        defaults(bool, optional): If true restore the original execution parameters.        
        
        Graphical preferences can also be set. Keys are equal to lowercase of Preference enum:
        "plot_disabled", "table_disabled", "enabled_plots", "plot_types", "print_scan", "auto_range", 
        "manual_range","domain_axis", "status".        
        See set_preference for more information.
        
        Shortcut entries: "line_plots": list of devices with enforced line plots.
    """
    get_context().setExecutionPars(args)

#TODO: Change return to dictionary
def get_exec_pars():
    """ Returns script execution parameters.

    Returns:
        ExecutionContext object. Fields: 
            name (str): execution name - {name} tag.
            type (str): execution type - {type} tag.
            path (str): output data root.
            open (bool): true if the output data root has been opened.
            layout (str): data output layout. If None then using the configuration.
            persist (bool): auto save scan data option. 
            flush (bool): flush file on each record.
            index (int): current scan index.
            group (str): data group currently used for scan data storage. 
                         if no ongoing scan return "/" if within a script, or else None if a console command.
            scanPath (str): dataset or group corresponding to current scan.
            scan (Scan): reference to current scan, if any
            source (CommandSource): return the source of the script or command.
            background (bool): return False if executing in main interpreter thread .
            aborted (bool): True if execution has been aborted
    """
    return get_context().getExecutionPars()

###################################################################################################
#EPICS channel access
###################################################################################################

def _adjust_channel_value(value, var_type=None):
    if (value is None):
        return value
    if (var_type is not None):
        if is_list(value):    
            var_type = var_type.replace(',','').replace('[','')
            ret = []
            for item in value:
               ret.append(_adjust_channel_value(item), var_type) 
            value = ret
        else:
            var_type = var_type.lower()
            if var_type=='b':
                value = byte(value)
            elif var_type=='i':
                value = short(value)
            elif var_type=='l':
                value = int(value)
            elif var_type=='f':
                value = float(value)
            elif var_type=='d':
                value = float(value)
            elif var_type=='s':
                value = str(value)

    if isinstance(value,tuple):
        value =  list(value)
    if isinstance(value,list):
        list_type = type(value[0])
        array_types = {
            int: "i",
            long: "l",
            float:"d",
            str:Class.forName("java.lang.String"),    
        }        
        array_type = array_types.get(type(value[0]),'d')
        array = PyArray(array_type)        
        array.fromlist(value)         
        value=array
    return value

def caget(name, type=None, size=None):    
    """Reads an Epics PV.

    Args:
        name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array: values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.

    Returns:
        PV value

    """          
    return Epics.get(name, Epics.getChannelType(type), size)

def cawait(name, value, timeout=None, comparator=None, type=None, size=None):    
    """Wait for a PV to have a given value.

    Args:
        name(str): PV name
        value (obj): value to compare to
        timeout(float, optional): time in seconds to wait. If None, waits forever.
        comparator(java.util.Comparator, optional): if None, equality.
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array: values: '[b', '[i,', '[l', '[d', '[s'.
         size (int, optional): for arrays, number of elements to be read. Default read all.

    Returns:
        None
    """          
    if (timeout is not None):
        timeout = int(timeout*1000)
    value = _adjust_channel_value(value)
    Epics.waitValue(name, value, comparator, timeout, Epics.getChannelType(type), size)

def caput(name, value, timeout = None):   
    """Writes to an Epics PV.

    Args:
        name(str): PV name
        value(scalar, string or array): new PV value.
        timeout(int, optional): timeout in seconds to the write. If None waits forever to completion.                    

    Returns:
        None
    """  
    value=_adjust_channel_value(value)
    if (timeout is not None):
        timeout = int(timeout*1000)
    return Epics.put(name, value, timeout)

def caputq(name, value):        
    """Writes to an Epics PV and does not wait.

    Args:
        name(str): PV name
        value(scalar, string or array): new PV value.

    Returns:
        None
    """  
    value=_adjust_channel_value(value)
    return Epics.putq(name, value)

def camon(name, type=None, size=None, wait = sys.maxint):     
    """Install a monitor to an Epics PV and print value changes.

    Args:
        name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array: values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
        wait (int, optional): blocking time for this function. By default blocks forever.
    Returns:
        None

    """          
    val = lambda x: x.tolist() if isinstance(x,PyArray) else x

    class MonitorListener(java.beans.PropertyChangeListener):
        def propertyChange(self, pce):
            print val(pce.getNewValue())

    channel = create_channel(name, type, size)
    print val(channel.getValue())
    channel.setMonitored(True)
    channel.addPropertyChangeListener(MonitorListener())

    try:
        time.sleep(wait)
    finally:
        Epics.closeChannel(channel)        
    
def create_channel_device(channel_name, type=None, size=None, device_name=None):
    """Create a device from an EPICS PV.

    Args:
        channel_name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array: values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
        device_name (str, optional): device name (if  different from hannel_name.
    Returns:
        None

    """          
    dev = Epics.newChannelDevice(channel_name if (device_name is None) else device_name , channel_name, Epics.getChannelType(type))
    if get_context().isSimulation():
        dev.setSimulated()
    dev.initialize()
    if (size is not None):
        dev.setSize(size)
    return dev
    
def create_channel(name, type=None, size=None):        
    return Epics.newChannel(name, Epics.getChannelType(type), size)

class Channel(java.beans.PropertyChangeListener, Writable, Readable):
    def __init__(self, channel_name, type = None, size = None, callback=None, alias = None):
        """ Create an object that encapsulates an Epics PV connection.
        Args: 
            channel_name(str):name of the channel
            type(str, optional): type of PV. By default gets the PV standard field type.
                Scalar values: 'b', 'i', 'l', 'd', 's'.
                Array: values: '[b', '[i,', '[l', '[d', '[s'.
            size(int, optional): the size of the channel
            callback(function, optional): The monitor callback.
            alias(str): name to be used on scans.
        """  
        self.channel = create_channel(channel_name, type, size)
        self.callback = callback
        if alias is not None:
            set_device_alias(self, alias)
        else:
            set_device_alias(self, channel_name)

    def get_name(self):
        """Return the name of the channel.
        """  
        return self.channel.name

    def get_size(self):
        """Return the size of the channel.
        """  
        return self.channel.size

    def set_size(self, size):
        """Set the size of the channel.
        """  
        self.channel.size = size

    def is_connected(self):
        """Return True if channel is connected.
        """  
        return self.channel.connected

    def is_monitored(self):
        """Return True if channel is monitored
        """  
        return self.channel.monitored

    def set_monitored(self, value):
        """Set a channel monitor to trigger the callback function defined in the constructor.
        """  
        self.channel.monitored = value
        if (value):
            self.channel.addPropertyChangeListener(self)
        else:
            self.channel.removePropertyChangeListener(self)

    def propertyChange(self, pce):        
        if pce.getPropertyName() == "value":
            if self.callback is not None:                
                self.callback(pce.getNewValue())

    def put(self, value, timeout=None):
        """Write to channel and wait value change. In the case of a timeout throws a TimeoutException.
        Args: 
            value(obj): value to be written
            timeout(float, optional): timeout in seconds. If none waits forever.
        """  
        if (timeout==None):
            self.channel.setValue(value)
        else:
            self.channel.setValueAsync(value).get(int(timeout*1000), java.util.concurrent.TimeUnit.MILLISECONDS);

    def putq(self, value):
        """Write to channel and don't wait.
        """  
        self.channel.setValueNoWait(value)

    def get(self, force = False):
        """Get channel value.
        """  
        return self.channel.getValue(force)

    def wait_for_value(self, value, timeout=None, comparator=None):
        """Wait channel to reach a value, using a given comparator. In the case of a timeout throws a TimeoutException.
        Args: 
            value(obj): value to be verified.                
            timeout(float, optional): timeout in seconds. If None waits forever.
            comparator (java.util.Comparator, optional). If None, uses Object.equals.
        """  
        if comparator is None:
            if timeout is None:
                self.channel.waitForValue(value)
            else:
                self.channel.waitForValue(value, int(timeout*1000))
        else:
            if timeout is None:
                self.channel.waitForValue(value, comparator)
            else:
                self.channel.waitForValue(value, comparator, int(timeout*1000))

    def close(self):
        """Close the channel.
        """  
        self.channel.destroy()     

    #Writable interface
    def write(self, value):
        self.put(value)

    #Readable interface
    def read(self):        
        return self.get()

        

###################################################################################################
#Concurrent execution 
###################################################################################################

class Callable(java.util.concurrent.Callable):
    def __init__(self, method, *args):
        self.method = method
        self.args = args
    def call(self):
        #try:
            return self.method(*self.args)
        #except:
        #    traceback.print_exc(file=sys.stderr)


def fork(*functions):
    """Start execution of functions in parallel. 

    Args:
        *functions(function references)

    Returns:
        List of callable objects
    """      
    callables = []
    for m in functions:
        if is_list(m):
            callables.append(Callable(m[0],*m[1]))
        else:
            callables.append(Callable(m))
    return Threading.fork(callables)

def join(futures):
    """Wait parallel execution of functions.

    Args:
        futures(list of Future) : as returned from fork

    Returns:
        None
"""      
    try:
        return Threading.join(futures)
    except java.util.concurrent.ExecutionException, ex:
        raise ex.getCause()
        
def parallelize(*functions):
    """Equivalent to fork + join

    Args:
        *functions(function references)

    Returns:
        None
    """   
    futures = fork(*functions)
    return join(futures)


###################################################################################################
#Script evaluation and background task control.
###################################################################################################
def run(script_name, args = None, locals = None):
    """Run script: can be absolute path, relative, or short name to be search in the path.
    Args:
        args(Dict ot List): Sets Sys.argv (if list) or gobal variables(if dict) to the script.
        locals(Dict): If not none sets the locals()for the runing script.
                      If locals is used then script definitions will not go to global namespace.

    Returns:
        The script return value (if set with set_return)
    """  
    global _
    script = get_context().scriptManager.library.resolveFile(script_name)
    if script is not None and os.path.isfile(script):
        get_context().startScriptExecution(args)
        _ = None
        if args is None:
            pass
        elif isinstance(args,tuple):
            sys.argv =  list(args)
        elif isinstance(args,list):
            sys.argv = args
        else:            
            for arg in args.keys():
                globals()[arg] = args[arg]
        if (locals is None):
            execfile(script, globals())
        else:
            execfile(script, globals(), locals)
        return _
    #print >> sys.stderr, "Invalid script: " + str(script_name)
    raise IOError("Invalid script: " + str(script_name))


def abort():
    """Abort the execution of ongoing task. It can be called from the script to quit.

    Args:                 
        None

    Returns:
        None
    """
    #Cannot be on script execution thread
    fork(get_context().abort)  


def start_task(script, delay = 0.0, interval = -1):
    """Start a background task

    Args:        
         script(str): Name of the script implementing the task
         delay(float, optional): time in seconds for the first execution. 
                Default starts immediately.
         interval(float, optional): time in seconds for between execution. 
                If negative (default), single-execution.

    Returns:
        None
    """      
    delay_ms=int(delay*1000)
    interval_ms=int(interval*1000) if (interval>=0) else int(interval)
    get_context().taskManager.create(script, delay_ms, interval_ms)
    get_context().taskManager.start(script)


def stop_task(script, force = False):
    """Stop a background task

    Args:        
         script(str): Name of the script implementing the task
         force(boolean, optional): interrupt current execution, if running

    Returns:
        None
    """      
    get_context().taskManager.remove(script, force)


def set_return(value):
    """Sets the script return value. This value is returned by the "run" function.

    Args:
        value(Object): script return value.

    Returns:
        None
    """  
    global _
    #In Jython, the output of last statement is not returned  when running a file so we have to use a return variable
    if __name__ == "__builtin__":
        _=value         #Used when running file
    else:
        #if startup is imported, cannot set global
        caller = inspect.currentframe().f_back    
        caller.f_globals["_"]=value
        
    return value    #Used when parsing file  

###################################################################################################
#Versioning tools
###################################################################################################

def commit(message, force = False):
    """Commit the changes to the repository. If manual commit is not configured then there is no need to call this function: commits are made as needed.

    Args:
        message(str): commit message
        force(bool, optional): if False, raises exception if no change detected in repo

    Returns:
        None
    """
    get_context().commit(message, force)

def diff():
    """Return list of changes in the repository

    Args:
        None

    Returns:
        None
    """   
    return get_context().diff()

def checkout_tag(tag):
    """Checkout a tag name.

    Args:
        tag(str): tag name.

    Returns:
        None
    """
    get_context().checkoutTag(tag)

def checkout_branch(tag):
    """Checkout a local branch name.

    Args:
        tag(str): branch name.

    Returns:
        None
    """
    get_context().checkoutLocalBranch(tag)

def pull_repository():
    """Pull from remote repository.

    """
    get_context().pullFromUpstream()

def push_repository(all_branches=True, force=False):
    """Push to remote repository.

    Args:
        all_branches(boolean, optional): all branches or just current.
        force(boolean, optional): force flag.

    Returns:
        None
    """
    get_context().pushToUpstream(all_branches, force)

def cleanup_repository():
    """Performs a repository cleanup.

    Args:
        None

    Returns:
        None
    """    
    get_context().cleanupRepository()


###################################################################################################
#Device Pool functions
###################################################################################################
def get_device(device_name):
    """Returns a configured device (or imaging source) by its name. 

    Args:
        device_name(str): name of the device.

    Returns:
        device
    """    
    return get_context().devicePool.getByName(device_name)

def add_device(device, force = False):
    """Add  a device (or imaging source) to the device pool.

    Args:
        device(Device or Source): device object.
        force(boolean, optional): if true then dispose existing device with same name. 
                    Otherwise will fail in case of name clash.

    Returns:
        True if device was added, false if was already in the pool, or exception in case of name clash.
    """
    if get_context().devicePool.contains(device):
        return False
    if force:
        dev = get_context().devicePool.getByName(device.getName())
        if dev is not None:
            remove_device(dev)
    return get_context().devicePool.addDevice(device)

def remove_device(device):
    """Remove a device (or imaging source) from the device pool.

    Args:
        device(Device or Source): device object.

    Returns:
        bool: true if device was removed.

    """
    return get_context().devicePool.removeDevice(device)

def set_device_alias(device, alias):
    """Set a device alias to be used in scans (datasets and plots).

    Args:
        device(Device): device object.
        alias(str): replace device name in scans.

    Returns:
        None
    """
    get_context().dataManager.setAlias(device, alias)

def stop():
    """Stop all devices implementing the Stoppable interface.

    Args:                 
        None

    Returns:
        None
    """
    get_context().stopAll()

def update():
    """Update all devices.

    Args:                 
        None

    Returns:
        None
    """
    get_context().updateAll()

def reinit(dev = None):
    """Re-initialize devices.

    Args:                 
        dev(Device, optional): the device to be re-initialized. 
                               If  None re-initialize all devices not yet initialized.

    Returns:
        List with devices not initialized.
    """
    return to_list(get_context().reinit())

def create_averager(dev, count, interval=0.0, name = None):
    """Creates and initializes and averager for dev.

    Args:                 
        dev(Device): the source device
        count(int): number of samples
        interval(float, optional): sampling interval in seconds.
                                   If less than zero, sampling is made on data change event.

    Returns:
        Averager device
    """
    dev = string_to_obj(dev)
    av = Averager(dev, count, int(interval*1000)) if (name is None) else Averager(name, dev, count, int(interval*1000))
    av.initialize()
    return av

###################################################################################################
#Standard libraries management
###################################################################################################
if __name__ == "__builtin__":
    ca_channel_path=os.path.join(get_context().setup.getStandardLibraryPath(), "epics")
    sys.path.append(ca_channel_path)
    #This is to destroy previous context of _ca (it is not shared with PShell)
    if run_count > 0:
        if sys.modules.has_key("_ca"):
            import _ca
            _ca.initialize()

###################################################################################################
#Mathematical functions
###################################################################################################

def arrmul(a, b):    
    """Multiply 2 series of the same size. 

    Args:

        a(list, tuple, array ...): subscriptable object containing numbers
        b(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        List 

    """
    return map(mul, a, b)

def arrdiv(a, b):    
    """Divide 2 series of the same size. 

    Args:

        a(list, tuple, array ...): subscriptable object containing numbers
        b(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        List 

    """
    return map(truediv, a, b)

def arradd(a, b):    
    """Add 2 series of the same size. 

    Args:

        a(list, tuple, array ...): subscriptable object containing numbers
        b(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        List 

    """
    return map(add, a, b)

def arrsub(a, b):    
    """Subtract 2 series of the same size. 

    Args:

        a(list, tuple, array ...): subscriptable object containing numbers
        b(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        List 

    """
    return map(sub, a, b)

def arrabs(a):    
    """Returns the absolute of all elements in series.

    Args:

        a(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        List 

    """
    return map(abs, a)

def arroff(a, value = "mean"):
    """Subtract offset to all elemets in series.

    Args:

        a(list, tuple, array ...): subscriptable object containing numbers
        type(int or str, optional): value to subtract from the array, or "mean" or "min".
        
    Returns:
        List 

    """    
    if value=="mean":
        value = mean(a)
    elif value=="min":
        value = min(a)
    return [x-value for x in a]     

def mean(data):
    """Calculate the mean of a sequence.

    Args:
        data(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        Mean of the elements in the object.

    """
    return reduce(lambda x, y: x + y, data) / len(data)

def variance(data):
    """Calculate the variance of a sequence.

    Args:
        data(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        Variance of the elements in the object.

    """
    c = mean(data)
    ss = sum((x-c)**2 for x in data)
    return ss/len(data) 

def stdev(data):
    """Calculate the standard deviation of a sequence.

    Args:
        data(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        Standard deviation of the elements in the object.

    """
    return variance(data)**0.5


def center_of_mass(data, x = None):
    """Calculate the center of mass of a series, and its rms. 

    Args:

        data(list, tuple, array ...): subscriptable object containing numbers
        x(list, tuple, array ..., optional): x coordinates

    Returns:
        Tuple (com, rms)

    """
    if x is None:
        x = Arr.indexesDouble(len(data))
    data_sum = sum(data)    
    if (data_sum==0):
        return float('nan')
    xmd = arrmul( x, data)
    com = sum(xmd) / data_sum
    xmd2 = arrmul( x, xmd)
    com2 = sum(xmd2) / data_sum
    rms = math.sqrt(abs(com2 - com * com))
    return (com, rms)    

def poly(val, coefs):
    """Evaluates a polinomial: (coefs[0] + coefs[1]*val + coefs[2]*val^2...

    Args:
        val(float): value
        coefs (list of loats): polinomial coefficients 
    Returns:
        Evaluated function for val 

    """
    r = 0
    p = 0
    for c in coefs:
        r = r + c * math.pow(val, p)
        p = p + 1
    return r

def histogram(data, range_min = None, range_max = None, bin = 1.0):
    """Creates histogram on data.

    Args:
        data (tuple, array, ArrayList or Array): input  data can be multi-dimensional or nested.
        range_min (int, optional): minimum histogram value. Default is floor(min(data))
        range_max (int, optional): maximul histogram value. Default is ceil(max(data))
        bin(int or float, optional): if int means number of bins. If float means bin size. Default = 1.0.
    Returns:
        tuple: (ydata, xdata)

    """
    if range_min is None: range_min = math.floor(min(flatten(data)))
    if range_max is None: range_max = math.ceil(max(flatten(data)))
    if type(bin) is float:
        bin_size = bin
        n_bin =  int(math.ceil(float(range_max - range_min)/bin_size))
    else:
        n_bin = bin
        bin_size = float(range_max - range_min)/bin
    
    result = [0] * n_bin  
    for  d in flatten(data):
        b = int( float(d - range_min) / bin_size)
        if (b >=0) and (b < n_bin):
          result[b] = result[b] + 1
    return (result, frange(range_min, range_max, bin_size))

def _turn(p, q, r):
    return cmp((q[0] - p[0])*(r[1] - p[1]) - (r[0] - p[0])*(q[1] - p[1]), 0)

def _keep(hull, r):
    while len(hull) > 1 and _turn(hull[-2], hull[-1], r) != 1:
        hull.pop()
    return (not len(hull) or hull[-1] != r) and hull.append(r) or hull    

def convex_hull(point_list=None, x=None, y=None):
    """Returns the convex hull from a list of points. Either point_list or x,y is provided.
       (Alhorithm taken from http://tomswitzer.net/2010/03/graham-scan/)
    Args:
        point_list (array of tuples, optional): arrays of the points
        x (array of float, optional): array with x coords of points
        y (array of float, optional): array with y coords of points
    Returns:
        Array of points or (x,y) 

    """
    is_point_list = point_list is not None
    if not point_list:
        point_list=[]
        for i in range(len(x)): 
            if((x[i] is not None) and (y[i] is not None)): point_list.append((x[i], y[i]))            
    point_list.sort()
    lh,uh = reduce(_keep, point_list, []), reduce(_keep, reversed(point_list), [])
    ret = lh.extend(uh[i] for i in xrange(1, len(uh) - 1)) or lh     
    if not is_point_list:
        x, y = [], []
        for i in range(len(ret)):
            x.append(ret[i][0])
            y.append(ret[i][1])
        return (x,y)
    return ret 

###################################################################################################
#Utilities
###################################################################################################

def exec_cmd(cmd):
    """Executes a shell command. If errors happens raises an exception.

    Args:
        cmd (str): command process input.
    Returns:
        Output of command process.
    """
    import subprocess
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    (ret, err) = proc.communicate()
    if (err is not None) and err!="":
        raise Exception(err)    
    return ret

def exec_cpython(script_name, args = [], method_name = None, python_name = "python"):
    """Executes an external cpython process.

    Args:
        script_name (str): name of the script (can be absolute or relative to script folder).
        args(list, optional): arguments to python process (or parameters to method, if not None)
        method_name (str, optional): if defined indicates a method to be called.
        python_name (str, optional): name of executable
    Returns:
        Return of python process.
    """
    if method_name is None:
        script = get_context().scriptManager.library.resolveFile(script_name)
        if script is None : 
            script= os.path.abspath(script_name)
        c = python_name + " " + script + " "
        if args is not None and (len(args)>0):
            for arg in args:
                c = c + str(arg) + " "    
        return exec_cmd(c)
    else:
        #Calling a method
        import json
        import tempfile
        import java.lang.Thread as Thread    
        script = os.path.abspath(get_context().scriptManager.library.resolveFile(script_name))         
        with open(get_context().setup.getContextPath()+ "/Temp" + str(Thread.currentThread().getId())+".py", "wb") as f:        
            f.write(("script = '" +script +"'\n").replace('\\', '\\\\'))
            f.write("function = '" +method_name +"'\n")
            f.write("jsonargs = '" + json.dumps(args) +"'\n")
            f.write("""import sys
import json
import os
args =json.loads(jsonargs)
i = script.rfind(os.sep)
module = script[i+1:-3]
sys.path.insert(1,script[:i+1])
exec ('from ' + module + ' import ' + function + ' as function')
print (json.dumps(function(*args)))
""")
        f.close()      
        ret = exec_cpython(os.path.abspath(f.name), python_name = python_name)        
        os.remove(f.name)
        ret = '\n'+ret[0:-len(os.linesep)]
        jsonret = ret[ret.rfind('\n')+1:].strip()  
        return json.loads(jsonret)
    

def flatten(data):
    """Flattens multi-dimentional or nested data.

    Args:
        data (tuple, array, ArrayList or Array): input data
    Returns:
        Iterator on the flattened data.

    """
    if isinstance(data,PyArray):
        if not data.typecode.startswith('['):
            return data
    
    import itertools
    return itertools.chain(*data)

#Float range -> Useful for scanning is a range
def frange_gen(start, finish, step):
    while ((step >= 0.0) and (start <= finish)) or ((step < 0.0) and (start >= finish)):
      yield start
      start += step

def frange(start, finish, step, enforce_finish = False, inclusive_finish = False):
    """Create a list with a range of float values (a float equivalent to "range").

    Args:
        start(float): start of range.
        finish(float): end of range.
        step(float): step size.
        enforce_finish(boolean, optional): adds the final element even if range was not exact.
        inclusive_finish(boolean, optional): if false finish is exclusive (like in "range").

    Returns:
        list

    """
    step = float(step)    
    ret = list(frange_gen(start, finish, step))
    if len(ret) > 0:
        if inclusive_finish == False:
            if ret[-1]==finish:
                del ret[-1]
        if enforce_finish and ret[-1]!=finish:
                ret.append(finish)
    return ret

def inject():
    """Restore initial globals: re-inject devices and startup variables to the interpreter.

    Args:
        None

    Returns:
        None

    """  
    if __name__ == "__builtin__":
        get_context().injectVars()
    else:
        caller = inspect.currentframe().f_back    
        caller.f_globals.update(get_context().scriptManager.injections)        

def notify(subject, text, attachments = None, to=None):
    """Send email message.

    Args:
        subject(str): Message subject.
        text(str): Message body.
        attachments(list of str, optional): list of files to be attached (expansion tokens are allowed).
        to (list ofd str, optional): recipients. If None uses the recipients defined in mail.properties.
    Returns:
        None

    """  
    get_context().notify(subject, text, to_list(attachments), to_list(to))

def string_to_obj(o):
    if is_string(o):
        if "://" in o:
            return ch.psi.pshell.core.UrlDevice(o)
        return eval(o)
    elif is_list(o):
        ret = []
        for i in o:
            ret.append(string_to_obj(i))
        return ret
    return o    

def _getBuiltinFunctions(filter = None):    
    ret = []
    for name in globals().keys():
        val = globals()[name]
        if type(val) is types.FunctionType:
            if filter is None or filter in name:
                #Only "public" documented functions
                if not name.startswith('_') and (val.__doc__ is not None):       
                    ret.append(val) 
    return to_array(ret)


def _getBuiltinFunctionNames(filter = None):
    ret = []
    for function in _getBuiltinFunctions(filter):
        ret.append(function.__name__) 
    return to_array(ret)

def _getFunctionDoc(function):      
    if type(function) is str:
        if function not in globals():
            return
        function = globals()[function]
    if type(function) is types.FunctionType and '__doc__' in dir(function):
        spec = inspect.getargspec(function)
        (args,defs) = (spec[0], spec[3])
        if defs is not None:
            for i in range (len(defs)):
                index = len(args) - len(defs) + i
                args[index] = args[index] + " = " + str(defs[i])
        #varargs
        if spec[1] is not None:
            args.append('*' + spec[1])
        #keywords
        if spec[2] is not None:
            args.append('**' + spec[2])
        
        return function.__name__ + "(" + ", ".join(args) + ")" + "\n\n" + function.__doc__    
    
def help(object = None):
    """
    Print help message for function or object (if available).

    Args:
        object (any, optional): function or object to get help. 
                    If null prints a list of the builtin functions.

    Returns:
        None
    
    """
    if object is None:        
        print "Built-in functions:"
        for f in _getBuiltinFunctionNames():
            print "\t" + f
    else:
        if type(object) is types.FunctionType:
            print _getFunctionDoc(object)
        elif '__doc__' in dir(object):
            #The default doc is now shown
            import org.python.core.BuiltinDocs.object_doc
            if object.__doc__ != org.python.core.BuiltinDocs.object_doc:
                print object.__doc__


###################################################################################################
#UI interaction
###################################################################################################

def set_status(status):
    """Set the application status.

    Args:
        status(str): new status.

    Returns:
        None
    """
    set_preference(Preference.STATUS, status)

def setup_plotting( enable_plots=None, enable_table=None,plot_list = None, line_plots = None, range = None, domain=None, defaults=None):
    if defaults == True: set_preference(Preference.DEFAULTS, True)
    if enable_plots is not None: set_preference(Preference.PLOT_DISABLED, not enable_plots)
    if enable_table is not None: set_preference(Preference.TABLE_DISABLED, not enable_table)
    if plot_list is not None: set_preference(Preference.ENABLED_PLOTS, None if plot_list == "all" else plot_list)
    if line_plots is not None:
        plots = None
        if line_plots != "none":            
            plots = {}
            for plot in line_plots:
                plots[plot]=1
        set_preference(Preference.PLOT_TYPES, plots)
    if range is not None:
         if range == "none":     
            set_preference(Preference.AUTO_RANGE, None)
         elif range == "auto":
            set_preference(Preference.AUTO_RANGE, True)
         else:
            set_preference(Preference.MANUAL_RANGE, range)
    if domain is not None:
        set_preference(Preference.DOMAIN_AXIS, domain)
    

def set_preference(preference, value):
    """Hints to graphical layer:    

    Args:
        preference(Preference): Preference name
            Preference.PLOT_DISABLED  #enable/disable scan plot (True/False)
            Preference.TABLE_DISABLED  #enable/disable scan table (True/False)
            Preference.ENABLED_PLOTS #select Readables to be plotted (list of Readable or 
                String (Readable names))
            Preference.PLOT_TYPES #Dictionary or (Readable or String):(String or int) pairs 
                where the key is a plot name and the value is the desired plot type
            Preference.PRINT_SCAN  #Print scan records to console           
            Preference.AUTO_RANGE # Automatic range scan plots x-axis
            Preference.MANUAL_RANGE # Manually set scan plots x-axis
            Preference.DOMAIN_AXIS #Set the domain axis source: "Time", "Index", or a readable name. 
                                    Default(None): first positioner
            Preference.STATUS # set application status

        value(object): preference value

    Returns:
        None
    """  
    value = to_array(value, 'o') #If list then convert to Object array
    get_context().setPreference(preference, value)

def get_string(msg, default = None, alternatives = None, password = False):
    """
    Reads a string from UI
    Args:
        msg(str): display message.
        default(str, optional): value displayed when window is shown.
        alternatives(list of str, optional): if provided presents a combo box instead of an editing field.
        password(boolean, optional): if True hides entered characters.

    Returns:
        String entered of null if canceled
    """
    if password :
        return get_context().getPassword(msg, None)
    return get_context().getString(msg, str(default) if (default is not None) else None, alternatives)

def get_option(msg, type = "YesNoCancel"):
    """
    Gets an option from UI        
    Args:
        msg(str): display message.
        type(str, optional): 'YesNo','YesNoCancel' or 'OkCancel'

    Returns:
        'Yes', 'No', 'Cancel'

    """
    return get_context().getOption(msg, type)

def show_message(msg, title=None, blocking = True):
    """
    Pops a blocking message to UI
    
    Args:
        msg(str): display message.    
        title(str, optional): dialog title
    """
    get_context().showMessage(msg, title, blocking)

def show_panel(device, title=None):
    """
    Show, if exists, the panel relative to this device.
    
    Args:
        device(Device or str or BufferedImage): device
        title only apply to BufferedImage objects. For devices the title is the device name.
    """
    if type(device) is BufferedImage:
        device = DirectSource(title, device)
        device.initialize()
    if type(device) is str:
        device = get_device(device)
    return get_context().showPanel(device)

