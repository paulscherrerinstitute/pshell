import requests
import json
try:
    from urllib import quote  # Python 2
except ImportError:
    from urllib.parse import quote  # Python 3


class PShellClient:
    def __init__(self, url):
        self.url = url
    
    def get_response(self, response, is_json=True):
        if response.status_code != 200: 
           raise Exception(response.text)
        return json.loads(response.text) if is_json else response.text

    def get_binary_response(self, response):
        if response.status_code != 200: 
           raise Exception(response.text)
        return  response.raw.read()     
    
    def get_version(self):
        return self.get_response(requests.get(url=self.url+"/version"), False)   
    
    def get_config(self):
        return self.get_response(requests.get(url=self.url+"/config"))   
            
    def get_state(self):
        return self.get_response(requests.get(url=self.url+"/state"))   
        
    def get_logs(self):
        return self.get_response(requests.get(url=self.url+"/logs"))   
        
    def get_history(self, index):
        return self.get_response(requests.get(url=self.url+"/history/"+str(index)), False) 
    
    def get_script(self, path):
        return self.get_response(requests.get(url=self.url+"/script/"+str(path)), False) 
                    
    def get_devices(self):
        return self.get_response(requests.get(url=self.url+"/devices"))   
        
    def abort(self, command_id=None):
        if command_id is None:
            requests.get(url=self.url+"/abort") 
        else:
            return requests.get(url=self.url+"/abort/"+str(command_id)) 
    
    def reinit(self):
        requests.get(url=self.url+"/reinit") 
    
    def stop(self):
        requests.get(url=self.url+"/stop")
    
    def update(self):
        requests.get(url=self.url+"/update")             
   
    def eval(self,statement, async=False):
        statement = quote(statement)
        if async:
            return int(self.get_response(requests.get(url=self.url+"/evalAsync/"+statement), False)) 
        else:
            return self.get_response(requests.get(url=self.url+"/eval/"+statement), False)              

    def run(self,script, pars=None, background=False, async=False):
        return self.get_response(requests.put(url=self.url+"/run", json={"script":script, "pars":pars, "background":background, "async":async }))  

    def get_result(self, command_id=-1):
        return self.get_response(requests.get(url=self.url+"/result/"+command_id))  

    def help(self, input = "<builtins>"):
        return self.get_response(requests.get(url=self.url+"/autocompletion/" + input))   

    def get_contents(self, path=None):
        return self.get_response(requests.get(url=self.url+ "/contents" + ("" if path is None else ( "/"+path))), False)    
   
    def get_data(self, path=None, type="txt"):
        if type == "json":
            return self.get_response(requests.get(url=self.url+ "/data-json/"+path), True)  
        elif type == "bin":
            return  self.get_binary_response(requests.get(url=self.url+"/data-bin/"+path, stream=True))
        elif type == "bs":    
            from collections import OrderedDict
            bs = self.get_binary_response(requests.get(url=self.url+"/data-bs/"+path, stream=True))
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
            
        return self.get_response(requests.get(url=self.url+ "/data" + ("" if path is None else ( "/"+path))), False)    
              
    def print_logs(self):
        for l in self.get_logs():
            print ("%s %s %-20s %-8s %s" % tuple(l))
    
    def print_devices(self):
        for l in self.get_devices():
            print ("%-16s %-32s %-10s %-32s %s" % tuple(l)) 
                   
    def print_help(self, input = "<builtins>"):
        for l in self.help(input):
            print (l)      
