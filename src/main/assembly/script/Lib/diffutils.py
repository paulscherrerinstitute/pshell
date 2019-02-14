###################################################################################################\
# Diffcalc utilities
###################################################################################################

###################################################################################################\
# Installaling  
###################################################################################################

#1- Download from: https://github.com/DiamondLightSource/diffcalc/archive/master.zip
#2- Extract the contents to {script}/Lib/diffcalc
#3- Download http://central.maven.org/maven2/gov/nist/math/jama/1.0.3/jama-1.0.3.jar 
#   to the extensions folder.
#4- On {script}/Lib/diffcalc/diffcalc/gdasupport/you.py, the line " wl.asynchronousMoveTo(1)"
#   must be commented for the energy not to move when the library is loaded.  

###################################################################################################\
# Library loading and Hardware setup
###################################################################################################

#1- Create a MotorGroup with the diffractometer motors 
#   e.g.   'sixc', containing mu, delta, gam, eta, chi, phi motors         (gam = nu)
#   or     'fivec', containing delta, gam, eta, chi, phi motors
#   or     'fourc', containing delta, eta, chi, phi motors
#2- Create  positioner to read/set the energy in kEv, e.g. named 'en'
#3- Execute: run("diffutils")
#4- Execute: setup_diff(sixc, en)


###################################################################################################\
# API
###################################################################################################

# Orientation commands defined in  https://github.com/DiamondLightSource/diffcalc#id19 are 
# defined heren with identical signatures, and so the constraint commands.
# Motion command names were changed because thge original can collide with other globals:
# hklci, hklca, hklwh, hklget, hklmv and hklsim(hkl).


from __future__ import absolute_import
import traceback


import Jama.Matrix
diffcalc_path = os.path.abspath(get_context().setup.expandPath("{script}/Lib/diffcalc"))
if not diffcalc_path in sys.path:
    sys.path.append(diffcalc_path)

import diffcalc
import math
from diffcalc import settings
from diffcalc.hkl.you.geometry import YouGeometry,SixCircle, FiveCircle, FourCircle, YouPosition
from diffcalc.hardware import HardwareAdapter
from diffcalc.ub.persistence import UbCalculationNonPersister
from diffcalc.gdasupport.minigda.scannable import ScannableBase, ScannableGroup
#from diffcalc.gdasupport.minigda import command
from diffcalc.hardware import HardwareAdapter


import ch.psi.pshell.device.PositionerConfig as PositionerConfig
import ch.psi.pshell.device.RegisterConfig as RegisterConfig
import ch.psi.pshell.device.Register as Register

_difcalc_names = {}

#
# Disable error handling designed for interactive use
#diffcalc.util.DEBUG = True

###################################################################################################
# Device mapping to difcalc
###################################################################################################
class PositionerScannable(ScannableBase):
    def __init__(self, positioner, name = None):
        self.positioner = positioner
        self.name = positioner.name if name is None else name
        self.inputNames = [self.name]
        self.outputFormat = ['% 6.4f']
        self.level = 3

    def isBusy(self):
        return self.positioner.state == State.Busy

    def waitWhileBusy(self):
        self.positioner.waitReady(-1)

    def asynchronousMoveTo(self, new_position):
        #print "Moving " , self.name, " to: ", new_position 
        self.positioner.moveAsync(float(new_position), -1)

    def getPosition(self):
        return self.positioner.getPosition()

def _get_diffcalc_axis_names():
    nu_name=diffcalc.hkl.you.constraints.NUNAME
    return ("mu", "delta", nu_name, "eta", "chi", "phi")

class PositionerScannableGroup(ScannableGroup):
    def __init__(self, name, motors, diffcalc_axis_names=None):
        self.name = name
        global _difcalc_names
        _difcalc_names = {}
        positioners = []
        if diffcalc_axis_names is None:
            if len(motors)   == 6: diffcalc_axis_names = _get_diffcalc_axis_names()
            elif len(motors) == 5: diffcalc_axis_names = ("delta", "gam", "eta", "chi", " phi")
            elif len(motors) == 4: diffcalc_axis_names = ("delta", "eta", "chi", " phi")
        self.diffcalc_axis_names = diffcalc_axis_names
        for i in range(len(motors)):
            _difcalc_names[motors[i]] = diffcalc_axis_names[i]
            exec('self.' + diffcalc_axis_names[i] + ' = PositionerScannable(' + motors[i].name  + ', "' +diffcalc_axis_names[i] + '")')
            exec('positioners.append(self.' + diffcalc_axis_names[i] + ')' ) 
        #for m in motors: 
        #    exec('self.' + m.name + ' = PositionerScannable(' + m.name + ', "' + m.name + '")')
        #    exec('positioners.append(self.' + m.name + ')' ) 
        ScannableGroup.__init__(self, self.name, positioners)
             
class MotorGroupScannable(PositionerScannableGroup):        
    def __init__(self, motor_group, diffcalc_axis_names=None):
        self.motor_group = motor_group
        PositionerScannableGroup.__init__(self, motor_group.name, motor_group.motors, diffcalc_axis_names)
      

class ScannableAdapter(HardwareAdapter):
    def __init__(self, diffractometer, energy, energy_multiplier_to_kev=1):
        self.diffractometer = diffractometer
        self.energy = energy
        self.energy_multiplier_to_kev = energy_multiplier_to_kev    
        input_names = diffractometer.getInputNames()
        HardwareAdapter.__init__(self, input_names)

    #Returns the current physical POSITIONS
    def get_position(self):
        """
        pos = getDiffractometerPosition() -- returns the current physical
        diffractometer position as a list in degrees
        """
        return self.diffractometer.getPosition()

    #returns energy in kEv 
    def get_energy(self):
        """energy = get_energy() -- returns energy in kEv (NOT eV!) """
        multiplier = self.energy_multiplier_to_kev
        energy = self.energy.getPosition() * multiplier
        if energy is None:
            raise DiffcalcException("Energy has not been set")
        return energy

    def get_motor(self,name):
        global _motor_group
        global _difcalc_names
        for m in _difcalc_names.keys():
            if _difcalc_names[m] == name:
                return m
        for m in _motor_group.motors:
            if m.name == name:
                return m
        raise Exception("Invalid axis name: " + str(name))
            

    def get_lower_limit(self, name):
        '''returns lower limits by axis name. Limit may be None if not set
        '''
        m = self.get_motor(name)
        ret = m.getMinValue()
        if ret == float("NaN"): ret = None
        return ret

    def get_upper_limit(self, name):
        '''returns upper limit by axis name. Limit may be None if not set
        '''
        m = self.get_motor(name)
        ret = m.getMaxValue()
        if ret == float("NaN"): ret = None
        return ret

    def set_lower_limit(self, name, value):
        """value may be None to remove limit"""
        if value is None: value = float("NaN")
        m = self.get_motor(name)
        m.config.minValue =value
        
    def set_upper_limit(self, name, value):
        """value may be None to remove limit"""
        if value is None: value = float("NaN")
        m = self.get_motor(name)
        m.config.maxValue =value

    def is_axis_value_within_limits(self, axis_name, value):
        m = self.get_motor(axis_name)   
        upper = self.get_upper_limit(axis_name)  
        lower = self.get_lower_limit(axis_name)  
        if (upper is None) or (math.isnan(upper)): upper = sys.float_info.max
        if (lower is None) or (math.isnan(lower)): lower = -sys.float_info.max
        return lower <= value <= upper 

    @property
    def name(self):
        return self.diffractometer.getName()

class MotorGroupAdapter(ScannableAdapter):
    def __init__(self, diffractometer, energy, energy_multiplier_to_kev=1, diffcalc_axis_names=None):
        self.diffractometer = MotorGroupScannable(diffractometer, diffcalc_axis_names)
        self.energy = PositionerScannable(energy)
        self.energy.level = 3    
        ScannableAdapter.__init__(self, self.diffractometer, self.energy, energy_multiplier_to_kev)

class Wavelength(RegisterBase):    
    def doRead(self):
        try:
            return get_wavelength().getPosition()
        except:
            return None

    def doWrite(self, val):
        get_wavelength().asynchronousMoveTo(val)
                

###################################################################################################
# HKL Pseudo-devices
###################################################################################################
class HklPositoner (PositionerBase):
    def __init__(self, name, index, hkl_group):
         PositionerBase.__init__(self, name, PositionerConfig())
         self.setParent(hkl_group)
         self.index  = index

    def isReady(self):
        return PositionerBase.isReady(self) and self.getParent().isReady()
        
    def doRead(self):
        return self.getParent()._setpoint[self.index]

    def doWrite(self, value): 
        #print "Setting " , self.getName(), "to: ", value
        pos = [None, None, None]
        pos[self.index] = value
        self.getParent().write(pos)
        
    def doReadReadback(self):
        if java.lang.Thread.currentThread() != self.getParent()._updating_thread:
             self.getParent().update()
        return self.getParent()._readback[self.index]

class HklGroup(RegisterBase, Register.RegisterArray):
    def __init__(self, name):        
        RegisterBase.__init__(self, name, RegisterConfig())
        self.hkl=get_hkl()
        self.h, self.k, self.l = HklPositoner("h", 0, self), HklPositoner("k", 1, self), HklPositoner("l", 2, self)
        add_device(self.h, True)
        add_device(self.k, True)
        add_device(self.l, True)           
        self._setpoint = self.doRead()
        self._updating = False
        
    def getSize(self):
        return 3
                
    def doRead(self):   
        try:
            self._readback = self.hkl.getPosition()  
            self._updating_thread = java.lang.Thread.currentThread()       
            self.h.update()
            self.k.update()
            self.l.update()
        except:
            #traceback.print_exc()    
            self._readback = (None, None, None)
        finally:
            self._updating_thread = None
        return self._readback 

    def doWrite(self, pos):
        self._setpoint = pos
        #print "Moving to: " + str(pos)
        self.hkl.asynchronousMoveTo(pos) 

    def sim(self, pos):
        return self.hkl.simulateMoveTo(pos)
    
###################################################################################################
# System setup
###################################################################################################
you = None
dc, ub, hardware, hkl = None, None, None, None
_motor_group = None
def setup_diff(diffractometer= None, energy= None, diffcalc_axis_names = None, geometry=None):
    """
        configure diffractometer. Display configuration if no parameter is given
        diffractometer: Diffraction motor group 
        energy: Positioner having energy in kev
        geometry: YouGeometry extension. If none, uses default
        diffcalc_axis_names: if None use defaults:
            - mu, delta, gam, eta, chi, phi (six circle)
            - delta, gam, eta, chi, phi (ficve circle)
            - delta, eta, chi, phi (four circle)        
    """
    global you, dc, ub, hardware, hkl, _motor_group 
    if diffractometer is not None:
        _motor_group = diffractometer
        you = None
        if geometry is not None:
            settings.geometry = geometry
        elif diffcalc_axis_names is not None:
            class CustomGeometry(YouGeometry):
                def __init__(self):
                    self.all_axis_names = _get_diffcalc_axis_names()
                    self.my_axis_names = diffcalc_axis_names
                    fixed_constraints = {}
                    for axis in self.all_axis_names:
                        if not axis in self.my_axis_names:
                            fixed_constraints[axis] = 0
                    YouGeometry.__init__(self, diffractometer.name, fixed_constraints)                        
                def physical_angles_to_internal_position(self, physical_angle_tuple):
                    pos=[]
                    index = 0
                    for axis in self.all_axis_names:
                        pos.append(physical_angle_tuple[index] if (axis in self.my_axis_names) else 0)
                        index = index+1
                    pos.append("DEG")#units
                    return YouPosition(*pos)               
                def internal_position_to_physical_angles(self, internal_position):
                    pos = internal_position.clone()
                    pos.changeToDegrees()
                    pos = pos.totuple()
                    ret = []
                    for i in range (len(self.all_axis_names)):                                    
                        if self.all_axis_names[i] in self.my_axis_names:                       
                            ret.append(pos[i])
                    return tuple(ret)
            settings.geometry = CustomGeometry()
        elif len(diffractometer.motors) == 6:
            settings.geometry = SixCircle()
        elif len(diffractometer.motors) == 5:
            settings.geometry = FiveCircle()
        elif len(diffractometer.motors) == 4:
            settings.geometry = FourCircle()
        else:    
            raise Exception("Invalid motor group")
        settings.hardware = MotorGroupAdapter(diffractometer, energy, diffcalc_axis_names = diffcalc_axis_names)
        settings.ubcalc_persister = UbCalculationNonPersister()
        settings.axes_scannable_group = settings.hardware.diffractometer
        settings.energy_scannable = settings.hardware.energy
        settings.ubcalc_strategy = diffcalc.hkl.you.calc.YouUbCalcStrategy()
        settings.angles_to_hkl_function = diffcalc.hkl.you.calc.youAnglesToHkl
        from diffcalc.gdasupport import you
        reload(you)
        
        # These must be imported AFTER the settings have been configured
        from diffcalc.dc import dcyou as dc
        from diffcalc.ub import ub
        from diffcalc import hardware
        from diffcalc.hkl.you import hkl   
        
        add_device(HklGroup("hkl_group"), True)
        add_device(Wavelength("wavelength", 6), True)
        hkl_group.polling = 250
        wavelength.polling = 250

    if settings.hardware is not None:
        print "Diffractometer defined with:"
        print " \t" + "Motor group: " + str(settings.hardware.diffractometer.name) 
        print " \t" + "Energy: " + str(settings.hardware.energy.name) 
        print "\nDiffcalc axis names:"
        for m in _difcalc_names.keys(): 
            print " \t Motor " + m.name + " = Axis " + _difcalc_names[m]        
    else:
        print "Diffractometer is not defined\n"
    print
    
def setup_axis(motor = None, min=None, max=None, cut=None):
    """
    configure axis range and cut.
    displays ranges if motor is None
    """
    if motor is not None:
        name = get_axis_name(motor)
        if min is not None: hardware.setmin(name, min)
        if max is not None: hardware.setmax(name, max)
        if cut is not None: hardware.setcut(name, cut) 
    else:
        print "Axis range configuration:"
        hardware.hardware()
        print

###################################################################################################
# Acceess functions
###################################################################################################
def get_diff():
    return settings.hardware.diffractometer

def get_energy():
    return settings.hardware.energy
    
def get_adapter():
    return settings.hardware
    
def get_motor_group():
    return _motor_group

def get_wavelength():
    return you.wl

def get_hkl():
    return you.hkl 

def get_axis_name(motor):
    if is_string(motor):
        motor = get_adapter().get_motor(motor)
    return _difcalc_names[motor]    

###################################################################################################
# Orientation Commands
###################################################################################################


# State

def newub(name):
    """
    start a new ub calculation name
    """
    return ub.newub(name)

def loadub(name_or_num):
    """
    load an existing ub calculation
    """
    return ub.loadub(name_or_num)

def lastub():
    """
    load the last used ub calculation
    """
    return ub.lastub()
    
def listub():
    """
    list the ub calculations available to load
    """
    return ub.listub()

def rmub(name_or_num):
    """
    remove existing ub calculation
    """
    return ub.rmub(name_or_num)    

def saveubas(name):
    """
    save the ub calculation with a new name
    """
    return ub.saveubas(name)


# Lattice

def setlat(name=None, *args):
    """
    set lattice parameters (Angstroms and Deg)
    setlat  -- interactively enter lattice parameters (Angstroms and Deg)
    setlat name a -- assumes cubic
    setlat name a b -- assumes tetragonal
    setlat name a b c -- assumes ortho
    setlat name a b c gamma -- assumes mon/hex with gam not equal to 90
    setlat name a b c alpha beta gamma -- arbitrary
    """
    return ub.setlat(name, *args)

def c2th(hkl, en=None):
    """
    calculate two-theta angle for reflection
    """
    return ub.c2th(hkl, en)

def hklangle(hkl1, hkl2):
    """
    calculate angle between [h1 k1 l1] and [h2 k2 l2] crystal planes
    """
    return ub.hklangle(hkl1, hkl2)


# Reference (surface)

def setnphi(xyz = None):
    """
    sets or displays (xyz=None) n_phi reference
    """
    return ub.setnphi(xyz)    
 

def setnhkl(hkl = None):
    """
    sets or displays (hkl=None)  n_hkl reference
    """
    return ub.setnhkl(hkl)    

# Reflections
        
def showref():
    """
    shows full reflection list
    """
    return ub.showref()   

def addref(*args):
    """
    Add reflection
    addref	--  add reflection interactively
    addref [h k l] {'tag'}	--  add reflection with current position and energy
    addref [h k l] (p1, .., pN) energy {'tag'}	--  add arbitrary reflection
    """
    return ub.addref(*args)       

def editref(idx):
    """
    interactively edit a reflection (idx is tag or index numbered from 1)
    """
    return ub.editref(idx)   

def delref(idx):
    """
    deletes a reflection (idx is tag or index numbered from 1)
    """
    return ub.delref(idx)   


def clearref():
    """
    deletes all the reflections
    """
    return ub.clearref()   

def swapref(idx1=None, idx2=None):
    """
    swaps two reflections
    swapref -- swaps first two reflections used for calculating U matrix
    swapref {num1 | 'tag1'} {num2 | 'tag2'} -- swaps two reflections
    """
    return ub.swapref(idx1, idx2)

        
# Crystal Orientations

def showorient():
    """
    shows full list of crystal orientations
    """
    #TODO: Workaround of bug on Diffcalc (str_lines needs parameter)
    if ub.ubcalc._state.orientlist:
        print '\n'.join(ub.ubcalc._state.orientlist.str_lines(None))   
        return 
    return ub.showorient()  

def addorient(*args):
    """
    addorient -- add crystal orientation interactively
    addorient [h k l] [x y z] {'tag'}	-- add crystal orientation in laboratory frame
    """
    return ub.addorient(*args)  

def editorient(idx):
    """
    interactively edit a crystal orientation (idx is tag or index numbered from 1)
    """
    return ub.editorient(tag_or_num)  

def delorient(idx):
    """
    deletes a crystal orientation (idx is tag or index numbered from 1)
    """
    return ub.delorient(tag_or_num)  

def clearorient():
    """
    deletes all the crystal orientations
    """
    return ub.clearorient()  
    
def swaporient(idx1=None, idx2=None):
    """
    swaps two swaporient
    swaporient -- swaps first two crystal orientations used for calculating U matrix
    swaporient {num1 | 'tag1'} {num2 | 'tag2'} -- swaps two crystal orientations
    """
    return ub.swaporient(idx1, idx2)


# UB Matrix
def showub():
    """
    show the complete state of the ub calculation
    NOT A DIFFCALC COMMAND
    """
    return ub.ub()  

def checkub():
    """
    show calculated and entered hkl values for reflections
    """
    return ub.checkub()  

def setu(U=None):
    """
    manually set U matrix
    setu -- set U matrix interactively
    setu [[..][..][..]] -- manually set U matrix
    """
    return ub.setu(U)  

def setub(UB=None):
    """
    manually set UB matrix
    setub -- set UB matrix interactively
    setub [[..][..][..]] -- manually set UB matrix
    """
    return ub.setub(UB)      

def getub():
    """
    returns current UB matrix
    NOT A DIFFCALC COMMAND
    """    
    return None if ub.ubcalc._UB is None else ub.ubcalc._UB.tolist()  

def calcub(idx1=None, idx2=None):
    """
    (re)calculate u matrix 
    calcub -- (re)calculate U matrix from the first two reflections and/or orientations.
    calcub idx1 idx2 -- (re)calculate U matrix from reflections and/or orientations referred by indices and/or tags idx1 and idx2.
    """
    return ub.calcub(idx1, idx2)  

def trialub(idx=1):
    """
    (re)calculate u matrix using one reflection only
    Use indice or tags idx1. Default: use first reflection.
    """
    return ub.trialub(idx)      

def refineub(*args):
    """
    refine unit cell dimensions and U matrix to match diffractometer angles for a given hkl value
    refineub -- interactively
    refineub [h k l] {pos}  
    """
    return ub.refineub(*args)     

def fitub(*args):
    """
    fitub ref1, ref2, ref3... -- fit UB matrix to match list of provided reference reflections.
    """
    return ub.fitub(*args)         

def addmiscut(angle, xyz=None):
    """
    apply miscut to U matrix using a specified miscut angle in degrees and a rotation axis (default: [0 1 0])
    """
    return ub.addmiscut(angle, xyz)     
        
def setmiscut(angle, xyz=None):
    """
    manually set U matrix using a specified miscut angle in degrees and a rotation axis (default: [0 1 0])
    """
    return ub.setmiscut(angle, xyz)  



###################################################################################################
# Motion Commands
###################################################################################################

#Constraints

def con(*args):
    """
    list or set available constraints and values
    con -- list available constraints and values
    con <name> {val} -- constrains and optionally sets one constraint
    con <name> {val} <name> {val} <name> {val} -- clears and then fully constrains    
    """
    return hkl.con(*args)

def uncon(name):
    """
    remove constraint
    """
    return hkl.uncon(name)


# HKL
def allhkl(_hkl, wavelength=None):
    """
    print all hkl solutions ignoring limits
    """
    return hkl.allhkl(_hkl, wavelength)   


#Hardware

def setmin(axis, val=None):
    """
    set lower limits used by auto sector code (nan to clear)
    """
    name = get_axis_name(axis)
    return hardware.setmin(name, val)

def setmax(axis, val=None):
    """
    set upper limits used by auto sector code (nan to clear)
    """
    name = get_axis_name(axis)
    return hardware.setmax(name, val)

def setcut(axis, val):
    """
    sets cut angle
    """
    name = get_axis_name(axis)
    return hardware.setcut(name, val)        




###################################################################################################
# Motion commands: not standard Diffcalc names
###################################################################################################
   

def hklci(positions, energy=None):
    """
    converts positions of motors to reciprocal space coordinates (H K L)
    """ 
    return dc.angles_to_hkl(positions, energy)

def hklca(hkl, energy=None):
    """
    converts reciprocal space coordinates (H K L) to positions of motors.
    """ 
    return dc.hkl_to_angles(hkl[0], hkl[1], hkl[2], energy)

def hklwh():
    """
    prints the current reciprocal space coordinates (H K L) and positions of motors.
    """ 
    hkl = hklget()
    print "HKL: " + str(hkl)
    for m in _difcalc_names.keys(): 
        print _difcalc_names[m] + " [" + m.name + "] :" + str(m.take()) 
    
def hklget():       
    """
    get current hkl position
    """           
    return hkl_group.read()  
    
def hklmv(hkl):  
    """
    move to hkl position
    """             
    hkl_group.write(hkl)

def hklsim(hkl):
    """
    simulates moving diffractometer
    """
    return hkl_group.sim(hkl)
  

###################################################################################################
# HKL Combined Scan
###################################################################################################
def hklscan(vector, readables,latency = 0.0,  passes = 1, **pars):
    """
    HKL Scan: 

    Args:
        vector(list of lists): HKL values to be scanned
        readables(list of Readable): Sensors to be sampled on each step.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.     
            - hidden(bool, optional): if true generates no effects on user interface.     
            - before_read (function, optional): callback on each step, before sampling. Arguments: positions, scan
            - after_read (function, optional): callback on each step, after sampling. Arguments: record, scan.
            - before_pass (function, optional): callback before each scan pass execution. Arguments: pass_num, scan.
            - after_pass (function, optional): callback after each scan pass execution. Arguments: pass_num, scan.
            - Aditional arguments defined by set_exec_pars.
    Returns:
        ScanResult object.

    """
    readables=to_list(string_to_obj(readables))
    pars["initial_move"]  = False
    scan = ManualScan([h,k,l], readables ,vector[0], vector[-1], [len(vector)-1] * 3, dimensions = 1)
    if not "domain_axis" in pars.keys(): 
        pars["domain_axis"] = "Index"
    processScanPars(scan, pars)
    scan.start()
    try:
        for pos in vector:
            #print "Writing ", pos
            hkl_group.write(pos)
            time.sleep(0.1) #Make sure is busy
            get_motor_group().update()
            get_motor_group().waitReady(-1)
            time.sleep(latency)
            hkl_group.update()
            scan.append ([h.take(), k.take(), l.take()], [h.getPosition(), k.getPosition(), l.getPosition()], [readable.read() for readable in readables ])
    finally:
        scan.end()    
    return scan.result


def test_diffcalc():
    print "Start test"
    energy.move(20.0)
    delta.config.maxSpeed = 50.0
    delta.speed = 50.0
    delta.move(1.0)
    
    #Setup
    setup_diff(sixc, energy)
    setup_axis('gam', 0, 179)
    setup_axis('delta', 0, 179)
    setup_axis('delta', min=0)
    setup_axis('phi', cut=-180.0)
    setup_axis()
    
    #Orientation
    listub()
    # Create a new ub calculation and set lattice parameters
    newub('test')
    setlat('cubic', 1, 1, 1, 90, 90, 90)
    # Add 1st reflection (demonstrating the hardware adapter)
    settings.hardware.wavelength = 1
    c2th([1, 0, 0])                 # energy from hardware
    settings.hardware.position = 0, 60, 0, 30, 0, 0
    addref([1, 0, 0])# energy and position from hardware
    # Add 2nd reflection (this time without the harware adapter)
    c2th([0, 1, 0], 12.39842)
    addref([0, 1, 0], [0, 60, 0, 30, 0, 90], 12.39842)
    # check the state
    showub()
    checkub()
    
    #Constraints
    con('qaz', 90)
    con('a_eq_b')
    con('mu', 0)
    con() 
    
    #Motion
    print  hklci((0., 60., 0., 30., 0., 0.))
    print hklca((1, 0, 0))
    sixc.write([0, 60, 0, 30, 90, 0])
    print "sixc=" , sixc.position
    wavelength.write(1.0)
    print "wavelength = ", wavelength.read()
    lastub()
    setu ([[1, 0, 0], [0, 1, 0], [0, 0, 1]])
    showref()
    swapref(1,2)
    hklwh()
    hklsim([0.0,1.0,1.0])
    hklmv([0.0,1.0,1.0])
    
    #Scans
    lscan(l, [sin], 1.0, 1.5, 0.1)
    ascan([k,l], [sin], [1.0, 1.0], [1.2, 1.3], [0.1, 0.1], zigzag=True, parallel_positioning = False) 
    vector = [[1.0,1.0,1.0], [1.0,1.0,1.1], [1.0,1.0,1.2], [1.0,1.0,1.4]]
    hklscan(vector, [sin, arr], 0.9)
    

    