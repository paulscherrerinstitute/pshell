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
    

class PShellClient:
    def __init__(self, url):
        self.url = url
        self.sse_event_loop_thread = None
        self.subscribed_events = None
        self.event_callback = None
    
    def _get_response(self, response, is_json=True):
        if response.status_code != 200: 
           raise Exception(response.text)
        return json.loads(response.text) if is_json else response.text

    def _get_binary_response(self, response):
        if response.status_code != 200: 
           raise Exception(response.text)
        return  response.raw.read()     
    
    def get_version(self):
        """Return application version.

        Args:

        Returns:
            String with application version.

        """
        return self._get_response(requests.get(url=self.url+"/version"), False)   
    
    def get_config(self):
        """Return application configuration.

        Args:

        Returns:
            Dictionary.
        """                
        return self._get_response(requests.get(url=self.url+"/config"))   
            
    def get_state(self):
        """Return application state.

        Args:

        Returns:
            String: Invalid, Initializing,Ready, Paused, Busy, Disabled, Closing, Fault, Offline
        """        
        return self._get_response(requests.get(url=self.url+"/state"))   
        
    def get_logs(self):
        """Return application logs.

        Args:

        Returns:
            List of logs.
            Format of each log: [date, time, origin, level, description]

        """        
        return self._get_response(requests.get(url=self.url+"/logs"))   
        
    def get_history(self, index):
        """Access console command history.

        Args:
            index(int): Index of history entry (0 is the most recent)

        Returns:
            History entry

        """
        return self._get_response(requests.get(url=self.url+"/history/"+str(index)), False) 
    
    def get_script(self, path):
        """Return script.

        Args:
            path(str): Script path (absolute or relative to script folder)

        Returns:
            String with file contents.

        """        
        return self._get_response(requests.get(url=self.url+"/script/"+str(path)), False) 
                    
    def get_devices(self):
        """Return global devices.

        Args:

        Returns:
            List of devices.
            Format of each device record: [name, type, state, value, age]

        """          
        return self._get_response(requests.get(url=self.url+"/devices"))   
        
    def abort(self, command_id=None):
        """Abort execution of command

        Args:
            command_id(optional, int): id of the command to be aborted.
                                       if None (default), aborts the foreground execution. 

        Returns:

        """          
        if command_id is None:
            requests.get(url=self.url+"/abort") 
        else:
            return requests.get(url=self.url+"/abort/"+str(command_id)) 
    
    def reinit(self):
        """Reinitialize the software.
 
        Args:

        Returns:

        """          
        requests.get(url=self.url+"/reinit") 
    
    def stop(self):
        """Stop all devices implementing the 'Stoppable' interface.
 
        Args:

        Returns:

        """        
        requests.get(url=self.url+"/stop")
    
    def update(self):
        """Update all global devices.
 
        Args:

        Returns:

        """        
        requests.get(url=self.url+"/update")             
   
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
        return self._get_response(requests.get(url=self.url+"/eval/"+statement), False)              

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
        return self._get_response(requests.put(url=self.url+"/run", json={"script":script, "pars":pars, "background":background, "async":False }))   
    
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
        return int(self._get_response(requests.get(url=self.url+"/evalAsync/"+statement), False))            

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
        return int(self._get_response(requests.put(url=self.url+"/run", json={"script":script, "pars":pars, "background":background, "async":True })))  

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
        return self._get_response(requests.get(url=self.url+"/result/"+str(command_id)))  

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
        return self._get_response(requests.get(url=self.url+"/autocompletion/" + input))   

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
        return self._get_response(requests.get(url=self.url+ "/contents" + ("" if path is None else ( "/"+path))), False)    
   
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
            return self._get_response(requests.get(url=self.url+ "/data-json/"+path), True)  
        elif type == "bin":
            return  self._get_binary_response(requests.get(url=self.url+"/data-bin/"+path, stream=True))
        elif type == "bs":    
            from collections import OrderedDict
            bs = self._get_binary_response(requests.get(url=self.url+"/data-bs/"+path, stream=True))
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
            
        return self._get_response(requests.get(url=self.url+ "/data" + ("" if path is None else ( "/"+path))), False)    
              
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
                    messages = SSEClient(self.url+"/events")
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
            subscribed_events: list of event names to substribe to. If None subscribes to all.
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
                                                     kwargs={}, \
                                                     daemon=True)
                self.sse_event_loop_thread.start()
        else:
            raise Exception ("sseclient library is not instlled: server events are not available")

    def on_event(self, name, value):
        pass
        