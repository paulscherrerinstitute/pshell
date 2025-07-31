#!/usr/bin/env python
## @package ca: EPICS-CA interface module for Python.
"""CA modlue : EPICS-CA interface module for Python.
This module provide a  version of EPICS-CA and Python interface.
It users C module _ca. _ca module basically maps C-API in EPICS ca library into python. Interface between ca.py and _ca module is  subject for change. You should not depend on it. API in ca.py will be preserved in future releases as much as possible.
Author: Noboru Yamamoto, KEK, JAPAN. -2007.
$Revision: 1.4 $
"""

__version__ = "$Revision: 1.4 $"
# $Source: /cvs/G/EPICS/extensions/src/PythonCA/src/ca.py,v $

import time,gc,sys,atexit
if sys.hexversion >= 0x03000000:
  import _thread as thread
else:
  import thread

# autGIL is not compatible with Tkinter and wx. So code was removed

# force thread module to call PyEval_InitThread in it.
__foo_lock=thread.allocate_lock()
def __foo():
    """
    test function foo

    This function is used to ensure thread module is initialized before
    loading _ca module.
    """
    global __foo_lock
    __foo_lock.release()
    thread.exit_thread()

# See Python/Include/ceval.h
__foo_lock.acquire()
thread.start_new_thread(__foo,()) # __foo release lock
__foo_lock.acquire() # make sure threading is activated

import _ca
# version from _ca314.cpp
version=_ca.version
revision=_ca.release

# some constants for EPICS channel Access library
from cadefs import *
from caError import *

# for FNAL version you need to provide _ca_fnal.py and import every thin from them
from _ca_fnal import *
