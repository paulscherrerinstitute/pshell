###################################################################################################
#  Global definitions and built-in functions
###################################################################################################

import sys
sys.argv=['']
import time
import math
import inspect
import os.path
from operator import add, mul, sub, truediv
from time import sleep
from array import array
import types
import threading
import functools
import socket
import numpy
import traceback
import ctypes
from jep import jproxy

#TODO
#from jep import PyJArray
def is_array(obj):
    try:
        return str(type(obj)) == "<class 'jep.PyJArray'>"
    except:
        return False


from java.lang import Class
from java.lang import Object
from java.lang import System
from java.lang import AutoCloseable
from java.beans import PropertyChangeListener
from java.util import concurrent
from java.util import List
from java.util import ArrayList
from java.lang import reflect
from java.lang import Thread

from java.awt.image import BufferedImage
from java.awt import Color
from java.awt import Point
from java.awt import Dimension
from java.awt import Rectangle
from java.awt import Font


from java.lang import Boolean, Integer, Float, Double, Short, Byte, Long, String

from ch.psi.pshell.framework import Setup
from ch.psi.pshell.framework import Context
from ch.psi.pshell.scripting import ScriptUtils
from ch.psi.pshell.scripting import ScriptType
from ch.psi.pshell.scripting import JepUtils
from ch.psi.pshell.utils import Convert
from ch.psi.pshell.utils import Arr

__THREAD_EXEC_RESULT__=None

###################################################################################################
#Access functions
###################################################################################################

def get_app():
    return Context.getApp()

def get_view():
    return Context.getView()

def get_sequencer():
    return Context.getSequencer()

def get_interpreter():
    return Context.getInterpreter()

def get_data_manager():
    return Context.getDataManager()

def get_version_control():
    return Context.getVersionControl()

def get_device_pool():
    return Context.getDevicePool()

def get_sessions():
    return Context.getSessions()

def get_plugin_manager():
    return Context.getPluginManager()

def get_state():
    return Context.getState()


###################################################################################################
#Default empty callbacks
###################################################################################################
def on_command_started(info): pass
def on_command_finished(info): pass
def on_session_started(id): pass
def on_session_finished(id): pass
def on_change_data_path(path): pass
def on_system_restart(): pass

###################################################################################################
#Type conversion and checking
###################################################################################################

def to_array(obj, type = None, primitive = True):
    """Convert Python list to Java array.

    Args:
        obj(list): Original data.
        type(str): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 'd' = double,
                              'c' = char, 'z' = boolean, 's' = String,  'o' = Object
    Returns:
        Java array.
    """
    if obj is None:
        return None
    if type is None:
        type = 'o'
        enforceArrayType=False
    else:
        enforceArrayType=True
    if type[0] == '[':
        type = type[1:]        
    element_type = ScriptUtils.getPrimitiveType(type) if primitive else ScriptUtils.getType(type)

    def convert_1d_array(obj):
        if type == 'c':
            ret = reflect.Array.newInstance(element_type,len(obj))
            for i in range(len(obj)): ret[i] = chr(obj[i])
            return ret
        if type == 'z':
            ret = reflect.Array.newInstance(element_type,len(obj))
            for i in range(len(obj)):
                ret[i]= True if obj[i] else False
            return ret
        if type == 'o':
            ret = reflect.Array.newInstance(element_type,len(obj))
            for i in range(len(obj)):
                ret[i]= obj[i] 
            return ret
        if type == "s":
            return Convert.toStringArray(obj)   
        if primitive:   
            ret = Convert.wrapperArrayToPrimitiveArray(obj, element_type)  
        else:
            ret = reflect.Array.newInstance(element_type,len(obj))
            for i in range(len(obj)): ret[i] = Convert.toType(obj[i],element_type)
        return ret
                
    if is_array(obj):
        if enforceArrayType:
            if Arr.getComponentType(obj) != element_type:
                rank = Arr.getRank(obj)
                if (rank== 1):
                    obj=convert_1d_array(obj)
                elif (rank>1):  
                    pars, aux = [element_type], obj
                    for i  in range(rank):
                        pars.append(len(aux))
                        aux = aux[0]
                    #TODO: OVERLOADING BUG
                    #ret = reflect.Array.newInstance(*pars)    
                    ret = Arr.newInstance(*pars) 
                    for i in range(len(obj)):
                        ret[i]=to_array(obj[i], type)                        
                    obj = ret
    elif is_list(obj):        
        if type=='o':
            ret = reflect.Array.newInstance(element_type, len(obj))
            for i in range (len(obj)):
                if is_list(obj[i]) or is_array(obj[i]):
                    ret[i] = to_array(obj[i],type)
                else:
                    ret[i] = obj[i]
            obj=ret
        elif len(obj)>0 and (is_list(obj[0]) or is_array(obj[0])):
            pars, aux = [element_type], obj
            while len(aux)>0 and (is_list(aux[0]) or is_array(aux[0])):
                pars.append(len(aux))
                aux = aux[0]
            pars.append(0)
            #ret = reflect.Array.newInstance(*pars)
            ret = Arr.newInstance(*pars) 
            for i in range(len(obj)):
                ret[i]=to_array(obj[i], type)
            obj=ret
        else:
         obj= convert_1d_array(obj)
    return obj

def np_to_java(obj, dtype=None):
    """Convert a numpy array into a java array, preserving dimensionality.

    Args:
        obj(numpy array): Original data.

    Returns:
        Java array
    """
    if type(obj) == numpy.ndarray:
        if dtype is not None:
            obj=obj.astype(dtype, copy=False)
        return JepUtils.toJavaArray(obj)
    return obj

def to_list(obj):
    """Convert an object into a Python List.

    Args:
        obj(tuple or array or List): Original data.
    
    Returns:
        List.
    """
    if obj is None:
        return None
    if isinstance(obj,tuple) or isinstance(obj,List) :
        return list(obj)
    #if is_array(obj):
    #    return obj.tolist()
    if not isinstance(obj,list):
        return [obj,]
    return obj

def is_list(obj):
    try:
        if obj.__class__.__name__=="PyJList":
            return True
    except:
        pass
    return isinstance(obj,tuple) or isinstance(obj,list) or isinstance (obj, List)

def is_string(obj):
    return (type(obj) is str)


def is_main_thread():
    return threading.current_thread() == threading.main_thread()


###################################################################################################
#Builtin classes
###################################################################################################

from ch.psi.pshell.utils import Threading as Threading
from ch.psi.pshell.utils import State as State
from ch.psi.pshell.utils import Convert as Convert
from ch.psi.pshell.utils import Str as Str
from ch.psi.pshell.utils import Sys as Sys
from ch.psi.pshell.utils import Arr as Arr
from ch.psi.pshell.utils import IO as IO
from ch.psi.pshell.utils import Chrono as Chrono
from ch.psi.pshell.utils import Folder as Folder
from ch.psi.pshell.utils import Histogram as Histogram
from ch.psi.pshell.utils import History as History
from ch.psi.pshell.utils import Condition as Condition
from ch.psi.pshell.utils import ArrayProperties as ArrayProperties
from ch.psi.pshell.utils import Audio as Audio
from ch.psi.pshell.utils import BitMask as BitMask
from ch.psi.pshell.utils import Config as Config
from ch.psi.pshell.utils import Mail as Mail
from ch.psi.pshell.utils import Posix as Posix
from ch.psi.pshell.utils import ProcessFactory as ProcessFactory
from ch.psi.pshell.utils import Range as Range
from ch.psi.pshell.utils import Reflection as Reflection
from ch.psi.pshell.utils import Serializer as Serializer
from ch.psi.pshell.utils import TimestampedValue as TimestampedValue
from ch.psi.pshell.utils import Windows as Windows
from ch.psi.pshell.utils import NumberComparator as NumberComparator
from java.util import Iterator as Iterator
from java.util import NoSuchElementException as NoSuchElementException

from ch.psi.pshell.archiver import Inventory as Inventory
from ch.psi.pshell.archiver import DataAPI as DataAPI
from ch.psi.pshell.archiver import DispatcherAPI as DispatcherAPI
from ch.psi.pshell.archiver import EpicsBootInfoAPI as EpicsBootInfoAPI
from ch.psi.pshell.archiver import IocInfoAPI as IocInfoAPI
from ch.psi.pshell.archiver import Daqbuf as Daqbuf

from ch.psi.pshell.sequencer import CommandSource as CommandSource
from ch.psi.pshell.sequencer import SequencerListener as SequencerListener
from ch.psi.pshell.sequencer import ChannelAccessServer as ChannelAccessServer

from ch.psi.pshell.data import DataSlice as DataSlice
from ch.psi.pshell.data import PlotDescriptor as PlotDescriptor
from ch.psi.pshell.data import Table as Table
from ch.psi.pshell.data import Format as  Format
from ch.psi.pshell.data import FormatHDF5 as  FormatHDF5
from ch.psi.pshell.data import FormatText as  FormatText
from ch.psi.pshell.data import FormatCSV as  FormatCSV
from ch.psi.pshell.data import FormatTIFF as FormatTIFF
from ch.psi.pshell.data import FormatFDA as  FormatFDA
from ch.psi.pshell.data import Converter as  Converter
from ch.psi.pshell.data import Layout as  Layout
from ch.psi.pshell.data import LayoutBase as LayoutBase
from ch.psi.pshell.data import LayoutDefault as LayoutDefault
from ch.psi.pshell.data import LayoutTable as LayoutTable
from ch.psi.pshell.data import LayoutFDA as LayoutFDA
from ch.psi.pshell.data import LayoutSF as LayoutSF

from ch.psi.pshell.device import Device as Device
from ch.psi.pshell.device import DeviceBase as DeviceBase
from ch.psi.pshell.device import DeviceConfig as DeviceConfig
from ch.psi.pshell.device import GenericDevice as GenericDevice
from ch.psi.pshell.device import PositionerConfig as PositionerConfig
from ch.psi.pshell.device import RegisterConfig as RegisterConfig
from ch.psi.pshell.device import ReadonlyProcessVariableConfig as ReadonlyProcessVariableConfig
from ch.psi.pshell.device import ProcessVariableConfig as ProcessVariableConfig
from ch.psi.pshell.device import MotorConfig as MotorConfig
from ch.psi.pshell.device import Register as Register
from ch.psi.pshell.device import RegisterBase as RegisterBase
from ch.psi.pshell.device import ProcessVariableBase as ProcessVariableBase
from ch.psi.pshell.device import ControlledVariableBase as ControlledVariableBase
from ch.psi.pshell.device import PositionerBase as PositionerBase
from ch.psi.pshell.device import MasterPositioner as MasterPositioner
from ch.psi.pshell.device import MotorBase as MotorBase
from ch.psi.pshell.device import DiscretePositionerBase as DiscretePositionerBase
from ch.psi.pshell.device import MotorGroupBase as MotorGroupBase
from ch.psi.pshell.device import MotorGroupDiscretePositioner as MotorGroupDiscretePositioner
from ch.psi.pshell.device import ReadonlyRegisterBase as ReadonlyRegisterBase
from ch.psi.pshell.device import ReadonlyAsyncRegisterBase as ReadonlyAsyncRegisterBase
from ch.psi.pshell.device import Register as Register
from ch.psi.pshell.device import Record as Record
from ch.psi.pshell.devices import InlineDevice as InlineDevice

RegisterArray = Register.RegisterArray
RegisterNumber = Register.RegisterNumber
RegisterBoolean = Register.RegisterBoolean
from ch.psi.pshell.device import RegisterCache as RegisterCache
from ch.psi.pshell.device import ReadonlyRegister
ReadonlyRegisterArray = ReadonlyRegister.ReadonlyRegisterArray 
ReadonlyRegisterMatrix = ReadonlyRegister.ReadonlyRegisterMatrix
from ch.psi.pshell.device import DummyPositioner as DummyPositioner
from ch.psi.pshell.device import DummyMotor as DummyMotor
from ch.psi.pshell.device import DummyRegister as DummyRegister
from ch.psi.pshell.device import Timestamp as Timestamp
from ch.psi.pshell.device import Interlock as Interlock
from ch.psi.pshell.device import Readable as Readable
ReadableArray = Readable.ReadableArray
ReadableMatrix = Readable.ReadableMatrix
ReadableCalibratedArray = Readable.ReadableCalibratedArray
ReadableCalibratedMatrix = Readable.ReadableCalibratedMatrix
from ch.psi.pshell.device import ArrayCalibration as ArrayCalibration
from ch.psi.pshell.device import MatrixCalibration as MatrixCalibration
from ch.psi.pshell.device import Writable as Writable
WritableArray = Writable.WritableArray
from ch.psi.pshell.device import Stoppable as Stoppable
from ch.psi.pshell.device import Averager as Averager
from ch.psi.pshell.device import ArrayAverager as ArrayAverager
from ch.psi.pshell.device import Delta as Delta
from ch.psi.pshell.device import DeviceListener as DeviceListener
from ch.psi.pshell.device import ReadbackDeviceListener as ReadbackDeviceListener
from ch.psi.pshell.device import MotorListener as MotorListener
from ch.psi.pshell.device import MoveMode as MoveMode
from ch.psi.pshell.device import SettlingCondition as SettlingCondition
from ch.psi.pshell.device import HistogramGenerator as HistogramGenerator

from ch.psi.pshell.epics import Epics as Epics
from ch.psi.pshell.epics import ChannelSettlingCondition as ChannelSettlingCondition
from ch.psi.pshell.epics import AreaDetector as AreaDetector
from ch.psi.pshell.epics import BinaryPositioner as BinaryPositioner
from ch.psi.pshell.epics import ChannelByte as ChannelByte
from ch.psi.pshell.epics import ChannelByteArray as ChannelByteArray
from ch.psi.pshell.epics import ChannelByteMatrix as ChannelByteMatrix
from ch.psi.pshell.epics import ChannelDouble as ChannelDouble
from ch.psi.pshell.epics import ChannelDoubleArray as ChannelDoubleArray
from ch.psi.pshell.epics import ChannelDoubleMatrix as ChannelDoubleMatrix
from ch.psi.pshell.epics import ChannelFloat as ChannelFloat
from ch.psi.pshell.epics import ChannelFloatArray as ChannelFloatArray
from ch.psi.pshell.epics import ChannelFloatMatrix as ChannelFloatMatrix
from ch.psi.pshell.epics import ChannelInteger as ChannelInteger
from ch.psi.pshell.epics import ChannelIntegerArray as ChannelIntegerArray
from ch.psi.pshell.epics import ChannelIntegerMatrix as ChannelIntegerMatrix
from ch.psi.pshell.epics import ChannelShort as ChannelShort
from ch.psi.pshell.epics import ChannelShortArray as ChannelShortArray
from ch.psi.pshell.epics import ChannelShortMatrix as ChannelShortMatrix
from ch.psi.pshell.epics import ChannelString as ChannelString
from ch.psi.pshell.epics import ControlledVariable as ControlledVariable
from ch.psi.pshell.epics import DiscretePositioner as DiscretePositioner
from ch.psi.pshell.epics import GenericChannel as GenericChannel
from ch.psi.pshell.epics import GenericArray as GenericArray
from ch.psi.pshell.epics import GenericMatrix as GenericMatrix
from ch.psi.pshell.epics import Manipulator as Manipulator
from ch.psi.pshell.epics import Motor as EpicsMotor
from ch.psi.pshell.epics import Positioner as Positioner
from ch.psi.pshell.epics import ProcessVariable as ProcessVariable
from ch.psi.pshell.epics import ReadonlyProcessVariable as ReadonlyProcessVariable
from ch.psi.pshell.epics import Scaler as Scaler
from ch.psi.pshell.epics import Scienta as Scienta
from ch.psi.pshell.epics import Slit as Slit
from ch.psi.pshell.epics import AreaDetectorSource as AreaDetectorSource
from ch.psi.pshell.epics import ArraySource as ArraySource
from ch.psi.pshell.epics import ByteArraySource as ByteArraySource
from ch.psi.pshell.epics import PsiCamera as PsiCamera
from ch.psi.pshell.epics import CAS as CAS

from ch.psi.pshell.serial import SerialPortDevice as SerialPortDevice
from ch.psi.pshell.serial import TcpDevice as TcpDevice
from ch.psi.pshell.serial import UdpDevice as UdpDevice
from ch.psi.pshell.serial import SerialPortDeviceConfig as SerialPortDeviceConfig
from ch.psi.pshell.serial import SocketDeviceConfig as SocketDeviceConfig

from ch.psi.pshell.modbus import ModbusTCP as ModbusTCP
from ch.psi.pshell.modbus import ModbusUDP as ModbusUDP
from ch.psi.pshell.modbus import ModbusSerial as ModbusSerial
from ch.psi.pshell.modbus import AnalogInput as ModbusAI
from ch.psi.pshell.modbus import AnalogInputArray as ModbusMAI
from ch.psi.pshell.modbus import AnalogOutput as ModbusAO
from ch.psi.pshell.modbus import AnalogOutputArray as ModbusMAO
from ch.psi.pshell.modbus import DigitalInput as ModbusDO
from ch.psi.pshell.modbus import DigitalInputArray as ModbusMDI
from ch.psi.pshell.modbus import DigitalOutput as ModbusDO
from ch.psi.pshell.modbus import DigitalOutputArray as ModbusMDO
from ch.psi.pshell.modbus import Register  as ModbusReg
from ch.psi.pshell.modbus import ReadonlyProcessVariable  as ModbusROPV
from ch.psi.pshell.modbus import ProcessVariable as ModbusPV
from ch.psi.pshell.modbus import ControlledVariable  as ModbusCB
from ch.psi.pshell.modbus import ModbusDeviceConfig as ModbusDeviceConfig

from ch.psi.pshell.imaging import Source as Source
from ch.psi.pshell.imaging import SourceBase as SourceBase
from ch.psi.pshell.imaging import DirectSource as DirectSource
from ch.psi.pshell.imaging import RegisterArraySource as RegisterArraySource
from ch.psi.pshell.imaging import RegisterMatrixSource as RegisterMatrixSource
ReadableMatrixSource=RegisterMatrixSource.ReadableMatrixSource
from ch.psi.pshell.imaging import ImageListener as ImageListener
from ch.psi.pshell.imaging import ImageMeasurement as ImageMeasurement
from ch.psi.pshell.imaging import CameraSource as CameraSource
from ch.psi.pshell.imaging import DeviceSource as DeviceSource
from ch.psi.pshell.imaging import ColormapSource as ColormapSource
from ch.psi.pshell.imaging import StreamSource as StreamSource
from ch.psi.pshell.imaging import ColormapAdapter as ColormapAdapter
from ch.psi.pshell.imaging import FileSource as FileSource
from ch.psi.pshell.imaging import MjpegSource as MjpegSource
from ch.psi.pshell.imaging import Webcam as Webcam
from ch.psi.pshell.imaging import Filter as Filter
from ch.psi.pshell.imaging import Utils as ImagingUtils
from ch.psi.pshell.imaging import Overlay as Overlay
from ch.psi.pshell.imaging import Overlays as Overlays
from ch.psi.pshell.imaging import Pen as Pen
from ch.psi.pshell.imaging import Data as Data
from ch.psi.pshell.imaging import Colormap as Colormap
from ch.psi.pshell.imaging import Renderer as Renderer
from ch.psi.pshell.imaging import DeviceRenderer as DeviceRenderer


from ch.psi.pshell.plot import RangeSelectionPlot as RangeSelectionPlot
RangeSelectionPlotListener= RangeSelectionPlot.RangeSelectionPlotListener
from ch.psi.pshell.plot import LinePlot as LinePlot
from ch.psi.pshell.plot import MatrixPlot as MatrixPlot
from ch.psi.pshell.plot import TimePlot as TimePlot
from ch.psi.pshell.plot import SlicePlot as SlicePlot
from ch.psi.pshell.plot import LinePlotJFree as LinePlotJFree
from ch.psi.pshell.plot import MatrixPlotJFree as MatrixPlotJFree
from ch.psi.pshell.plot import TimePlotJFree as TimePlotJFree
from ch.psi.pshell.plot import SlicePlotDefault as SlicePlotDefault
from ch.psi.pshell.plot import LinePlotSeries as LinePlotSeries
from ch.psi.pshell.plot import LinePlotErrorSeries as LinePlotErrorSeries
from ch.psi.pshell.plot import MatrixPlotSeries as MatrixPlotSeries
from ch.psi.pshell.plot import TimePlotSeries as TimePlotSeries
from ch.psi.pshell.plot import SlicePlotSeries as SlicePlotSeries
AxisId=Class.forName("ch.psi.pshell.plot.Plot$AxisId")
LinePlotStyle=Class.forName("ch.psi.pshell.plot.LinePlot$Style")

from ch.psi.pshell import scan as scans
from ch.psi.pshell.scan import ScanBase as ScanBase
from ch.psi.pshell.scan import ScanResult
from ch.psi.pshell.scan import Otf as Otf
from ch.psi.pshell.scan import ScanAbortedException as ScanAbortedException
from ch.psi.pshell.scan import ScanCallbacks 

from ch.psi.pshell.crlogic import CrlogicPositioner as CrlogicPositioner
from ch.psi.pshell.crlogic import CrlogicSensor as CrlogicSensor

from ch.psi.pshell.bs import Stream as Stream
from ch.psi.pshell.bs import StreamMerger as StreamMerger
from ch.psi.pshell.bs import Provider as Provider
from ch.psi.pshell.bs import Dispatcher as Dispatcher
from ch.psi.pshell.bs import Scalar as Scalar
from ch.psi.pshell.bs import StreamChannel as StreamChannel
from ch.psi.pshell.bs import Waveform as Waveform
from ch.psi.pshell.bs import Matrix as Matrix
from ch.psi.pshell.bs import StreamCamera as StreamCamera
from ch.psi.pshell.bs import ProviderConfig as ProviderConfig
from ch.psi.pshell.bs import StreamConfig as StreamConfig
from ch.psi.pshell.bs import StreamChannelConfig as StreamChannelConfig
from ch.psi.pshell.bs import WaveformConfig as WaveformConfig
from ch.psi.pshell.bs import MatrixConfig as MatrixConfig

from ch.psi.pshell.camserver import PipelineClient as PipelineClient
from ch.psi.pshell.camserver import CameraClient as CameraClient
from ch.psi.pshell.camserver import ProxyClient as ProxyClient
from ch.psi.pshell.camserver import CameraSource as CameraSource
from ch.psi.pshell.camserver import CameraStream as CameraStream
from ch.psi.pshell.camserver import PipelineSource as PipelineSource
from ch.psi.pshell.camserver import PipelineStream as PipelineStream
from ch.psi.pshell.camserver import CamServerService as CamServerService

from redis.clients.jedis import Jedis as Redis
from ch.psi.pshell.utils import RedisX as RedisX

from ch.psi.pshell.detector import DetectorConfig as DetectorConfig
from ch.psi.pshell.detector import Array10 as Array10

from org.zeromq import ZMQ as ZMQ
from org.zeromq import SocketType as ZMQ_SocketType #Not to collide with bsread.SocketType

from ch.psi.pshell.framework import App as App

from ch.psi.pshell.scripting import ViewPreference as Preference
from ch.psi.pshell.scripting import ScriptUtils as ScriptUtils
from ch.psi.pshell.device import Record 
from javax.swing import SwingUtilities 


from org.jfree.chart.ui import RectangleAnchor as RectangleAnchor
from org.jfree.chart.ui import RectangleAnchor as TextAnchor
try:
    from ch.psi.pshell.xscan import ProcessorXScan as ProcessorXScan
except:
    ProcessorXScan = None

def string_to_obj(o):
    if is_string(o):
        o=str(o)
        if "://" in o:
            return InlineDevice(o)
        ret =  get_sequencer().getInterpreterVariable(o)
        if ret is None:
            try:
                ret = get_interpreter().evalBackground(o).result
            except:                        
                return None
        o=ret
    elif is_list(o):
        ret = []
        for i in o:
            ret.append(string_to_obj(i))
        o=ret
    proxy_method = getattr(o, "get_proxy", None)
    if callable(proxy_method):
        return o.get_proxy()
    return o    

def json_to_obj(o):
    if is_string(o):
        import json
        return json.loads(o)
    elif is_list(o):
        ret = []
        for i in o:
            ret.append(json_to_obj(i))
        return ret
    return o

###################################################################################################
#Scan device interfaces
###################################################################################################

class Nameable():
    def __init__(self, name=None, interfaces=[]):
        self.name = name
        self.proxy=jproxy(self, interfaces)
            
    def getName(self):        
        if self.name:
            return self.name
        return self.__class__.__name__  

    def get_proxy(self):
        return self.proxy
    
class Writable(Nameable):
    __interfaces__=['ch.psi.pshell.device.Writable']
    def __init__(self, name=None):
        Nameable.__init__(self, name, Writable.__interfaces__)
    def write(self, value):
        raise Exception ("Not implemented")
        
class Readable(Nameable):
    __interfaces__=['ch.psi.pshell.device.Readable']
    def __init__(self, name=None):
        Nameable.__init__(self, name, Readable.__interfaces__)
         
    def read(self):   
        raise Exception ("Not implemented")

class ReadableArray(Readable):
    __interfaces__=['ch.psi.pshell.device.Readable$ReadableArray']
    def __init__(self, name=None):
        Nameable.__init__(self, name, Readable.__interfaces__ + ReadableArray.__interfaces__)
            
    def read(self):
        raise Exception ("Not implemented")
         
    def getSize(self):
        raise Exception ("Not implemented")

class ReadableCalibratedArray(ReadableArray):
    __interfaces__=['ch.psi.pshell.device.Readable$ReadableCalibratedArray']
    def __init__(self, name=None):
         Nameable.__init__(self, name, Readable.__interfaces__ + ReadableArray.__interfaces__ +  ReadableCalibratedArray.__interfaces__)
        
    def read(self):
        raise Exception ("Not implemented")
        
    def getSize(self):
        raise Exception ("Not implemented")
        
    def getCalibration(self):
        raise Exception ("Not implemented")
                
class ReadableMatrix(Readable):
    __interfaces__=['ch.psi.pshell.device.Readable$ReadableMatrix']
    def __init__(self, name=None):
        Nameable.__init__(self, name, Readable.__interfaces__ + ReadableMatrix.__interfaces__)
            
    def read(self):
        raise Exception ("Not implemented")
    
    def getWidth(self):
        raise Exception ("Not implemented")

    def getHeight(self):
        raise Exception ("Not implemented")
   

class ReadableCalibratedMatrix(ReadableMatrix):
    __interfaces__=['ch.psi.pshell.device.Readable$ReadableCalibratedMatrix']
    def __init__(self, name=None):
        Nameable.__init__(self, name, Readable.__interfaces__ + ReadableMatrix.__interfaces__ + ReadableCalibratedMatrix.__interfaces__)
        
    def read(self):
        raise Exception ("Not implemented")
        
    def getWidth(self):
        raise Exception ("Not implemented")

    def getHeight(self):
        raise Exception ("Not implemented")

    def getCalibration(self):
        raise Exception ("Not implemented")   


###################################################################################################
#Other Java interfaces
###################################################################################################

        
class GenIterator(Nameable):
    __interfaces__ = ['java.util.Iterator']
    def __init__(self, gen):
        Nameable.__init__(self, None, GenIterator.__interfaces__)
        self.gen = gen
        self.cache=self

    def remove():
        pass

    def forEachRemaining(action):
        pass

    def hasNext(self):
        if self.cache != self:
            return True
        try:
            self.cache=self.gen.__next__()
            return True
        except:
            self.cache = self
            return False

    def next(self):
        try:
            if self.hasNext():
                return self.cache
            else:
                raise NoSuchElementException()
        finally:
             self.cache = self

###################################################################################################
#Scan classes
###################################################################################################

def __no_args(f):
    ret = f.func_code.co_argcount
    return (ret-1) if type(f)==PyMethod else ret

def __before_readout(scan, pos):
    try:
        if scan.before_read != None:
            args = __no_args(scan.before_read)
            if   args==0: scan.before_read()
            elif args==1: scan.before_read(pos.tolist())
            elif args==2: scan.before_read(pos.tolist(), scan)
    except AttributeError:
        pass

def __after_readout(scan, record):
    try:
        if scan.after_read != None:
            args = __no_args(scan.after_read)
            if   args==0: scan.after_read()
            elif args==1: scan.after_read(record)
            elif args==2: scan.after_read(record, scan)
    except AttributeError:
        pass

def __before_pass(scan, num_pass):
    try:
        if scan.before_pass != None:
            args = __no_args(scan.before_pass)
            if   args==0:scan.before_pass()
            elif args==1:scan.before_pass(num_pass)
            elif args==2:scan.before_pass(num_pass, scan)
    except AttributeError:
        pass

def __after_pass(scan, num_pass):
    try:
        if scan.after_pass != None:
            args = __no_args(scan.after_pass)
            if   args==0:scan.after_pass()
            elif args==1:scan.after_pass(num_pass)
            elif args==2:scan.after_pass(num_pass, scan)
    except AttributeError:
        pass

def __before_region(scan, num_region):
    try:
        if scan.before_region != None:
            args = __no_args(scan.before_region)
            if   args==0:scan.before_region()
            elif args==1:scan.before_region(num_region)
            elif args==2:scan.before_region(num_region, scan)
    except AttributeError:
        pass

#TODO implement scan callbacks with different method, as cannot extend Java classes
LineScan=scans.LineScan
AreaScan=scans.AreaScan
RegionScan=scans.RegionScan
VectorScan=scans.VectorScan
ContinuousScan=scans.ContinuousScan
TimeScan=scans.TimeScan
MonitorScan=scans.MonitorScan
BsScan=scans.BsScan
EpicsScan=scans.EpicsScan
#ManualScan=scans.ManualScan
BinarySearch=scans.BinarySearch
HillClimbingSearcharySearch=scans.HillClimbingSearch

class ManualScan():
    def __init__(self, writables, readables, start = None, end = None, steps = None, relative = False, dimensions = None, **pars):
        start=to_list(start)
        end=to_list(end)
        steps=to_list(steps)
        self.scan=scans.ManualScan.ManualScanStr(writables, readables, start, end, steps, relative)
        self.dimensions = dimensions
        processScanPars(self.scan, pars)

    def start(self):
        self.scan.start()

    def end(self):
        self.scan.end()

    def append(self,setpoints, positions, values, timestamps=None):
        self.scan.append(np_to_java(to_array(setpoints)), np_to_java(to_array(positions)), np_to_java(to_array(values)), \
                        None if (timestamps is None) else np_to_java(to_array(timestamps)))

    def getDimensions(self):
        if self._dimensions == None:
            return self._scan.getDimensions()
        else:
            return self.dimensions

def _no_args(f):
    ret = f.__code__.co_argcount
    return (ret-1) if isinstance(f, types.MethodType) else ret

class Callbacks():
    def __init__(self,pars):
        self.pars=pars
        self.before_read = pars.pop("before_read",None)
        self.after_read = pars.pop("after_read",None)
        self.before_pass = pars.pop("before_pass",None)
        self.after_pass =  pars.pop("after_pass",None)
        self.before_region= pars.pop("before_region",None)
        self.proxy=jproxy(self, ["ch.psi.pshell.scan.ScanCallbacks"])
    def onBeforeScan(self, scan):
        pass
    def onAfterScan(self, scan):
        pass
    def onBeforeReadout(self, scan, pos):
        try:
            if self.before_read is not None:
                args = _no_args(self.before_read)
                if   args==0: self.before_read()
                elif args==1: self.before_read(list(pos))
                elif args==2: self.before_read(list(pos), scan)
        except:
            traceback.print_exc()   
    def onAfterReadout(self, scan, record):
        try:
            if self.after_read is not None:
                args = _no_args(self.after_read)
                if   args==0: self.after_read()
                elif args==1: self.after_read(record)
                elif args==2: self.after_read(record, scan)
        except:
            traceback.print_exc()   
    def onBeforePass(self, scan, num_pass):
        try:
            if self.before_pass is not None:
                args = _no_args(self.before_pass)
                if   args==0:self.before_pass()
                elif args==1:self.before_pass(num_pass)
                elif args==2:self.before_pass(num_pass, scan)
        except:
            traceback.print_exc()    
     
    def onAfterPass(self, scan, num_pass):
        try:
            if self.after_pass is not None:
                args = _no_args(self.after_pass)
                if   args==0:self.after_pass()
                elif args==1:self.after_pass(num_pass)
                elif args==2:self.after_pass(num_pass, scan)
        except:
            traceback.print_exc()    
    def onBeforeRegion(self, scan, num_region):  
        try:
            if self.before_region != None:
                args = _no_args(self.before_region)
                if   args==0:self.before_region()
                elif args==1:self.before_region(num_region)
                elif args==2:self.before_region(num_region, scan)
        except:
            traceback.print_exc()   

def processScanPars(scan, pars):
    scan.setCallbacks(Callbacks(pars).proxy)
    scan.setPlotTitle(pars.pop("title",None))
    scan.setHidden(pars.pop("hidden",False))
    scan.setSettleTimeout (pars.pop("settle_timeout",ScanBase.getScansSettleTimeout()))
    scan.setUseWritableReadback (pars.pop("use_readback",ScanBase.getScansUseWritableReadback()))
    scan.setInitialMove(pars.pop("initial_move",ScanBase.getScansTriggerInitialMove()))
    scan.setParallelPositioning(pars.pop("parallel_positioning",ScanBase.getScansParallelPositioning()))
    scan.setAbortOnReadableError(pars.pop("abort_on_error",ScanBase.getAbortScansOnReadableError()))
    scan.setRestorePosition (pars.pop("restore_position",ScanBase.getRestorePositionOnRelativeScans()))
    scan.setCheckPositions(pars.pop("check_positions",ScanBase.getScansCheckPositions()))
    scan.setMonitors(to_list(string_to_obj(pars.pop("monitors",None))))
    scan.setSnaps(to_list(string_to_obj(pars.pop("snaps",None))))
    scan.setDiags(to_list(string_to_obj(pars.pop("diags",None))))
    scan.setMeta(pars.pop("meta",None))
    get_sequencer().setCommandPars(scan, pars)



###################################################################################################
#EPICS Channela abstraction
###################################################################################################

def create_channel(name, type=None, size=None):
    return Epics.newChannel(name, Epics.getChannelType(type), size)


###################################################################################################
#Help and access to function documentation
###################################################################################################
def _getBuiltinFunctions(filter = None):
    ret = []
    for name in globals().keys():
        val = globals()[name]
        if isinstance(val, types.FunctionType):
            if filter is None or filter in name:
                #Only "public" documented functions
                if not name.startswith('_') and (val.__doc__ is not None):
                    ret.append(val)
    return to_array(ret)


def _getBuiltinFunctionNames(filter = None):
    ret = []
    for function in _getBuiltinFunctions(filter):
        ret.append(function.__name__)
    ret.sort()
    return to_array(ret)

def _getFunctionDoc(function):
    if is_string(function):
        if function not in globals():
            return 
        function = globals()[function]
    if isinstance(function, types.FunctionType):
        if '__doc__' in dir(function):
            return function.__name__ + str(inspect.signature(function)) + "\n\n" + function.__doc__


def help(object = None):
    """
    Print help message for function or object (if available).

    Args:
        object (any, optional): function or object to get help.
                    If null prints a list of the builtin functions.

    Returns:
        None
    """
    if object is None:
        print ("Built-in functions:")
        for f in _getBuiltinFunctionNames():
            print ("\t" + f)
    else:
        if isinstance(object, types.FunctionType):
            print (_getFunctionDoc(object))
        elif '__doc__' in dir(object):
            print (object.__doc__)

###################################################################################################
#Variable injection
###################################################################################################

def _get_caller():
    #Not doing inspect.currentframe().f_back because inspect is slow to load
    return sys._getframe(1).f_back if hasattr(sys, "_getframe") else None

def inject():
    """Restore initial globals: re-inject devices and startup variables to the interpreter.

    Args:
        None

    Returns:
        None
    """
    if __name__ == "__main__":
        g=globals()
    else:
        g=_get_caller().f_globals
     
    i = get_interpreter().getInjections()
    for k in i.keySet():
        g[k]=i[k]


###################################################################################################
#Script evaluation and return values
###################################################################################################

def run(script_name, args = None, locals = None):
    """Run script: can be absolute path, relative, or short name to be search in the path.
    Args:
        args(Dict or List): Sets sys.argv (if list) or gobal variables(if dict) to the script.
        locals(Dict): If not none sets the locals()for the runing script.
                      If locals is used then script definitions will not go to global namespace.

    Returns:
        The script return value (if set with set_return)
    """
    script = get_interpreter().getLibrary().resolveFile(script_name)
    if script is not None and os.path.isfile(script):
        info = get_sequencer().startScriptExecution(script_name, args)
        try:
            set_return(None)
            if args is not None:
                if isinstance(args,list) or isinstance(args,tuple):
                    sys.argv =  list(args)
                    globals()["args"] = sys.argv
                else:
                    for arg in args.keys():
                        globals()[arg] = args[arg]
            if (locals is None):
                exec(open(script).read(), globals())
            else:
                exec(open(script).read(), globals(), locals)
            ret = get_return()
            get_sequencer().finishScriptExecution(info, ret)
            return ret
        except Exception as ex:
            get_sequencer().finishScriptExecution(info, ex)
            raise ex
    raise IOError("Invalid script: " + str(script_name))

def abort():
    """Abort the execution of ongoing task. It can be called from the script to quit.

    Args:
        None

    Returns:
        None
    """
    Context.abort()
    raise KeyboardInterrupt()

def set_return(value):
    """Sets the script return value. This value is returned by the "run" function.

    Args:
        value(Object): script return value.

    Returns:
        None
    """
    global __THREAD_EXEC_RESULT__
    __THREAD_EXEC_RESULT__=value
    return value    #Used when parsing file

def get_return():
    if __name__ == "__main__":
        global __THREAD_EXEC_RESULT__
        return __THREAD_EXEC_RESULT__
    else:
        return _get_caller().f_globals["__THREAD_EXEC_RESULT__"]



###################################################################################################
#Builtin functions
###################################################################################################


###################################################################################################
#Scan commands
###################################################################################################

def lscan(writables, readables, start, end, steps, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
    """Line Scan: positioners change together, linearly from start to end positions.

    Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(int or float or list of float): number of scan steps (int) or step size (float).
        relative (bool, optional): if true, start and end positions are relative to current.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writables invert direction on each pass.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - hidden(bool, optional): if true generates no effects on user interface.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - settle_timeout(int, optional): timeout for each positioner get to position. Default (-1) waits forever.
            - initial_move (bool, optional): if true (default) perform move to initial position prior to scan start.
            - parallel_positioning (bool, optional): if true (default) all positioners are set in parallel.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - check_positions (bool, optional): if true (default) verifies if in correct positions after move finishes.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readables=to_list(string_to_obj(readables))
    start=to_list(start)
    end=to_list(end)
    if type(steps) is float or is_list(steps):
        steps = to_list(steps)
        scan = LineScan.LineScanStepSize(writables,readables, start, end , steps, relative, latency_ms, int(passes), zigzag)
    else:
        scan = LineScan.LineScanNumSteps(writables,readables, start, end , steps, relative, latency_ms, int(passes), zigzag)
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def vscan(writables, readables, vector, line = False, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
    """Vector Scan: positioner values provided in a vector.

    Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        vector (generator (floats or lists of float)  or list of list of float): positioner values.
        line (bool, optional): if true, processs as line scan (1d)
        relative (bool, optional): if true, start and end positions are relative to current.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes (disregarded if vector is a generator).
        zigzag(bool, optional): if true writables invert direction on each pass (disregarded if vector is a generator).
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - settle_timeout(int, optional): timeout for each positioner get to position. Default (-1) waits forever.
            - initial_move (bool, optional): if true (default) perform move to initial position prior to scan start.
            - parallel_positioning (bool, optional): if true (default) all positioners are set in parallel.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - check_positions (bool, optional): if true (default) verifies if in correct positions after move finishes.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readables=to_list(string_to_obj(readables))
    if inspect.isgenerator(vector):
        vector = GenIterator(vector).proxy
        scan = VectorScan(writables,readables, vector, line, relative, latency_ms)
    else:
        if len(vector) == 0:
            vector.append([])
        elif (not is_list(vector[0])) and (not is_array(vector[0])):
            vector = [[x,] for x in vector]
        vector = np_to_java(to_array(vector, 'd'),'d')
        scan = VectorScan(writables,readables, vector, line, relative, latency_ms, int(passes), zigzag)
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def ascan(writables, readables, start, end, steps, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
    """Area Scan: multi-dimentional scan, each positioner is a dimention.

    Args:
        writables(list of Writable): Positioners set on each step.
        readables(list of Readable): Sensors to be sampled on each step.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(list of int or list of float): number of scan steps (int) or step size (float).
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        relative (bool, optional): if true, start and end positions are relative to current.
        passes(int, optional): number of passes
        zigzag (bool, optional): if true writables invert direction on each row.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - settle_timeout(int, optional): timeout for each positioner get to position. Default (-1) waits forever.
            - initial_move (bool, optional): if true (default) perform move to initial position prior to scan start.
            - parallel_positioning (bool, optional): if true (default) all positioners are set in parallel.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - check_positions (bool, optional): if true (default) verifies if in correct positions after move finishes.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readables=to_list(string_to_obj(readables))
    start=to_list(start)
    end=to_list(end)
    steps = to_list(steps)
    if type(steps[0]) is int:
        scan = AreaScan.AreaScanNumSteps(writables,readables, start, end , steps, relative, latency_ms, int(passes), zigzag)
    else:
        scan = AreaScan.AreaScanStepSize(writables,readables, start, end , steps, relative, latency_ms, int(passes), zigzag)   
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()


def rscan(writable, readables, regions, latency=0.0, relative=False, passes=1, zigzag=False, **pars):
    """Region Scan: positioner scanned linearly, from start to end positions, in multiple regions.

    Args:
        writable(Writable): Positioner set on each step, for each region.
        readables(list of Readable): Sensors to be sampled on each step.
        regions (list of tuples (float,float, int)   or (float,float, float)): each tuple define a scan region
                                (start, stop, steps) or (start, stop, step_size)
        relative (bool, optional): if true, start and end positions are relative to current.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        passes(int, optional): number of passes
        zigzag(bool, optional): if true writable invert direction on each pass.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - before_region (function(region_num, scan), optional): callback before entering a region.
            - settle_timeout(int, optional): timeout for each positioner get to position. Default (-1) waits forever.
            - initial_move (bool, optional): if true (default) perform move to initial position prior to scan start.
            - parallel_positioning (bool, optional): if true (default) all positioners are set in parallel.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - check_positions (bool, optional): if true (default) verifies if in correct positions after move finishes.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    start=[]
    end=[]
    steps=[]
    for region in regions:
        start.append(region[0])
        end.append(region[1])
        steps.append(region[2])
    latency_ms=int(latency*1000)
    writable=string_to_obj(writable)
    readables=to_list(string_to_obj(readables))
    start=to_list(start)
    end=to_list(end)
    if type(steps[0]) is float:
        scan = RegionScan.RegionScanStepSize(writable,readables, start, end , steps, relative, latency_ms, int(passes), zigzag)
    else:
        scan = RegionScan.RegionScanNumSteps(writable,readables, start, end , steps, relative, latency_ms, int(passes), zigzag)
        
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def cscan(writables, readables, start, end, steps, latency=0.0, time=None, relative=False, passes=1, zigzag=False, **pars):
    """Continuous Scan: positioner change continuously from start to end position and readables are sampled on the fly.

    Args:
        writable(Speedable or list of Motor): A positioner with a  getSpeed method or
                    a list of motors.
        readables(list of Readable): Sensors to be sampled on each step.
        start(float or list of float): start positions of writables.
        end(float or list of float): final positions of writabless.
        steps(int or float or list of float): number of scan steps (int) or step size (float).
        latency(float, optional): sleep time in each step before readout, defaults to 0.0.
        time (float, seconds): if not None then speeds are set according to time.
        relative (bool, optional): if true, start and end positions are relative to current.
        passes(int, optional): number of passes
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    latency_ms=int(latency*1000)
    readables=to_list(string_to_obj(readables))
    writables=to_list(string_to_obj(writables))
    start=to_list(start)
    end=to_list(end)
    #A single Writable with fixed speed
    if time is None:
        if is_list(steps): steps=steps[0]
        scan = ContinuousScan.ContinuousScanSingle(writables[0],readables, start[0], end[0] , int(steps), relative, latency_ms, int(passes), zigzag)
    #A set of Writables with speed configurable
    else:
        if type(steps) is float or is_list(steps):
            steps = to_list(steps)
        if type(steps) is int:
            scan = ContinuousScan.ContinuousScanNumSteps(writables,readables, start, end , steps, time, relative, latency_ms, int(passes), zigzag)       
        else:
            steps = to_list(steps)            
            scan = ContinuousScan.ContinuousScanStepSize(writables,readables, start, end , steps, time, relative, latency_ms, int(passes), zigzag)

    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()
    

def hscan(config, writable, readables, start, end, steps, passes=1, zigzag=False, **pars):
    """Hardware Scan: values sampled by external hardware and received asynchronously.

    Args:
        config(dict): Configuration of the hardware scan. The "class" key provides the implementation class.
                      Other keys are implementation specific.
        writable(Writable): A positioner appropriated to the hardware scan type.
        readables(list of Readable): Sensors appropriated to the hardware scan type.
        start(float): start positions of writable.
        end(float): final positions of writables.
        steps(int or float): number of scan steps (int) or step size (float).
        passes(int, optional): number of passes
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - after_read (function(record, scan), optional): callback on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): callback before each scan pass execution.
            - after_pass (function(pass_num, scan), optional): callback after each scan pass execution.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - meta (dict, optional): scan metadata.

    Returns:
        ScanResult.
    """
    cls = Class.forName(config["class"])
    readables=to_list(string_to_obj(readables))
    scan = cls(config, writable,readables, start, end , steps, int(passes), zigzag)
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def bscan(stream, records, timeout = None, passes=1, **pars):
    """BS Scan: records all values in a beam synchronous stream.

    Args:
        stream(Stream): stream object or list of chanel names to build stream from
        records(int): number of records to store
        timeout(float, optional): maximum scan time in seconds.
        passes(int, optional): number of passes
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    timeout_ms=int(timeout*1000) if ((timeout is not None) and (timeout>=0)) else -1
    if not is_list(stream):
        stream=string_to_obj(stream)
    scan = BsScan(stream,int(records), timeout_ms, int(passes))
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def tscan(readables, points, interval, passes=1, fixed_rate=True, **pars):
    """Time Scan: sensors are sampled in fixed time intervals.

    Args:
        readables(list of Readable): Sensors to be sampled on each step.
        points(int): number of samples.
        interval(float): time interval between readouts. Minimum temporization is 0.001s
        passes(int, optional): number of passes
        fixed_rate(bool, optional): in the case of delays in sampling:
                            If True tries to preserve to total scan time, accelerating following sampling.
                            If False preserves the interval between samples, increasing scan time.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    interval= max(interval, 0.001)   #Minimum temporization is 1ms
    interval_ms=int(interval*1000)
    readables=to_list(string_to_obj(readables))
    scan = TimeScan(readables, points, interval_ms, int(passes), bool(fixed_rate))
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def mscan(trigger, readables, points=-1, timeout=None, asynchronous=True, take_initial=False, passes=1, **pars):
    """Monitor Scan: sensors are sampled when received change event of the trigger device.

    Args:
        trigger(Device or list of Device): Source of the sampling triggering.
        readables(list of Readable): Sensors to be sampled on each step.
                                     If  trigger has cache and is included in readables, it is not read
                                     for each step, but the change event value is used.
        points(int, optional): number of samples (-1 for undefined).
        timeout(float, optional): maximum scan time in seconds (None for no timeout).
        asynchronous(bool, optional): if True then records are sampled and stored on event change callback. Enforce
                               reading only cached values of sensors.
                               If False, the scan execution loop waits for trigger cache update. Do not make
                               cache only access, but may loose change events.
        take_initial(bool, optional): if True include current values as first record (before first trigger).
        passes(int, optional): number of passes
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - before_pass (function(pass_num, scan), optional): called before each pass.
            - after_pass (function(pass_num, scan), optional): callback after each pass.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - monitors (list of Device, optional): device values are saved on every change event during the scan.
            - snaps (list of Readable, optional): snapshot device values are saved before the scan.
            - diags (list of Readable, optional): diagnostic device values are saved at each scan point.
            - meta (dict, optional): scan metadata.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    raise Exception("Not implemented")
    timeout_ms=int(timeout*1000) if ((timeout is not None) and (timeout>=0)) else -1
    trigger = string_to_obj(trigger)
    readables=to_list(string_to_obj(readables))
    scan = MonitorScan(trigger, readables, points, timeout_ms, asynchronous, take_initial, int(passes))
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def escan(name, **pars):
    """Epics Scan: execute an Epics Scan Record.

    Args:
        name(str): Name of scan record.
        title(str, optional): plotting window name.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - Additional arguments defined by set_exec_pars.

    Returns:
        ScanResult.
    """
    scan = EpicsScan(name)
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def xscan(file_name, arguments={}):
    """ Run FDA's XScan (devined in XML file)

    Args:
        file_name(string): Name of the file (relative to XScan base folder)
        arguments(dict):  map of of XScan variables 
                          E.g: in a linear positioner  {"idXXXX.start":0.0, "idXXXX.end":5.0, "idXXXX.step_size":0.1})
        
    """ 
    if ProcessorXScan is None:
        raise Exception("XScan is not present in class path")   
    ProcessorXScan().startExecute(file_name,arguments)


def bsearch(writables, readable, start, end, steps, maximum = True, strategy = "Normal", latency=0.0, relative=False, **pars):
    """Binary search: searches writables in a binary search fashion to find a local maximum for the readable.

    Args:
        writables(list of Writable): Positioners set on each step.
        readable(Readable): Sensor to be sampled.
        start(list of float): start positions of writables.
        end(list of float): final positions of writables.
        steps(float or list of float): resolution of search for each writable.
        maximum (bool , optional): if True (default) search maximum, otherwise minimum.
        strategy (str , optional): "Normal": starts search midway to scan range and advance in the best direction.
                                             Uses orthogonal neighborhood (4-neighborhood for 2d)
                                   "Boundary": starts search on scan range.
                                   "FullNeighborhood": Uses complete neighborhood (8-neighborhood for 2d)

        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        relative (bool, optional): if true, start and end positions are relative to current.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - settle_timeout(int, optional): timeout for each positioner get to position. Default (-1) waits forever.
            - parallel_positioning (bool, optional): if true (default) all positioners are set in parallel.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - check_positions (bool, optional): if true (default) verifies if in correct positions after move finishes.
            - Additional arguments defined by set_exec_pars.

    Returns:
        SearchResult.
    """
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readable=string_to_obj(readable)
    start=to_list(start)
    end=to_list(end)
    steps = to_list(steps)
    strategy = BinarySearch.Strategy.valueOf(strategy)
    scan = BinarySearch(writables,readable, start, end , steps, maximum, strategy, relative, latency_ms)
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()

def hsearch(writables, readable, range_min, range_max, initial_step, resolution, filter=1, maximum=True, latency=0.0, relative=False, **pars):
    """Hill Climbing search: searches writables in decreasing steps to find a local maximum for the readable.
    Args:
        writables(list of Writable): Positioners set on each step.
        readable(Readable): Sensor to be sampled.
        range_min(list of float): minimum positions of writables.
        range_max(list of float): maximum positions of writables.
        initial_step(float or list of float):initial step size for for each writable.
        resolution(float or list of float): resolution of search for each writable (minimum step size).
        filter(int): number of additional steps to filter noise
        maximum (bool , optional): if True (default) search maximum, otherwise minimum.
        latency(float, optional): settling time for each step before readout, defaults to 0.0.
        relative (bool, optional): if true, start and end positions are relative to current.
        pars(keyworded variable length arguments, optional): scan optional named arguments:
            - title(str, optional): plotting window name.
            - before_read (function(positions, scan), optional): called on each step, before sampling.
            - after_read (function(record, scan), optional): called on each step, after sampling.
            - settle_timeout(int, optional): timeout for each positioner get to position. Default (-1) waits forever.
            - parallel_positioning (bool, optional): if true (default) all positioners are set in parallel.
            - abort_on_error (bool, optional): if true then aborts scan in sensor failures. Default is false.
            - restore_position (bool, optional): if true (default) then restore initial position after relative scans.
            - check_positions (bool, optional): if true (default) verifies if in correct positions after move finishes.
            - Additional arguments defined by set_exec_pars.

    Returns:
        SearchResult.
    """
    latency_ms=int(latency*1000)
    writables=to_list(string_to_obj(writables))
    readable=string_to_obj(readable)
    range_min=to_list(range_min)
    range_max=to_list(range_max)
    initial_step = to_list(initial_step)
    resolution = to_list(resolution)
    scan = HillClimbingSearch(writables,readable, range_min, range_max , initial_step, resolution, filter, maximum, relative, latency_ms)
    processScanPars(scan, pars)
    scan.start()
    return scan.getResult()


###################################################################################################
#Data plotting
###################################################################################################

def plot(data, name = None, xdata = None, ydata=None, title=None):
    """Request one or multiple plots of user data (1d, 2d or 3d).

    Args:
        data: array or list of values. For multiple plots, list of arrays.
        name(str or list of str, optional): plot name. For multiple plots, list of names.
        xdata: array or list of values. For multiple plots, list of arrays.
        ydata: array or list of values. For multiple plots, list of arrays.
        title(str, optional): plotting window name.

    Returns:
        List of Plot.
    """
    data = json_to_obj(data)
    xdata = json_to_obj(xdata)
    ydata = json_to_obj(ydata)
    if isinstance(data, Table):
        if is_list(xdata):
            xdata = np_to_java(to_array(xdata, 'd'), 'd')
        return get_sequencer().plot(data,xdata,name,title)

    if isinstance(data, ScanResult):
        return get_sequencer().plot(data,title)

    if (name is not None) and is_list(name):
        if len(name)==0:
            name=None;
        else:
            if (data==None):
                data = []
                for n in name:
                    data.append([])
        plots = reflect.Array.newInstance(Class.forName("ch.psi.pshell.data.PlotDescriptor"), len(data))
        for i in range (len(data)):
            plotName = None if (name is None) else name[i]
            x = xdata
            if is_list(x) and len(x)>0 and (is_list(x[i]) or isinstance(x[i] , List) or is_array(x[i])):
                x = x[i]
            y = ydata
            if is_list(y) and len(y)>0 and (is_list(y[i]) or isinstance(y[i] , List) or is_array(y[i])):
                y = y[i]
            plots[i] =  PlotDescriptor(plotName , np_to_java(to_array(data[i], 'd'), 'd'), np_to_java(to_array(x, 'd'), 'd'), np_to_java(to_array(y, 'd'), 'd'))
        return get_sequencer().plot(plots,title)
    else:
        plot = PlotDescriptor(name, np_to_java(to_array(data, 'd'), 'd'), np_to_java(to_array(xdata, 'd'), 'd'), np_to_java(to_array(ydata, 'd'), 'd'))
        return get_sequencer().plot(plot,title)

def get_plots(title=None):
    """Return all current plots in the plotting window given by 'title'.

    Args:
        title(str, optional): plotting window name.

    Returns:
        List of Plot.
    """
    return get_sequencer().getPlots(title)

def get_plot_snapshots(title = None, file_type = "png", size = None, temp_path = None):
    """Returns list with file names of plots snapshots from a plotting context.

    Args:
        title(str, optional): plotting window name.
        file_type(str, optional): "png", "jpg", "bmp" or "gif"
        size(array, optional): [width, height]
        temp_path(str, optional): path where the files will be generated.

    Returns:
        list of strings
    """
    if not temp_path:
        temp_path = Setup.getContextPath()
    time.sleep(0.1) #Give some time to plot to be finished - it is not sync  with acquisition
    ret = []
    if size != None:
        size = Dimension(size[0], size[1])
    plots = get_plots(title)
    for i in range(len(plots)):
        p = plots[i]
        name = p.getTitle()
        if name is None or name == "":
            name = str(i)
        file_name = os.path.abspath(temp_path + "/" + name + "." + file_type)
        p.saveSnapshot(file_name , file_type, size)
        ret.append(file_name)
    return ret


###################################################################################################
#Data access
###################################################################################################

def load_data(path, index=0, shape=None, root=None):
    """Read data from the current persistence context or from data files.

    Args:
        path(str): Path to group or dataset relative to the root.
                   If path is in the format 'root|path', or else if 'root' is defined, then 
                   reads from data file given by root. Otherwise uses current data persistence file.
        root(str, optional):  data file.
        index(int or list, optional):
                if integer, data depth (used for 3D datasets returning a 2d matrix)
                If a list, specifies the full coordinate for multidimensional datasets.
        shape(list, optional): only valid if index is a list, provides the shape of the data array.
                In this case return a flattened a one-dimensional array.

    Returns:
        Data array
    """
    dm=get_data_manager()
    if index is not None and is_list(index):
        slice = dm.getData(path, index, shape) if (root==None) else dm.getData(root, path, index, shape)
    else:
        slice = dm.getData(path, index) if (root==None) else dm.getData(root, path, index)
    return slice.sliceData

def get_attributes(path, root=None):
    """Get the attributes from group or dataset.

    Args:
        path(str): Path to group or dataset relative to the root.
                   If path is in the format 'root|path', or else if 'root' is defined, then 
                   reads from data file given by root. Otherwise uses current data persistence file.
        root(str, optional):  data file.
    Returns:
        Dictionary
    """
    dm=get_data_manager()
    if (root is None):
        return dm.getAttributes(path)
    return dm.getAttributes(root, path)

def get_data_info(path, root=None):
    """Get information about the group or dataset.

    Args:
        path(str): Path to group or dataset relative to the current persistence context root.
                   If path is in the format 'root|path', or else if 'root' is defined, then 
                   reads from data file given by root. Otherwise uses current data persistence file.
        root(str, optional):  data file.
    Returns:
        Dictionary
    """
    dm=get_data_manager()
    if (root is None):
        return dm.getInfo(path)
    return dm.getInfo(root, path)

def save_dataset(path, data, type='d', unsigned=False, features=None):
    """Save data into a dataset within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        type(str, optional): array type - 'd'=double (default), 'b'=byte, 'h'=short, 'i'=int, 
                             'l'=long, 'f'=float, 'c'=char, 's'=String, 'z'=bool, 'o'=Object
        data (array or list): data to be saved
        unsigned(boolean, optional): create a dataset of unsigned type.
        features(dictionary, optional): See create_dataset.

    Returns:
        Dictionary
    """
    dm=get_data_manager()
    data = np_to_java(to_array(data, type), type)
    dm.setDataset(path, data, unsigned, features)

def create_group(path):
    """Create an empty dataset within the current persistence context.

    Args:
        path(str): Path to group relative to the current persistence context root.
    Returns:
        None
    """
    dm=get_data_manager()
    dm.createGroup(path)

def create_dataset(path, type, unsigned=False, dimensions=None, features=None):
    """Create an empty dataset within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        type(str or Readable): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float,
                              'd' = double, 'c' = char, 's' = String, 'z'=bool, 'o' = Object
        unsigned(boolean, optional)
        dimensions(tuple of int, optional): a 0 value means variable length in that dimension.
        features(dictionary, optional): storage features for the dataset, format specific.
            Keys for HDF5: "layout": "compact", "contiguous" or "chunked"
                           "compression": True, "max" or deflation level from 1 to 9
                           "shuffle": Byte shuffle before compressing.
                           "chunk": tuple, setting the chunk size
            Default: No compression, contiguous for fixed size arrays, chunked for variable size, compact for scalars.
    Returns:
        None
    """
    dm=get_data_manager()
    if "read" in (dir(type)): #If is Readable
        dm.createDataset(path, type,dimensions, features)
    else:
        dm.createDataset(path, ScriptUtils.getType(type), unsigned, dimensions, features)

def create_table(path, names, types=None, lengths=None, features=None):
    """Create an empty table (dataset of compound type) within the current persistence context.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        names(list of strings): name of each column
        types(array of str):  'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float,
                              'd' = double, 'c' = char, 's' = String,  'o' = Object
                              Note:A '[' prefix on type name indicates an array type.
        lengths(list of int): the array length for each columns(0 for scalar types).
        features(dictionary, optional): See create_dataset.
    Returns:
        None
    """
    dm=get_data_manager()
    type_classes = []
    if (types is not None):
        for i in range (len(types)):
            type_classes.append(ScriptUtils.getType(types[i]))
    dm.createTable(path, names, type_classes, lengths, features)

def append_dataset(path, data, index=None, type='d', shape=None):
    """Append data to dataset.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(number or array or list): name of each column.
        index(int or list, optional): if set then add the data in a specific position in the dataset.
                If integer is the index in an array (data must be 1 order lower than dataset)
                If a list, specifies the full coordinate for multidimensional datasets.
        type(str, optional): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float,
                              'd' = double, 'c' = char, 's' = String,  'o' = Object
                   default: 'd' (convert data to array of doubles)
        shape(list, optional): only valid if index is a list, provides the shape of the data array.
                In this case data must be a flattened one-dimensional array.
    Returns:
        None
    """
    dm=get_data_manager()
    data = np_to_java(to_array(data, type))
    if index is None:
        dm.appendItem(path, data)
    else:
        if is_list(index):
            if shape is None:
                shape = [len(index)]
            dm.setItem(path, data, index, shape)
        else:
            dm.setItem(path, data, index)

def append_table(path, data):
    """Append data to a table (dataset of compound type)

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        data(list): List of valus for each column of the table. 
    Returns:
        None
    """
    dm=get_data_manager()
    if is_list(data):
        arr = reflect.Array.newInstance(Class.forName("java.lang.Object"),len(data))
        for i in range (len(data)):
            if is_list(data[i]):
                arr[i] = to_array(data[i], 'd')
            else:
                arr[i] = np_to_java(data[i])
        data=arr
    dm.appendItem(path, data)

def flush_data():
    """Flush all data files immediately.

    Args:
        None
    Returns:
        None
    """
    dm=get_data_manager()
    dm.flush()

def set_attribute(path, name, value, unsigned = False):
    """Set an attribute to a group or dataset.

    Args:
        path(str): Path to dataset relative to the current persistence context root.
        name(str): name of the attribute
        value(Object): the attribute value
        unsigned(bool, optional):  if applies, indicate if  value is unsigned.
    Returns:
        None
    """
    dm=get_data_manager()
    if is_list(value):
        value = Convert.toStringArray(to_array(value))
    elif type(value) == numpy.ndarray:
        value = np_to_java(value)
    dm.setAttribute(path, name, value, unsigned)

def log(log, data_file=None):
    """Writes a log to the system log and data context - if there is an ongoing scan or script execution.

    Args:
         log(str): Log string.
         data_file(bool, optional): if true logs to the data file, in addiction to the system logger.
                                    If None(default) appends to data file only if it exists.

    Returns:
        None
    """
    get_sequencer().scriptingLog(str(log))
    if data_file is None:
        data_file = get_exec_pars().isOpen()
    if data_file:
        try:
            get_data_manager().appendLog(str(log))
        except:
            #Do not generate exception if cannot write to data file
            pass

def set_exec_pars(**args):
    """  Configures the script execution parameters, overriding the system configuration.

    Args:
      args(optional arguments):
        name(str): value of the {name} tag. Default is the running script name.
        type(str): value of the {type} tag. Default is empty.
                             This field can be used to store data in  sub-folders of standard location.
        path(str):  If defined provides the full path name for data output root (overriding config))
                             The tag {data} can be used to enter a path relative to the standard data folder.
        layout(str): Change data layout.
        format(str): Change data format.
        split(scan or True): Split scan data to another table. If set to True in scan command then split every pass.
        depth_dim(int): dimension of 2d-matrixes in 3d datasets.
        save(bool): Change option to auto save scan data.
        flush(bool): Change option to flush file on each record.
        keep(bool): Change option keep scan records in memory. If false do not add records to scan result.
        lazy(bool): Change option for lazy table creation. If true create tables only after first record is received.
        preserve(bool): Change option to preserve device types. If false all values are converted to double.
        setpoints(bool): Save the positioner setpoints too.
        verbose(bool): Enable options to save additional information (output, script).
        compression(obj): True for enabling default compression, int for specifying deflation level.
                          Device or list of devices for specifying devices to be compressed.
        shuffle(obj): True for enabling shuffling before compression.
                      Device or list of devices for specifying devices to be shuffled.
        contiguous(obj): True for setting contiguous datasets for all devices.
                         Device or list of devices for specifying device datasets to be contiguous.
        seq(int): Set next data file sequence number. 
        open(bool): If true create data output path immediately. If false closes output root, if open.
        reset(bool): If true reset the scan counter - the {count} tag and set the timestamp to now.
        group(str): Change layout group name for scans
        tag(str): Change tag for scan names (affecting group or dataset name, according to layout)
        then, then_success, then_exception(str): Sets statement to be executed on the completion of current.
        defaults(bool): If true restore the original execution parameters.

        Graphical preferences:
        line_plots(list): list of devices with enforced line plots.
        range(str or list): "none", "auto", [min_x, max_x]  or [min_x, max_x, min_y, max_y]
        display(bool): if false disables scan data plotting and printing.
        print_scan(bool): Enable/disables scan data printing to console.
        plot_disabled(bool): Enable/disable scan plot
        plot_layout (str):"Horizontal", "Vertical" or "Grid"
        table_disabled(bool): Enable/disable scan table 
        enabled_plots (list of str or Readable): list of devices (Readables) to be plotted
        plot_types(dict):  Dictionary - Plot name(Readable or String) : Plot type(String or int)
        auto_range(bool): If true automatic range scan plots x-axis.
        manual_range(tuple): : Set  range (min_x, max_x) or  (min_x, max_x, min_y, max_y). None sets fixed range.
        manual_range_y(tuple): Set y range (min_y, max_y). None sets fixed range.
        domain_axis(str): Set the domain axis source: "Time", "Index", or a readable name. Default: first positioner.
        status(str): set application status
    """
    get_sequencer().setExecutionPars(args)

def get_exec_pars():
    """ Returns script execution parameters.

    Returns:
        ExecutionParameters object. Fields:
            name (str): execution name - {name} tag.
            type (str): execution type - {type} tag.
            path (str): output data root.
            seq(int): data file sequence number. 
            open (bool): true if the output data root has been opened.
            layout (str): data output layout. If None then using the configuration.
            save (bool): auto save scan data option.
            flush (bool): flush file on each record.
            index (int): current scan index.
            group (str): data group currently used for scan data storage.
                         if no ongoing scan return "/" if within a script, or else None if a console command.
            scanPath (str): dataset or group corresponding to current scan.
            scan (Scan): reference to current scan, if any
            source (CommandSource): return the source of the script or command.
            background (bool): return False if executing in main interpreter thread .
            debug (bool): True if executing from statements in editor.
            simulation (bool): global simulation flag.
            aborted (bool): True if execution has been aborted
    """
    return get_sequencer().getExecutionPars()


###################################################################################################
#EPICS
###################################################################################################

def _adjust_channel_value(value, var_type=None):
    if (value is None):
        return value
    if (var_type is not None):
        if is_list(value):
            var_type = var_type.replace(',','').replace('[','')
            ret = []
            for item in value:
               ret.append(_adjust_channel_value(item), var_type)
            value = ret
        else:
            var_type = var_type.lower()
            if var_type=='b':
                value = byte(value)
            elif var_type=='i':
                value = short(value)
            elif var_type=='l':
                value = int(value)
            elif var_type=='f':
                value = float(value)
            elif var_type=='d':
                value = float(value)
            elif var_type=='s':
                value = str(value)

    if isinstance(value,tuple):
        value =  list(value)
    if isinstance(value,list):
        list_type = type(value[0])
        array_types = {
            int: "i",
            long: "l",
            float:"d",
            str:Class.forName("java.lang.String"),
        }
        array_type = array_types.get(type(value[0]),'d')
        array = PyArray(array_type)
        array.fromlist(value)
        value=array
    return value

def caget(name, type=None, size=None, meta = False ):
    """Reads an Epics PV.

    Args:
        name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
        meta (bool, optional): if true gets channel value and metadata (timestamp, severity).

    Returns:
        PV value if meta is false, otherwise a dictionary containing PV value and metadata
    """
    if meta:
        return Epics.getMeta(name, Epics.getChannelType(type), size)
    return Epics.get(name, Epics.getChannelType(type), size)

def cawait(name, value, timeout=None, comparator=None, type=None, size=None):
    """Wait for a PV to have a given value.

    Args:
        name(str): PV name
        value (obj): value to compare to
        timeout(float, optional): time in seconds to wait. If None, waits forever.
        comparator(java.util.Comparator or float, optional): if None waits for equality.
            If a numeric value is provided, waits for channel to be in range.
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array values: '[b', '[i,', '[l', '[d', '[s'.
         size (int, optional): for arrays, number of elements to be read. Default read all.

    Returns:
        None
    """
    if (timeout is not None):
        timeout = int(timeout*1000)
    value = _adjust_channel_value(value)
    Epics.waitValue(name, value, comparator, timeout, Epics.getChannelType(type), size)

def caput(name, value, timeout = None):
    """Writes to an Epics PV.

    Args:
        name(str): PV name
        value(scalar, string or array): new PV value.
        timeout(int, optional): timeout in seconds to the write. If None waits forever to completion.

    Returns:
        None
    """
    value=_adjust_channel_value(value)
    if (timeout is not None):
        timeout = int(timeout*1000)
    return Epics.put(name, value, timeout)

def caputq(name, value):
    """Writes to an Epics PV and does not wait.

    Args:
        name(str): PV name
        value(scalar, string or array): new PV value.

    Returns:
        None
    """
    value=_adjust_channel_value(value)
    return Epics.putq(name, value)

def camon(name, type=None, size=None, wait = sys.maxsize):
    """Install a monitor to an Epics PV and print value changes.

    Args:
        name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
        wait (int, optional): blocking time for this function. By default blocks forever.
    Returns:
        None
    """ 
    start = time.time()  
    with create_channel_device(name, type=type, size=size, device_name=name, monitored=True) as dev:
        print (dev.read())
        while(True):
            if (wait != sys.maxsize) and ((time.time()-start)>wait):
               break
            if dev.waitCacheChange(100):
                print (dev.take())          

def create_channel_device(channel_name, type=None, size=None, device_name=None, monitored=False):
    """Create a device from an EPICS PV.

    Args:
        channel_name(str): PV name
        type(str, optional): type of PV. By default gets the PV standard field type.
            Scalar values: 'b', 'i', 'l', 'd', 's'.
            Array values: '[b', '[i,', '[l', '[d', '[s'.
        size (int, optional): for arrays, number of elements to be read. Default read all.
        device_name (str, optional): device name (if  different from hannel_name.
    Returns:
        None
    """
    dev = Epics.newChannelDevice(channel_name if (device_name is None) else device_name , channel_name, Epics.getChannelType(type))
    if Context.isSimulation():
        dev.setSimulated()
    dev.initialize()
    if (size is not None):
        dev.setSize(size)
    if (monitored):
        dev.setMonitored(True)
    return dev


###################################################################################################
#Java threading is not possible with as cannot extend Runnable and Callable
###################################################################################################

def fork(*functions):
    """Start execution of functions in parallel.

    Args:
        *functions(function references)

    Returns:
        List of future objects
    """
    raise Exception("Not implemented")

def join(futures):
    """Wait parallel execution of functions.

    Args:
        futures(Future or list of Future) : as returned from fork

    Returns:
        None
    """
    raise Exception("Not implemented")

def parallelize(*functions):
    """Equivalent to fork + join

    Args:
        *functions(function references)

    Returns:
        None
    """
    raise Exception("Not implemented")



###################################################################################################
#Background task control.
###################################################################################################

def start_task(script, delay = 0.0, interval = -1):
    """Start a background task

    Args:
         script(str): Name of the script implementing the task
         delay(float, optional): time in seconds for the first execution.
                Default starts immediately.
         interval(float, optional): time in seconds for between execution.
                If negative (default), single-execution.

    Returns:
        Task object.
    """
    raise Exception("Not implemented")

def stop_task(script, force = False):
    """Stop a background task

    Args:
         script(str): Name of the script implementing the task
         force(boolean, optional): interrupt current execution, if running

    Returns:
        None
    """
    raise Exception("Not implemented")


###################################################################################################
#Versioning
###################################################################################################

def commit(message, force = False):
    """Commit the changes to the repository.

    Args:
        message(str): commit message
        force(bool, optional): if False, raises exception if no change detected in repo

    Returns:
        None
    """
    get_version_control().commit(message, force)

def diff():
    """Return list of changes in the repository

    Args:
        None

    Returns:
        None
    """
    return get_version_control().diff()

def checkout_tag(tag):
    """Checkout a tag name.

    Args:
        tag(str): tag name.

    Returns:
        None
    """
    get_version_control().checkoutTag(tag)

def checkout_branch(tag):
    """Checkout a local branch name.

    Args:
        tag(str): branch name.

    Returns:
        None
    """
    get_version_control().checkoutLocalBranch(tag)

def pull_repository():
    """Pull from remote repository.

    """
    get_version_control().pullFromUpstream()

def push_repository(all_branches=True, force=False, push_tags=False):
    """Push to remote repository.

    Args:
        all_branches(boolean, optional): all branches or just current.
        force(boolean, optional): force flag.
        push_tags(boolean, optional): push tags.

    Returns:
        None
    """
    get_version_control().pushToUpstream(all_branches, force, push_tags)

def cleanup_repository():
    """Performs a repository cleanup.

    Args:
        None

    Returns:
        None
    """
    get_version_control().cleanupRepository()

###################################################################################################
#Device Pool
###################################################################################################

def get_device(device_name):
    """Returns a configured device (or imaging source) by its name.

    Args:
        device_name(str): name of the device.

    Returns:
        device
    """
    return get_device_pool().getByName(device_name)

def add_device(device, force = False):
    """Add  a device (or imaging source) to the device pool.

    Args:
        device(Device or Source)
        force(boolean, optional): if true then dispose existing device with same name.
             Otherwise will fail in case of name clash.

    Returns:
        True if device was added, false if was already in the pool, or exception in case of name clash.
    """
    proxy_method = getattr(device, "get_proxy", None)
    if callable(proxy_method):
        device=device.get_proxy()
    return get_device_pool().addDevice(device, force, True)

def remove_device(device):
    """Remove a device (or imaging source) from the device pool.

    Args:
        device(Device or Source)

    Returns:
        bool: true if device was removed.
    """
    device=string_to_obj(device)
    return get_device_pool().removeDevice(device)

def set_device_alias(device, alias):
    """Deprecated, use "dev.set_alias" instead. Set a device alias to be used in scans (datasets and plots).

    Args:
        device(Device)
        alias(str): replace device name in scans.

    Returns:
        None
    """
    device=string_to_obj(device)
    device.setAlias(alias)

def stop():
    """Stop all devices implementing the Stoppable interface.

    Args:
        None

    Returns:
        None
    """
    get_sequencer().stopAll()

def update():
    """Update all devices.

    Args:
        None

    Returns:
        None
    """
    get_sequencer().updateAll()

def reinit(dev = None):
    """Re-initialize devices.

    Args:
        dev(Device, optional): Device to be re-initialized (if None, all devices not yet initialized)

    Returns:
        List with devices not initialized.
    """
    if dev is not None:
        dev=string_to_obj(dev)
        return get_sequencer().reinit(dev)
    return to_list(get_sequencer().reinit())

def create_device(url, parent=None):
    """Create a device form a definition string(see InlineDevice)

    Args:
        url(str or list of string): the device definition string (or list of strings)
        parent(bool, optional): parent device

    Returns:
        The created device (or list of devices)
    """
    if parent is not None:
        parent=string_to_obj(parent)
    return InlineDevice.create(url, parent)


def create_averager(dev, count, interval=0.0, name = None,  monitored = False):
    """Creates and initializes and averager for dev.

    Args:
        dev(Device): the source device
        count(int): number of samples
        interval(float, optional): sampling interval(s). If negative sampling is made on data change event.
        name(str, optional): sets the name of the device (default is: <dev name> averager)
        monitored (bool, optional): if true then averager processes asynchronously.

    Returns:
        Averager device
    """
    dev = string_to_obj(dev)
    if isinstance(dev, ReadableArray):
        av = ArrayAverager(dev, count, int(interval*1000)) if (name is None) else ArrayAverager(name, dev, count, int(interval*1000))
    else:
        av = Averager(dev, count, int(interval*1000)) if (name is None) else Averager(name, dev, count, int(interval*1000))
    av.initialize()
    if (monitored):
       av.monitored = True
    return av

def tweak(dev, step, is2d=False):
    """Move one or more positioners in steps using the arrow keys. 
       Steps are increased/decreased using the shift and control keys. 

    Args:
        dev(Positioner or List): the device or list of devices to move.
        step(float or List): step size or list of step sizes
        is2d(bool, optional): if true moves second motor with up/down arrows.
    """
    if (get_exec_pars().isBackground()): return
    dev,step = to_list(string_to_obj(dev)),to_list(step)
    while (not (get_exec_pars().getAborted())):
        key=get_sequencer().waitKey(0)
        for i in range(len(dev)):
            if not is2d or i==0:
                if key == 0x25: dev[i].moveRel(-step[i]) #Left
                elif key == 0x27: dev[i].moveRel(step[i]) #Right
            if key in (0x10, 0x11): 
                step[i] = step[i]*2 if key == 0x10 else step[i]/2
                print ("Tweak step for " + dev[i].getName() + " set to: "+str(step[i]))
        if is2d and len(dev)>1: 
            if key == 0x26: dev[1].moveRel(step[1]) #Top
            elif key == 0x28: dev[1].moveRel(-step[1]) #Bottom  


###################################################################################################
#Maths
###################################################################################################

def arrmul(a, b):
    """Multiply 2 series of the same size.

    Args:

        a(subscriptable)
        b(subscriptable)

    Returns:
        List
    """
    return map(mul, a, b)

def arrdiv(a, b):
    """Divide 2 series of the same size.

    Args:

        a(subscriptable)
        b(subscriptable)

    Returns:
        List
    """
    return map(truediv, a, b)

def arradd(a, b):
    """Add 2 series of the same size.

    Args:

        a(subscriptable)
        b(subscriptable)

    Returns:
        List
    """
    return map(add, a, b)

def arrsub(a, b):
    """Subtract 2 series of the same size.

    Args:

        a(subscriptable)
        b(subscriptable)

    Returns:
        List
    """
    return map(sub, a, b)

def arrabs(a):
    """Returns the absolute of all elements in series.

    Args:

        a(subscriptable)

    Returns:
        List
    """
    return map(abs, a)

def arroff(a, value = "mean"):
    """Subtract offset to all elemets in series.

    Args:

        a(subscriptable)
        type(int or str, optional): value to subtract from the array, or "mean" or "min".

    Returns:
        List
    """
    if value=="mean":
        value = mean(a)
    elif value=="min":
        value = min(a)
    return [x-value for x in a]

def mean(data):
    """Calculate the mean of a sequence.

    Args:
        data(subscriptable)

    Returns:
        Mean of the elements in the object.
    """
    return functools.reduce(lambda x, y: x + y, data) / len(data)

def variance(data):
    """Calculate the variance of a sequence.

    Args:
        data(subscriptable)

    Returns:
        Variance of the elements in the object.
    """
    c = mean(data)
    ss = sum((x-c)**2 for x in data)
    return ss/len(data)

def stdev(data):
    """Calculate the standard deviation of a sequence.

    Args:
        data(subscriptable)

    Returns:
        Standard deviation of the elements in the object.
    """
    return variance(data)**0.5


def center_of_mass(data, x = None):
    """Calculate the center of mass of a series, and its rms.

    Args:

        data(subscriptable)
        x(list, tuple, array ..., optional): x coordinates

    Returns:
        Tuple (com, rms)
    """
    if x is None:
        x = Arr.indexesDouble(len(data))
    data_sum = sum(data)
    if (data_sum==0):
        return float('nan')
    xmd = arrmul( x, data)
    com = sum(xmd) / data_sum
    xmd2 = arrmul( x, xmd)
    com2 = sum(xmd2) / data_sum
    rms = math.sqrt(abs(com2 - com * com))
    return (com, rms)

def poly(val, coefs):
    """Evaluates a polinomial: (coefs[0] + coefs[1]*val + coefs[2]*val^2...

    Args:
        val(float): value
        coefs (list of loats): polinomial coefficients
    Returns:
        Evaluated function for val
    """
    r = 0
    p = 0
    for c in coefs:
        r = r + c * math.pow(val, p)
        p = p + 1
    return r

def histogram(data, range_min = None, range_max = None, bin = 1.0):
    """Creates histogram on data.

    Args:
        data (tuple, array, List or Array): input  data can be multi-dimensional or nested.
        range_min (int, optional): minimum histogram value. Default is floor(min(data))
        range_max (int, optional): maximul histogram value. Default is ceil(max(data))
        bin(int or float, optional): if int means number of bins. If float means bin size. Default = 1.0.
    Returns:
        tuple: (ydata, xdata)
    """
    if range_min is None: range_min = math.floor(min(flatten(data)))
    if range_max is None: range_max = math.ceil(max(flatten(data)))
    if type(bin) is float:
        bin_size = bin
        n_bin =  int(math.ceil(float(range_max - range_min)/bin_size))
    else:
        n_bin = bin
        bin_size = float(range_max - range_min)/bin

    result = [0] * n_bin
    for  d in flatten(data):
        b = int( float(d - range_min) / bin_size)
        if (b >=0) and (b < n_bin):
          result[b] = result[b] + 1
    return (result, frange(range_min, range_max, bin_size))

def cmp(a, b):
    return (a > b) - (a < b) 

def _turn(p, q, r):
    return cmp((q[0] - p[0])*(r[1] - p[1]) - (r[0] - p[0])*(q[1] - p[1]), 0)

def _keep(hull, r):
    while len(hull) > 1 and _turn(hull[-2], hull[-1], r) != 1:
        hull.pop()
    return (not len(hull) or hull[-1] != r) and hull.append(r) or hull

def convex_hull(point_list=None, x=None, y=None):
    """Returns the convex hull from a list of points. Either point_list or x,y is provided.
       (Alhorithm taken from http://tomswitzer.net/2010/03/graham-scan/)
    Args:
        point_list (array of tuples, optional): arrays of the points
        x (array of float, optional): array with x coords of points
        y (array of float, optional): array with y coords of points
    Returns:
        Array of points or (x,y)
    """
    is_point_list = point_list is not None
    if not point_list:
        point_list=[]
        for i in range(len(x)):
            if((x[i] is not None) and (y[i] is not None)): point_list.append((x[i], y[i]))
    point_list.sort()
    lh,uh = functools.reduce(_keep, point_list, []), functools.reduce(_keep, reversed(point_list), [])
    ret = lh.extend(uh[i] for i in range(1, len(uh) - 1)) or lh
    if not is_point_list:
        x, y = [], []
        for i in range(len(ret)):
            x.append(ret[i][0])
            y.append(ret[i][1])
        return (x,y)
    return ret

###################################################################################################
#Utilities
###################################################################################################

def get_setting(name=None):
    """Get a persisted script setting value.

    Args:
        name (str): name of the setting.
    Returns:
        String with setting value or None if setting is undefined.
        If name is None then returns map with all settings.
    """
    return Context.getSettings() if (name is None) else Context.getSetting(name)

def set_setting(name, value):
    """Set a persisted script setting value.

    Args:
        name (str): name of the setting.
        value (obj): value for the setting, converted to string (if None then remove the setting).
    Returns:
        None.
    """
    Context.setSetting(name, value)

def exec_cmd(cmd, stderr_raise_ex = True):
    """Executes a shell command. If errors happens raises an exception.

    Args:
        cmd (str or list of str): command process input and parameters. If stderr_raise_ex is set then raise exception if stderr is not null.
    Returns:
        Output of command process.
    """
    import subprocess
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE if stderr_raise_ex else subprocess.STDOUT)
    ret=result.stdout.decode('utf-8')
    err=result.stderr.decode('utf-8')
    if stderr_raise_ex and (err is not None) and err!="":
        raise Exception(err)
    return ret
    
def bsget(channel, modulo=1, offset=0, timeout = 5.0):
    """Reads an values a bsread stream, using the default provider.

    Args:
        channel(str or  list of str): channel name(s)
        module(int, optional): stream modulo
        offset(int, optional): stream offset
        timeout(float, optional): stream timeout in secs
    Returns:
        BS value or list of  values
    """
    channels = to_list(channel)
    ret = Stream.readChannels(channels, modulo, offset, int(timeout * 1000))
    if is_string(channel):
        return ret[0]
    return ret

def flatten(data):
    """Flattens multi-dimentional or nested data.

    Args:
        data (tuple, array, List or Array): input data
    Returns:
        Iterator on the flattened data.
    """
    if is_array(data):
        if not data.typecode.startswith('['):
            return data

    import itertools
    return itertools.chain(*data)

def frange_gen(start, finish, step):
    while ((step >= 0.0) and (start <= finish)) or ((step < 0.0) and (start >= finish)):
      yield start
      start += step

def frange(start, finish, step, enforce_finish = False, inclusive_finish = False):
    """Create a list with a range of float values (a float equivalent to "range").

    Args:
        start(float): start of range.
        finish(float): end of range.
        step(float): step size.
        enforce_finish(boolean, optional): adds the final element even if range was not exact.
        inclusive_finish(boolean, optional): if false finish is exclusive (like in "range").

    Returns:
        list
    """
    step = float(step)
    ret = list(frange_gen(start, finish, step))
    if len(ret) > 0:
        if inclusive_finish == False:
            if ret[-1]==finish:
                del ret[-1]
        if enforce_finish and ret[-1]!=finish:
                ret.append(finish)
    return ret

def notify(subject, text, attachments = None, to=None):
    """Send email message.

    Args:
        subject(str): Message subject.
        text(str): Message body.
        attachments(list of str, optional): list of files to be attached (expansion tokens are allowed).
        to (list ofd str, optional): recipients. If None uses the recipients defined in mail.properties.
    Returns:
        None
    """
    Context.notify(subject, text, to_list(attachments), to_list(to))

def expand_path(path, timestamp=-1):
    """Expand path  containing tokens.

    Args:
        path(str): path name.
        timestamp(int): If not defined(-1), uses now.
    Returns:
        Expanded path name.
    """

    return Setup.expandPath(path, timestamp)

def send_event(name, value=True):
    """Send an interpreter event, which is  propagated as a SSE.

    Args:
        name(str): event name.
        value(Object): event value.
    """
    get_sequencer().sendEvent(name, value)


###################################################################################################
#UI
###################################################################################################

def set_status(status):
    """Set the application status.

    Args:
        status(str): new status.

    Returns:
        None
    """
    set_preference(Preference.STATUS, status)

def setup_plotting( enable_plots=None, enable_table=None,plot_list=None, line_plots=None, range=None, domain=None, defaults=None):
    if defaults == True: set_preference(Preference.DEFAULTS, True)
    if enable_plots is not None: set_preference(Preference.PLOT_DISABLED, not enable_plots)
    if enable_table is not None: set_preference(Preference.TABLE_DISABLED, not enable_table)
    if plot_list is not None: set_preference(Preference.ENABLED_PLOTS, None if plot_list == "all" else plot_list)
    if line_plots is not None:
        plots = None
        if line_plots != "none":
            plots = {}
            for p in line_plots: plots[p]=1
        set_preference(Preference.PLOT_TYPES, plots)
    if range is not None:
         if range == "none": set_preference(Preference.AUTO_RANGE, None)
         elif range == "auto": set_preference(Preference.AUTO_RANGE, True)
         else: set_preference(Preference.MANUAL_RANGE, range)
    if domain is not None: set_preference(Preference.DOMAIN_AXIS, domain)

def set_preference(preference, value):
    """Hints to graphical layer:

    Args:
        preference(Preference): Enum of preference types:
            PLOT_DISABLED: enable/disable scan plot (True/False)
            PLOT_LAYOUT: "Horizontal", "Vertical" or "Grid"
            TABLE_DISABLED: enable/disable scan table (True/False)
            ENABLED_PLOTS: select Readables to be plotted (list of Readable or String (names))
            PLOT_TYPES: Dictionary - Plot name(Readable or String) : Plot type(String or int)
            PRINT_SCAN: Print scan records to console
            AUTO_RANGE: Automatic range scan plots x-axis
            MANUAL_RANGE: Manually set scan plots x-axis
            MANUAL_RANGE_Y: Manually set scan plots y-axis
            DOMAIN_AXIS: Set the domain axis source: "Time", "Index", or a readable name.
                Default(None): first positioner
            STATUS: set application status
        value(object): preference value

    Returns:
        None
    """
    value = to_array(value, 'o') #If list then convert to Object array
    get_sequencer().setPreference(preference, value)

def get_string(msg, default = None, alternatives = None, password = False):
    """
    Reads a string from UI
    Args:
        msg(str): display message.
        default(str, optional): value displayed when window is shown.
        alternatives(list of str, optional): if provided presents a combo box instead of an editing field.
        password(boolean, optional): if True hides entered characters.

    Returns:
        String entered of null if canceled
    """
    if password :
        return get_sequencer().getPassword(msg, None)
    return get_sequencer().getString(msg, str(default) if (default is not None) else None, alternatives)

def get_option(msg, type = "YesNoCancel"):
    """
    Gets an option from UI
    Args:
        msg(str): display message.
        type(str, optional): 'YesNo','YesNoCancel' or 'OkCancel'

    Returns:
        'Yes', 'No', 'Cancel'
    """
    return get_sequencer().getOption(msg, type)

def show_message(msg, title=None, blocking = True):
    """
    Pops a blocking message to UI

    Args:
        msg(str): display message.
        title(str, optional): dialog title
    """
    get_sequencer().showMessage(msg, title, blocking)

def show_panel(device, title=None):
    """
    Show, if exists, the panel relative to this device.

    Args:
        device(Device or str or BufferedImage): device
        title only apply to BufferedImage objects. For devices the title is the device name.
    """
    if isinstance(device, BufferedImage):
        device = DirectSource(title, device)
        device.initialize()
    if is_string(device):
        device = get_device(device)
    return get_sequencer().showPanel(device)

    
###################################################################################################
#Executed on startup
###################################################################################################

if __name__ == "__main__":
    def on_ctrl_cmd(cmd):   
        #print ("Control command: ", cmd)
        pass

    def on_close(parent_thread):   
        on_abort(parent_thread) 

    def on_abort(parent_thread):   
        _default_abort(parent_thread)

    def _default_abort(parent_thread):   
        #This does not work because don't run actually on the main thread
        #import _thread
        #_thread.interrupt_main()

        if _interrupt_sleep_event is not None:
            _interrupt_sleep_event.set()

        tid=parent_thread.ident
        exception = KeyboardInterrupt
        ctypes.pythonapi.PyThreadState_SetAsyncExc(ctypes.c_long(tid), ctypes.py_object(exception))        
 

    _interrupt_sleep_event = None 
    def _sleep(secs):
        global _interrupt_sleep_event
        _interrupt_sleep_event = threading.Event()
        _interrupt_sleep_event.wait(secs)
        _interrupt_sleep_event = None
    sleep = _sleep    
    time.sleep=sleep

    #Handle control command server   
    if ("ctrl_cmd_socket" in globals()) and (ctrl_cmd_socket is not None):
        if ("ctrl_cmd_task_thread" in globals()) and (ctrl_cmd_task_thread.is_alive()):
            ctrl_cmd_socket.close()
            ctrl_cmd_task_thread.join(5.0)
            if ctrl_cmd_task_thread.is_alive():
                raise Exception("Cannot stop control command task thread")

    def ctlm_cmd_task(port,parent_thread, rc):
        try:
            global ctrl_cmd_socket
            get_sequencer().scriptingLog("Starting control command task")
            quit=False
            with socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM) as ctrl_cmd_socket:
                ctrl_cmd_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
                ctrl_cmd_socket.bind(("127.0.0.1", port))
                ctrl_cmd_socket.settimeout(2.0) 
                while(quit==False) and (run_count==rc) and parent_thread.is_alive() and not ctrl_cmd_socket._closed:
                    try:      
                        msg,add = ctrl_cmd_socket.recvfrom(100) 
                    except socket.timeout:
                        continue                    
                    cmd =msg.decode('UTF-8')
                    
                    if cmd=="close":
                        quit=True  
                        on_close(parent_thread)
                    elif cmd=="abort":
                        on_abort(parent_thread)
                    else:
                        on_ctrl_cmd(cmd)
                    ctrl_cmd_socket.sendto("ack".encode('UTF-8'), add)
        finally:
            get_sequencer().scriptingLog("Quitting control command task")

    ctrl_cmd_task_thread = threading.Thread(target=functools.partial(ctlm_cmd_task, CTRL_CMD_PORT, threading.current_thread(), run_count))
    ctrl_cmd_task_thread.daemon = True
    ctrl_cmd_task_thread.start()   
