from builtin_utils import *

import ch.psi.utils.Threading as Threading
import ch.psi.utils.State as State
import ch.psi.utils.Convert as Convert
import ch.psi.utils.Str as Str
import ch.psi.utils.Sys as Sys
import ch.psi.utils.Arr as Arr
import ch.psi.utils.IO as IO
import ch.psi.utils.Chrono as Chrono
import ch.psi.utils.Folder as Folder
import ch.psi.utils.Histogram as Histogram
import ch.psi.utils.History as History
import ch.psi.utils.Condition as Condition
import ch.psi.utils.ArrayProperties as ArrayProperties
import ch.psi.utils.Audio as Audio
import ch.psi.utils.BitMask as BitMask
import ch.psi.utils.Config as Config
import ch.psi.utils.Inventory as Inventory
import ch.psi.utils.DataAPI as DataAPI
import ch.psi.utils.DispatcherAPI as DispatcherAPI
import ch.psi.utils.EpicsBootInfoAPI as EpicsBootInfoAPI
import ch.psi.utils.Mail as Mail
import ch.psi.utils.Posix as Posix
import ch.psi.utils.ProcessFactory as ProcessFactory
import ch.psi.utils.Range as Range
import ch.psi.utils.Reflection as Reflection
import ch.psi.utils.Serializer as Serializer
import ch.psi.utils.Windows as Windows
import ch.psi.utils.NumberComparator as NumberComparator
import ch.psi.utils.BufferConverter as BufferConverter
import ch.psi.utils.Type as Type

import ch.psi.pshell.core.CommandSource as CommandSource
import ch.psi.pshell.core.ContextAdapter as ContextListener
import ch.psi.pshell.core.Context
import ch.psi.pshell.core.InlineDevice as InlineDevice

import ch.psi.pshell.data.DataManager as DataManager
import ch.psi.pshell.data.DataSlice as DataSlice
import ch.psi.pshell.data.PlotDescriptor as PlotDescriptor
import ch.psi.pshell.data.Table as Table
import ch.psi.pshell.data.Provider as  DataProvider
import ch.psi.pshell.data.ProviderHDF5 as  ProviderHDF5
import ch.psi.pshell.data.ProviderText as  ProviderText
import ch.psi.pshell.data.ProviderCSV as  ProviderCSV
import ch.psi.pshell.data.ProviderFDA as  ProviderFDA
import ch.psi.pshell.data.Converter as  DataConverter
import ch.psi.pshell.data.Layout as  Layout
import ch.psi.pshell.data.LayoutBase as LayoutBase
import ch.psi.pshell.data.LayoutDefault as LayoutDefault
import ch.psi.pshell.data.LayoutTable as LayoutTable
import ch.psi.pshell.data.LayoutFDA as LayoutFDA
import ch.psi.pshell.data.LayoutSF as LayoutSF

import ch.psi.pshell.device.Camera as Camera
import ch.psi.pshell.device.CameraBase as CameraBase
import ch.psi.pshell.device.Device as Device
import ch.psi.pshell.device.DeviceBase as DeviceBase
import ch.psi.pshell.device.GenericDevice as GenericDevice 
import ch.psi.pshell.device.DeviceConfig as DeviceConfig
import ch.psi.pshell.device.PositionerConfig as PositionerConfig
import ch.psi.pshell.device.RegisterConfig as RegisterConfig
import ch.psi.pshell.device.ReadonlyProcessVariableConfig as ReadonlyProcessVariableConfig
import ch.psi.pshell.device.ProcessVariableConfig as ProcessVariableConfig
import ch.psi.pshell.device.MotorConfig as MotorConfig
import ch.psi.pshell.device.Register as Register
import ch.psi.pshell.device.RegisterBase as RegisterBase
import ch.psi.pshell.device.ProcessVariableBase as ProcessVariableBase
import ch.psi.pshell.device.ControlledVariableBase as ControlledVariableBase
import ch.psi.pshell.device.PositionerBase as PositionerBase
import ch.psi.pshell.device.MasterPositioner as MasterPositioner
import ch.psi.pshell.device.MotorBase as MotorBase
import ch.psi.pshell.device.DiscretePositionerBase as DiscretePositionerBase
import ch.psi.pshell.device.MotorGroupBase as MotorGroupBase
import ch.psi.pshell.device.MotorGroupDiscretePositioner as MotorGroupDiscretePositioner
import ch.psi.pshell.device.ReadonlyRegisterBase as ReadonlyRegisterBase
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase as ReadonlyAsyncRegisterBase
import ch.psi.pshell.device.Register as Register
import ch.psi.pshell.device.Register.RegisterArray as RegisterArray
import ch.psi.pshell.device.Register.RegisterNumber as RegisterNumber
import ch.psi.pshell.device.Register.RegisterBoolean as RegisterBoolean
import ch.psi.pshell.device.Register.RegisterString as RegisterString
import ch.psi.pshell.device.RegisterCache as RegisterCache
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray as ReadonlyRegisterArray
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix as ReadonlyRegisterMatrix
import ch.psi.pshell.device.DummyPositioner as DummyPositioner
import ch.psi.pshell.device.DummyMotor as DummyMotor
import ch.psi.pshell.device.DummyRegister as DummyRegister
import ch.psi.pshell.device.Timestamp as Timestamp
import ch.psi.pshell.device.Interlock as Interlock
import ch.psi.pshell.device.Cacheable as Cacheable
import ch.psi.pshell.device.Readable as Readable
import ch.psi.pshell.device.Readable.ReadableArray as ReadableArray
import ch.psi.pshell.device.Readable.ReadableMatrix as ReadableMatrix
import ch.psi.pshell.device.Readable.ReadableCalibratedArray as ReadableCalibratedArray
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix as ReadableCalibratedMatrix
import ch.psi.pshell.device.ArrayRegisterStats as ArrayRegisterStats
import ch.psi.pshell.device.DescStatsDouble as DescStatsDouble
import ch.psi.pshell.device.ArrayCalibration as ArrayCalibration
import ch.psi.pshell.device.MatrixCalibration as MatrixCalibration
import ch.psi.pshell.device.Writable as Writable
import ch.psi.pshell.device.Writable.WritableArray as WritableArray
import ch.psi.pshell.device.Stoppable as Stoppable
import ch.psi.pshell.device.Averager as Averager
import ch.psi.pshell.device.ArrayAverager as ArrayAverager
import ch.psi.pshell.device.Delta as Delta
import ch.psi.pshell.device.DeviceAdapter as DeviceListener
import ch.psi.pshell.device.ReadbackDeviceAdapter as ReadbackDeviceListener
import ch.psi.pshell.device.MotorAdapter as MotorListener
import ch.psi.pshell.device.MoveMode as MoveMode
import ch.psi.pshell.device.SettlingCondition as SettlingCondition
import ch.psi.pshell.device.HistogramGenerator as HistogramGenerator

import ch.psi.pshell.epics.Epics as Epics
import ch.psi.pshell.epics.EpicsScan as EpicsScan
import ch.psi.pshell.epics.ChannelSettlingCondition as ChannelSettlingCondition
import ch.psi.pshell.epics.AreaDetector as AreaDetector
import ch.psi.pshell.epics.BinaryPositioner as BinaryPositioner
import ch.psi.pshell.epics.ChannelByte as ChannelByte
import ch.psi.pshell.epics.ChannelByteArray as ChannelByteArray
import ch.psi.pshell.epics.ChannelByteMatrix as ChannelByteMatrix
import ch.psi.pshell.epics.ChannelDouble as ChannelDouble
import ch.psi.pshell.epics.ChannelDoubleArray as ChannelDoubleArray
import ch.psi.pshell.epics.ChannelDoubleMatrix as ChannelDoubleMatrix
import ch.psi.pshell.epics.ChannelFloat as ChannelFloat
import ch.psi.pshell.epics.ChannelFloatArray as ChannelFloatArray
import ch.psi.pshell.epics.ChannelFloatMatrix as ChannelFloatMatrix
import ch.psi.pshell.epics.ChannelInteger as ChannelInteger
import ch.psi.pshell.epics.ChannelIntegerArray as ChannelIntegerArray
import ch.psi.pshell.epics.ChannelIntegerMatrix as ChannelIntegerMatrix
import ch.psi.pshell.epics.ChannelShort as ChannelShort
import ch.psi.pshell.epics.ChannelShortArray as ChannelShortArray
import ch.psi.pshell.epics.ChannelShortMatrix as ChannelShortMatrix
import ch.psi.pshell.epics.ChannelString as ChannelString
import ch.psi.pshell.epics.ControlledVariable as ControlledVariable
import ch.psi.pshell.epics.DiscretePositioner as DiscretePositioner
import ch.psi.pshell.epics.GenericChannel as GenericChannel
import ch.psi.pshell.epics.GenericArray as GenericArray
import ch.psi.pshell.epics.GenericMatrix as GenericMatrix
import ch.psi.pshell.epics.Manipulator as Manipulator
import ch.psi.pshell.epics.Motor as EpicsMotor
import ch.psi.pshell.epics.Positioner as Positioner
import ch.psi.pshell.epics.ProcessVariable as ProcessVariable
import ch.psi.pshell.epics.ReadonlyProcessVariable as ReadonlyProcessVariable
import ch.psi.pshell.epics.Scaler as Scaler
import ch.psi.pshell.epics.Scienta as Scienta
import ch.psi.pshell.epics.Slit as Slit
import ch.psi.pshell.epics.AreaDetectorSource as AreaDetectorSource
import ch.psi.pshell.epics.ArraySource as ArraySource
import ch.psi.pshell.epics.ByteArraySource as ByteArraySource
import ch.psi.pshell.epics.PsiCamera as PsiCamera
import ch.psi.pshell.epics.CAS as CAS

import ch.psi.pshell.serial.SerialPortDevice as SerialPortDevice
import ch.psi.pshell.serial.TcpDevice as TcpDevice
import ch.psi.pshell.serial.UdpDevice as UdpDevice
import ch.psi.pshell.serial.SerialPortDeviceConfig as SerialPortDeviceConfig
import ch.psi.pshell.serial.SocketDeviceConfig as SocketDeviceConfig

import ch.psi.pshell.modbus.ModbusTCP as ModbusTCP
import ch.psi.pshell.modbus.ModbusUDP as ModbusUDP
import ch.psi.pshell.modbus.ModbusSerial as ModbusSerial
import ch.psi.pshell.modbus.AnalogInput as ModbusAI
import ch.psi.pshell.modbus.AnalogInputArray as ModbusMAI
import ch.psi.pshell.modbus.AnalogOutput as ModbusAO
import ch.psi.pshell.modbus.AnalogOutputArray as ModbusMAO
import ch.psi.pshell.modbus.DigitalInput as ModbusDO
import ch.psi.pshell.modbus.DigitalInputArray as ModbusMDI
import ch.psi.pshell.modbus.DigitalOutput as ModbusDO
import ch.psi.pshell.modbus.DigitalOutputArray as ModbusMDO
import ch.psi.pshell.modbus.Register  as ModbusReg
import ch.psi.pshell.modbus.ReadonlyProcessVariable  as ModbusROPV
import ch.psi.pshell.modbus.ProcessVariable as ModbusPV
import ch.psi.pshell.modbus.ControlledVariable  as ModbusCB
import ch.psi.pshell.modbus.ModbusDeviceConfig as ModbusDeviceConfig

import ch.psi.pshell.imaging.Source as Source
import ch.psi.pshell.imaging.SourceBase as SourceBase
import ch.psi.pshell.imaging.DirectSource as DirectSource
import ch.psi.pshell.imaging.RegisterArraySource as RegisterArraySource
import ch.psi.pshell.imaging.RegisterMatrixSource as RegisterMatrixSource
import ch.psi.pshell.imaging.ImageListener as ImageListener
import ch.psi.pshell.imaging.ImageMeasurement as ImageMeasurement
import ch.psi.pshell.imaging.CameraSource as CameraSource
import ch.psi.pshell.imaging.DeviceSource as DeviceSource
import ch.psi.pshell.imaging.ColormapSource as ColormapSource
import ch.psi.pshell.imaging.StreamSource as StreamSource
import ch.psi.pshell.imaging.ColormapAdapter as ColormapAdapter
import ch.psi.pshell.imaging.FileSource as FileSource
import ch.psi.pshell.imaging.MjpegSource as MjpegSource
import ch.psi.pshell.imaging.Webcam as Webcam
import ch.psi.pshell.imaging.Filter as Filter
import ch.psi.pshell.imaging.Utils as ImagingUtils
import ch.psi.pshell.imaging.Overlay as Overlay
import ch.psi.pshell.imaging.Overlays as Overlays
import ch.psi.pshell.imaging.Pen as Pen
import ch.psi.pshell.imaging.Data as Data
import ch.psi.pshell.imaging.Colormap as Colormap
import ch.psi.pshell.imaging.Renderer as Renderer


import ch.psi.pshell.plot.Plot as Plot
import ch.psi.pshell.plot.Plot.AxisId as AxisId
import ch.psi.pshell.plot.LinePlot.Style as LinePlotStyle
import ch.psi.pshell.plot.RangeSelectionPlot as RangeSelectionPlot
import ch.psi.pshell.plot.RangeSelectionPlot.RangeSelectionPlotListener as RangeSelectionPlotListener
import ch.psi.pshell.plot.LinePlot as LinePlot
import ch.psi.pshell.plot.MatrixPlot as MatrixPlot
import ch.psi.pshell.plot.TimePlot as TimePlot
import ch.psi.pshell.plot.SlicePlot as SlicePlot

import ch.psi.pshell.plot.LinePlotJFree as LinePlotJFree
import ch.psi.pshell.plot.MatrixPlotJFree as MatrixPlotJFree
import ch.psi.pshell.plot.TimePlotJFree as TimePlotJFree
import ch.psi.pshell.plot.SlicePlotDefault as SlicePlotDefault

import ch.psi.pshell.plot.LinePlotSeries as LinePlotSeries
import ch.psi.pshell.plot.LinePlotErrorSeries as LinePlotErrorSeries
import ch.psi.pshell.plot.MatrixPlotSeries as MatrixPlotSeries
import ch.psi.pshell.plot.TimePlotSeries as TimePlotSeries
import ch.psi.pshell.plot.SlicePlotSeries as SlicePlotSeries

import ch.psi.pshell.scan.ScanBase as ScanBase
import ch.psi.pshell.scan.LineScan
import ch.psi.pshell.scan.ContinuousScan
import ch.psi.pshell.scan.AreaScan
import ch.psi.pshell.scan.VectorScan
import ch.psi.pshell.scan.ManualScan
import ch.psi.pshell.scan.HardwareScan
import ch.psi.pshell.scan.RegionScan
import ch.psi.pshell.scan.TimeScan
import ch.psi.pshell.scan.MonitorScan
import ch.psi.pshell.scan.BinarySearch
import ch.psi.pshell.scan.HillClimbingSearch
import ch.psi.pshell.scan.ScanResult
import ch.psi.pshell.scan.Otf as Otf
import ch.psi.pshell.scan.ScanCallbacks as ScanCallbacks

import ch.psi.pshell.crlogic.CrlogicPositioner as CrlogicPositioner
import ch.psi.pshell.crlogic.CrlogicSensor as CrlogicSensor

import ch.psi.pshell.scan.ScanAbortedException as ScanAbortedException

import ch.psi.pshell.bs.BsScan
import ch.psi.pshell.bs.Stream as Stream
import ch.psi.pshell.bs.Provider as Provider
import ch.psi.pshell.bs.ProviderConfig.SocketType as SocketType
import ch.psi.pshell.bs.Dispatcher as Dispatcher
import ch.psi.pshell.bs.StreamChannel as StreamChannel
import ch.psi.pshell.bs.Scalar as Scalar
import ch.psi.pshell.bs.Waveform as Waveform
import ch.psi.pshell.bs.Matrix as Matrix
import ch.psi.pshell.bs.StreamCamera as StreamCamera
import ch.psi.pshell.bs.CameraServer as CameraServer
import ch.psi.pshell.bs.PipelineServer as PipelineServer
import ch.psi.pshell.bs.ProviderConfig as ProviderConfig
import ch.psi.pshell.bs.StreamConfig as StreamConfig
import ch.psi.pshell.bs.StreamChannelConfig as StreamChannelConfig
import ch.psi.pshell.bs.WaveformConfig as WaveformConfig
import ch.psi.pshell.bs.MatrixConfig as MatrixConfig

import ch.psi.pshell.camserver.PipelineClient as PipelineClient
import ch.psi.pshell.camserver.CameraClient as CameraClient
import ch.psi.pshell.camserver.ProxyClient as ProxyClient
import ch.psi.pshell.camserver.PipelineStream as PipelineStream
import ch.psi.pshell.camserver.PipelineSource as PipelineSource
import ch.psi.pshell.camserver.CameraStream as CameraStream
import ch.psi.pshell.camserver.CameraSource as CameraSource
import ch.psi.pshell.camserver.CamServerService as CamServerService

import ch.psi.pshell.detector.DetectorConfig as DetectorConfig

import ch.psi.pshell.ui.App as App

import ch.psi.pshell.scripting.ViewPreference as Preference
import ch.psi.pshell.scripting.ScriptUtils as ScriptUtils
import ch.psi.pshell.scripting.ScriptType as ScriptType

from ch.psi.pshell.device.Record import *
from javax.swing.SwingUtilities import invokeLater, invokeAndWait

import org.jfree.ui.RectangleAnchor as RectangleAnchor
import org.jfree.ui.TextAnchor as TextAnchor

import ch.psi.pshell.xscan.ProcessorXScan as ProcessorXScan


def string_to_obj(o):
    if is_string(o):
        o=str(o)
        if "://" in o:
            return InlineDevice(o)
        ret =  get_context().getInterpreterVariable(o)
        if ret is None:
            try:
                return get_context().scriptManager.evalBackground(o).result
            except:                        
                return None
        return ret
    elif is_list(o):
        ret = []
        for i in o:
            ret.append(string_to_obj(i))
        return ret
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

class LineScan(ch.psi.pshell.scan.LineScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class AreaScan(ch.psi.pshell.scan.AreaScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class RegionScan(ch.psi.pshell.scan.RegionScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)
    def onBeforeRegion(self, num): __before_region(self,num)

class VectorScan(ch.psi.pshell.scan.VectorScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class ContinuousScan(ch.psi.pshell.scan.ContinuousScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class TimeScan(ch.psi.pshell.scan.TimeScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class MonitorScan(ch.psi.pshell.scan.MonitorScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class BsScan(ch.psi.pshell.bs.BsScan):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)
    def onBeforePass(self, num): __before_pass(self, num)
    def onAfterPass(self, num): __after_pass(self, num)

class ManualScan (ch.psi.pshell.scan.ManualScan):
    def __init__(self, writables, readables, start = None, end = None, steps = None, relative = False, dimensions = None, **pars):
        ch.psi.pshell.scan.ManualScan.__init__(self, writables, readables, start, end, steps, relative)
        self._dimensions = dimensions
        processScanPars(self, pars)

    def append(self,setpoints, positions, values, timestamps=None):
        ch.psi.pshell.scan.ManualScan.append(self, to_array(setpoints), to_array(positions), to_array(values), None if (timestamps is None) else to_array(timestamps))

    def getDimensions(self):
        if self._dimensions == None:
            return ch.psi.pshell.scan.ManualScan.getDimensions(self)
        else:
            return self._dimensions

def getHardwareScanClass(config):
    cls = Class.forName(config["class"])
    class HardwareScan(cls):
        def __init__(self, config, writable, readables, start, end, stepSize, passes, zigzag):
            cls.__init__(self, config, writable, readables, start, end, stepSize, passes, zigzag)
        def onAfterReadout(self, record):
            __after_readout(self, record)
        def onBeforePass(self, num_pass):
            __before_pass(self, num_pass)
        def onAfterPass(self, num_pass):
            __after_pass(self, num_pass)
    return HardwareScan


class BinarySearch(ch.psi.pshell.scan.BinarySearch):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)

class HillClimbingSearch(ch.psi.pshell.scan.HillClimbingSearch):
    def onBeforeReadout(self, pos): __before_readout(self, pos)
    def onAfterReadout(self, rec): __after_readout(self, rec)

def processScanPars(scan, pars):
    scan.before_read = pars.pop("before_read",None)
    scan.after_read = pars.pop("after_read",None)
    scan.before_pass = pars.pop("before_pass",None)
    scan.after_pass =  pars.pop("after_pass",None)
    scan.before_region= pars.pop("before_region",None)
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
    get_context().setCommandPars(scan, pars)



###################################################################################################
#Simple EPICS Channel abstraction
###################################################################################################

def create_channel(name, type=None, size=None):
    return Epics.newChannel(name, Epics.getChannelType(type), size)

#Not using finalizer: closing channels in garbage collection generate errors
class Channel(java.beans.PropertyChangeListener, Writable, Readable, DeviceBase):
    def __init__(self, channel_name, type = None, size = None, callback=None, alias = None, monitored=None, name = None):
        """ Create an object that encapsulates an Epics PV connection.
        Args:
            channel_name(str):name of the channel
            type(str, optional): type of PV. By default gets the PV standard field type.
                Scalar values: 'b', 'i', 'l', 'd', 's'.
                Array values: '[b', '[i,', '[l', '[d', '[s'.
            size(int, optional): the size of the channel
            callback(function, optional): The monitor callback.
            alias(str): name to be used on scans.
        """
        super(DeviceBase, self).__init__(name if (name is not None) else channel_name.replace(":","_").replace(".","_"))
        self.channel = create_channel(channel_name, type, size)
        self.callback = callback
        self._alias = alias
        if monitored is not None:self.setMonitored(monitored)
        self.initialize()

    def get_channel_name(self):
        """Return the name of the channel.
        """
        return self.channel.name

    def get_size(self):
        """Return the size of the channel.
        """
        return self.channel.size

    def set_size(self, size):
        """Set the size of the channel.
        """
        self.channel.size = size

    def is_connected(self):
        """Return True if channel is connected.
        """
        return self.channel.connected

    def doSetMonitored(self, value):
        self.channel.monitored = value
        if (value):
            self.channel.addPropertyChangeListener(self)
        else:
            self.channel.removePropertyChangeListener(self)    
        
        
    def is_monitored(self):
        """Return True if channel is monitored
        """
        return self.channel.monitored

    def set_monitored(self, value):
        """Set a channel monitor to trigger the callback function defined in the constructor.
        """
        self.setMonitored(value)

    def propertyChange(self, pce):
        if pce.getPropertyName() == "value":
            value=pce.getNewValue()
            self.setCache(value, None)        
            if self.callback is not None:
                self.callback(value)

    def put(self, value, timeout=None):
        """Write to channel and wait value change. In the case of a timeout throws a TimeoutException.
        Args:
            value(obj): value to be written
            timeout(float, optional): timeout in seconds. If none waits forever.
        """
        if (timeout==None):
            self.channel.setValue(value)
        else:
            self.channel.setValueAsync(value).get(int(timeout*1000), java.util.concurrent.TimeUnit.MILLISECONDS)
        self.setCache(value, None)

    def putq(self, value):
        """Write to channel and don't wait.
        """
        self.channel.setValueNoWait(value)

    def get(self, force = False):
        """Get channel value.
        """
        ret = self.channel.getValue(force)
        self.setCache(ret, None)
        return ret

    def wait_for_value(self, value, timeout=None, comparator=None):
        """Wait channel to reach a value, using a given comparator. In the case of a timeout throws a TimeoutException.
        Args:
            value(obj): value to be verified.
            timeout(float, optional): timeout in seconds. If None waits forever.
            comparator (java.util.Comparator, optional). If None, uses Object.equals.
        """
        if comparator is None:
            if timeout is None:
                self.channel.waitForValue(value)
            else:
                self.channel.waitForValue(value, int(timeout*1000))
            self.setCache(value, None)
        else:
            if timeout is None:
                self.channel.waitForValue(value, comparator)
            else:
                self.channel.waitForValue(value, comparator, int(timeout*1000))

    def doUpdate(self):
        self.get()
        
    def close(self):
        """Close the channel.
        """
        Epics.closeChannel(self.channel)

    def setAlias(self, alias):
        self._alias = alias

    def getAlias(self):
        return self._alias if self._alias else self.getName()

    def write(self, value):
        self.put(value)

    def read(self):
        return self.get()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()


###################################################################################################
#  Command API implementation through EPICS channels 
###################################################################################################


class EpicsCmdAPI(RegisterBase, RegisterArray):
    def __init__(self, name, channel, as_string=True, background=False, max_size=10000):
        RegisterBase.__init__(self, name)
        self.channel=channel
        self.background=background
        self.as_string=as_string
        self.val = "INIT"
        self.debug=False
        self.max_size = 10000
        self.cas = None

    def doInitialize(self):   
        super(EpicsCmdAPI, self).doInitialize()
        if self.as_string:
            self.cas = CAS(self.channel, self, 'string')
        else:
            self.cas = CAS(self.channel, self, 'byte')
        self.val = "READY"
            
    def doClose(self):
        if self.cas:
            self.cas.close()
            self.cas = None
        RegisterBase.doInitialize(self)         

    def getSize(self):
        if self.as_string:
            return 1            
        else:
            return self.max_size #len(self.val)
        
    def doRead(self):
        if self.debug:
            print "READ: ", self.val
        if self.as_string:
            return self.val
        else:
            return string_to_list(self.val)
        
    def doWrite(self, val):
        self.val = "RUNNING"
        try:
            if self.as_string:
                cmd = str(val[0])
            else:
                cmd = list_to_string(val)
            if self.debug:            
                print "WRITE: ", cmd
            class eval_callback(BiFunction):
                def apply(self_callback, ret, ex):
                    try:
                        if ex is not None:
                            err=ex.message
                            if "Exception:" in err:
                                err = err[err.index("Exception:")+10:].strip()
                            self.val = "ERR:" + err  
                        else:
                            self.val = "RET:" + str(ret)  
                        self.val = self.val[0:self.max_size]
                    except:
                        err=str(sys.exc_info()[1])
                        self.val = "EXC: " + err
                    if self.debug:
                        print self.val
                                            
            #self.val = cmd      
            if self.background:     
                get_context().evalLineBackgroundAsync(cmd).handle(eval_callback())
            else:
                get_context().evalLineAsync(cmd).handle(eval_callback())
            
        except:
            err=str(sys.exc_info()[1])
            if "Exception:" in err:
                err = err[err.index("Exception:")+10:].strip()
            self.val = "EXC: " + err
            self.val = self.val[0:self.max_size]
            if self.debug:
                print self.val        