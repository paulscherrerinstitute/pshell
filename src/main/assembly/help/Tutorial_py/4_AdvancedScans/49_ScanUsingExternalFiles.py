################################################################################################### 
# Example on scans creating and referencing external data
# - ei1 writes each sampled as an external hdf5 files and saves the paths to targets in a dataset
# - ei2 after_read all images in an external hdf5 file and saves the paths to targets in a dataset
# - after_read create external links to the image files
################################################################################################### 

from data_utils import save_as_hdf5 
        
class ExternalImageHdf5Single(ReadonlyRegisterBase, RegisterString):
    def doInitialize(self):
        self.folder = os.path.splitext(get_exec_pars().path)[0]
        self.index=0

    def doRead(self):
        self.filename = self.folder + "/images/" + ("%04d" % (self.index,)) + ".h5"
        save_as_hdf5(src1.data, filename= self.filename, path="/image", parallel=True, metadata={"Index": self.index})
        self.index = self.index+1
        return IO.getRelativePath(self.filename, expand_path("{data}"))

class ExternalImageHdf5Mult(ReadonlyRegisterBase, RegisterString):
    def doInitialize(self):
        self.folder = os.path.splitext(get_exec_pars().path)[0]
        self.index=0
        self.filename = self.folder + "/images.h5"
        if not os.path.exists(os.path.dirname(self.filename)):
            os.makedirs(os.path.dirname(self.filename))    
        self.dm = DataManager(self.filename, "h5")
        
    def doRead(self):
        self.path = "%04d" % (self.index,)
        save_as_hdf5(src1.data,dm=self.dm, path=self.path,  parallel=True, metadata={"Index": self.index})
        self.index = self.index+1
        return DataManager.getFullPath(IO.getRelativePath(self.filename, expand_path("{data}")), self.path)


add_device(ExternalImageHdf5Single("ei1"), True)
add_device(ExternalImageHdf5Mult("ei2"), True)

#Must be initialized before each scan to reset image file index and prefix
ei1.initialize()
ei2.initialize()

#Callback to create external links
def after_read(record, scan):
    try:
        create_link("/images/%04d" % (record.index,), ei2.path,  ei2.filename)
    except:
        pass #Not supported in txt
        
        
r = lscan(ao1, (ei1, ei2),  0, 10, 4, 0.1, after_read=after_read)   