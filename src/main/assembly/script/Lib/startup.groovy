///////////////////////////////////////////////////////////////////////////////////////////////////
// Global definitions and built-in functions
///////////////////////////////////////////////////////////////////////////////////////////////////

import java.lang.Class as Class
import java.lang.Object as Object
import java.beans.PropertyChangeListener
import java.util.concurrent.Callable
import java.util.List
import java.util.Map
import java.lang.reflect.Array

import ch.psi.utils.Threading as Threading
import ch.psi.utils.State as State
import ch.psi.utils.Convert as Convert
import ch.psi.pshell.core.Context 
import ch.psi.pshell.data.PlotDescriptor as PlotDescriptor
import ch.psi.pshell.device.Cacheable as Cacheable
import ch.psi.pshell.device.Device as Device
import ch.psi.pshell.device.DeviceBase as DeviceBase
import ch.psi.pshell.device.DeviceConfig as DeviceConfig
import ch.psi.pshell.device.Interlock as Interlock
import ch.psi.pshell.device.Readable as Readable
import ch.psi.pshell.device.Readable.ReadableArray as ReadableArray
import ch.psi.pshell.device.Readable.ReadableMatrix as ReadableMatrix
import ch.psi.pshell.device.Writable as Writable
import ch.psi.pshell.device.DeviceListener as DeviceListener
import ch.psi.pshell.device.MoveMode as MoveMode
import ch.psi.pshell.epics.Epics as Epics
import ch.psi.pshell.epics.EpicsScan as EpicsScan
import ch.psi.pshell.imaging.Source as Source
import ch.psi.pshell.imaging.SourceBase as SourceBase
import ch.psi.pshell.plot.LinePlotSeries as LinePlotSeries
import ch.psi.pshell.plot.MatrixPlotSeries as MatrixPlotSeries
import ch.psi.pshell.scan.LineScan 
import ch.psi.pshell.scan.ContinuousScan
import ch.psi.pshell.scan.AreaScan
import ch.psi.pshell.scan.VectorScan
import ch.psi.pshell.scan.ManualScan
import ch.psi.pshell.scan.RegionScan
import ch.psi.pshell.scan.HardwareScan
import ch.psi.pshell.scan.ScanRecord
import ch.psi.pshell.scan.TimeScan
import ch.psi.pshell.bs.BsScan
import ch.psi.pshell.bs.Stream
import ch.psi.pshell.bs.StreamMerger
import ch.psi.pshell.scripting.ViewPreference as Preference
import ch.psi.pshell.scripting.ScriptUtils as ScriptUtils


def get_context(){
    return ch.psi.pshell.core.Context.getInstance();
}

def on_command_started(info) {
}

def on_command_finished(info) {
}

def on_session_started(id) {
}

def on_session_finished(id) {
}

def on_change_data_path(path) {
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Type conversion and checking
///////////////////////////////////////////////////////////////////////////////////////////////////

def get_rank(obj){    
    def rank = 0;
    
    while (obj != null) {        
        Class type = obj.getClass();
        if (type.isArray()){
            while (type.isArray()) {
                rank++;
                type = type.getComponentType();                
            }            
            return rank
        } else if (obj  instanceof List){
            while (obj  instanceof List){
                rank++;
                if (((List)obj).size() == 0) return rank;
                obj = obj.get(0);
            }
        } else {
            break;
        }
    }
    
    return rank;
}

def get_length(obj){
    if (obj == null) return 0
    if (obj instanceof List) return obj.size() 
    if (obj.getClass().isArray()) return obj.length 
    return 1        
}

def to_array(obj, type){
    if (obj==null) {
        return null;
    }

    def rank = get_rank(obj);
    if (rank == 0) {
        rank = 1;
        obj = [obj]
    }
    def size = get_length(obj)
    //Custom class
    if (!ScriptUtils.isStandardType(type)) {
        def ret = java.lang.reflect.Array.newInstance(java.lang.Class.forName(type), size)
        for (def i = 0; i < size; i++) {
            ret[i] = obj[i]
        }
        return ret;
    }
    def name = ScriptUtils.getType(type).getName()  
    if (type[0] != "[") {
        def array_type = "[" + type;
        name = ScriptUtils.getType(array_type).getName()  
        for (def i = 1; i < rank; i++) {
            name = "[" + name;
        }
    }
    def ret
    switch(name) {
    case "[B": ret = new byte[size] ; break
    case "[S": ret = new short[size] ; break
    case "[I": ret = new int[size] ; break
    case "[J": ret = new long[size] ; break
    case "[C": ret = new char[size] ; break
    case "[F": ret = new float[size] ; break
    case "[D": ret = new double[size] ; break
    default: 
        ret = java.lang.reflect.Array.newInstance(java.lang.Class.forName( name [1..-1]), size)//Remove first array marker because Array.newInstance will create an array of type   
    }
    
    for (def i = 0; i < size; i++) {                        
        if (obj[i] instanceof List || obj[i].getClass().isArray()){
            ret[i] = to_array(obj[i],type)
        } else {
            ret[i] = obj[i]
        }
    }
    return ret
}

def to_list(array){
    return Arrays.asList(array)
}
//
///////////////////////////////////////////////////////////////////////////////////////////////////
// Standard scan commands
///////////////////////////////////////////////////////////////////////////////////////////////////


def lscan(writables, readables, start, end, steps, latency=0.0, relative=false, passes=1, zigzag =false, Closure before_read=null, Closure after_read=null, title=null) {
    /*
    Line Scan: positioners change together, linearly from start to end positions.
     
    Args:
    writables(list of Writable): Positioners set on each step.
    readables(list of Readable): Sensors to be sampled on each step.
    start(list of float): start positions of writables.
    end(list of float): final positions of writables.
    steps(int or float or list of float): number of scan steps (int) or step size (float).
    relative (bool, optional): if true, start and end positions are relative to current.
    latency(float, optional): sleep time in each step before readout, defaults to 0.0.
    passes(int, optional): number of passes
    zigzag(bool, optional): if true writables invert direction on each pass.    
    before_read (function): callback on each step, before each readout. Callback may have as 
    optional parameters list of positions.
    after_read (function): callback on each step, after each readout. Callback may have as 
    optional parameters a ScanRecord object. 
    title(str, optional): plotting window name.
     
    Returns:
    ScanResult object.
     */
    def int latency_ms = latency * 1000
    writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    start = to_array(start, 'd')
    end = to_array(end, 'd')
    steps = get_rank(steps)>0 ? to_array(steps, steps[0].getClass()==java.lang.Integer ? 'i' : 'd') : (int)steps    
    def scan = new LineScan(writables, readables, start, end, steps, relative, (int)latency_ms, passes, zigzag) {
        protected void onBeforeReadout(double[] pos) {
            if (before_read!=null) before_read(pos)
        }
        protected void onAfterReadout(ScanRecord record) {
            if (after_read!=null) after_read(record)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

def vscan(writables, readables, vector, line, latency=0.0, relative=false, passes=1, zigzag=false, Closure before_read=null, Closure after_read=null, title=null) {
    /*
    Vector Scan: positioners change following values provided in a vector.
     
    Args:
    writables(list of Writable): Positioners set on each step.
    readables(list of Readable): Sensors to be sampled on each step.
    vector(list of list of float): table of positioner values.
    line (bool, optional): if true, processs as line scan (1d)
    relative (bool, optional): if true, start and end positions are relative to current.
    latency(float, optional): sleep time in each step before readout, defaults to 0.0.        
    passes(int, optional): number of passes
    zigzag(bool, optional): if true writables invert direction on each pass.
    before_read (function): callback on each step, before each readout.
    after_read (function): callback on each step, after each readout.
    title(str, optional): plotting window name.
     
    Returns:
    ScanResult object.
     */
    def int latency_ms = latency * 1000
    writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    if (get_length(vector) == 0) 
    vector.append([])
    vector = to_array(vector, 'd')
    def scan = new VectorScan(writables, readables, vector, line, relative, (int)latency_ms, passes, zigzag) {
        protected void onBeforeReadout(double[] pos) {
            if (before_read!=null) before_read(pos)
        }
        protected void onAfterReadout(ScanRecord record) {
            if (after_read!=null) after_read(record)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

def ascan(writables, readables, start, end, steps, latency=0.0, relative=false, passes=1, zigzag=false, Closure before_read=null, Closure after_read=null, title=null) {
    /*      
    Area Scan: multi-dimentional scan, each positioner is a dimention.
     
    Args:
    writables(list of Writable): Positioners set on each step.
    readables(list of Readable): Sensors to be sampled on each step.
    start(list of float): start positions of writables.
    end(list of float): final positions of writables.
    steps(list of int or list of float): number of scan steps (int) or step size (float).
    relative (bool, optional): if true, start and end positions are relative to current.
    latency(float, optional): sleep time in each step before readout, defaults to 0.0.
    passes(int, optional): number of passes
    zigzag (bool, optional): if true writables invert direction on each row.
    before_read (function): callback on each step, before each readout.
    after_read (function): callback on each step, after each readout.
    title(str, optional): plotting window name.
     
    Returns:
    ScanResult object.
     */

    def int latency_ms = latency * 1000
    writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    start = to_array(start, 'd')
    end = to_array(end, 'd')    
    steps = to_array(steps, steps[0].getClass()==java.lang.Integer ? 'i' : 'd')
    
    def scan = new AreaScan(writables, readables, start, end, steps, relative, (int)latency_ms, passes, zigzag) {
        protected void onBeforeReadout(double[] pos) {
            if (before_read!=null) before_read(pos)
        }
        protected void onAfterReadout(ScanRecord record) {
            if (after_read!=null) after_read(record)
        }
    }

    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

def rscan(writable, readables, regions, latency=0.0, relative=false, passes=1, zigzag=false, Closure before_read=null, Closure after_read=null, title=null){
    /*
    Region Scan: positioner scanned linearly, from start to end positions, in multiple regions.

    Args:
        writable(Writable): Positioner set on each step, for each region.
        readables(list of Readable): Sensors to be sampled on each step.
        regions (list of tuples (float,floar, int)   or (float,floar, float)): each tuple define a scan region
                                (start, stop, steps) or (start, stop, step_size)                                  
        relative (bool, optional): if true, start and end positions are relative to current.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writable invert direction on each pass.
        before_read (function): callback on each step, before each readout. Callback may have as 
            optional parameters list of positions.
        after_read (function): callback on each step, after each readout. Callback may have as 
            optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.

    Returns:
        ScanResult object.
     */
    
    def int latency_ms = latency * 1000
    writable = string_to_obj(writable)
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")    
    start=[]
    end=[]
    steps=[]
    for (def region in regions) {
        start.add(region[0])
        end.add(region[1])
        steps.add(region[2])
    }
    start = to_array(start, 'd')
    end = to_array(end, 'd')
    steps = to_array(steps, steps[0].getClass()==java.lang.Integer ? 'i' : 'd')
    
    def scan = new RegionScan(writable, readables, start, end, steps, relative, (int)latency_ms, passes, zigzag) {
        protected void onBeforeReadout(double[] pos) {
            if (before_read!=null) before_read(pos)
        }
        protected void onAfterReadout(ScanRecord record) {
            if (after_read!=null) after_read(record)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
 }


def cscan(writables, readables, start, end, steps,latency=0.0, time=null, relative=false, passes=1, zigzag=false, Closure before_read=null, Closure after_read=null, title=null) {
    /*
    Continuous Scan: positioner change continuously from start to end position and readables are sampled on the fly.
     
    Args:
    writablse(Speedable or list of Motor): A positioner with a  getSpeed method or 
    a list of motors.
    readables(list of Readable): Sensors to be sampled on each step.
    start(float or list of float): start positions of writables.
    end(float or list of float): final positions of writabless.
    steps(int or float or list of float): number of scan steps (int) or step size (float).
    time = null
    time (float, seconds): if not null then writables is Motor array and speeds are 
    set according to time.
    relative (bool, optional): if true, start and end positions are relative to current.
    latency(float, optional): sleep time in each step before readout, defaults to 0.0.
    before_read (function): callback on each step, before each readout. 
    Callback may have as optional parameters list of positions.
    after_read (function): callback on each step, after each readout. 
    Callback may have as optional parameters a ScanRecord object. 
    title(str, optional): plotting window name.
     
    Returns:
    ScanResult object.
     
     */
    def int latency_ms = latency * 1000
    writables = to_array(string_to_obj(writables), "ch.psi.pshell.device.Writable")
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    start = to_array(start, 'd')
    end = to_array(end, 'd')
    if (get_rank(steps)>0){
        steps = to_array(steps, steps[0].getClass()==java.lang.Integer ? 'i' : 'd')
    } else {
        steps = to_array(steps, steps.getClass()==java.lang.Integer ? 'i' : 'd')
    }
    //A single Writable with fixed speed
    def scan
    if (time == null) {
        scan = new ContinuousScan(writables[0], readables, start[0], end[0], steps[0], relative, (int)latency_ms, passes, zigzag) {
            protected void onBeforeReadout(double[] pos) {
                if (before_read!=null) before_read(pos)
            }
            protected void onAfterReadout(ScanRecord record) {
                if (after_read!=null) after_read(record)
            }        
        }
    }
    else {
        //A set of Writables with speed configurable
        scan = new ContinuousScan(writables, readables, start, end, steps, time, relative, (int)latency_ms, passes, zigzag) {
            protected void onBeforeReadout(double[] pos) {
                if (before_read!=null) before_read(pos)
            }
            protected void onAfterReadout(ScanRecord record) {
                if (after_read!=null) after_read(record)
            }        
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

def hscan(config, writable, readables, start, end, steps, passes=1, zigzag=False, before_stream=None, after_stream=None, after_read=None, title=None) {
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
        before_stream (function): callback before just before starting positioner move. 
        after_stream (function): callback before just after stopping positioner move. 
        after_read (function): callback on each readout. 
                    Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.

    Returns:
        ScanResult object.

    */    
    writable = string_to_obj(writable)
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    start = to_array(start, 'd')
    end = to_array(end, 'd')    
    steps = to_array(steps, steps[0].getClass()==java.lang.Integer ? 'i' : 'd')
    scan = HardwareScan.newScan(config, writable,readables, start, end , steps, passes, zigzag);
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

def bscan(stream, records,Closure before_read=null, Closure after_read=null, title=null) {
    /*
    BS Scan: records all values in a beam synchronous stream.

    Args:
        stream(Stream): stream object
        records(int): number of records to store
        before_read (function): callback on each step, before each readout. 
                    Callback may have as optional parameters list of positions.
        after_read (function): callback on each step, after each readout. 
                    Callback may have as optional parameters a ScanRecord object. 
        title(str, optional): plotting window name.

    Returns:
        ScanResult object.

    */
    stream = string_to_obj(stream)    
    def scan = new BsScan(stream, (int)records) {
        protected void onBeforeReadout(double[] pos) {
            if (before_read!=null) before_read(pos)
        }
        protected void onAfterReadout(ScanRecord record) {
            if (after_read!=null) after_read(record)
        }        
    }    
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}

def tscan(readables, points, interval, Closure before_read=null, Closure after_read=null, title=null) {
    /* 
    Time Scan: sensors are sampled in fixed time intervals.
     
    Args:
        readables(list of Readable): Sensors to be sampled on each step.
        points(int): number of samples.
        interval(float): time interval between readouts.
        before_read (function): callback on each step, before each readout.
        after_read (function): callback on each step, after each readout.
        title(str, optional): plotting window name.
     
    Returns:
        ScanResult object.
     */
    if (interval>0){
        interval= Math.max(interval, 0.001)   //Minimum temporization is 1ms    
    }
    def int latency_ms = interval * 1000
    readables = to_array(string_to_obj(readables), "ch.psi.pshell.device.Readable")
    def scan = new TimeScan(readables, points, latency_ms) {
        protected void onBeforeReadout(double[] pos) {
            if (before_read!=null) before_read(pos)
        }
        protected void onAfterReadout(ScanRecord record) {
            if (after_read!=null) after_read(record)
        }
    }
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()

}

def escan(name, title=null) {
    /*
    Epics Scan: execute an Epics Scan Record.
    Args:
        name(str): Name of scan record.
        title(str, optional): plotting window name.
     
    Returns:
        ScanResult object.
     */
    def scan = new EpicsScan(name)
    scan.setPlotTitle(title)
    scan.start()
    return scan.getResult()
}


///////////////////////////////////////////////////////////////////////////////////////////////////
// Data Plotting
///////////////////////////////////////////////////////////////////////////////////////////////////
def  plot(data, name= null, xdata= null, ydata= null, title=null) {
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

    if (name != null) {
        if (get_length(name) == 0) {
            name = null
        } else {
            if (data==null) {
                data = []
                for (def n in name) data.add([])
            }
        }
        def plots = new PlotDescriptor[get_length(data)]
        for (def i = 0; i < get_length(data); i++) {
            def plotName = name ? name[i] : null
            def x = xdata
            if (get_length(x)>0) {
                x = x[i]
            }
            def y = ydata
            if (get_length(y)>0) {
                y = y[i];
            }
            plots[i] = new PlotDescriptor(plotName, to_array(data[i], 'd'), to_array(x, 'd'), to_array(y, 'd'));
        }
        return get_context().plot(plots, title);
    } else {
        def plot = new PlotDescriptor(name, to_array(data, 'd'), to_array(xdata, 'd'), to_array(ydata, 'd'));
        return get_context().plot(plot, title);
    }
}


///////////////////////////////////////////////////////////////////////////////////////////////////
// Data file access
///////////////////////////////////////////////////////////////////////////////////////////////////
def load_data(path, page=0) {
    /*
     Read data from the current persistence context or from data files.
     
     Args:
        path(str): Path to group or dataset relative to the persistence context root.
                  If in the format 'root|path' then read from path given by 'root'.
        page(int, optional): Data page (used for 3D datasets)
     Returns:
         Data array
     
     */
    def slice = get_context().dataManager.getData(path, page)
    return slice.sliceData
}

def get_attributes(path) {
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

def save_dataset(path, data) {
    /*
     Save data into a dataset within the current persistence context.
     
     Args:
        path(str): Path to dataset relative to the current persistence context root.
        data (array or list): data to be saved
     Returns:
         Dictionary
     
     */
    get_context().dataManager.setDataset(path, to_array(data, 'd'))
}

def create_dataset(path, type, unsigned=false, dimensions=null) {
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
    get_context().dataManager.createDataset(path, ScriptUtils.getType(type), unsigned, dimensions)
}

def create_table(path, names, types=null, lengths=null) {
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
    if (types != null) {
        for (i = 0; i < types.length; i++)
            types[i] = ScriptUtils.getType(types[i])
    }
    get_context().dataManager.createDataset(path, names, types, lengths)
}

def append_dataset(path, data, index=null) {
    /*
     Append data to dataset.

     Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(number or array or list): name of each column.
        index(int, optional): if set then add the data in a specific position in the dataset.
     Returns:
         null
     
     */
    data = to_array(data, 'd')
    if (index == null)
        get_context().dataManager.appendItem(path, data);
    else
        get_context().dataManager.setItem(path, data, index);
}

def append_table(path, data) {
    /*
     Append data to a table (dataset of compound type) 

     Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(list): List of valus for each column of the table. Array types can be expressed as lists.
     Returns:
         null
     
     */
    get_context().dataManager.appendItem(path, data)
}

def flush_data() {
    /*
    Flush all data files immediately.     
    Args:
        null
    Returns:
        null
     */
    get_context().dataManager.flush()
}

def set_attribute(path, name, value, unsigned=false) {
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
    if (value.getClass()==java.math.BigDecimal){
        value = value.toDouble();
    }
    get_context().dataManager.setAttribute(path, name, value, unsigned)
}

def log(log){
    /*
    Writes a log to the automatic data saving context - only if there is an ongoing scan or 
       script execution.        

    Args:        
         log(str): Log string.

    Returns:
        None
    */  
    get_context().scriptingLog(String.valueOf(log))
    get_context().dataManager.appendLog(String.valueOf(log))
}

def set_exec_pars(Map args){
    /*
    Configures the script execution parameters, overriding the system configuration.
    
    Args: 
      args(dictionary). Keys:
      args(optional arguments):
        name(str): value of the {name} tag. Default is the running script name.
        type(str): value of the {type} tag. Default is empty.
                             This field can be used to store data in  sub-folders of standard location.
        path(str):  If defined provides the full path name for data output root (overriding config))
                             The tag {data} can be used to enter a path relative to the standard data folder.
        layout(str): Overrides default data layout.
        format(str): Overrides default data format.
        depth_dim(int): dimension of 2d-matrixes in 3d datasets.
        save(bool): Overrides the configuration option to auto save scan data.
        flush(bool): Overrides the configuration option to flush file on each record.
        keep(bool): Overrides the configuration option to release scan records.
                                    If false disable accumulation of scan records to scan result.
        lazy(bool): Change option for lazy table creation. 
                                    If true create tables only after first record is received.
        preserve(bool): Overrides the configuration option to preserve device types.
                                  If false all values are converted to double.
        compression(obj): True for enabling default compression, int for specifying deflation level.                                    
                                    Device or list of devices for specifying devices to be compressed.
        shuffle(obj): True for enabling shuffling before compression. 
                                Device or list of devices for specifying devices to be shuffled.
        contiguous(obj): True for setting contiguous datasets for all devices.
                                   Device or list of devices for specifying device datasets to be contiguous.
        open(bool): If true opens data output root (instead of only doing in the first data access call)
                              If false closes output root, if open.
        reset(bool): If true reset the scan counter - the {count} tag and set the timestamp to now.
        group(str): Overrides default layout group name for scans
        tag(str): Overrides default tag for scan names (affecting group or dataset name, according to layout)
        then, then_success, then_exception(str): Sets statement to be executed on the completion of current. 
        defaults(bool): If true restore the original execution parameters.

        Graphical preferences can also be set. Keys are equal to lowercase of Preference enum:
        "plot_disabled", "plot_layout", "table_disabled", "enabled_plots", "plot_types", "print_scan", "auto_range", 
        "manual_range","domain_axis", "status".
        See set_preference for more information.    
        
        Shortcut entries: "line_plots": list of devices with enforced line plots.
    */
    get_context().setExecutionPars(args)
}

def get_exec_pars(){
    /*
    Returns script execution parameters.

    Returns:
        ExecutionParameters object. Fields: 
            name (str): execution name - {name} tag.
            type (str): execution type - {type} tag.
            path (str): output data root.
            open (bool): true if the output data root has been opened.
            layout (str): data output layout. If None then using the configuration.
            save (bool): auto save scan data option. 
            flush (bool): flush file on each record.
            index (int): current scan index.
            group (str): data group currently used for scan data storage. 
                         if no ongoing scan return "/" if within a script, or else None if a console command.
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

def caget(name, type=null, size = null) {
    /*
     Reads an Epics PV.
     
     Args:
        name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
        Scalar values: 'b', 'i', 'l', 'd', 's'.
        Array values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
     
     Returns:
         PV value
     
     */
    return Epics.get(name, Epics.getChannelType(type), size)
}

def cawait(name, value, timeout=null, comparator = null,  type=null, size = null) {
    /*
     Wait for a PV to have a given value.
     
     Args:
        name(str): PV name
        value (obj): value to compare to
        timeout(float, optional): time in seconds to wait. If None, waits forever.
        comparator(java.util.Comparator or float, optional): if None waits for equality. 
            If a numeric value is provided, waits for channel to be in range.
        type(str, optional): type of PV. By default gets the PV standard field type.
        Scalar values: 'b', 'i', 'l', 'd', 's'.
        Array values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
     
     Returns:
         None
     */
    if (timeout!=null)
        timeout = timeout * 1000
    return Epics.waitValue(name, value, comparator, (int)timeout, Epics.getChannelType(type), size)
}

def caput(name, value, timeout=null) {
    /*
     Writes to an Epics PV.
     
     Args:
        name(str): PV name
        value(scalar, string or array): new PV value.
        timeout(int, optional): timeout in seconds to the write. If None waits forever to completion.                    
     
     Returns:
         None
     */
    if (timeout!=null)
        timeout = timeout * 1000
    return Epics.put(name, value, (int)timeout)
}

def caputq(name, value) {
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


def create_channel(name, type = null, size = null) {
    if (type == null) type = null;
    return Epics.newChannel(name, Epics.getChannelType(type), size)
}

class Channel implements java.beans.PropertyChangeListener, Writable, Readable{
    final channel
    final callback
    Channel(name, type = null, size = null, Closure callback=null){
        /*
        Create an object that encapsulates an Epics PV connection.
        Args: 
            name(str): value to be written
            type(str, optional): type of PV. By default gets the PV standard field type.
                Scalar values: 'b', 'i', 'l', 'd', 's'.
                Array values: '[b', '[i,', '[l', '[d', '[s'.
            size(int, optional): the size of the channel
            callback(function, optional): The monitor callback.
        */  
        this.channel = Epics.newChannel(name, Epics.getChannelType(type), size)
        this.callback = callback
    }
    
    def get_channel_name(){
        /*
        Return the name of the channel.
        */  
        return channel.name
    }
    
    def get_size(){
        /*
        Return the size of the channel.
        */  
        return channel.size
    }
    
    def set_size(size){
        /*
        Set the size of the channel.
        */  
        channel.size = size
    }
    
    def is_connected(){
        /*
        Return True if channel is connected.
        */  
        return channel.connected
    }
    
    def is_monitored(){
        /*
        Return True if channel is monitored
        */
        return channel.monitored
    }
    def set_monitored(boolean value){
        /*
        Set a channel monitor to trigger the callback function defined in the constructor.
        */  
        channel.monitored = value
        if (value)
            channel.addPropertyChangeListener(this);
        else
            channel.removePropertyChangeListener(this);
    }
    def void propertyChange(java.beans.PropertyChangeEvent pce){
        if ((callback != null) && (pce.getPropertyName() == "value")){
                callback(pce.getNewValue())
        }
    }
    def put(value, timeout=None){
        /*
        Write to channel and wait value change. In the case of a timeout throws a TimeoutException.
        Args: 
            value(obj): value to be written
            timeout(float, optional): timeout in seconds. If none waits forever.
        */  
        if (timeout==None)
            channel.setValue(value);
        else
            channel.setValueAsync(value).get((int)(timeout*1000), java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    def putq(value){
        /*
        Write to channel and don't wait.
        */  
        channel.setValueNoWait(value)
    }
    
    def get(){
        /*
        Get channel value.
        */  
        return channel.getValue()
    }
    
    def wait_for_value(value, timeout=None, comparator=None){
        /*
        Wait channel to reach a value, using a given comparator. In the case of a timeout throws a TimeoutException.
        Args: 
            value(obj): value to be verified.                
            timeout(float, optional): timeout in seconds. If None waits forever.
            comparator (java.util.Comparator, optional). If None, uses Object.equals.
        */  
        if (comparator == null){
            if (timeout == null)
                channel.waitForValue(value);
            else
                channel.waitForValue(value, (int)(timeout*1000));
        } else{
            if (timeout == null)
                channel.waitForValue(value, comparator);
            else
                channel.waitForValue(value, comparator, (int)(timeout*1000));
        }
    }
        
    def close(){
        /*
        Close the channel.
        */  
        channel.destroy()        
    }
    //Writable interface
    def void write(value){
        putq(value)
    }
    //Readable interface
    def read() {
        return get()
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Concurrent execution 
///////////////////////////////////////////////////////////////////////////////////////////////////
def fork(Closure[] closures){
    """Start execution of functions in parallel. 

    Args:
        *functions(function references)

    Returns:
        List of future objects
    """      
    //callables = []
    //for m in functions:
    //    if is_list(m):
    //        callables.append(Callable(m[0],*m[1]))
    //    else:
    //        callables.append(Callable(m))
    return Threading.fork(closures)
}

def join(futures){
    """Wait parallel execution of functions.

    Args:
        futures(list of Future) : as returned from fork

    Returns:
        None
    """      
    try{
        return Threading.join(futures)
    } catch (java.util.concurrent.ExecutionException ex){
        throw ex.getCause()
    }
}

def parallelize(Closure[] closures){
    """Equivalent to fork + join

    Args:
        *functions(function references)

    Returns:
        None
    """   
    futures = fork(closures)
    return join(futures)
}


///////////////////////////////////////////////////////////////////////////////////////////////////
// Script evaluation and Background task control.
///////////////////////////////////////////////////////////////////////////////////////////////////
Object run(script_name, args = null, locals = null) {
    /*
    Run script: can be absolute path, relative, or short name to be search in the path.
    Args:
    args(Dict or List): Sets Sys.argv (if list) or gobal variables(if dict) to the script.
    locals(Dict): If not null sets the locals()for the runing script.
    If locals is used then script definitions will not go to global namespace.
     
    Returns:
    The script return value (if set with set_return)
     */
    get_context().scriptManager.evalFile(script_name)
}

def abort() {
    /*
    Abort the execution of ongoing task. It can be called from the script to quit.

    Args:                 
        None

    Returns:
        None
    */
    //Cannot be on script execution thread
    def task = {
         get_context().abort()
     }
    
    fork(task)  
    return null
}

def start_task(script, delay = 0.0, interval = -1){
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
    def delay_ms = delay * 1000
    def interval_ms = (interval>=0) ? interval * 1000 : interval
    get_context().taskManager.create(script, (int)delay_ms, (int)interval_ms)
    get_context().taskManager.start(script)
}
    
def stop_task(script, force=false){
    /*
    Stop a background task

    Args:        
         script(str): Name of the script implementing the task
         force(boolean, optional): interrupt current execution, if running

    Returns:
        None
    */      
    get_context().taskManager.remove(script, force)
}

def set_return(value){
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
    __THREAD_EXEC_RESULT__[Thread.currentThread()]=value  //Used when running file          
    return value    //Used when parsing file  
}


def is_interpreter_thread(){
    return Thread.currentThread().name == "Interpreter Thread" 
}

///////////////////////////////////////////////////////////////////////////////////////////////////
//UI interaction
///////////////////////////////////////////////////////////////////////////////////////////////////

def set_status(status){
    /*
    Set the application status.

    Args:
        status(str): new status.

    Returns:
        None
    */
    set_preference(Preference.STATUS, status)
}

def set_preference(preference, value){
    /*
    Hints to graphical layer:    

    Args:
        preference(Preference): Preference name
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

///////////////////////////////////////////////////////////////////////////////////////////////////
// Versioning tools
///////////////////////////////////////////////////////////////////////////////////////////////////
def commit(message){
    /*
    Commit the changes to the repository. If manual commit is not configured then there is no need to call this function: commits are made as needed.

    Args:
        message(str): commit message

    Returns:
        None
    */
    get_context().commit(message)
}

def add_repository(path){
    /*
    Add file or folder to repository

    Args:
        path(str): relative path to the home folder.

    Returns:
        None
    */  
    get_context().addRepository(path)
}

def diff(){
    /*
    Return list of changes in the repository

    Args:
        None

    Returns:
        None
    */   
    return get_context().diff()
}


def checkout_tag(tag){
    /*     
    Checkout a tag name.

    Args:
        tag(str): tag name.

    Returns:
        None
    */
    get_context().checkoutTag(tag)
}

def checkout_branch(tag){
    /*
    Checkout a local branch name.

    Args:
        tag(str): branch name.

    Returns:
        None
    */
    get_context().checkoutLocalBranch(tag)
}

def pull_repository(){
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

def push_repository(all_branches=true, force=false){
    /*
    Push to remote repository.

    Args:
        all_branches(boolean, optional): all branches or just current.
        force(boolean, optional): force flag.

    Returns:
        None
    */    
    get_context().pushToUpstream(all_branches, force)
}

def cleanup_repository(){
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
def get_device(device_name){
    /*
    Returns a configured device (or imaging source) by its name. 

    Args:
        device_name(str): name of the device.

    Returns:
        device
    */    
    return get_context().devicePool.getByName(device_name)
}

def add_device(device, force = false){
    /*
    Add  a device (or imaging source) to the device pool.

    Args:
        device(Device or Source): device object.
        force(boolean, optional): if true then dispose existing device with same name. 
                    Otherwise will fail in case of name clash.

    Returns:
        True if device was added, false if was already in the pool, or exception in case of name clash.
    */
    if (get_context().devicePool.contains(device)){
        return false
    }
    if (force){
        dev = get_context().devicePool.getByName(device.getName())
        if (dev != null) 
            remove_device(dev)
    }
    return get_context().devicePool.addDevice(device)
}

def remove_device(device){
    /*
    Remove a device (or imaging source) from the device pool.

    Args:
        device(Device or Source): device object.

    Returns:
        bool: true if device was removed.

    */
    return get_context().devicePool.removeDevice(device)
}


def set_device_alias(device, alias){
    /*
    Set a device alias to be used in scans (datasets and plots).

    Args:
        device(Device): device object.
        alias(str): replace device name in scans.

    Returns:
        None
    */
    device.setAlias(alias)
}    

///////////////////////////////////////////////////////////////////////////////////////////////////
// Utilities
///////////////////////////////////////////////////////////////////////////////////////////////////

def string_to_obj(obj) {
    if (obj instanceof String) {
        return evaluate(obj)
    } else if (obj instanceof Array) {
        ret = []
        for (i = 0; i < o.length; i++)
        ret[i] = string_to_obj(obj[i]);
        return ret
    }
    return obj
}

def stop(){
    /*
    Stop all devices implementing the Stoppable interface.

    Args:                 
        None

    Returns:
        None
    */
    get_context().stopAll()
}
    
def update(){
    /*
    Update all devices.

    Args:                 
        None

    Returns:
        None
    */
    get_context().updateAll()
}

def inject(){
    /*
    Restore initial globals: re-inject devices and startup variables to the interpreter.

    Args:
        None

    Returns:
        None

    */  
    get_context().injectVars()
}


def sleep(seconds){
    Thread.sleep(millis * 1000);
}

_ = true