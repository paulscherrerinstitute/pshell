import threading
import time
import sys
import requests
import json

try:
    from urllib import quote  # Python 2
except ImportError:
    from urllib.parse import quote  # Python 3

try:
    from sseclient import SSEClient
except:
    SSEClient = None 
    

class TimeoutException(Exception):
   pass

class PShellClient:
    def __init__(self, url):
        if not url.endswith('/'):
            url=url+"/"
        self.url = url
        self.sse_event_loop_thread = None
        self.subscribed_events = None
        self.event_callback = None
        self.plot_defaults={"format":"png", "width":600, "height":400}
        self.debug = False
    
    def _get(self, url, stream=False):
        url=self.url+url
        if self.debug:
            print ("GET " + url)
        return requests.get(url=url, stream=stream)
        
        
    def _put(self, url, json_data=None):
        url=self.url+url
        if self.debug:
            print ("PUT " + url + " -> " + json.dumps(json_data))
        return requests.put(url=url, json=json_data)           
    
    def _del(self, url):
        url=self.url+url
        if self.debug:
            print ("DEL " + url)
        return requests.delete(url=url)
    
    def _get_response(self, response, is_json=True):
        if self.debug==True or  self.debug=="rx":
            print (" -> " + str(response.status_code) + ((" - " + response.text) if self.debug=="rx" else ""))
        try:
            response.raise_for_status()
        except: 
            print (response.text)
            raise
        return json.loads(response.text) if is_json else response.text

    def _get_binary_response(self, response):
        response.raise_for_status()
        return  response.raw.read()     
    
    def get_plot_defaults(self):
        """Return plot default properties.

        Args:

        Returns:
            Dictionary

        """        
        return self.plot_defaults.copy()
    
    def set_plot_defaults(self, defaults):
        """Update plot default properties.

        Args:
            Dictionary that will updated into the plot default properties.
        Returns:

        """        
        self.plot_defaults.update(defaults)        
        
    def get_version(self):
        """Return application version.

        Args:

        Returns:
            String with application version.

        """
        return self._get_response(self._get("version"), False)   
    
    def get_config(self):
        """Return application configuration.

        Args:

        Returns:
            Dictionary.
        """                
        return self._get_response(self._get("config"))   
            
    def get_state(self):
        """Return application state.

        Args:

        Returns:
            String: Invalid, Initializing,Ready, Paused, Busy, Disabled, Closing, Fault, Offline
        """        
        return self._get_response(self._get("state"))   
    
    def wait_state(self, state, timeout = -1):
        """Wait application state equals.

        Args:
            state (string or list of strings)
        Returns:
        """
        if type(state)==str:
            state = [state]
        start = time.time()
        while self.get_state() not in state:
            if (timeout>=0) and ((time.time()-start)>timeout):
                raise TimeoutException()
            time.sleep(0.1)

    def wait_state_not(self, state, timeout = -1):
        """Wait application state different than.

        Args:
            state (string or list of strings)
        Returns:
        """
        if type(state)==str:
            state = [state]
        start = time.time()
        while self.get_state() in state:
            if (timeout>=0) and ((time.time()-start)>timeout):
                raise TimeoutException()
            time.sleep(0.1)
            
    def get_logs(self):
        """Return application logs.

        Args:

        Returns:
            List of logs.
            Format of each log: [date, time, origin, level, description]

        """        
        return self._get_response(self._get("logs"))   
        
    def get_history(self, index):
        """Access console command history.

        Args:
            index(int): Index of history entry (0 is the most recent)

        Returns:
            History entry

        """
        return self._get_response(self._get("history/"+str(index)), False) 
    
    def get_script(self, path):
        """Return script.

        Args:
            path(str): Script path (absolute or relative to script folder)

        Returns:
            String with file contents.

        """        
        return self._get_response(self._get("script/"+str(path)), False) 
                    
    def get_devices(self):
        """Return global devices.

        Args:

        Returns:
            List of devices.
            Format of each device record: [name, type, state, value, age]

        """          
        return self._get_response(self._get("devices"))   
        
    def abort(self, command_id=None):
        """Abort execution of command

        Args:
            command_id(optional, int): id of the command to be aborted.
                                       if None (default), aborts the foreground execution. 

        Returns:

        """          
        if command_id is None:
            self._get("abort") 
        else:
            return self._get("abort/"+str(command_id)) 
    
    def pause(self):
        """Pause execution of command

        Args:
           
        Returns:

        """          
        self._get("pause") 

    def resume(self):
        """Resume execution of command

        Args:
           
        Returns:

        """          
        self._get("resume") 


    def reinit(self):
        """Reinitialize the software.
 
        Args:

        Returns:

        """          
        self._get("reinit") 
    
    def stop(self):
        """Stop all devices implementing the 'Stoppable' interface.
 
        Args:

        Returns:

        """        
        self._get("stop")
    
    def update(self):
        """Update all global devices.
 
        Args:

        Returns:

        """        
        self._get("update")             
   
    def eval(self,statement):
        """Evaluates a statement in the interpreter.
           If the statement finishes by '&', it is executed in background.
           Otherwise statement is executed in foreground (exclusive).
 
        Args:       
            statement(str): input statement

        Returns: 
            String containing the console return. 
            If an exception is produces in the interpretor, it is re-thrown here.
        """             
        statement = quote(statement)
        return self._get_response(self._get("eval/"+statement), False)              

    def run(self,script, pars=None, background=False):
        """Executes script in the interpreter.
 
        Args:       
            script(str): name of the script (absolute or relative to the script base folder). Extension may be omitted.
            pars(optional, list or dict): if a list is given, it sets sys.argv for the script.
                                          If a dict is given, it sets global variable for the script.
            background(optional, bool): if True script is executed in background.

        Returns: 
            Return value of the script.
            If an exception is produces in the interpretor, it is re-thrown here.
        """          
        return self._get_response(self._put("run", {"script":script, "pars":pars, "background":background, "async":False }))   
        
    def start_eval(self,statement):
        """Starts evaluation of a statement in the interpreter.
           If the statement finishes by '&', it is executed in background.
           Otherwise statement is executed in foreground (exclusive).
 
        Args:       
            statement(str): input statement

        Returns: 
            Command id (int), which is used to retrieve command execution status/result (get_result). 
        """            
        statement = quote(statement)
        return int(self._get_response(self._get("evalAsync/"+statement), False))            
    
    def eval_json(self,statement):
        """Evaluates a statement in the interpreter. 
        Args:       
            statement(str): input statement

        Returns: 
            Return object  decoded from JSON string
        """             
        statement = quote(statement)
        return self._get_response(self._get("eval-json/"+statement), True)                      

    def set_var(self,name, value):
        """Sets interpreter variable. 
        Args:       
            name(str): variable name
            value(obj): value - must be JSON compatible

        Returns: 
            
        """             
        data = {}
        data["name"]=name
        data["value"]=value
        return self._get_response(self._put("set-var", data), False)           

    def start_run(self,script, pars=None, background=False):
        """Starts execution of a script in the interpreter.
 
        Args:       
            script(str): name of the script (absolute or relative to the script base folder). Extension may be omitted.
            pars(optional, list or dict): if a list is given, it sets sys.argv for the script.
                                          If a dict is given, it sets global variable for the script.
            background(optional, bool): if True script is executed in background.

        Returns: 
            Command id (int), which is used to retrieve command execution status/result (get_result). 
        """        
        return int(self._get_response(self._put("run", {"script":script, "pars":pars, "background":background, "async":True })))  

    def get_result(self, command_id=-1):
        """Gets status/result of a command executed asynchronously (start_eval and start_run).
 
        Args:       
            command_id(optional, int): command id. If equals to -1 (default) return status/result of the foreground task. 

        Returns: 
            Dictionary with the fields: 'id' (int): command id
                                        'status' (str): unlaunched, invalid, removed, running, aborted, failed or completed.
                                        'exception' (str): if status equals 'failed', holds exception string.
                                        'return' (obj): if status equals 'completed', holds return value of script (start_run) 
                                                        or console return (start_eval)
        """          
        return self._get_response(self._get("result/"+str(command_id)))  

    def help(self, input = "<builtins>"):
        """Returns help or auto-completion strings.
 
        Args:       
            input(optional, str): - ":" for control commands
                                  - "<builtins>" for builtin functions
                                  - "devices" for device names
                                  - builtin function name for function help
                                  - else contains entry for auto-completion

        Returns: 
            List
             
        """        
        return self._get_response(self._get("autocompletion/" + input))   

    def get_contents(self, path=None):
        """Returns contents of data path.
 
        Args:       
            path(optional, str): Path to data relative to data home path.
                                 - Folder
                                 - File
                                 - File (data root) |  internal path
                                 - internal path (on currently open data root)

        Returns: 
            List of contents
             
        """           
        return self._get_response(self._get("contents" + ("" if path is None else ( "/"+path))), False)    
   
    def get_data(self, path, type="txt"):
        """Returns data on a given path.
 
        Args:       
            path(str): Path to data relative to data home path.
                                 - File (data root) |  internal path
                                 - internal path (on currently open data root)
            type(optional, str): txt, "json", "bin", "bs"

        Returns: 
            Data accordind to selected format/.
             
        """           
        if type == "json":
            return self._get_response(self._get("data-json/"+path), True)  
        elif type == "bin":
            return  self._get_binary_response(self._get("data-bin/"+path, stream=True))
        elif type == "bs":    
            from collections import OrderedDict
            bs = self._get_binary_response(self._get("data-bs/"+path, stream=True))
            index=0
            msg = []
            for i in range(4):
               size =int.from_bytes(bs[index:index+4], byteorder='big', signed=False)
               index=index+4
               msg.append(bs[index:index+size])
               index=index+size            
            [main_header, data_header, data, timestamp] = msg
            main_header = json.loads(main_header, object_pairs_hook=OrderedDict)
            data_header = json.loads(data_header, object_pairs_hook=OrderedDict)
            channel =  data_header["channels"][0]
            channel["encoding"] = "<" if channel.get("encoding", "little") else ">"
            from bsread.data.helpers import get_channel_reader
            channel_value_reader = get_channel_reader(channel)
            return channel_value_reader(data)            
            
        return self._get_response(self._get("data" + ("" if path is None else ( "/"+path))), False)    
    
    def get_data_attrs(self, path):
        return self._get_response(self._get("data-attr/"+path), True)  

    def get_data_info(self, path):
        return self._get_response(self._get("data-info/"+path), True)  

    def get_scan_data(self, layout, path, group, device, type="txt"):
        """Returns scan data of a device.
 
        Args:       
            layout(str): data layout
            path(str): scan path
            group(str): scan group
            device(str): device name
            type(optional, str): txt, "json", "bin"

        Returns: 
            Data accordind to selected format.
             
        """     
        if layout is None or layout.strip()=="" or path is None:
            raise Exception ("Invalid scan persistence path or layout")
        path=path.replace("/", "<br>")
        path=path.replace("|", "<p>")        
        group=group.replace("/", "<br>")
        layout=layout.replace(".", "<br>")
        url=layout+"/"+path+"/"+group+"/"+device
        if type == "json":            
            url= "scandata-json/"+url
            return self._get_response(self._get(url), True)  
        elif type == "bin":
            url= "scandata-bin/"+url
            return  self._get_binary_response(self._get(url, stream=True))            
        url= "scandata/"+url
        return self._get_response(self._get(url), False)        
    
    def get_plot_contexts(self):
        """Return list of plot contexts

        Args:

        Returns:
            List of names

        """          
        return self._get_response(self._get("plots"))
    
    
    def delete_plot_context(self, title):
        """
        Delete a plotting context.

        Args:
            title(str): name of the plotting context

        Returns:

        """          
        return self._get_response(self._del("plots/"+title), False)  
    
    def get_num_plots(self, title=None):
        """Return number of plots in a given plotting context.

        Args:
            title(str): name of the plotting context

        Returns:
            Number of plots

        """          
        if title is None:
            title="null"
        return int(self._get_response(self._get("plots/"+title)))   

    def get_plot(self, title=None, index=0, format="png", width=None, height=None):
        """Return a plot as a given image type.

        Args:
            title(str): name of the plotting context
            index(int): plot index (0-based)
            format(str): plot format ("jpg", "png", "gif", "tif")
            width(int): plot width (if 0 gets plot staddard size)
            height(int): plot height (if 0 gets plot staddard size)
        Returns:
            Image file byte array

        """          
        if title is None:
            title="null"      
        if format is None:
            format=self.plot_defaults["format"]      
        if width is None:
            width=self.plot_defaults["width"]      
        if height is None:
            height=self.plot_defaults["height"]      

        url = "plot/"+title+"/"+str(index)+"/"+format+"/"+str(width)+"/"+str(height)
        return  self._get_binary_response(self._get(url, stream=True))
    
    def print_logs(self):
        for l in self.get_logs():
            print ("%s %s %-20s %-8s %s" % tuple(l))
    
    def print_devices(self):
        for l in self.get_devices():
            print ("%-16s %-32s %-10s %-32s %s" % tuple(l)) 
                   
    def print_help(self, input = "<builtins>"):
        for l in self.help(input):
            print (l)  

    #Events       
    def _sse_event_loop_task(self):  
        try:
            while True:
                try:
                    messages = SSEClient(self.url+"events")
                    for msg in messages:
                        if (self.subscribed_events is None) or (msg.event in self.subscribed_events):
                            try:
                                value = json.loads(msg.data)                               
                            except:
                                value = str(msg.data)
                            self.event_callback(msg.event, value)
                except IOError as e:
                    #print(e)
                    pass
                except:
                    print("Error:", sys.exc_info()[1])
                    #raise
        finally:
            print ("Exit SSE loop task")
            self.sse_event_loop_thread = None


    def start_sse_event_loop_task(self, subscribed_events = None, event_callback = None):
        """
        Initializes server event loop task.
        Args:    
            subscribed_events: list of event names to subscribe to. If None subscribes to all.
            event_callback: callback function. If None, self.on_event is called instead.

        Usage example:
            def on_event(name, value):
                if name == "state":
                    print ("State changed: ",  value)
                elif name == "record":
                    print ("Received scan record: ", value) 

            pc.start_sse_event_loop_task(["state", "record"], on_event)    
           
        """
        self.event_callback = event_callback if event_callback is not None else self.on_event
        self.subscribed_events = subscribed_events
        if SSEClient is not None:
            if self.sse_event_loop_thread is None:
                self.sse_event_loop_thread = threading.Thread(target=self._sse_event_loop_task, \
                                                     args = (), \
                                                     kwargs={})
                self.sse_event_loop_thread.daemon = True
                self.sse_event_loop_thread.start()
        else:
            raise Exception ("sseclient library is not installed: server events are not available")

    def on_event(self, name, value):
        pass


if __name__ == "__main__":
    import socket
    ps = PShellClient( "http://" + socket.gethostname() + ":8080")
    print (ps.get_state())

