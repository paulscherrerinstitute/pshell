from startup import get_context, set_exec_pars
import ch.psi.utils.SciCat as SciCat
import java.lang.Boolean

def _sm():
    return get_context().sessionManager


def session_start(name, metadata=None):
    """ Starts new session. If a session os open, completes it first.

    Args:
         name(str): Session name.
         metadata(dict): Map of initial metadata parameters
                         If None(Default) use the default metadata definition. 

    Returns:
        session id (int)     
    """
    set_exec_pars(open=False)
    return _sm().start(name, metadata)

def session_complete():
    """ Completes current session, if started.         
    """
    set_exec_pars(open=False)
    return _sm().stop()

def session_pause():
    """ Pauses current session, if started.         
    """
    return _sm().pause()    

def session_resume():
    """ Resumes current session, if paused.         
    """
    return _sm().resume()    

def session_cancel():
    """ Cancels current session, if started and empty (no generated data).      
    """
    return _sm().cancel()        

def session_restart(id):
    """ Reopens a completed if not yet archived and if belongs to the same user.

    Args:
         id(int): Session id.
    """
    return _sm().restart(id)  

def session_move(origin, files, dest):
    """ Moves a list of run files (relative to root) to another session.
        Sessions must not be archived and belong to the same user.

    Args:
        origin(int): Origin session id.
        files(list): file names
        dest(int): Destination session id.
    """
    return _sm().move(origin, files, dest)  

def session_detach(name, id, files):
    """ Detaches a list of run files (relative to root) to a new session.
        Session must not be archived and  belong to the same user.

    Args:
        name(str): Name of new session.
        id(int): Session id.
        files(list): file names        

    Returns:
        New session id (int)  
    """
    return _sm().detach(name, id, files)  


def session_create(name, files, metadata=None, root=None):
    """ Create a session from existing data files.

    Args:
        name(str): Name of new session.
        files(list): file names relative to root       
        metadata(dict): Map of initial metadata parameters
                         If None(Default) use the default metadata definition. 
        root(str): data root path. If None(Default) uses default data path.

    Returns:
        New session id (int)  
    """
    return _sm().create(name, files, metadata, root)  

def session_id():
    """ Returns current session id (0 if no session is started).

    Returns:
        session id (int)       
    """
    return _sm().getCurrentSession()


def session_name():
    """ Returns current session name ("unknown" if no session is started) 

    Returns:
        session name(str)       
    """
    return _sm().getCurrentName()

def session_started():
    """ Returns true if a session is started.

    Returns:
        bool       
    """
    return _sm().isStarted()
            
def session_paused():
    """ Returns true if current session is paused.

    Returns:
        bool    
    """
    return _sm().isPaused()

        
def session_add_file(path):
    """ Adds additional file to session, if started. 

    Args:
         path(str): Relative to data path or absolute.
    """
    return _sm().addAdditionalFile(path)


def session_ids():
    """ Returns list of completed sessions.

    Returns:
        list of int        
    """
    return _sm().getIDs()

def session_get_name(id=None):
    """ Return the name of a session.

    Args:
         id(int): Session id. Default (None) is the current session.

    Returns:
        session name (str)         
    """
    return _sm().getName() if id is None else _sm().getName(id)


def session_get_state(id=None):
    """ Returns the session state 

    Args:
         id(int): Session id. Default (None) is the current session.

    Returns:
        session state (str)    
    """
    return _sm().getState() if id is None else _sm().getState(id)  

def session_get_start(id=None):
    """ Returns the start timestamp of a session.

    Args:
         id(int): Session id. Default (None) is the current session.

    Returns:
        long  
    """
    return _sm().getStart() if id is None else _sm().getStart(id)

def session_get_stop(id):
    """ Returns the stop timestamp of a completed session.

    Args:
         id(int): Session id. 
    Returns:
        Timestamp (long)         
    """
    return _sm().getStop(id)    

def session_get_root(id=None):
    """ Returns the root data path of a session.

    Args:
         id(int): Session id. Default (None) is the current session.

    Returns:
        str
    """
    return _sm().getRoot() if id is None else _sm().getRoot(id)    


def session_get_info(id=None):
    """ Returns the session information.

    Args:
         id(int): Session id. Default (None) is the current session.

    Returns:
        session info (dict)    
    """
    return _sm().getInfo() if id is None else _sm().getInfo(id)  


def session_get_metadata(id=None):
    """ Returns a session info metadata.

    Args:
         id(int): Session id. Default (None) is the current session.

    Returns:
        session metadata (dict)    
    """
    return _sm().getMetadata() if id is None else _sm().getMetadata(id)  


def session_set_metadata(key, value,id=None):
    """ Set session  metadata entry.

    Args:
         key(str): Metadata key
         value(obj): Metadata value
         id(int): Session id. Default (None) is the current session.
    """
    return _sm().setMetadata(key, value) if id is None else _sm().setMetadata(id,key, value)  

    
def session_get_metadata_keys():
    """ Return the default metadata definition for samples.
    
    Returns:
        list of map entries       
    """
    return [str(e.key) for e in _sm().getMetadataDefinition()]


def session_get_metadata_type(key):
    """ Return the metadata type for a given key:
        String, Integer, Double, Boolean, List or Map.
    Args:
         key(str): Metadata key.

    Returns:
        str         
    """
    return str(_sm().getMetadataType(key))
       
def session_get_metadata_default(key):
    """ Return the metadata default value for a given key.
        
    Args:
         key(str): Metadata key.

    Returns:
         Object    
    """
    return _sm().getMetadataDefault(key)

def session_get_runs(id=None, relative=True):
    """ Return the runs of a session.

    Args:
         id(int): Session id. Default (None) is the current session.
         relative(bool): if True use relative file names (for files under the data root path)

    Returns:
        List of dicts     
    """
    return _sm().getRuns(java.lang.Boolean(relative)) if id is None else _sm().getRuns(id, relative)


def session_set_run_enabled(enabled, id=None, index=-1):
    """ Enable or disable a run.
        
    Args:
        enabled(bool): true for enabling, false for disabling
        id(int): Session id. Default (None) is the current session.
        index: Index of the run. Default (-1) for the last run.

    Returns:
         Object    
    """
    return _sm().setRunEnabled(index, enabled) if id is None else  _sm().setRunEnabled(id, index, enabled)

def session_get_additional_files(id=None, relative=True):
    """ Return additional files of a session.

    Args:
         id(int): Session id. Default (None) is the current session.
         relative(bool): if True use relative file names (for files under the data root path)

    Returns:
        List of str     
    """
    return _sm().getAdditionalFiles(java.lang.Boolean(relative)) if id is None else _sm().getAdditionalFiles(id, relative)

def session_get_file_list(id=None, relative=True):
    """ Return complete list of data files of a session.

    Args:
         id(int): Session id. Default (None) is the current session.
         relative(bool): if True use relative file names (for files under the data root path)

    Returns:
        List of str     
    """
    return _sm().getFileList(java.lang.Boolean(relative)) if id is None else _sm().getFileList(id, relative)

def session_create_zip(file_name, id=None, preserve_folder_structure=True):
    """ Create ZIP file with session contents

    Args:
        file_name(str): name of the zip file
        id(int): Session id. Default (None) is the current session.
        preserve_folder_structure: if False all data files are added to the root of the file.
                                   if True the folder structure under data root is preserved.
    """
    return _sm().createZipFile(file_name, preserve_folder_structure) if id is None else _sm().createZipFile(id, file_name, preserve_folder_structure)


def session_ingest_scicat(id, matadata={}):
    """ Ingest a completed session to SciCat
    
    Args:
        id(int): Session id. 
        matadata(dict): session metadata 
        
    Returns:
        Tuple (Dataset Name, Dataset ID) in case of success. Otherwise throws an exception.               
    """
    sciCat= SciCat()
    result  = sciCat.ingest(id, matadata)
    print result.output
    if not result.success:
        raise  Exception ("Error ingesting session " + str(id))
    return result.datasetName, result.datasetId
    




