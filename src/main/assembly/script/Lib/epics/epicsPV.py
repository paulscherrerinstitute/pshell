"""
This module defines the epicsPV class, which adds additional features to
Geoff Savage's CaChannel class.

Author:         Mark Rivers
Created:        Sept. 16, 2002.
Modifications:
"""
import CaChannel

class epicsPV(CaChannel.CaChannel):
   """
   This class subclasses Geoff Savage's CaChannel class to add the following
   features:
   
      - If a PV name is given then the class constructor will do a searchw()
        by default.
        
      - setMonitor() sets a generic callback routine for value change events.
        Subsequent getw(), getValue() or array_get() calls will return the
        value from the most recent callback, and hence do not result in any
        network activity or latency.  This can greatly improve performance.
        
      - checkMonitor() returns a flag to indicate if a callback has occured
        since the last call to checkMonitor(), getw(), getValue() or
        array_get().  It can be used to increase efficiency in polling
        applications.
        
      - getControl() reads the "control" and other information from an
        EPICS PV without having to use callbacks.
        In addition to the PV value, this will return the graphic, control and
        alarm limits, etc.

      - putWait() calls array_put_callback() and waits for the callback to
        occur before it returns.  This allows programs to use array_put_callback()
        synchronously and without user-written callbacks.
        
   Created:  Mark Rivers, Sept. 16, 2002.
   Modifications:
   """

   def __init__(self, pvName=None, wait=1):
      """
      Keywords:
         pvName:
            An optional name of an EPICS Process Variable.
         
         wait:
            If wait==1 and pvName != None then this constructor will do a
            CaChannel.searchw() on the PV.  If wait==0 and pvName != None then
            this constructor will do a CaChannel.search() on the PV, and the user
            must subsequently do a pend_io() on this or another epicsPV or CaChannel
            object.
            
      Procedure:
         Invokes CaChannel.__init__() and then searchw() or search() as explained
         above
      """
      # Invoke the base class initialization
      self.callBack = callBack()
      CaChannel.CaChannel.__init__(self)
      if (pvName != None):
         if (wait):
            self.searchw(pvName)
         else:
            self.search(pvName)

   def setMonitor(self):
      """
      Sets a generic callback routine for value change events.
      Subsequent getw(), getValue() or array_get() calls will return the
      value from the most recent callback, do not result in any network
      latency.  This can greatly improve efficiency.
      """
      self.add_masked_array_event(None, None, CaChannel.ca.DBE_VALUE, 
                                  getCallback, self.callBack)
      self.callBack.monitorState = 1

   def clearMonitor(self):
      """
      Cancels the effect of a previous call to setMonitor().
      Calls CaChannel.clear_event().
      Subsequent getw(), getValue() or array_get() calls will no longer
      return the value from the most recent callback, but will actually result
      in channel access calls. 
      """
      self.clear_event()
      self.callBack.monitorState = 0

   def checkMonitor(self):
      """
      Returns 1 to indicate if a value callback has occured
      since the last call to checkMonitor(), getw(), getValue() or
      array_get(), indicating that a new value is available.  Returns 0 if
      no such callback has occurred.
      It can be used to increase efficiency in polling applications.
      """
      # This should be self.poll(), but that is generating errors
      self.pend_event(.0001)
      m = self.callBack.newMonitor
      self.callBack.newMonitor = 0
      return m

   def getControl(self, req_type=None, count=None, wait=1, poll=.01):
      """
      Provides a method to read the "control" and other information from an
      EPICS PV without having to use callbacks.
      It calls CaChannel.array_get_callback() with a database request type of
      CaChannel.ca.dbf_type_to_DBR_CTRL(req_type).
      In addition to the PV value, this will return the graphic, control and
      alarm limits, etc.
      
      Example:
      >>> pv = epicsPV('13IDC:m1')
      >>> pv.getControl()
      >>> for field in dir(pv.callBack):
      >>>    print field, ':', getattr(pv.callBack, field)
          chid : _bfffec34_chid_p
          count : 1
          monitorState : 0
          newMonitor : 1
          putComplete : 0
          pv_loalarmlim : 0.0
          pv_loctrllim : -22.0
          pv_lodislim : -22.0
          pv_lowarnlim : 0.0
          pv_precision : 4
          pv_riscpad0 : 256
          pv_severity : 0
          pv_status : 0
          pv_units : mm
          pv_upalarmlim : 0.0
          pv_upctrllim : 28.0
          pv_updislim : 28.0
          pv_upwarnlim : 0.0
          pv_value : -15.0
          status : 1
          type : 34

      Note the fields such as pv_plocrtllim, the lower control limit, and
      pv_precision, the display precision.

      Keywords:
         wait:
            If this keyword is 1 (the default) then this routine waits for
            the callback before returning.  If this keyword is 0 then it is
            the user's responsibility to wait or check for the callback
            by calling checkMonitor().
            
         poll:
            The timeout for pend_event() calls, waiting for the callback
            to occur.  Shorter times reduce the latency at the price of CPU
            cycles.
      """
      if (req_type == None): req_type = self.field_type()
      if (wait != 0): self.callBack.newMonitor = 0
      self.array_get_callback(CaChannel.ca.dbf_type_to_DBR_CTRL(req_type),
                              count, getCallback, self.callBack)
      if (wait != 0):
         while(self.callBack.newMonitor == 0):
            self.pend_event(poll)            

   def array_get(self, req_type=None, count=None):
      """
      If setMonitor() has not been called then this function simply calls
      CaChannel.array_get().  If setMonitor has been called then it calls
      CaChannel.pend_event() with a very short timeout, and then returns the
      PV value from the last callback.
      """
      if (self.callBack.monitorState != 0):
         # This should be self.poll(), but that is generating errors
         self.pend_event(.0001)
      if (self.callBack.monitorState == 2):
         self.callBack.newMonitor = 0
         return self.callBack.pv_value
      else:
         return CaChannel.CaChannel.array_get(self, req_type, count)

   def getw(self, req_type=None, count=None):
      """
      If setMonitor() has not been called then this function simply calls
      CaChannel.getw().  If setMonitor has been called then it calls
      CaChannel.pend_event() with a very short timeout, and then returns the
      PV value from the last callback.
      """
      if (self.callBack.monitorState != 0):
         # This should be self.poll(), but that is generating errors
         self.pend_event(.0001)
      if (self.callBack.monitorState == 2):
         self.callBack.newMonitor = 0
         if (count == None):
            return self.callBack.pv_value
         else:
            return self.callBack.pv_value[0:count]
      else:
         return CaChannel.CaChannel.getw(self, req_type, count)

   def getValue(self):
      """
      If setMonitor() has not been called then this function simply calls
      CaChannel.getValue().  If setMonitor has been called then it calls
      CaChannel.pend_event() with a very short timeout, and then returns the
      PV value from the last callback.
      """
      if (self.callBack.monitorState != 0):
         # This should be self.poll(), but that is generating errors
         self.pend_event(.0001)
      if (self.callBack.monitorState == 2):
         self.callBack.newMonitor = 0
         return self.callBack.pv_value
      else:         
         return CaChannel.CaChannel.getValue(self)

   def putWait(self, value, req_type=None, count=None, poll=.01):
      """
      Calls CaChannel.array_put_callback() and waits for the callback to
      occur before it returns.  This allows programs to use array_put_callback()
      without having to handle asynchronous callbacks.
      
      Keywords:
         req_type:
            See CaChannel.array_put_callback()
            
         count:
            See CaChannel.array_put_callback()
            
         poll:
            The timeout for pend_event() calls, waiting for the callback
            to occur.  Shorter times reduce the latency at the price of CPU
            cycles.
      """
      self.callBack.putComplete=0
      self.array_put_callback(value, req_type, count, putCallBack, self.callBack)
      while(self.callBack.putComplete == 0):
         self.pend_event(poll)         

class callBack:
   """
   This class is used by the epicsPV class to handle callbacks.  It is required
   to avoid circular references to the epicsPV object itself when dealing with
   callbacks, in order to allow the CaChannel destructor to be called.
   Users will only be interested in the fields that are copied to this class in
   the callback resulting from a call to epicsPV.getControl().
   """
   def __init__(self):
      self.newMonitor = 0
      self.putComplete = 0
      self.monitorState = 0
      # monitorState:  
      #   0=not monitored 
      #   1=monitor requested, but no callback yet
      #   2=monitor requested, callback has arrived


def putCallBack(epicsArgs, userArgs):
   """
   This is the generic callback function used by the epicsPV.putWait() method.
   It simply sets the callBack.putComplete flag to 1.
   """
   userArgs[0].putComplete=1

def getCallback(epicsArgs, userArgs):
   """
   This is the generic callback function enabled by the epicsPV.setMonitor() method.
   It sets the callBack.monitorState flag to 2, indicating that a monitor has
   been received.  It copies all of the attributes in the epicsArgs dictionary
   to the callBack attribute of the epicsPV object.
   """
   for key in epicsArgs.keys():
      setattr(userArgs[0], key, epicsArgs[key])
   if (userArgs[0].monitorState == 1): userArgs[0].monitorState = 2
   userArgs[0].newMonitor = 1

