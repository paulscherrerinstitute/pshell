################################################################################################### 
# Example on scans saving data as external tiff files
################################################################################################### 

from ijutils import load_array, save_image
    
def save_as_tiff(data, filename, parallel=True, metadata={}):
    """
    Save data as tiff file

    Args:
        device(Array, Data or ImageProcessor object): Data to be saved
        parallel(Bool, optiona): if True image is saved in separated thread.
        metadata(Dict, optional): written to tiff info property.
        
    """
    def _save_as_tiff(data, filename, metadata={}):
      try:
        if isinstance(data,PyArray):
            ip = load_array(data)
        elif type(data) == Data:
            ip = load_array(data.matrix)
        else:
            ip = data
    
        #Metadata
        info = "Timestamp: " + time.strftime("%y/%m/%d %H:%M:%S",time.localtime())
        for key,val in metadata.items():
            info = info + "\n" + str(key) + ": " + str(val)        
        ip.setProperty("Info", info)
        
        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))    
        save_image(ip, filename,"tiff")    
      except:
        print sys.exc_info()    
    if parallel:
        return fork((_save_as_tiff,(data, filename, metadata)),)
    else:
        _save_as_tiff(data, filename, metadata)

        
        
class ExternalImage(ReadonlyRegisterBase, RegisterString):
    def doInitialize(self):
        self.folder = os.path.splitext(get_exec_pars().path)[0]
        self.folder = IO.getRelativePath(self.folder, expand_path("{data}"))
        self.index=0
        
    def doRead(self):
        time.sleep(0.001)
        self.filename = self.folder + "/images/" + ("%04d" % (self.index,)) + ".tiff"
        save_as_tiff(src1.data, expand_path("{data}") + "/" + self.filename, parallel=True, metadata={"Index": self.index})
        self.index = self.index+1
        return self.filename


       
add_device(ExternalImage("ei1"), True)

#Must be initialized before each scan to reset image file index and prefix
ei1.initialize()
r = lscan(ao1, ei1,  0, 10, 4, 0.1)    