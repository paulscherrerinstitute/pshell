"""
This module provides support for the EPICS motor record.

Author:         Mark Rivers
Created:        Sept. 16, 2002
Modifications:
"""
import time

import epicsPV

class epicsMotor:
   """
   This module provides a class library for the EPICS motor record.
   It uses the epicsPV class, which is in turn a subclass of CaChannel.

   Virtual attributes:
      These attributes do not appear in the dictionary for this class, but
      are implemented with the __getattr__ and __setattr__ methods.  They
      simply do getw() or putw(value) to the appropriate motor record fields.
      All attributes can be both read and written unless otherwise noted.

      Attribute        Description                  Field
      ---------        -----------------------      -----
      slew_speed       Slew speed or velocity       .VELO
      base_speed       Base or starting speed       .VBAS
      acceleration     Acceleration time (sec)      .ACCL
      description      Description of motor         .DESC
      resolution       Resolution (units/step)      .MRES
      high_limit       High soft limit (user)       .HLM
      low_limit        Low soft limit (user)        .LLM
      dial_high_limit  High soft limit (dial)       .DHLM
      dial_low_limit   Low soft limit (dial)        .DLLM
      backlash         Backlash distance            .BDST
      offset           Offset from dial to user     .OFF
      done_moving      1=Done, 0=Moving, read-only  .DMOV
 
   Exceptions:
      The check_limits() method raises an "epicsMotorException" if a soft limit
      or hard limit is detected.  The move() and wait() methods call 
      check_limits() before they return, unless they are called with the 
      ignore_limits=1 keyword set.

   Example use:
      from epicsMotor import *
      m=epicsMotor('13BMD:m38')
      m.move(10)               # Move to position 10 in user coordinates
      m.move(100, dial=1)      # Move to position 100 in dial coordinates
      m.move(1, step=1, relative=1) # Move 1 step relative to current position
      m.wait()                 # Wait for motor to stop moving
      m.wait(start=1)          # Wait for motor to start moving
      m.wait(start=1, stop=1)  # Wait for motor to start, then to stop
      m.stop()                 # Stop moving immediately
      high = m.high_limit      # Get the high soft limit in user coordinates
      m.dial_high_limit = 100  # Set the high limit to 100 in dial coodinates
      speed = m.slew_speed     # Get the slew speed
      m.acceleration = 0.1     # Set the acceleration to 0.1 seconds
      p=m.get_position()       # Get the desired motor position in user coordinates
      p=m.get_position(dial=1) # Get the desired motor position in dial coordinates
      p=m.get_position(readback=1) # Get the actual position in user coordinates
      p=m.get_position(readback=1, step=1) Get the actual motor position in steps
      p=m.set_position(100)   # Set the current position to 100 in user coordinates
         # Puts motor in Set mode, writes value, puts back in Use mode.
      p=m.set_position(10000, step=1) # Set the current position to 10000 steps
"""

   def __init__(self, name):
      """
      Creates a new epicsMotor instance.

      Inputs:
         name:
            The name of the EPICS motor record without any trailing period or field
            name.

      Example:
         m=epicsMotor('13BMD:m38')
      """
      self.pvs = {'val' : epicsPV.epicsPV(name+'.VAL',  wait=0), 
                  'dval': epicsPV.epicsPV(name+'.DVAL', wait=0),
                  'rval': epicsPV.epicsPV(name+'.RVAL', wait=0),
                  'rlv' : epicsPV.epicsPV(name+'.RLV',  wait=0),
                  'rbv' : epicsPV.epicsPV(name+'.RBV',  wait=0),
                  'drbv': epicsPV.epicsPV(name+'.DRBV', wait=0),
                  'rrbv': epicsPV.epicsPV(name+'.RRBV', wait=0),
                  'dmov': epicsPV.epicsPV(name+'.DMOV', wait=0),
                  'stop': epicsPV.epicsPV(name+'.STOP', wait=0),
                  'velo': epicsPV.epicsPV(name+'.VELO', wait=0),
                  'vbas': epicsPV.epicsPV(name+'.VBAS', wait=0),
                  'accl': epicsPV.epicsPV(name+'.ACCL', wait=0),
                  'desc': epicsPV.epicsPV(name+'.DESC', wait=0),
                  'mres': epicsPV.epicsPV(name+'.MRES', wait=0),
                  'hlm':  epicsPV.epicsPV(name+'.HLM',  wait=0),
                  'llm':  epicsPV.epicsPV(name+'.LLM',  wait=0),
                  'dhlm': epicsPV.epicsPV(name+'.DHLM', wait=0),
                  'dllm': epicsPV.epicsPV(name+'.DLLM', wait=0),
                  'bdst': epicsPV.epicsPV(name+'.BDST', wait=0),
                  'set':  epicsPV.epicsPV(name+'.SET',  wait=0),
                  'lvio': epicsPV.epicsPV(name+'.LVIO', wait=0),
                  'lls':  epicsPV.epicsPV(name+'.LLS',  wait=0),
                  'hls':  epicsPV.epicsPV(name+'.HLS',  wait=0),
                  'off':  epicsPV.epicsPV(name+'.OFF',  wait=0)
                  }
      # Wait for all PVs to connect
      self.pvs['val'].pend_io() 

   def move(self, value, relative=0, dial=0, step=0, ignore_limits=0):
      """
      Moves a motor to an absolute position or relative to the current position
      in user, dial or step coordinates.
         
      Inputs:
         value:
            The absolute position or relative amount of the move
            
      Keywords:
         relative:
            Set relative=1 to move relative to current position.
            The default is an absolute move.
            
         dial:
            Set dial=1 if "value" is in dial coordinates.
            The default is user coordinates.
            
         step:
            Set step=1 if "value" is in steps.
            The default is user coordinates.
            
         ignore_limits:
            Set ignore_limits=1 to suppress raising exceptions
            if the move results in a soft or hard limit violation.
            
      Notes:
         The "step" and "dial" keywords are mutually exclusive.
         The "relative" keyword can be used in user, dial or step 
         coordinates.
         
      Examples:
         m=epicsMotor('13BMD:m38')
         m.move(10)          # Move to position 10 in user coordinates
         m.move(100, dial=1) # Move to position 100 in dial coordinates
         m.move(2, step=1, relative=1) # Move 2 steps 
      """
      if (dial != 0):
         # Position in dial coordinates
         if (relative != 0):
            current = self.get_position(dial=1)
            self.pvs['dval'].putw(current+value)
         else:
            self.pvs['dval'].putw(value)
    
      elif (step != 0):
         # Position in steps
         if (relative != 0):
            current = self.get_position(step=1)
            self.pvs['rval'].putw(current + value)
         else:
            self.pvs['rval'].putw(value)
      else:
         # Position in user coordinates
         if (relative != 0):
            self.pvs['rlv'].putw(value)
         else:
            self.pvs['val'].putw(value)

      # Check for limit violations
      if (ignore_limits == 0): self.check_limits()

   def check_limits(self):
      limit = self.pvs['lvio'].getw()
      if (limit!=0): 
         raise epicsMotorException('Soft limit violation')
      limit = self.pvs['lls'].getw()
      if (limit!=0): 
         raise epicsMotorException('Low hard limit violation')
      limit = self.pvs['hls'].getw()
      if (limit!=0): 
         raise epicsMotorException('High hard limit violation')

   
   def stop(self):
      """
      Immediately stops a motor from moving by writing 1 to the .STOP field.
         
      Examples:
         m=epicsMotor('13BMD:m38')
         m.move(10)          # Move to position 10 in user coordinates
         m.stop()            # Stop motor
      """
      self.pvs['stop'].putw(1)

   def get_position(self, dial=0, readback=0, step=0):
      """
      Returns the target or readback motor position in user, dial or step
      coordinates.
      
      Keywords:
         readback:
            Set readback=1 to return the readback position in the
            desired coordinate system.  The default is to return the
            target position of the motor.
            
         dial:
            Set dial=1 to return the position in dial coordinates.
            The default is user coordinates.
            
         step:
            Set step=1 to return the position in steps.
            The default is user coordinates.

         Notes:
            The "step" and "dial" keywords are mutually exclusive.
            The "readback" keyword can be used in user, dial or step 
            coordinates.
            
      Examples:
         m=epicsMotor('13BMD:m38')
         m.move(10)          # Move to position 10 in user coordinates
         p=m.get_position(dial=1) # Read the target position in 
                                  # dial coordinates
         p=m.get_position(readback=1, step=1) # Read the actual position in 
                                              # steps
      """
      if (dial != 0):
         if (readback != 0):
            return self.pvs['drbv'].getw()
         else:
            return self.pvs['dval'].getw()
      elif (step != 0):
         if (readback != 0):
            return self.pvs['rrbv'].getw()
         else:
            return self.pvs['rval'].getw()
      else:
         if (readback != 0):
            return self.pvs['rbv'].getw()
         else:
            return self.pvs['val'].getw()
   
   def set_position(self, position, dial=0, step=0):
      """
      Sets the motor position in user, dial or step coordinates.
      
      Inputs:
         position:
            The new motor position
            
      Keywords:
         dial:
            Set dial=1 to set the position in dial coordinates.
            The default is user coordinates.
            
         step:
            Set step=1 to set the position in steps.
            The default is user coordinates.
            
      Notes:
         The "step" and "dial" keywords are mutually exclusive.
         
      Examples:
         m=epicsMotor('13BMD:m38')
         m.set_position(10, dial=1)   # Set the motor position to 10 in 
                                      # dial coordinates
         m.set_position(1000, step=1) # Set the motor position to 1000 steps
      """
      # Put the motor in "SET" mode
      self.pvs['set'].putw(1)
      if (dial != 0):
         self.pvs['dval'].putw(position)
      elif (step != 0):
         self.pvs['rval'].putw(position)
      else:
         self.pvs['val'].putw(position)
      # Put the motor back in "Use" mode
      self.pvs['set'].putw(0)


   def wait(self, start=0, stop=0, poll=0.01, ignore_limits=0):
      """
      Waits for the motor to start moving and/or stop moving.
      
      Keywords:
         start:
            Set start=1 to wait for the motor to start moving.
            
         stop:
            Set stop=1 to wait for the motor to stop moving.
            
         poll:
            Set this keyword to the time to wait between reading the
            .DMOV field of the record to see if the motor is moving.
            The default is 0.01 seconds.
            
         ignore_limits:
            Set ignore_limits=1 to suppress raising an exception if a soft or
            hard limit is detected
            
      Notes:
         If neither the "start" nor "stop" keywords are set then "stop"
         is set to 1, so the routine waits for the motor to stop moving.
         If only "start" is set to 1 then the routine only waits for the
         motor to start moving.
         If both "start" and "stop" are set to 1 then the routine first
         waits for the motor to start moving, and then to stop moving.
         
      Examples:
         m=epicsMotor('13BMD:m38')
         m.move(100)               # Move to position 100
         m.wait(start=1, stop=1)   # Wait for the motor to start moving
                                   # and then to stop moving
      """
      if (start == 0) and (stop == 0): stop=1
      if (start != 0):
         while(1):
            done = self.pvs['dmov'].getw()
            if (done != 1): break
            time.sleep(poll)
      if (stop != 0):
         while(1):
            done = self.pvs['dmov'].getw()
            if (done != 0): break
            time.sleep(poll)
      if (ignore_limits == 0): self.check_limits()

   def __getattr__(self, attrname):
      if   (attrname == 'slew_speed'):      return self.pvs['velo'].getw()
      elif (attrname == 'base_speed'):      return self.pvs['vbas'].getw()
      elif (attrname == 'acceleration'):    return self.pvs['accl'].getw()
      elif (attrname == 'description'):     return self.pvs['desc'].getw()
      elif (attrname == 'resolution'):      return self.pvs['mres'].getw()
      elif (attrname == 'high_limit'):      return self.pvs['hlm'].getw()
      elif (attrname == 'low_limit'):       return self.pvs['llm'].getw()
      elif (attrname == 'dial_high_limit'): return self.pvs['dhlm'].getw()
      elif (attrname == 'dial_low_limit'):  return self.pvs['dllm'].getw()
      elif (attrname == 'backlash'):        return self.pvs['bdst'].getw()
      elif (attrname == 'offset'):          return self.pvs['off'].getw()
      elif (attrname == 'done_moving'):     return self.pvs['dmov'].getw()
      else: raise AttributeError(attrname)

   def __setattr__(self, attrname, value):
      if   (attrname == 'pvs'): self.__dict__[attrname]=value
      elif (attrname == 'slew_speed'):      self.pvs['velo'].putw(value)
      elif (attrname == 'base_speed'):      self.pvs['vbas'].putw(value)
      elif (attrname == 'acceleration'):    self.pvs['accl'].putw(value)
      elif (attrname == 'description'):     self.pvs['desc'].putw(value)
      elif (attrname == 'resolution'):      self.pvs['mres'].putw(value)
      elif (attrname == 'high_limit'):      self.pvs['hlm'].putw(value)
      elif (attrname == 'low_limit'):       self.pvs['llm'].putw(value)
      elif (attrname == 'dial_high_limit'): self.pvs['dhlm'].putw(value)
      elif (attrname == 'dial_low_limit'):  self.pvs['dllm'].putw(value)
      elif (attrname == 'backlash'):        self.pvs['bdst'].putw(value)
      elif (attrname == 'offset'):          self.pvs['off'].putw(value)
      else: raise AttributeError(attrname)

class epicsMotorException(Exception):
   def __init__(self, args=None):
      self.args=args
