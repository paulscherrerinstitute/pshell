# Session Utilities

The scripting API for handling sessions is not defined as built-in functions, but
in  sessions.py. The module is imported as:

```
from sessions import *

```

* session_start(name, metadata=None)
    - Starts new session. If a session os open, completes it first.
* session_complete()
    - Stops current session, if open.         
* session_pause()
    - Pauses current session, if open.         
* session_resume()
    - Resumes current session, if paused.         
* session_cancel()
    - Cancels current session, if started and empty (no generated data).      
* session_id()
    - Returns current session id (0 if no session is started).
* session_name()
    - Returns current session name ("unknown" if no session is started) 
* session_started()
    - Returns true if a session is started.
* session_paused()
    - Returns true if current session is paused.
* session_add_file(path)
    - Adds additional file to session, if opened. 
* session_ids()
    - Returns list of completed sessions.
* session_get_name(id=None)
    - Return the name of a session.
* session_get_state(id=None)
    - Returns the session state 
* session_get_start(id=None)
    - Returns the start timestamp of a session.
* session_get_stop(id)
    - Returns the stop timestamp of a completed session.
* session_get_root(id=None)
    - Returns the root data path of a session.
* session_get_info(id=None)
    - Returns the session information.
* session_get_metadata(id=None)
    - Returns a session info metadata.
* session_set_metadata(key, value,id=None)
    - Set session  metadata entry.
* session_get_metadata_keys()
    - Return the default metadata definition for samples.
* session_get_metadata_type(key)
    - Return the metadata type for a given key: String, Integer, Double, Boolean, List or Map.
* session_get_metadata_default(key)
    - Return the metadata default value for a given key.
* session_get_runs(id=None, relative=True)
    - Return the runs of a session.
* session_set_run_enabled(enabled, id=None, index=-1)
    - Enable or disable a run.
* session_get_additional_files(id=None, relative=True)
    - Return additional files of a session.
* session_get_file_list(id=None, relative=True)
    - Return complete list of data files of a session.
* session_create_zip(file_name, id=None, preserve_folder_structure=True)
    - Create ZIP file with session contents
* ingest_scicat(id, matadata={}, parameters=None)
    - Ingest a completed session to SciCat