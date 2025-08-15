################################################################################################### 
# Example on scans saving data as external tiff files
################################################################################################### 

from data_utils import save_as_tiff
                
class ExternalImage(ReadonlyRegisterBase, RegisterString):
    def doInitialize(self):
        self.folder = os.path.splitext(get_exec_pars().path)[0]
        self.folder = IO.getRelativePath(self.folder, expand_path("{data}"))
        self.index=0
        
    def doRead(self):
        self.filename = self.folder + "/images/" + ("%04d" % (self.index,)) + ".tiff"
        save_as_tiff(src1.data, expand_path("{data}") + "/" + self.filename, parallel=True, metadata={"Index": self.index})
        self.index = self.index+1
        return self.filename


       
add_device(ExternalImage("ei1"), True)

#Must be initialized before each scan to reset image file index and prefix
ei1.initialize()
r = lscan(ao1, ei1,  0, 10, 4, 0.1)    