from . import PShellClient
import json

class ScanResult(list):
    def __init__(self, data, proxy):
        self.data=data
        self.proxy = proxy
        
    def __len__(self):
        if 'records' in self.data['return']:
            return len(self.data['return']['records'])       
        
    def __repr__(self):
        return str(self)
    
    def __str__(self):
        return "Scan " + str(self.get_id()) + " - " + str(self.get_status())
        
    def __getitem__(self, key):
        if type(key) == int:
            return self.data['return']['records'][key]
        if type(key) == str:
            index = self._get_readable_index(key)
            if index is not None:
                return self.get_readable(key)
            index = self._get_writable_index(key)
            if index is not None:
                return self.get_writable(key)
            
    def _get_readable_index(self, name):
        names = self.data['return']["readableNames"]
        for i in range(len(names)):
            if names[i]==name:
                return i
            
    def _get_writable_index(self, name):
        names = self.data['return']["writableNames"]
        for i in range(len(names)):
            if names[i]==name:
                return i  
        
    def get_id(self):
        return self.data['id']
   
    def get_return(self):
        return self.data['return']
    
    def get_status(self):
        return self.data['status']    
    
    def get_exception(self):
        return self.data['exception']
    
    def get_in_memory(self):
        return self.get_return()["inMemory"]    

    def get_index(self):
        return self.get_return()["index"]
    
    def get_layout(self):
        return self.get_return()["dataLayout"]    

    def get_provider(self):
        return self.get_return()["dataProvider"]    

    def get_size(self):
        return self.get_return()["size"]    
    
    def get_tag(self):
        return self.get_return()["tag"]    
    
    def get_path(self):
        return self.get_return()["path"]    

    def get_tag(self):
        return self.get_return()["tag"]    
    
    def get_root(self):
        return self.get_return()["root"]   
    
    def get_group(self):
        return self.get_return()["group"]      
    
    def get_dimensions(self):
        return self.get_return()["dimensions"]    

    def get_error_code(self):
        return self.get_return()["errorCode"]    
    
    def get_readables(self):
        return self.get_return()["readableNames"]
    
    def get_writables(self):
        return self.get_return()["writableNames"]  
    
    def get_diags(self):
        return self.get_return()["diagNames"]        
    
    def get_monitors(self):
        return self.get_return()["monitorNames"]          
    
    def get_snaps(self):
        return self.get_return()["snapValues"]  
    
    def get_records(self):
        return self.get_return()["records"]
    
    def get_time_elapsed(self):
        return self.get_return()["timeElapsed"]
    
    def get_timestamps(self):
        return self.get_return()["timestamps"]    
    
    def get_readable(self, index):
        if index is not None:
            if self.get_in_memory():
                if type(index) is str:
                    index = self._get_readable_index(index)
                return [item['readables'][index] for item in self.get_records()] 
            else:
                #data = self.proxy.get_data(self.get_path(), "json")
                #return [item[index] for item in data["data"]['sliceData'] ]            
                return self.get_saved_data(str(index), type="json")

    def get_writable(self, index):
        if index is not None:
            if self.get_in_memory():
                if type(index) is str:
                    index = self._get_writable_index(index)
                return [item['writables'][index] for item in self.get_records()]         
            else:
                #data = self.proxy.get_data(self.get_path(), "json")
                #return [item[index] for item in data["data"]['sliceData']]
                return self.get_saved_data(str(index), type="json")
                
    def get_saved_data(self, device, type="txt"):
        return self.proxy.get_scan_data(self.get_layout(), self.get_path(), self.get_group(), device, type)
            
                
class PShellProxy(PShellClient):
    def __init__(self, url): 
        PShellClient.__init__(self, url)        
        self.inject()
        self.data_home=self.eval("expand_path('{data}')")+"/"
        self.scan_defaults={}

    def get_scan_defaults(self):
        """Return scan default properties.

        Args:

        Returns:
            Dictionary

        """        
        return self.scan_defaults.copy()
    
    def set_scan_defaults(self, defaults):
        """Update scan default properties.

        Args:
            Dictionary that will updated into the scan default properties.
        Returns:

        """        
        self.scan_defaults.update(defaults)                
    
    def inject(self):        
        self.devices = self.get_device_names()
        for dev in self.devices:
            setattr(self, dev, dev)
    
    def _get_arg_str(self, arg):
        if type(arg) is str:
            return "'" + arg + "'"
        else:
            return str(arg)
        
    def _get_cmd(self, function, *args, **kwargs):
        pars = ""
        for val in args:
            pars = pars+ self._get_arg_str(val) + ","
        for key, val in kwargs.items():
            pars = pars +  key + "=" + self._get_arg_str(val) + ","
        ret= function + "(" + pars + ")"
        if self.debug:
            print ("CMD: " + ret)
        return ret

    def get_device_names(self):
        return [d[0] for d in self.get_devices()]
        
        
    #Scans    
    def lscan(self, writables, readables, start, end, steps, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
        return self._run_scan("lscan", writables, readables, start, end, steps, latency, relative, passes, zigzag, **pars)  

    def vscan(self, writables, readables, vector, line = False, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
        return self._run_scan("vscan", writables, readables, vector, line, latency, relative, passes, zigzag, **pars) 
        
    def ascan(self, writables, readables, start, end, steps, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
        return self._run_scan("ascan", writables, readables, start, end, steps, latency, relative, passes, zigzag, **pars)  
    
    def rscan(self, writable, readables, regions, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
        return self._run_scan("rscan", writable, readables, regions, latency, relative, passes, zigzag, **pars)  
        
    def cscan(self, writables, readables, start, end, steps, latency=0.0, time=None, relative=False, passes=1, zigzag=False, **pars):
        return self._run_scan("cscan", writables, readables, start, end, steps, latency, time, relative, passes, zigzag, **pars)  
    
    def hscan(self, config, writable, readables, start, end, steps, passes=1, zigzag=False, **pars):
        return self._run_scan("rscan", config, writable, readables, start, end, steps, passes, zigzag, **pars)
    
    def bscan(self, stream, records, timeout = None, passes=1, **pars):
        return self._run_scan("bscan", stream, records, timeout, passes, **pars)
    
    def tscan(self, readables, points, interval, passes=1, fixed_rate=True, **pars):
        return self._run_scan("tscan", readables, points, interval, passes, fixed_rate, **pars)        
    
    def mscan(self, trigger, readables, points=-1, timeout=None, asynchronous=True, take_initial=False, passes=1, **pars):
        return self._run_scan("mscan", trigger, readables, points, timeout, asynchronous, take_initial, passes, **pars)
    
    def escan(self, name, **pars):
        return self._run_scan("escan", name, **pars)  
        
    def bsearch(self, writables, readable, start, end, steps, maximum = True, strategy = "Normal", latency=0.0, relative=False, **pars):
        return self._run_scan("bsearch", writables, readable, start, end, steps, maximum , strategy, latency, relative, **pars)  
        
    def hsearch(self, writables, readable, range_min, range_max, initial_step, resolution, filter=1, maximum=True, latency=0.0, relative=False, **pars):
        return self._run_scan("hsearch", writables, readable, range_min, range_max, initial_step, resolution, filter, maximum, latency, relative, **pars)  
                
    def _run_scan(self, scan, *args, **kwargs):
        try:
            kw = self.scan_defaults.copy()
            kw.update(kwargs)
            self.scan_cmd = self._get_cmd(scan, *args, **kw)
            r=self.eval(self.scan_cmd)
            return ScanResult(self.get_result(), self)        
        except KeyboardInterrupt:
            print ("Interrrupting scan...", end='')
            self.abort()
            print (" done")

    
    
    #Plots
    def plot(self, data, name = None, xdata = None, ydata=None, title=None):
        if (data is not None):
            data = json.dumps(data)
        if (xdata is not None):
            xdata = json.dumps(xdata)
        if (ydata is not None):
            ydata = json.dumps(ydata)
        return self.eval(self._get_cmd("plot", data, name, xdata, ydata, title))
    
    
    #EPICS
    def caget(self, name, type=None, size=None, meta=False ):
        return self.eval(self._get_cmd("caget", name, type, size, meta))
    
    def cawait(self, name, value, timeout=None, comparator=None, type=None, size=None):
        return self.eval(self._get_cmd("cawait", name, value, timeout, comparator, type, size))
    
    def caput(self, name, value, timeout = None):
        return self.eval(self._get_cmd("caput", name, value, timeout))
    
    def caputq(self, name, value):
        return self.eval(self._get_cmd("caputq", name, value))
        
        
    #Devices        
    def remove_device(self, device):
        return self.eval(self._get_cmd("remove_device", device))
    
    def set_device_alias(self, device, alias):
        return self.eval(self._get_cmd("set_device_alias", device, alias))
    
    def stop(self):
        return self.eval(self._get_cmd("stop"))
    
    def update(self):
        return self.eval(self._get_cmd("update"))
    
    def reinit(self, dev = None):
        return self.eval(self._get_cmd("reinit", dev))
    
    def create_device(self, url, parent=None):
        self.eval(self._get_cmd("__dev__=create_device", url, parent))
        self.eval("add_device(__dev__,True)")
        return self.eval("__dev__.getName()")

    def create_channel_device(self, channel_name, type=None, size=None, device_name=None, monitored=False):
        self.eval(self._get_cmd("__dev__=create_channel_device", channel_name, type, size, device_name, monitored))
        self.eval("add_device(__dev__,True)")
        return self.eval("__dev__.getName()")
            
    def create_averager(self, dev, count, interval=0.0, device_name = None, monitored=False):
        ret=self.eval(self._get_cmd("__dev__=create_averager",  dev, count, interval, device_name, monitored))
        self.eval("add_device(__dev__,True)")
        return self.eval("__dev__.getName()")
    
    def write(self, dev, value):
        r=self.set_var("__val__", value)
        self.eval(dev+".write(__val__)")
        
    def read(self, dev):
        return self.eval_json(dev+".read()")
    
    def take(self, dev):
        return self.eval_json(dev+".read()")


    #Utilities
    def get_setting(self, name=None):
        return self.eval(self._get_cmd("get_setting", name))
    
    def set_setting(self, name, value):
        return self.eval(self._get_cmd("set_setting", name, value))

    def log(self, log, data_file=None):
        return self.eval(self._get_cmd("log", log, data_file))

    #IPython utils
    def display_plots(self, context=None, index=None, format="png", width=None, height=None, horizontal=False, clear=False):
        images = []
        from IPython.display import Image, display, clear_output
        if index is None:
            for index in range(self.get_num_plots(context)):
                images.append(Image(self.get_plot(context,index,format,width,height)))
        else:
            images.append(Image(self.get_plot(context,index, format,width,height)))
        if clear:
            clear_output()
        if horizontal:
            from ipywidgets import widgets, HBox
            imageA = widgets.Image(value=images[0].data)
            imageB = widgets.Image(value=images[1].data)
            hbox = HBox([imageA, imageB])
            display(hbox)            
        else:            
            display(*images)
            
