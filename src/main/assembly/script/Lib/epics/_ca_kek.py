#!/usr/bin/env python
## @package ca: EPICS-CA interface module for Python.
"""CA modlue : EPICS-CA interface module for Python.
This module provide a  version of EPICS-CA and Python interface.
It users C module _ca. _ca module basically maps C-API in EPICS ca library into python. Interface between ca.py and _ca module is  subject for change. You should not depend on it. API in ca.py will be preserved in future releases as much as possible.
Author: Noboru Yamamoto, KEK, JAPAN. -2007.
$Revision: 1.1 $
"""
from __future__ import print_function
__version__ = "$Revision: 1.1 $"
# $Source: /cvs/G/EPICS/extensions/src/PythonCA/src/_ca_kek.py,v $
#
try:
    import signal
except:
    print("signal module is not avaialble")
    
import time,thread,gc,sys,atexit
from exceptions import ValueError

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

#export pend_xxx routines for global operation
pendio =_ca.pendio
pend_io=_ca.pendio
pend_event=_ca.pend_event
poll=_ca.poll
poll_event=_ca.poll
flush_io=_ca.flush
flush=_ca.flush
test_io=_ca.test_io # test_io retunrs 42 for IODONE , 43 for IOINPROGRESS
add_fd_registration=_ca.add_fd_registration

#Error Object
error=_ca.error
shutdown=_ca.__ca_task_exit

#private dictionary for Get/Put functions

__ca_dict={}
__ca_dict_lock=thread.allocate_lock()
_channel__debug=False

class channel:
    """
    a channel object for EPICS Channel Access.

    It does not have direct connection
    to channel object in C-library for EPICS Channel Access. 
    for creation just supply channel name to connect
    """
    dbr_types=(
        DBR_NATIVE, # default type
        DBR_STRING, DBR_CHAR, DBR_FLOAT,
        DBR_SHORT, #/* same as DBR_INT */
        DBR_ENUM, DBR_LONG, DBR_DOUBLE,
        DBR_TIME_STRING, DBR_TIME_CHAR, DBR_TIME_FLOAT,
        DBR_TIME_SHORT, #:/* same as DBR_TIME_INT */
        DBR_TIME_ENUM, DBR_TIME_LONG, DBR_TIME_DOUBLE,
        DBR_CTRL_CHAR, DBR_CTRL_LONG,
        DBR_CTRL_ENUM, DBR_CTRL_DOUBLE
        )

    def __init__(self, name, cb=None,noflush=None):
        if (not cb) : cb=self.update_info
        if name == "":
            raise ValueError(name)
        self.name=name
        self.field_type = None
        self.element_count = None
        self.puser = None
        self.conn_state = -1
        self.hostname = None
        self.raccess = None
        self.waccess = None
        self.sevr=None
        self.ts=None
        self.status=None
        self.evid=[]
        self.autoEvid=None
        self.__callbacks={}
        self.cbstate=None
        self.updated=False
        self.val=None
        self.chid=_ca.search(name,cb)
        if not noflush:
            self.flush()
        
    def clear(self):
        if self.chid:
            self.clear_monitor()
            self.flush()
            _ca.clear(self.chid)
            self.flush()
        self.chid=None
 
    def __del__(self):
        self.clear()

    def wait_conn(self, wait=20, dt=0.05):
        n=0
        self.pend_event(dt)
        self.update_info()
        self.poll()
        while (not self.isConnected()):
            self.update_info()
            self.pend_event(dt)
            n=n+1
            if (n > wait ) :
                raise  ECA_BADCHID("%s %d"%(self.name,n))
                return -1
        
    def get(self,cb=None,Type=DBR_NATIVE, count=0, type=DBR_NATIVE, type_=DBR_NATIVE):
        try:
            if not self.isConnected():
                raise ECA_BADCHID(self.name)
        except:
            raise ECA_BADCHID(self.name)
        if (Type == DBR_NATIVE):
            if not(type == DBR_NATIVE):
                Type=type
            elif not(type_ == DBR_NATIVE):
                Type=type_
        rType=max(Type,type,type_)
        if rType not in self.dbr_types:
            raise TypeError(rType)
        if not cb: cb=self.update_val

        self.cbstate=None
        self.updated=False
        try:
            _ca.get(self.chid, cb, Type, count)
        finally:
            pass
        
    def put(self,*val,**kw):
        """
        channel.put(valu) will put scalar value to channel. You may need to call channel.flush()
        
        """
        if( val == ()):
            print("No value(s) to put")
        else:
            if kw.has_key('cb'):
                cb=kw['cb']
            else:
                cb=None
            #self.__lock.acquire()
            try:
                _ca.put(self.chid, val, self.val, cb, DBR_NATIVE)
            finally:
                #self.__lock.release()
                pass

    def put_and_notify(self,*val,**kw):
        if kw.has_key('cb'):
            cb=kw['cb']
        else:
            cb=None # ca_put_array_callback does not return value.
        if( val == ()):
            print("No value(s) to put")
        else:
            #self.__lock.acquire()
            try:
                _ca.put(self.chid,val,self.val,cb, DBR_NATIVE)
            finally:
                #self.__lock.release()
                pass
            
    def monitor(self,callback=None,count=0,evmask=(DBE_VALUE|DBE_ALARM)):
        if(not callback):
            raise PyCa_NoCallback
        if (self.conn_state != 2):
            #print self.name,self.get_info()
            raise ECA_BADCHID(self.name)

        self.update_info()
        if (self.field_type == DBR_NATIVE):
            #print self.name,self.get_info()
            raise ECA_BADTYPE(self.name)
        self.evid.append(_ca.monitor(self.chid,callback,count,evmask))
        self.__callbacks[self.evid[-1]]=callback
        return self.evid[-1]
    
    def __clear_event(self,evid):
        if(_channel__debug): print("clearing evid:",evid)
        _ca.clear_monitor(evid)
        del self.__callbacks[evid]
         
    def clear_monitor(self,evid=None):
        if(evid):
            if ( evid in self.evid):
                self.__clear_event(evid)
                i=self.evid.index(evid)
                del self.evid[i]
                i=self.evid.index(evid)
                del self.evid[i]
        else:
            for evid in self.evid:
                self.__clear_event(evid) 
            self.evid=[]

    def autoUpdate(self):
        if self.autoEvid == None:
            self.monitor(self.update_val)
            self.autoEvid=self.evid[-1]
        self.flush()
        
    def clearAutoUpdate(self):
        if self.autoEvid is not None:
            self.clear_monitor(self.autoEvid)
            self.autoEvid=None
        self.flush()
        
    def pendio(self,tmo=0.001):
        v=_ca.pendio(float(tmo))
        return v

    def pend_io(self,tmo=0.001):
        v=_ca.pendio(float(tmo))
        return v

    def pend_event(self,tmo=0.001):
        v=_ca.pend_event(float(tmo))
        return v

    def poll(self):
        _ca.poll()
            
    def flush(self,wait=0.001):
        v=_ca.flush(wait)
        return v

    def update_val(self,valstat=None):
        if valstat ==None:
            raise caError("No value")
        #self.__lock.acquire()
        try:
            self.val=valstat[0]
            self.sevr=valstat[1]
            self.status=valstat[2]
            self.cbstate=1
            try:
                self.ts=valstat[3]
            except:
                pass
            try:
                self.ctrl=valstat[4]
            except:
                pass
        finally:
            #self.__lock.release()
            self.updated=True
            pass

    def clear_cbstate(self):
        #self.__lock.acquire()
        self.cbstate=None
        #self.__lock.release()
        
    def state(self):
        self.get_info()
        return (self.conn_state - ch_state.cs_conn)

    def isNeverConnected(self):
        self.get_info()
        return (self.conn_state == ch_state.cs_never_conn)

    def isConnected(self):
        self.get_info()
        return (self.conn_state == ch_state.cs_conn)

    def isPreviouslyConnected(self):
        self.get_info()
        return (self.conn_state == ch_state.cs_prev_conn)

    def isDisonnected(self):
        self.get_info()
        return (self.conn_state == ch_state.cs_prev_conn)
    
    def isClosed(self):
        self.get_info()
        return (self.conn_state == ch_state.cs_closed)
    
    def get_info(self):
        """
        update channel status information. return channel staus as a tuple.
        """
        #self.__lock.acquire()
        try:
            info=(self.field_type, self.element_count, self.puser,
                  self.conn_state, self.hostname, self.raccess,
                  self.waccess) = _ca.ch_info(self.chid)
        finally:
            #self.__lock.release()
            pass
        return info

    def update_info(self):
        """
        Just update channel status information. No return value. 
        """
        self.get_info()

    def fileno(self):
        """returns socket number used to connect.Scoket id is shared by
        channels which are connected to the same IOC.
        It became obsolete in EPICS 3.14 version of Python-CA.
        You need to use fd_register function. But you may not need it anyway in multi-thread environment.
        """
        return _ca.fileno(self.chid)

# convenient functions
# you need to call Clear() function before stopping Python, otherwise it cause coredump. 2009/2/11 NY
def __Ch(name,tmo=0.01):
    if (type(name) == type("")):
        if (__ca_dict.has_key(name)):
            ch=__ca_dict[name]
        else:
            try:
                ch=channel(name)
                ch.wait_conn()
            except:
                raise ECA_BADCHID(name)
            tmo=20*tmo
            __ca_dict_lock.acquire()
            try:
                __ca_dict[name]=ch
            finally:
                __ca_dict_lock.release()
        if( ch.state() != 0):
            ch.wait_conn(10)
        return ch
    else:
        raise ECA_BADTYPE(name)

def Info(name = "",tmo=0.01):
    """
    returns a tuple as channel information.
    tuple format=(field_type, element_count, puser argument,
                  connection_status, hostname:port,
                  read access mode, write access mode)
    """
    ch=__Ch(name,tmo=tmo)
    return ch.get_info()

def ClearAll():
    for name in __ca_dict.keys():
        Clear(name)

# __ca_dict should be cleared before Stopping Python
atexit.register(ClearAll)

def Clear(name= ""):
    if (type(name) == type("")):
        __ca_dict_lock.acquire()
        try:
            if (__ca_dict.has_key(name)):
                ch=__ca_dict[name]
                del __ca_dict[name]
                ch.clear()
                del ch
            else:
                __ca_dict_lock.release()
                raise ECA_BADTYPE(name)
        finally:
            __ca_dict_lock.release()
    else:
        raise ECA_BADTYPE(name)

def Get(name="",count=0,Type=DBR_NATIVE,tmo=0.01,maxtmo=3):
    """
    Get value from a channel "name".
    """
    ch=__Ch(name,tmo)
    def CB(vals,ch=ch):
        ch.update_val(vals)
    ch.get(cb=CB,Type=Type,count=count)
    ch.flush()
    while not ch.updated:
        time.sleep(tmo)
        maxtmo -=tmo
        if maxtmo <=0:
            raise caError("No get response")
    return ch.val

def Put_and_Notify(name,val=None,cb=None):
    """
    Convenient function:Put_and_Notify 

    calls put_and_notify with callback. 
    If callback is None, then just put data to a channel.
    """
    ch=__Ch(name,tmo=0.1)
    ch.put_and_notify(val,cb=cb)
    ch.flush()
    return ch.val

# define synonym
Put=Put_and_Notify

def Put_and_Notify_Array(name,val,cb=None):
    """
    put array test version : not tested with string arrays yet
    2007.8.30 T. Matsumoto
    """
    ch=__Ch(name,tmo=0.1)
    apply(ch.put_and_notify,val,dict(cb=cb))
    ch.flush()
    return ch.val

# define synonym
Put_Array=Put_and_Notify_Array
                
def Monitor(name,cb=None,evmask=(DBE_VALUE|DBE_ALARM)):
    ch=__Ch(name,tmo=0.1)
    if not cb:
        def myCB(val,ch=ch):
            print(ch.name,":",val[0],val[1],val[2],TS2Ascii(val[3]))
    else:
        def myCB(val, ch=ch, cb=cb):
            cb(ch,val)
    ch.clear_monitor()
    evid=ch.monitor(myCB,evmask=evmask)
    ch.flush()
    return evid

def ClearMonitor(name,evid=None):
    ch=__Ch(name,tmo=0.1)
    try:
        ch.clear_monitor(evid)
        return
    except:
        raise ECA_BADCHID(name)
#
def isIODone():
    if _ca.test_io()== 42:
        return 1
    else:
        return 0
#
# syncronus group class
# Author: N.Yamamoto 
# Date: May 27.1999 (first version)
#

class SyncGroup:
    def __init__(self):
        self.gid=_ca.sg_create()
        self.chs={}

    def add(self, chs):
        try:
            for ch in chs:
                if(not self.chs.has_key(ch)):
                    self.chs[ch]=0
        except:
            if(not self.chs.has_key(chs)):
                self.chs[chs]=0

    def test(self):
        return _ca.sg_test(self.gid)

    def reset(self):
        return _ca.sg_reset(self.gid)

    def wait(self,tmo=1.0):
        return _ca.sg_block(self.gid,float(tmo))

    def put(self,ch,*value,**kw):
        if kw.has_key("Type"):
            Type=kw["Type"]
        else:
            Type=DBR_NATIVE
        if self.chs.has_key(ch):
            self.chs[ch]=_ca.sg_put(self.gid, ch.chid,
                        self.chs[ch], value , Type)

    def get(self,ch):
        if self.chs.has_key(ch):
            self.chs[ch]=_ca.sg_get(self.gid,
                   ch.chid, self.chs[ch])
        else:
            pass

    def convert(self,ch):
        if self.chs.has_key(ch):
            val=_ca.ca_convert(ch.chid, self.chs[ch])
            ch.update_val(val[0])
            
    def GetAll(self,tmo=1.0):
        for ch in self.chs.keys():
            self.chs[ch]=_ca.sg_get(self.gid,
                   ch.chid, self.chs[ch])
        st=_ca.sg_block(self.gid,tmo)
        if st == 0:
            for ch in self.chs.keys():
                val=_ca.ca_convert(ch.chid,self.chs[ch])
                ch.update_val(val[0])
        else:
            raise Exception("CA_SG time out")

# TimeStamp utilities
# time.gmtime(631152000.0)=(1990, 1, 1, 0, 0, 0, 0, 1, 0)
#
__EPICS_TS_EPOCH=631152000.0

def TS2Ascii(ts):
    import math
    tstr=time.ctime(ts+__EPICS_TS_EPOCH)
    nsstr=".%03d"%(math.modf(ts + __EPICS_TS_EPOCH)[0]*1000)
    return tstr[:-5]+nsstr+tstr[-5:]

def TS2time(ts):
    return time.localtime(ts+__EPICS_TS_EPOCH)

def TS2UTC(ts):
    return (ts+__EPICS_TS_EPOCH)

