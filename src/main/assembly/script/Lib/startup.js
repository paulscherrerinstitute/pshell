///////////////////////////////////////////////////////////////////////////////////////////////////
// Global definitions and built-in functions
///////////////////////////////////////////////////////////////////////////////////////////////////

load("nashorn:mozilla_compat.js");

importClass(Packages.jdk.nashorn.api.scripting.ScriptUtils);
importClass(java.beans.PropertyChangeListener)
importClass(java.lang.Thread)
importClass(java.awt.image.BufferedImage)
importClass(java.awt.Color)
importClass(java.awt.Font)
importClass(java.awt.Dimension)
importClass(java.io.File)

CommandSource = Java.type('ch.psi.pshell.core.CommandSource')
ContextListener = Java.type('ch.psi.pshell.core.ContextAdapter')
Context = Java.type('ch.psi.pshell.core.Context')
UrlDevice = Java.type('ch.psi.pshell.core.UrlDevice')
PlotDescriptor = Java.type('ch.psi.pshell.data.PlotDescriptor')
Table = Java.type('ch.psi.pshell.data.Table')
Device = Java.type('ch.psi.pshell.device.Device')
DeviceBase = Java.type('ch.psi.pshell.device.DeviceBase')
RegisterBase = Java.type('ch.psi.pshell.device.RegisterBase')
ProcessVariableBase = Java.type('ch.psi.pshell.device.ProcessVariableBase') 
ControlledVariableBase = Java.type('ch.psi.pshell.device.ControlledVariableBase') 
PositionerBase = Java.type('ch.psi.pshell.device.PositionerBase') 
MotorBase = Java.type('ch.psi.pshell.device.MotorBase') 
DiscretePositionerBase = Java.type('ch.psi.pshell.device.DiscretePositionerBase') 
MotorGroupBase= Java.type('ch.psi.pshell.device.MotorGroupBase') 
MotorGroupDiscretePositioner = Java.type('ch.psi.pshell.device.MotorGroupDiscretePositioner') 
ReadonlyRegisterBase = Java.type('ch.psi.pshell.device.ReadonlyRegisterBase') 
ReadonlyAsyncRegisterBase = Java.type('ch.psi.pshell.device.ReadonlyAsyncRegisterBase') 
RegisterCache = Java.type('ch.psi.pshell.device.RegisterCache') 
ReadonlyRegisterArray = Java.type('ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray') 
ReadonlyRegisterMatrix = Java.type('ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix') 
DummyPositioner = Java.type('ch.psi.pshell.device.DummyPositioner') 
DummyMotor = Java.type('ch.psi.pshell.device.DummyMotor') 
DummyRegister = Java.type('ch.psi.pshell.device.DummyRegister') 
Timestamp = Java.type('ch.psi.pshell.device.Timestamp') 
Interlock = Java.type('ch.psi.pshell.device.Interlock') 
Readable = Java.type('ch.psi.pshell.device.Readable') 
ReadableArray = Java.type('ch.psi.pshell.device.Readable.ReadableArray') 
ReadableMatrix = Java.type('ch.psi.pshell.device.Readable.ReadableMatrix') 
ReadableCalibratedArray = Java.type('ch.psi.pshell.device.Readable.ReadableCalibratedArray') 
ReadableCalibratedMatrix = Java.type('ch.psi.pshell.device.Readable.ReadableCalibratedMatrix') 
ArrayCalibration = Java.type('ch.psi.pshell.device.ArrayCalibration') 
MatrixCalibration = Java.type('ch.psi.pshell.device.MatrixCalibration') 
Writable = Java.type('ch.psi.pshell.device.Writable') 
WritableArray = Java.type('ch.psi.pshell.device.Writable.WritableArray') 
Stoppable = Java.type('ch.psi.pshell.device.Stoppable') 
Averager = Java.type('ch.psi.pshell.device.Averager') 
Delta = Java.type('ch.psi.pshell.device.Delta') 
DeviceListener = Java.type('ch.psi.pshell.device.DeviceAdapter') 
ReadbackDeviceListener = Java.type('ch.psi.pshell.device.ReadbackDeviceAdapter') 
MotorListener = Java.type('ch.psi.pshell.device.MotorAdapter') 
MoveMode  = Java.type('ch.psi.pshell.device.MoveMode') 
Epics = Java.type('ch.psi.pshell.epics.Epics') 
EpicsScan = Java.type('ch.psi.pshell.epics.EpicsScan') 
Source = Java.type('ch.psi.pshell.imaging.Source') 
SourceBase = Java.type('ch.psi.pshell.imaging.SourceBase') 
DirectSource = Java.type('ch.psi.pshell.imaging.DirectSource') 
RegisterMatrixSource = Java.type('ch.psi.pshell.imaging.RegisterMatrixSource') 
LinePlotSeries = Java.type('ch.psi.pshell.plot.LinePlotSeries') 
LinePlotErrorSeries = Java.type('ch.psi.pshell.plot.LinePlotErrorSeries') 
MatrixPlotSeries = Java.type('ch.psi.pshell.plot.MatrixPlotSeries') 
AxisId = Java.type('ch.psi.pshell.plot.Plot.AxisId')
LinePlotStyle = Java.type('ch.psi.pshell.plot.LinePlot.Style')
LineScan = Java.type('ch.psi.pshell.scan.LineScan')
ContinuousScan = Java.type('ch.psi.pshell.scan.ContinuousScan')
AreaScan = Java.type('ch.psi.pshell.scan.AreaScan')
VectorScan = Java.type('ch.psi.pshell.scan.VectorScan')
ManualScan = Java.type('ch.psi.pshell.scan.ManualScan')
RegionScan = Java.type('ch.psi.pshell.scan.RegionScan')
HardwareScan = Java.type('ch.psi.pshell.scan.HardwareScan')
TimeScan = Java.type('ch.psi.pshell.scan.TimeScan')
MonitorScan = Java.type('ch.psi.pshell.scan.MonitorScan')
BinarySearch = Java.type('ch.psi.pshell.scan.BinarySearch')
HillClimbingSearch = Java.type('ch.psi.pshell.scan.HillClimbingSearch')
ScanResult = Java.type('ch.psi.pshell.scan.ScanResult')
BsScan = Java.type('ch.psi.pshell.bs.BsScan')
Stream = Java.type('ch.psi.pshell.bs.Stream') 
Preference = Java.type('ch.psi.pshell.scripting.ViewPreference')
Threading =  Java.type('ch.psi.utils.Threading')
Convert =  Java.type('ch.psi.utils.Convert')
Arr =  Java.type('ch.psi.utils.Arr')
Chrono =  Java.type('ch.psi.utils.Chrono')
State =  Java.type('ch.psi.utils.State')
ScriptingUtils = Java.type('ch.psi.pshell.scripting.ScriptUtils')


function get_context() {
    return Context.getInstance()
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Type conversion and checking
///////////////////////////////////////////////////////////////////////////////////////////////////

function is_defined(obj) {
    return (typeof obj != 'undefined')
}

function get_rank(obj) {    
    if (typeof obj == 'string') return 0
    var rank = 0;
    while ((obj != null) && (typeof obj != 'string')  &&(obj[0] != null)) {
        rank++;
        obj = obj[0];
    }
    return rank;
}

function assert(condition, message) {
    if (!condition) {
        throw message || "Assertion failed";
    }
}

//TODO: this is wrong: returns false for 1.0
function is_float(n) {
    if (n == null){
        return false;
    }
    if (get_rank(n) == 1) {
        for (var i = 0; i < n.length; i++) {
            if (is_float(n[i])) {
                return true;
            }
        }
        return false;
    }
    //return n === Number(n) && n % 1 !== 0;
    return (n.class.simpleName == "Float"  ||  n.class.simpleName == "Double")
}

function is_array(obj) {
    //return get_rank(obj) == 0
    //return !(obj.length == undefined)
    //return obj.constructor === Array
    return Array.isArray(obj)
}


function is_java_array(obj){
    return !Array.isArray(obj)  && obj.class.isArray()	
}

function is_java_list(obj){
    return obj instanceof java.util.List	
}

function to_array(obj, type) {
    //var javaObjectArray = Java.to(obj);

    if (obj == null) {
        return null;
    }
    if (!is_defined(type)) {
        //If undefined type, convert to a JavaScript array
        if (is_array(obj)){
            return obj
        }
        return  Java.from(obj);
    }

    if (obj instanceof java.util.List){
        obj = to_array(obj)
    }

    var rank = get_rank(obj);
    if (rank == 0) {
        rank = 1;
        obj = [obj]
    }

    //Custom class
    if (!ScriptingUtils.isStandardType(type)) {
        var ret = java.lang.reflect.Array.newInstance(java.lang.Class.forName(type), obj.length)
        for (var i = 0; i < obj.length; i++) {
            ret[i] = obj[i]
        }
        return ret;
    }

    if (is_array(obj) && (type=='o')){
        for (var i = 0; i < obj.length; i++) {
            if (is_array(obj[i])){            	
                obj[i] = to_array(obj[i], type)                

            }
        }        
    }    
    
    var name = ScriptingUtils.getType(type).getName()
    if (type[0] != "[") {
        type = "[" + type;
        name = ScriptingUtils.getType(type).getName();
        //Type 'o' always create a 1d array
        if (type!='[o'){
            for (var i = 1; i < rank; i++) {
                name = "[" + name;               
            }
        }
    }

    return ScriptUtils.convert(obj, java.lang.Class.forName(name))
}


///////////////////////////////////////////////////////////////////////////////////////////////////
// Standard scan commands
///////////////////////////////////////////////////////////////////////////////////////////////////


function lscan(writables, readables, start, end, steps, latency, relative, passes, zigzag, before_read, after_read, title) {
    /*
     Line Scan: positioners change together, linearly from start to end positions.
     
     Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(int or float or list of float): number of scan steps (int) or step size (float).
        relative (bool, optional): if true, start and end positions are relative to 
        current at start of the scan
        latency(float, optional): sleep time in each step before readout, defaults to 0.0.     
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writables invert direction on each pass.    
        before_read (function, optional): callback on each step, before each readout. Callback may have as 
        optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. Callback may have as 
        optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
     
     Returns:
         ScanResult object.
     */
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(passes))    passes = 1;
    if (!is_defined(zigzag))    zigzag = false;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(title))    title = null;
    
    var latency_ms = latency * 1000
    var writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    var start = to_array(start, 'd')
    var end = to_array(end, 'd')
    var steps = is_float(steps) ? to_array(steps, 'd') : steps
    //var scan = new LineScan(writables, readables, start, end, steps, relative, latency_ms)
    var scanClass = Java.extend(LineScan)
    var scan = new scanClass(writables, readables, start, end, steps, relative, latency_ms, passes, zigzag) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record , scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}


function vscan(writables, readables, vector, line, latency, relative, passes, zigzag, before_read, after_read, title) {
    /*
     Vector Scan: positioners change following values provided in a vector.
     
     Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        vector(list of list of float): table of positioner values.
        line (bool, optional): if true, processs as line scan (1d)
        relative (bool, optional): if true, start and end positions are relative to current at 
        start of the scan
        latency(float, optional): sleep time in each step before readout, defaults to 0.0.        
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writables invert direction on each pass.
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
     
     Returns:
         ScanResult object.
     */
    if (!is_defined(line))   line = false;
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(passes))    passes = 1;
    if (!is_defined(zigzag))    zigzag = false;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(title))    title = null;
    var latency_ms = latency * 1000
    var writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    

    if (vector.length == 0)
        vector.push([]);
    else if (!is_array(vector[0])) {    	
        var aux = []
        for (var i = 0; i < vector.length; i++)
            aux.push([vector[i]]);
        var vector = aux
    }

    vector = to_array(vector, 'd')
    var scanClass = Java.extend(VectorScan)
    
    var scan = new scanClass(writables, readables, vector, line, relative, latency_ms, passes, zigzag) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function ascan(writables, readables, start, end, steps, latency, relative, passes, zigzag, before_read, after_read, title) {
    /*      
     Area Scan: multi-dimentional scan, each positioner is a dimention.
     
     Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(list of int or list of float): number of scan steps (int) or step size (float).
        relative (bool, optional): if true, start and end positions are relative to current at 
        start of the scan
        latency(float, optional): sleep time in each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        zigzag (bool, optional): if true writables invert direction on each row.
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
     
     Returns:
         ScanResult object.
     */
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(passes))    passes = 1;
    if (!is_defined(zigzag))    zigzag = false;
    if (!is_defined(title))    title = null;

    var latency_ms = latency * 1000
    var writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    var start = to_array(start, 'd')
    var end = to_array(end, 'd')
    var steps = is_float(steps) ? to_array(steps, 'd') : to_array(steps, 'i') 
    var scanClass = Java.extend(AreaScan)
    var scan = new scanClass(writables, readables, start, end, steps, relative, latency_ms, passes, zigzag) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function  rscan(writable, readables, regions, latency, relative, passes, zigzag, before_read, after_read, title) {
    /*
    Region Scan: positioner scanned linearly, from start to end positions, in multiple regions.

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

    Returns:
        ScanResult object.
     */
    
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(passes))    passes = 1;
    if (!is_defined(zigzag))    zigzag = false;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(title))    title = null;
    
    var latency_ms = latency * 1000
    var writable = string_to_obj(writable)
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")

    var start = []
    var end = []
    var steps = []
    
    for (var region in regions) {
        start.push(regions[region][0])
        end.push(regions[region][1])
        steps.push(regions[region][2])
    }

    var start = to_array(start, 'd')
    var end = to_array(end, 'd')
    var steps = is_float(steps) ? to_array(steps, 'd') : to_array(steps, 'i')  
    

    var scanClass = Java.extend(RegionScan)
    var scan = new scanClass(writable, readables, start, end, steps, relative, latency_ms, passes, zigzag) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
 }


function  cscan(writables, readables, start, end, steps, latency, time, relative, passes, zigzag, before_read, after_read, title) {
    /*
     Continuous Scan: positioner change continuously from start to end position and readables are sampled on the fly.
     
     Args:
        writable(Speedable or list of Motor): A positioner with a  getSpeed method or 
        a list of motors.
        readables(list of Readable): Sensors to be sampled on each step.
        start(float or list of float): start positions of writables.
        end(float or list of float): final positions of writabless.
        steps(int or float or list of float): number of scan steps (int) or step size (float).
        time = null
        time (float, seconds): if not null then writables is Motor array and speeds are 
        set according to time.
        relative (bool, optional): if true, start and end positions are relative to 
        current at start of the scan
        latency(float, optional): sleep time in each step before readout, defaults to 0.0.
        before_read (function, optional): callback on each step, before each readout. 
        Callback may have as optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. 
        Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.
     
     Returns:
        ScanResult object.
     
     */
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(time))   time = null;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(passes))    passes = 1;
    if (!is_defined(zigzag))    zigzag = false;    
    if (!is_defined(title))    title = null;

    var latency_ms = latency * 1000    
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")    
    var scanClass = Java.extend(ContinuousScan)
    //A single Writable with fixed speed
    if (time == null) {    	  
        var scan = new scanClass(writables, readables, start, end, steps, relative, latency_ms, passes, zigzag){
            onBeforeReadout: function (pos) {
                if (is_defined(before_read))
                    before_read(pos , scan)
            },
            onAfterReadout: function (record) {
                if (is_defined(after_read))
                    after_read(record, scan)
            }
            	
        }                
    }
    else {
    	var writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.ControlledSpeedable")
        //A set of Writables with speed configurable
        var start = to_array(start, 'd')
        var end = to_array(end, 'd')    
        if ((steps.length!=null) && is_float(steps)){
        	steps = to_array(steps, 'd')
        }
        var scan = new ContinuousScan(writables, readables, start, end, steps, time, relative, latency_ms, passes, zigzag){
            onBeforeReadout: function (pos) {
                if (is_defined(before_read))
                    before_read(pos , scan)
            },
            onAfterReadout: function (record) {
                if (is_defined(after_read))
                    after_read(record, scan)
            }            	
        }                
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function hscan(config, writable, readables, start, end, steps, passes, zigzag, before_stream, after_stream, after_read, title) {
    /*
    Hardware Scan: values sampled by external hardware and received asynchronously.

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

    Returns:
        ScanResult object.

    */    
    if (!is_defined(passes))    passes = 1;
    if (!is_defined(zigzag))    zigzag = false;    
    if (!is_defined(title))    title = null;    
    //var scan = HardwareScan.newScan(config, writable,readables, start, end , steps, passes, zigzag);
    var scanClass = Java.extend(HardwareScan)
    var scan = new scanClass(config, writable, readables, start, end, steps, passes, zigzag) {
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        },
        onBeforeStream: function (current_pass) {
            if (is_defined(before_stream))
                before_stream(current_pass)
        },
        onAfterStream: function (current_pass) {
            if (is_defined(after_stream))
                after_stream(current_pass)
        },
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function bscan(stream, records, before_read, after_read, title) {
    /*
    BS Scan: records all values in a beam synchronous stream.

    Args:
        stream(Stream): stream object
        records(int): number of records to store
        before_read (function, optional): callback on each step, before each readout. 
                    Callback may have as optional parameters list of positions.
        after_read (function, optional): callback on each step, after each readout. 
                    Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.

    Returns:
        ScanResult object.

    */
    if (!is_defined(title))    title = null;
    var stream = string_to_obj(stream)
    var scanClass = Java.extend(BsScan)
    var scan = new scanClass(stream,records) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }            	
    }                    
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function  tscan(readables, points, interval, title, before_read, after_read) {
    /* 
     Time Scan: sensors are sampled in fixed time intervals.
     
     Args:
        readables(list of Readable): Sensors to be sampled on each step.
        points(int): number of samples.
        interval(float): time interval between readouts.
        before_read (function, optional): callback on each step, before each readout.
        after_read (function, optional): callback on each step, after each readout.
        title(str, optional): plotting window name.
     
     Returns:
         ScanResult object.
     */
    if (!is_defined(title))    title = null;


    var interval_ms = interval * 1000
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    var scanClass = Java.extend(TimeScan)
    var scan = new scanClass(readables, points, interval_ms) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos, scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function  mscan(trigger, readables, points, timeout, async, take_initial, before_read, after_read, title) {
    /* 
    Monitor Scan: sensors are sampled in timestamp change event of the trigger.

    Args:
        trigger(Device or list of Device): Source of the sampling triggering.
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

    Returns:
        ScanResult object.
    */
    if (!is_defined(timeout))    timeout = -1;
    if (!is_defined(async))    async = true;
    if (!is_defined(take_initial))    take_initial = false;
    if (!is_defined(title))    title = null;

    var timeout_ms = timeout * 1000
    var trigger = string_to_obj(trigger)
    var readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    var scanClass = Java.extend(MonitorScan)
    var scan = new scanClass(trigger, readables, points, timeout_ms, async, take_initial) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos, scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function escan(name, title) {
    /*
     Epics Scan: execute an Epics Scan Record.
     Args:
        name(str): Name of scan record.
        title(str, optional): plotting window name.
     
     Returns:
         ScanResult object.
     */

    if (!is_defined(title))    title = null;
    var scan = new EpicsScan(name)
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function bsearch(writables, readable, start, end, steps, maximum, strategy, latency, relative, before_read, after_read, title){
    /*
    Binary search: searches writables in a binary search fashion to find a local maximum for the readable.

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

    Returns:
        SearchResult object.

    */
    if (!is_defined(maximum))   maximum = true;
    if (!is_defined(strategy))   strategy = "Normal";
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(title))    title = null;

    var latency_ms = latency * 1000    
    var writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    var readable = string_to_obj(readable)
    var start = to_array(start, 'd')
    var end = to_array(end, 'd')
    var steps = to_array(steps, 'd')
    strategy = BinarySearch.Strategy.valueOf(strategy)
    var scanClass = Java.extend(BinarySearch)
    var scan = new scanClass(writables,readable, start, end , steps, maximum, strategy, relative, latency_ms) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

function hsearch(writables, readable, range_min,  range_max, initial_step, resolution, noise_filtering_steps, maximum, latency, relative, before_read, after_read, title){
    /*
    Hill Climbing search: searches writables in decreasing steps to find a local maximum for the readable.
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

    Returns:
        SearchResult object.

    */    
    if (!is_defined(maximum))   maximum = true;
    if (!is_defined(noise_filtering_steps))   noise_filtering_steps = 1;
    if (!is_defined(latency))    latency = 0.0;
    if (!is_defined(relative))   relative = false;
    if (!is_defined(title))    title = null;

    var latency_ms = latency * 1000   
    var latency_ms = latency * 1000    
    var readable = string_to_obj(readable)
    var range_min = to_array(range_min, 'd')
    var range_max = to_array(range_max, 'd')
    var initial_step = to_array(initial_step, 'd')
    var resolution = to_array(resolution, 'd')
    var scanClass = Java.extend(HillClimbingSearch)
    var scan = new scanClass(writables,readable, range_min, range_max , initial_step, resolution, noise_filtering_steps, maximum, relative, latency_ms) {
        onBeforeReadout: function (pos) {
            if (is_defined(before_read))
                before_read(pos , scan)
        },
        onAfterReadout: function (record) {
            if (is_defined(after_read))
                after_read(record, scan)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Data Plotting
///////////////////////////////////////////////////////////////////////////////////////////////////
function plot(data, name, xdata, ydata, title) {
    /*Request one or multiple plots of user data (1d, 2d or 3d)
     
     Args:
        data: array or list of values. For multiple plots, array of arrays or lists of values.
        name(str or list of str, optional): Plot name or list of names (if multiple plots). 
        xdata: array or list of values. For multiple plots, array of arrays or lists of values.
        ydata: array or list of values. For multiple plots, array of arrays or lists of values.
        title(str, optional): plotting window name.
     
     Returns:
         ArrayList of Plot objects.
     
     */
    if (!is_defined(name))   name = null;
    if (!is_defined(xdata))  xdata = null;
    if (!is_defined(ydata))  ydata = null;
    if (!is_defined(title))  title = null;

    if (name != null) {
        if( typeof name === 'string' ) {
            name = [name]
            data =  [data]
        }
        if (name.length == 0) {
            name = null;
        } else {
            if (!data) {
                data = []
                for (var n in name) {
                    data.push([]);
                }
            }
        }
        var plots = java.lang.reflect.Array.newInstance(java.lang.Class.forName("ch.psi.pshell.data.PlotDescriptor"), data.length)
        for (var i = 0; i < data.length; i++) {
            var plotName = name ? name[i] : null;
            var x = xdata;
            if (x && x.length && x[0].length) { 
                x = x[i];
            }
            var y = ydata
            if (y && y.length && x[0].length) { 
                y = y[i];
            }
            plots[i] = new PlotDescriptor(plotName, to_array(data[i], 'd'), to_array(x, 'd'), to_array(y, 'd'));
        }
        return get_context().plot(plots, title);
    } else {
        var plot = new PlotDescriptor(name, to_array(data, 'd'), to_array(xdata, 'd'), to_array(ydata, 'd'));

        return get_context().plot(plot, title);
    }

}

function get_plots(title){
    /*
    Return all current plots in the plotting window given by 'title'.

    Args:
        title(str, optional): plotting window name.

    Returns:
        ArrayList of Plot objects.

    */
    if (!is_defined(title))
        title = null;
    return get_context().getPlots(title)
}

function get_plot_snapshots(title, file_type, temp_path){
    /*
    Returns list with file names of plots snapshots from a plotting context.

    Args:
        title(str, optional): plotting window name.
        file_type(str, optional): "png", "jpg", "bmp" or "gif"
        temp_path(str, optional): path where the files will be generated.

    Returns:
        list of strings

    */
    if (!is_defined(title))   title = null;
    if (!is_defined(file_type))   file_type = "png";   
    if (!is_defined(temp_path))   temp_path = get_context().setup.getContextPath();
    sleep(0.1) //Give some time to plot to be finished - it is not sync  with acquisition
    var ret = []
    var plots = get_plots(title)
    for (p in plots){
        var file_name = new File(temp_path + "/" + plots[p].getTitle() + "." + file_type).getCanonicalPath()
        plots[p].saveSnapshot(file_name , file_type)
        ret.push(file_name)
    }
    return ret
}
///////////////////////////////////////////////////////////////////////////////////////////////////
// Data file access
///////////////////////////////////////////////////////////////////////////////////////////////////

function load_data(path, index, shape) {
    /*
     Read data from the current persistence context or from data files.
     
     Args:
        path(str): Path to group or dataset relative to the persistence context root.
                   If in the format 'root|path' then read from path given by 'root'.
        index(int or listr, optional): 
                if integer, data depth (used for 3D datasets returning a 2d matrix)
                If a list, specifies the full coordinate for multidimensional datasets.
     Returns:
         Data array
     
     */
    if (!is_defined(index))
        index = 0;
    if (!is_defined(shape))
        shape = null;

    if ((shape!=null) && (is_array(index)))
        var slice = get_context().dataManager.getData(path, index, shape)
    else
        var slice = get_context().dataManager.getData(path, index)
    return slice.sliceData
}

function get_attributes(path) {
    /*
     Get the attributes from the current persistence context or from data files.
     
     Args:
        path(str): Path to group or dataset relative to the current persistence context root.
                  If in the format 'root|path' then read from path given by 'root'.
     Returns:
         Dictionary
     
     */
    return get_context().dataManager.getAttributes(path)
}

function save_dataset(path, data, type) {
    /*
     Save data into a dataset within the current persistence context.
     
     Args:
        path(str): Path to dataset relative to the current persistence context root.
        type(str, optional): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
                              'd' = double, 'c' = char, 's' = String,  'o' = Object 
                   default: 'd' (convert data to array of doubles)
        data (array or list): data to be saved
     Returns:
        Dictionary
     
     */
    if (!is_defined(type))
        type = 'd';
    if (is_array(data)){
        data = to_array(data, type)    
    }
    get_context().dataManager.setDataset(path, data)
}

function create_group(path) {
    /*
    Create an empty dataset within the current persistence context.

    Args:
        path(str): Path to group relative to the current persistence context root.
    Returns:
        null
     
     */
    get_context().dataManager.createGroup(path)
}


function create_dataset(path, type, unsigned, dimensions) {
    /*
    Create an empty dataset within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        type(str): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
        'd' = double, 'c' = char, 's' = String,  'o' = Object 
        unsigned(boolean, optional): create a dataset of unsigned type.
        dimensions(tuple of int, optional): a 0 value means variable length in that dimension.
     Returns:
         null
     
     */
    if (!is_defined(unsigned))
        unsigned = false;
    if (!is_defined(dimensions))
        dimensions = null;
    if (!is_defined(type))
        type = null;    
    get_context().dataManager.createDataset(path, ScriptingUtils.getType(type), unsigned, dimensions)
}

function create_table(path, names, types, lengths) {
    /*
     Create an empty table (dataset of compound type) within the current persistence context.

     Args:
        path(str): Path to dataset relative to the current persistence context root.
        names(list of strings): name of each column
        types(array of str):  'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 
        'd' = double, 'c' = char, 's' = String,  'o' = Object 
        Note:A '[' prefix on type name indicates an array type.
        lengths(list of int): the array length for each columns(0 for scalar types).
     Returns:
         null
     
     */
    if (!is_defined(types))
        types = null;
    if (!is_defined(lengths))
        lengths = null;
    var new_types = null
    if (types != null) {
        new_types = []
        for (var i = 0; i < types.length; i++){
            new_types.push(ScriptingUtils.getType(types[i]))
        }
    }
    get_context().dataManager.createDataset(path, names, new_types, lengths)
}

function append_dataset(path, data, index, type, shape) {
    /*
     Append data to dataset.

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
                In this case data must be a one-dimensional array.
     Returns:
        null
     
     */
    if (!is_defined(index))
        index = null;
    if (!is_defined(type))
        type = 'd';
    if (!is_defined(shape))
        shape = null;
    if (is_array(data)){
        data = to_array(data, type)
    }
    if (index == null)
        get_context().dataManager.appendItem(path, data);
    else {
        if (is_array(index))
            get_context().dataManager.setItem(path, data, index, shape);
        else
            get_context().dataManager.setItem(path, data, index);
    }
}

function append_table(path, data) {
    /*
     Append data to a table (dataset of compound type) 

     Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(list): List of valus for each column of the table. Array types can be expressed as lists.
     Returns:
         null
     
     */

    if (is_array(data)){
        for (var i=0; i< data.length; i++){
            if (is_array(data[i])){
                data[i] = to_array(data[i], 'd')
            }
        }
        data = to_array(data, 'o')
    }        
    get_context().dataManager.appendItem(path, data)
}

function flush_data() {
    /*
     Flush all data files immediately.
     
     Args:
        null
     Returns:
         null
     */
    get_context().dataManager.flush()
}

function set_attribute(path, name, value, unsigned) {
    /*
     Set an attribute to a group or dataset.
     
     Args:
        path(str): Path to dataset relative to the current persistence context root.
        name(str): name of the atttribute
        value(Object): the attribute value
        unsigned(bool, optional):  if applies, indicate if  value is unsigned.
     Returns:
         null
     */
    if (!is_defined(unsigned))
        unsigned = false;
    get_context().dataManager.setAttribute(path, name, value, unsigned)
}

function log(log){
    /*
    Writes a log to the automatic data saving context - only if there is an ongoing scan or 
       script execution.        

    Args:        
         log(str): Log string.

    Returns:
        None
    */  
    get_context().scriptingLog(String(log))
    get_context().dataManager.appendLog(String(log))
}

function set_exec_pars(args){
    /*
       Configures the script execution parameters, overriding the system configuration.
    
    Args: 
      args(dictionary). Keys:
        name(str, optional): value of the {name} tag. Default is the running script name 
                             (or "scan" in the case of  a command line scan command.)
        type(str, optional): value of the {type} tag. Default is empty.
                             This field can be used to store data in  sub-folders of standard location.
        path(str, optional):  If defined provides the full path name for data output root (overriding config))
                             The tag {data} can be used to enter a path relative to the standard data folder.
        layout(str, optional): Overrides default data layout.
        depth_dim(int, optional): dimension of the depth for 2d-matrixes in 3d datasets.
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
        tag(str, optional): Overrides default tag for scan names (affecting group or dataset name, according to layout)
        defaults(bool, optional): If true restore the original execution parameters.

        Graphical preferences can also be set. Keys are equal to lowercase of Preference enum:
        "plot_disabled", "table_disabled", "enabled_plots", "plot_types", "print_scan", "auto_range", 
        "manual_range","domain_axis", "status".
        See set_preference for more information.

        Shortcut entries: "line_plots": list of devices with enforced line plots.
    */
    get_context().setExecutionPars(args)
}
    
function get_exec_pars(){
    /*
    Returns script execution parameters.

    Returns:
        ExecutionContext object. Fields: 
            name (str): execution name - {name} tag.
            type (str): execution type - {type} tag.
            path (str): output data root.
            open (bool): true if the output data root has been opened.
            layout (str): data output layout. If null then using the configuration.
            persist (bool): auto save scan data option. 
            flush (bool): flush file on each record.
            index (int): current scan index.
            group (str): data group currently used for scan data storage. 
                         if no ongoing scan return "/" if within a script, or else null if a console command.
            scanPath (str): dataset or group corresponding to current scan.
            scan (Scan): reference to current scan, if any
            source (CommandSource): return the source of the script or command.
            args (obj): return the arguments for the script.
            background (bool): return False if executing in main interpreter thread .
    */
    return get_context().getExecutionPars()    
}
///////////////////////////////////////////////////////////////////////////////////////////////////
// Epics Channels access
///////////////////////////////////////////////////////////////////////////////////////////////////

function caget(name, type, size) {
    /*
     Reads an Epics PV.
     
     Args:
        name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
        Scalar values: 'b', 'i', 'l', 'd', 's'.
        Array: values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
     
     Returns:
         PV value
     
     */
    if (!is_defined(type))
        type = null;
    if (!is_defined(size))
        size = null;
    return Epics.get(name, Epics.getChannelType(type), size)
}

function cawait(name, value, timeout, comparator, type, size) {
    /*
     Wait for a PV to have a given value.
     
     Args:
        name(str): PV name
        value (obj): value to compare to
        timeout(float, optional): time in seconds to wait. If null, waits forever.
        comparator(java.util.Comparator or float, optional): if None waits for equality. 
            If a numeric value is provided, waits for channel to be in range.
        type(str, optional): type of PV. By default gets the PV standard field type.
        Scalar values: 'b', 'i', 'l', 'd', 's'.
        Array: values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
     
     Returns:
         None
     */
    if (!is_defined(type))   type = null;
    if (!is_defined(size))   size = null;
    if (!is_defined(comparator)) comparator = null;
    if (is_defined(timeout))
        timeout = timeout * 1000
    else
        timeout = null
    return Epics.waitValue(name, value, comparator, timeout, Epics.getChannelType(type), size)
}

function caput(name, value, timeout) {
    /*
     Writes to an Epics PV.
     
     Args:
        name(str): PV name
        value(scalar, string or array): new PV value.
        timeout(int, optional): timeout in seconds to the write. If null waits forever to completion.                    

     Returns:
         None
     */
    if (is_defined(timeout))
        timeout = timeout * 1000
    else
        timeout = null
    return Epics.put(name, value, timeout)
}

function caputq(name, value) {
    /*
     Writes to an Epics PV and does not wait.
     
     Args:
        name(str): PV name
        value(scalar, string or array): new PV value.
     
     Returns:
         None
     */
    return Epics.putq(name, value)
}


function create_channel(name, type, size) {
    if (!is_defined(type))
        type = null;
    if (!is_defined(size))
        size = null;
    return Epics.newChannel(name, Epics.getChannelType(type), size)
}


function create_channel_device(channelName, type, size, deviceName){
    if (!is_defined(type))
        type = null;
    if (!is_defined(size))
        size = null;
    if (!is_defined(deviceName))
        deviceName = null;
    dev = Epics.newChannelDevice(deviceName, channelName,Epics.getChannelType(type))
    if (get_context().isSimulation()){
        dev.setSimulated()
    }
    dev.initialize()
    if (size != null){
        dev.setSize(size)
    }
    return dev
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Concurrent execution 
///////////////////////////////////////////////////////////////////////////////////////////////////
function _getCallable(func, args) {
    var callable = new java.util.concurrent.Callable() {
        call: function () {           
            //return func(args)
            return func.apply( this, args );
        }
    }
    return callable
}

function fork() {
    /*
     Start execution of methods in parallel. 
     
     Args:
        *functions(functions references)
     
     Returns:
        List of callable objects
     */
    var callables = []
    for( var i =0; i<arguments.length; i++){
        var m = arguments[i]
        if (get_rank(m)>0){
            callables.push(_getCallable(m[0], m[1]))
        }
        else{
            callables.push(_getCallable(m))
        }
        
    }
    return Threading.fork(callables)
}

function join(futures) {
    /*
     Wait parallel execution of methods.
     
     Args:
        callables(list of Callables) : as returned from fork
     
     Returns:
        None
     */
    try{
        return Threading.join(futures)
    } catch(err){    
        throw  err.getCause()
    }
}

function parallelize() {
    /*
     Equivalent to fork + join
     
     Args:
        *methods(method references)
     
     Returns:
        None
     */
    futures = fork.apply(this, arguments)
    return join(futures)
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Script evaluation and Background task control.
///////////////////////////////////////////////////////////////////////////////////////////////////
function run(script_name, args) {
    /*
     Run script: can be absolute path, relative, or short name to be search in the path.
     Args:
        args(Dict ot Array): gobal variables set to the script(if dict), or argv varialble (if array).
     
     Returns:
        The script return value
    */
    var script = get_context().scriptManager.library.resolveFile(script_name)
    var file = script!=null ? new File(script) : null
    if ((file == null) ||  ( ! file.exists())) throw "Invalid script: " + script_name
    get_context().startScriptExecution(args)

    if (is_defined(args) &&  (args!=null)){
        if (is_array(args)){
            argv = args
        } else {
            for (var key in args) {
                eval(key+"="+args[key])
            }
        }
    }
    //eval(new String(Files.readAllBytes(file.toPath())))
    //get_context().scriptManager.interpreter.evalFile(script);
    load(script)
}


function _abort() {
     get_context().abort()
}

function abort() {
    /*
    Abort the execution of ongoing task. It can be called from the script to quit.

    Args:                 
        None

    Returns:
        None
    */
    //Cannot be on script execution thread
    fork(_abort)  
}

function start_task(script, delay, interval){
    /*
    Start a background task

    Args:        
         script(str): Name of the script implementing the task
         delay(float, optional): time in seconds for the first execution. 
                Default starts immediately.
         interval(float, optional): time in seconds for between execution. 
                If negative (default), single-execution.

    Returns:
        None
    */      
    if (!is_defined(delay))   delay = 0.0;
    if (!is_defined(interval))   interval = -1;
    var delay_ms = delay_ms * 1000
    var interval_ms = (interval>=0) ? interval_ms * 1000 : interval
    get_context().taskManager.create(script, delay_ms, interval_ms)
    get_context().taskManager.start(script)
}
    
function stop_task(script, force){
    /*
    Stop a background task

    Args:        
         script(str): Name of the script implementing the task
         force(boolean, optional): interrupt current execution, if running

    Returns:
        None
    */      
    if (!is_defined(force))   force = false;
    get_context().taskManager.remove(script, force)
}

function set_return(value){
    /*
    Sets the script return value. This value is returned by the "run" function.

    Args:
        value(Object): script return value.

    Returns:
        None
    */  
    if (is_interpreter_thread()){
        _=value       
    }
    __THREAD_EXEC_RESULT__.put(java.lang.Thread.currentThread(),value)         //Used when running file
    return value    //Used when parsing file  
}

function is_interpreter_thread(){
    return java.lang.Thread.currentThread().name == "Interpreter Thread" 
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Versioning tools
///////////////////////////////////////////////////////////////////////////////////////////////////
function commit(message, force){
    /*
    Commit the changes to the repository. If manual commit is not configured then there is no need to call this function: commits are made as needed.

    Args:
        message(str): commit message
        force(bool, optional): if False, raises exception if no change detected in repo
    Returns:
        None
    */
    if (!is_defined(force))   force = false;   
    get_context().commit(message, force)
}

function diff(){
    /*
    Return list of changes in the repository

    Args:
        None

    Returns:
        None
    */   
    return get_context().diff()
}

function checkout_tag(tag){
    /*     
    Checkout a tag name.

    Args:
        tag(str): tag name.

    Returns:
        None
    */
    get_context().checkoutTag(tag)
}

function checkout_branch(tag){
    /*
    Checkout a local branch name.

    Args:
        tag(str): branch name.

    Returns:
        None
    */
    get_context().checkoutLocalBranch(tag)
}

function pull_repository(){
    /*
    Push to remote repository.

    Args:
        all_branches(boolean, optional): all branches or just current.
        force(boolean, optional): force flag.

    Returns:
        None
    */
    get_context().pullFromUpstream()
}

function push_repository(all_branches, force){
    /*
    Push to remote repository.

    Args:
        all_branches(boolean, optional): all branches or just current.
        force(boolean, optional): force flag.

    Returns:
        None
    */
    if (!is_defined(all_branches))    all_branches = true;   
    if (!is_defined(force))    force = false;
    
    get_context().pushToUpstream(all_branches, force)
}

function cleanup_repository(){
    /*
    Performs a repository cleanup.

    Args:
        None

    Returns:
        None
    */    
    get_context().cleanupRepository()
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Device Pool & Imaging setup
///////////////////////////////////////////////////////////////////////////////////////////////////
function get_device(device_name){
    /*
    Returns a configured device (or imaging source) by its name. 

    Args:
        device_name(str): name of the device.

    Returns:
        device
    */    
    return get_context().devicePool.getByName(device_name)
}

function add_device(device, force){
    /*
    Add  a device (or imaging source) to the device pool.

    Args:
        device(Device or Source): device object.
        force(boolean, optional): if true then dispose existing device with same name. 
                    Otherwise will fail in case of name clash.

    Returns:
        True if device was added, false if was already in the pool, or exception in case of name clash.
    */
    if (!is_defined(force))    force = false;
    if (get_context().devicePool.contains(device)){
        return false
    }
    if (force){
        dev = get_context().devicePool.getByName(device.getName())
        if (dev!=null) 
            remove_device(dev)
    }
    return get_context().devicePool.addDevice(device)
}

function remove_device(device){
    /*
    Remove a device (or imaging source) from the device pool.

    Args:
        device(Device or Source): device object.

    Returns:
        bool: true if device was removed.

    */
    return get_context().devicePool.removeDevice(device)
}

function set_device_alias(device, alias){
    /*
    Set a device alias to be used in scans (datasets and plots).

    Args:
        device(Device): device object.
        alias(str): replace device name in scans.

    Returns:
        None
    */
    get_context().dataManager.setAlias(device, alias)
}    

function stop(){
    /*
    Stop all devices implementing the Stoppable interface.

    Args:                 
        None

    Returns:
        None
    */
    get_context().stopAll()
}
    
function update(){
    /*
    Update all devices.

    Args:                 
        None

    Returns:
        None
    */
    get_context().updateAll()
}

function reinit(dev){
    /*
    Re-initialize devices.

    Args:                 
        dev(Device, optional): the device to be re-initialized. 
                               If  null re-initialize all devices not yet initialized.

    Returns:
        List with devices not initialized.
    */
    if (!is_defined(dev))    dev = null;
    return to_array(get_context().reinit())
}

function create_averager(dev, count, interval, name, monitored){
    /*
    Creates and initializes and averager for dev.

    Args:                 
        dev(Device): the source device
        count(int): number of samples
        interval(float, optional): sampling interval in seconds. 
                                   If less than zero, sampling is made on data change event.
    name(str, optional): sets the name of the device (default is: <dev name> averager)    
    monitored (bool, optional): if true then averager processes asynchronously.

    Returns:
        Averager device
    */
    if (!is_defined(interval))    interval = 0.0;
    if (!is_defined(name))    name = null;
    if (!is_defined(monitored))    monitored = false;
    dev = string_to_obj(dev)
    var averager = (name == null) ? new Averager(dev, count, interval*1000) : new Averager(name, dev, count, interval*1000)
    if (monitored){
        averager.setMonitored(true)
    }
    averager.initialize()
    return averager
}

///////////////////////////////////////////////////////////////////////////////////////////////////
//Mathematical functions
///////////////////////////////////////////////////////////////////////////////////////////////////

function mean(data){
    /*
    Calculate the mean of a sequence.

    Args:
        data(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        Mean of the elements in the object.

    */
    if (!is_array(data)){
        data = to_array(data)
    }
    return data.reduce(function(sum, value){return sum + value;}, 0) / data.length;
}

function variance(data){
    /*
    Calculate the variance of a sequence.

    Args:
        data(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        Variance of the elements in the object.

    */
    var c = mean(data)
    return mean(data.map(function(value){return Math.pow((value - c),2);}));
}

function stdev(data){
    /*
    Calculate the standard deviation of a sequence.

    Args:
        data(list, tuple, array ...): subscriptable object containing numbers

    Returns:
        Standard deviation of the elements in the object.

    */
    return Math.sqrt(variance(data))
}

function poly(val, coefs){
    /*
    Evaluates a polinomial: (coefs[0] + coefs[1]*val + coefs[2]*val^2...

    Args:
        val(float): value
        coefs (list of loats): polinomial coefficients 
    Returns:
        Evaluated function for val 

    */
    var r = 0
    var p = 0
    for (c in coefs){
        r = r + coefs[c] * Math.pow(val, p)
        p = p + 1
    }
    return r
}

function histogram(data, range_min, range_max, bin){
    /*
    Creates histogram on data.

    Args:
        data (tuple, array, ArrayList or Array): input  data can be multi-dimensional or nested.
        range_min (int, optional): minimum histogram value. Default is floor(min(data))
        range_max (int, optional): maximul histogram value. Default is ceil(max(data))
        bin(int or float, optional): if int means number of bins. If float means bin size. Default = 1.0.
    Returns:
        tuple: (ydata, xdata)

    */
    flat = flatten(data)
    if (!is_defined(range_min))    range_max = null
    if (!is_defined(range_max))    range_max = null
    if (!is_defined(bin))    bin = 1.0    
    if (range_min == null) range_min = Math.floor(Math.min.apply(null,flat))
    if (range_max == null) range_max = Math.ceil(Math.max.apply(null,flat))
    
    if (is_float(bin)){    	
        bin_size = bin
        n_bin =  Math.ceil((range_max - range_min)/bin_size)
    }
    else{    	
        n_bin = bin
        bin_size = (range_max - range_min)/bin
    }   
    var result = []; var size=n_bin; while(size--) result.push(0)        
    for  (var d in flat){
        b = Math.floor( (flat[d] - range_min) / bin_size)
        if ((b >=0) && (b < n_bin)){
          result[b] = result[b] + 1
        }
    }
    var result_x = []; var size=result.length; var p=range_min; while(size--) {result_x.push(p); p+=bin_size }    
    return [result,result_x]
}

function cmp(a, b){
    if (a>b) return 1
    if (a<b) return -1
    return 0
}

function hypot(a,b){
    if (!is_defined(a))    a = 0
    if (!is_defined(b))    b = 0

    return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2))
}

function cross(a, b, o) {
   return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
}

/**
 * From https://en.wikibooks.org/wiki/Algorithm_Implementation/Geometry/Convex_hull/Monotone_chain#JavaScript
 */
function convex_hull(points, fx, fy) {
    if (!is_defined(points))    points = null
    if (!is_defined(fx))    fx = null
    if (!is_defined(fy))    fy = null
    var is_point_list = points != null    
    if (!is_point_list){
        points=[]
        for (var i=0; i<fx.length; i++){
            if((fx[i] != null) && (fy[i] != null)) points.push([fx[i], fy[i]])            
        }
    }    	
   points.sort(function(a, b) {
      return a[0] == b[0] ? a[1] - b[1] : a[0] - b[0];
   });

   var lower = [];
   for (var i = 0; i < points.length; i++) {
      while (lower.length >= 2 && cross(lower[lower.length - 2], lower[lower.length - 1], points[i]) <= 0) {
         lower.pop();
      }
      lower.push(points[i]);
   }

   var upper = [];
   for (var i = points.length - 1; i >= 0; i--) {
      while (upper.length >= 2 && cross(upper[upper.length - 2], upper[upper.length - 1], points[i]) <= 0) {
         upper.pop();
      }
      upper.push(points[i]);
   }

   upper.pop();
   lower.pop();
   var hull = lower.concat(upper);
    if (!is_point_list){
        var px=[]
        var py=[]
        for (var i=0; i<hull.length; i++){
            px.push(hull[i][0])
            py.push(hull[i][1])
        }
        return [px,py]
    }   
    return hull
}


///////////////////////////////////////////////////////////////////////////////////////////////////
// Utilities
///////////////////////////////////////////////////////////////////////////////////////////////////

function exec_cmd(cmd){
    /*
    Executes a shell command. If errors happens raises an exception.

    Args:
        cmd (str): command process input.
    Returns:
        Output of command process.
    */
    proc = java.lang.Runtime.getRuntime().exec(cmd, null);
    proc.waitFor()

    bytes = proc.getErrorStream().available()
    if (bytes>0){
        var ByteArray = Java.type("byte[]");
        var arr = new ByteArray(bytes);
        proc.getErrorStream().read(arr, 0, bytes)
        throw new java.lang.String(arr)
    }

    bytes = proc.getInputStream().available()    
    if (bytes>0){
        var ByteArray = Java.type("byte[]");
        var arr = new ByteArray(bytes);
        proc.getInputStream().read(arr, 0, bytes)
        return new java.lang.String(arr)
    }
    return null
}

function exec_cpython(script_name, args, python_name){
    /*Executes an external cpython process.

    Args:
        script_name (str): name of the script (can be absolute or relative to script folder).
        args(list, optional): arguments to python process (or parameters to method, if not null)
        python_name (str, optional): name of executable
    Returns:
        Return of python process.
    */
    if (!is_defined(args))    args = []  
    if (!is_defined(python_name))    python_name = "python"

    if (!script_name.toLowerCase().endsWith(".py")){
            script_name += ".py"
    }
    script = get_context().scriptManager.library.resolveFile(script_name)
    if (script == null){
        script= os.path.abspath(script_name)
    }
    c = python_name + " " + script + " "
    if ( args ){
        for (var arg in args){
            c = c + args[arg] + " "    
        }
    }
    return exec_cmd(c)
}

function string_to_obj(o) {
    if (typeof o === 'string') {
        if (o.contains("://")){
            return new UrlDevice(o)   
        }            
        return eval(o)
    } else if (o instanceof Array) {
        ret = []
        for (var i = 0; i < o.length; i++)
            ret.push(string_to_obj(o[i]));
        return ret
    }
    return o
}

function bsget(channel){
    /* Reads an values a bsread stream, using the default provider.

    Args:
        channel(str or  list of str): channel name(s)
    Returns:
        BS value or list of  values
    
    */
    var channels = (typeof channel === 'string')? [channel]: channel
    var ret = Stream.readChannels(channels)
    if (typeof channel === 'string') {
        return ret[0]
    }
    return ret
}

function flatten(arr) {
    var ret = [];
    if (is_java_list(arr)){
        arr = to_array(arr)
    } else if (is_java_array(arr)) {
        arr =  Java.from(arr);
    }
    
    for(var i = 0; i < arr.length; i++) {
        if((is_java_list(arr[i])) || (is_java_array(arr[i])) || (is_array(arr[i]))) {
            ret = ret.concat(flatten(arr[i]));
        } else {
            ret.push(arr[i]);
        }
    }
    return ret;
}
function range(start, stop, step, add_last) {
    if (!is_defined(add_last))    add_last = false 
    var ret = []
    var cur = start;
    if (((start>stop) && (step>0)) ||((start<stop) && (step<0))){
        throw "Invalid range parameters"
    }
    while (((step >= 0.0) && (cur < stop)) || ((step < 0.0) && (cur > start))){        
        ret.push(cur)
        cur += step;
    }
    if (add_last)  ret.push(stop)
    return ret;
}

function inject(){
    /*
    Restore initial globals: re-inject devices and startup variables to the interpreter.

    Args:
        None

    Returns:
        None

    */  
    get_context().injectVars()
}

function notify(subject, text, attachments, to){
    /*Send email message.

    Args:
        subject(str): Message subject.
        text(str): Message body.
        attachments(list of str, optional): list of files to be attached (expansion tokens are allowed).
        to (list ofd str, optional): recipients. If None uses the recipients defined in mail.properties.
    Returns:
        None

    */  
    if (!is_defined(attachments))    attachments = null
    if (!is_defined(to))    to = null
    get_context().notify(subject, text, attachments, to)
}

function sleep(seconds){
    java.lang.Thread.sleep(seconds * 1000);
}


function sort_indexes(arr, decreasing) {
  arr = to_array(arr)
  var sort = arr.slice(0)
  for (var i = 0; i < sort.length; i++) {
    sort[i] = [sort[i], i];
  }
  sort.sort(function(left, right) {
    return left[0] < right[0] ? -1 : 1;
  });
  var sort_indices = [];
  for (var i = 0; i < sort.length; i++) {
    sort_indices.push(sort[i][1]);
    sort[i] = sort[i][0];
  }
  if (decreasing){
      sort_indices.reverse()  
  }
  return sort_indices;
}

function _getBuiltinFunctionNames(){
    ret = []
    for (var obj in this) { 	
        if (typeof this[obj] == "function" && 
            this.hasOwnProperty(obj) &&
                !(/[A-Z]/.test(obj)) &&         //TODO: This checking is not good, it is to filter all classes names seen as function
                !(obj.startsWith("_"))
            ) {
                var str = this[obj].toString()
                if (str.contains("\*\/") && str.contains("\/\*") &&  str.contains("{")){
                    ret.push(obj.toString())
                }
            } 
    }
    return ret
}

function _getFunctionDoc(f){
	if (typeof f == "function"){
		f = f.name
	}	
    var str = this[f].toString()
    
    str = str.trim()
    if (str.startsWith("function ")){
        str = str.substring(9).trim()
    }
    if (str.contains("\*\/")){    	
        str = str.substring(0, str.indexOf("\*\/")+2)        
        if (str.contains("{")){
            var index = str.indexOf("{")
            return  (str.substring(0, index) + str.substring(index+1)).trim()
        }        
    }        
    return null      
}
    
function help(object){
    /* Print help message for function or object (if available).

    Args:
        object (any, optional): function or object to get help. 
                    If null prints a list of the builtin functions.

    Returns:
        None
    
    */
    if (!is_defined(object)){
        print ("Built-in functions:")
        var names = _getBuiltinFunctionNames()
        for (var f in names){
            print ("\t" + names[f])
        }
    } else {        
        //if (typeof object == "function"){
        //    print this[f.name].toString() 
        //} else {
            print(object)
        //}
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////
//UI interaction
///////////////////////////////////////////////////////////////////////////////////////////////////

function set_status(status){
    /*
    Set the application status.

    Args:
        status(str): new status.

    Returns:
        None
    */
    set_preference(ViewPreference.STATUS, status)
}

function set_preference(preference, value){
    /*
    Hints to graphical layer:    

    Args:
        preference(ViewPreference): Preference name
            Preference.SCAN_PLOT_DISABLED  #enable/disable scan plot (True/False)
            Preference.SCAN_TABLE_DISABLED  #enable/disable scan table (True/False)
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
    */  
    if (get_rank(value)>0){
        value = to_array(value, 'o') 
    }
    get_context().setPreference(preference, value)
}

function get_string(msg, default_value, alternatives, password){
    /*
    Reads a string from UI
    Args:
        msg(str): display message.
        default_value(str, optional): value displayed when window is shown.
        alternatives(list of str, optional): if provided presents a combo box instead of an editing field.
        password(boolean, optional): if True hides entered characters.

    Returns:
        String entered of null if canceled
    */
    if (!is_defined(default_value))    default_value =  null;
    if (!is_defined(alternatives))    alternatives = null;
    if (!is_defined(password))    password = false;
    if (password){
        return get_context().getPassword(msg, null)
    }
    return get_context().getString(msg, (default_value==null) ? null: default_value.toString(), alternatives)
}

function get_option(msg, type ){
    /*
    Gets an option from UI        
    Args:
        msg(str): display message.
        type(str, optional): 'YesNo','YesNoCancel' or 'OkCancel'

    Returns:
        'Yes', 'No', 'Cancel'

    */
    if (!is_defined(type))    type = "YesNoCancel";
    return get_context().getOption(msg, type)
}

function show_message(msg, title, blocking){
    /*
    Pops a blocking message to UI
    
    Args:
        msg(str): display message.    
        title(str, optional): dialog title
    */
    if (!is_defined(title))    title = null;
    if (!is_defined(blocking))    title = true;
    get_context().showMessage(msg, title, blocking)
}

function show_panel(device, title){
    /*
    Show, if exists, the panel relative to this device.
    
    Args:
        device(Device or str or BufferedImage): device
        title only apply to BufferedImage objects. For devices the title is the device name.
    */
    if (!is_defined(title))    title = null;
    if (device.class == BufferedImage.class){
        device = new DirectSource(title, device)
        device.initialize()
    }
    if (typeof device == 'string'){
        device = get_device(device)
    }
    return get_context().showPanel(device)
}

