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


from __future__ import absolute_import
import traceback


import Jama.Matrix
diffcalc_path = os.path.abspath(get_context().setup.expandPath("{script}/Lib/diffcalc"))
if not diffcalc_path in sys.path:
    sys.path.append(diffcalc_path)

import diffcalc
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
        if upper is None: upper = sys.float_info.max
        if lower is None: lower = sys.float_info.min
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
def setup_diff(diffractometer, energy, diffcalc_axis_names = None, geometry=None):
    """
        diffractometer: Diffraction motor group 
        energy: Positioner having energy in kev
        geometry: YouGeometry extension. If none, uses default
        diffcalc_axis_names: if None use defaults:
            - mu, delta, gam, eta, chi, phi (six circle)
            - delta, gam, eta, chi, phi (ficve circle)
            - delta, eta, chi, phi (four circle)        
    """
    global you, dc, ub, hardware, hkl, _motor_group 
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

def setup_axis(motor, min=None, max=None, cut=None):
    name = _difcalc_names[motor]
    if min is not None: hardware.setmin(name, min)
    if max is not None: hardware.setmax(name, max) 
    if cut is not None: hardware.setcut(name, cut) 

def print_axis_setup():
    print "Diffcalc names:"
    for m in _difcalc_names.keys(): 
        print " \t" + m.name + " = " + _difcalc_names[m]        
    print "------------------------------------------------------" 
    hardware.hardware()
    

###################################################################################################
# Acceess functions
###################################################################################################
def get_diff():
    return settings.hardware.diffractometer

def get_en():
    return settings.hardware.energy

def get_motor_group():
    return _motor_group

def get_wavelength():
    return you.wl

def get_hkl():
    return you.hkl 

def hkl_to_angles(h, k, l, energy=None):
    return dc.hkl_to_angles(h, k, l, energy)

def angles_to_hkl(positions, energy=None):
    return dc.angles_to_hkl(positions, energy)

def hkl_read():                
    return hkl_group.read()

def hkl_write(h, k, l):                
    hkl_group.write([h,k,l])
    
def hkl_simulate(h, k, l):                
    return hkl_group.sim([h,k,l])

def con(*args):
    hkl.con(*args)

def uncon(name):
    hkl.uncon(name)

def print_con():
    hkl.con()  

def get_ub_matrix():
    return ub.ubcalc._UB.tolist()  
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
    en.move(20.0)
    delta.config.maxSpeed = 50.0
    delta.speed = 50.0
    delta.move(1.0)

    #Setup
    setup_diff(sixc, en)
    setup_axis('gam', 0, 179)
    setup_axis('delta', 0, 179)
    setup_axis('delta', min=0)
    setup_axis('phi', cut=-180.0)
    print_axis_setup()

    #Orientation
    help(ub.ub)
    ub.listub()
    # Create a new ub calculation and set lattice parameters
    ub.newub('test')
    ub.setlat('cubic', 1, 1, 1, 90, 90, 90)
    # Add 1st reflection (demonstrating the hardware adapter)
    settings.hardware.wavelength = 1
    ub.c2th([1, 0, 0])                 # energy from hardware
    settings.hardware.position = 0, 60, 0, 30, 0, 0
    ub.addref([1, 0, 0])# energy and position from hardware
    # Add 2nd reflection (this time without the harware adapter)
    ub.c2th([0, 1, 0], 12.39842)
    ub.addref([0, 1, 0], [0, 60, 0, 30, 0, 90], 12.39842)
    # check the state
    ub.ub()
    ub.checkub()

    #Constraints
    help(hkl.con)
    hkl.con('qaz', 90)
    hkl.con('a_eq_b')
    hkl.con('mu', 0)
    hkl.con() 

    #Motion
    print  angles_to_hkl((0., 60., 0., 30., 0., 0.))
    print hkl_to_angles(1, 0, 0)
    sixc.write([0, 60, 0, 30, 90, 0])
    print "sixc=" , sixc.position
    wavelength.write(1.0)
    print "wavelength = ", wavelength.read()
    ub.lastub()
    ub.setu ([[1, 0, 0], [0, 1, 0], [0, 0, 1]])
    ub.showref()
    ub.swapref(1,2)
    #print you.hkl
    #pos(get_hkl())
    hkl_group.read()
    #you.hkl.simulateMoveTo([0,1,1])
    #sim(get_hkl(), [0,1,1])
    hkl_group.sim([0.0,1.0,1.0])
    #pos(get_hkl(), [0,1,1])
    hkl_group.write([0.0,1.0,1.0])
    
    #Scans
    lscan(l, [sin], 1.0, 1.5, 0.1)
    ascan([k,l], [sin], [1.0, 1.0], [1.2, 1.3], [0.1, 0.1], zigzag=True, parallel_positioning = False) 
    vector = [[1.0,1.0,1.0], [1.0,1.0,1.1], [1.0,1.0,1.2], [1.0,1.0,1.4]]
    hklscan(vector, [sin, arr], 0.9)