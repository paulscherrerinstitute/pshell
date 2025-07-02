###################################################################################################
#  Data utilities
###################################################################################################
from startup import *
from ijutils import load_array, save_image

    
def save_as_tiff(data, filename, parallel=True, metadata={}):
    """
    Save data as tiff file

    Args:
        data(array, Data or ImageProcessor): Data to be saved
        filename(str): File Name
        parallel(bool, optiona): if True image is saved in separated thread.
        metadata(dict, optional): written to tiff info property.
        
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


def save_as_hdf5(data, filename=None, dm=None, path="/data", parallel=True, metadata={}):
    """
    Save data as hdf5 file

    Args:
        data(array, Data): Data to be saved
        filename(str, optional): File to save to  (if equals None then dm must be set )
        dm(str, optional): DataManager to save to
        parallel(bool, optional): if True image is saved in separated thread.
        metadata(dict, optional): written to tiff info property.        
    """
    def _save_as_hdf5(data, filename, dm, path, metadata={}):
        try:
            if type(data) == Data:
                data = data.matrix
            if filename is not None:
                if not os.path.exists(os.path.dirname(filename)):
                    os.makedirs(os.path.dirname(filename))               
                dm = DataManager(File(filename), "h5")
            try:
                dm.setDataset(path, data)
                for key,val in metadata.items():
                   dm.setAttribute(path, key, val)
            finally:
                if filename is not None:
                    dm.close()
        except:
            print sys.exc_info()    
    if parallel:
        return fork((_save_as_hdf5,(data, filename, dm, path, metadata)),)
    else:
        _save_as_hdf5(data, filename, dm, path, metadata)