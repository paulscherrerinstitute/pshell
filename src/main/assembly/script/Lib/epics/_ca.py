import sys
import traceback
import java.lang.System
import java.lang.Thread
import java.lang.InterruptedException
import gov.aps.jca.CAStatus
import gov.aps.jca.JCALibrary
import gov.aps.jca.configuration.DefaultConfiguration
import gov.aps.jca.dbr.DBRType
import gov.aps.jca.dbr.Severity
import gov.aps.jca.event.PutListener
import gov.aps.jca.event.GetListener
import gov.aps.jca.event.MonitorListener
import ch.psi.jcae.impl.JcaeProperties

version = "PShell wrapper"
release = "1.0.0"
revision = "1.0.0"
error = Exception

def dbf_type_is_valid(type):
    return (type >= 0) and (type <= LAST_TYPE)
def dbr_type_is_valid(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE)
def dbr_type_is_plain(type):
    return (type >= DBR_STRING) and (type <= DBR_DOUBLE)
def dbr_type_is_STS(type):
    return (type >= DBR_STS_STRING) and (type <= DBR_STS_DOUBLE)
def dbr_type_is_TIME(type):
    return (type >= DBR_TIME_STRING) and (type <= DBR_TIME_DOUBLE)
def dbr_type_is_GR(type):
    return (type >= DBR_GR_STRING) and (type <= DBR_GR_DOUBLE)
def dbr_type_is_CTRL(type):
    return (type >= DBR_CTRL_STRING) and (type <= DBR_CTRL_DOUBLE)
def dbr_type_is_STRING(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_STRING)
def dbr_type_is_SHORT(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_SHORT)
def dbr_type_is_FLOAT(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_FLOAT)
def dbr_type_is_ENUM(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_ENUM)
def dbr_type_is_CHAR(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_CHAR)
def dbr_type_is_LONG(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_LONG)
def dbr_type_is_DOUBLE(type):
    return (type >= 0) and (type <= LAST_BUFFER_TYPE) and (type%(LAST_TYPE+1) == DBR_DOUBLE)

#I am assuming have JCAE, but it is possible to implement over JCA only, prividing the configuration
properties  = ch.psi.jcae.impl.JcaeProperties.getInstance()
jca= gov.aps.jca.JCALibrary.getInstance()
context = None
configuration = gov.aps.jca.configuration.DefaultConfiguration("jython ca context")
configuration.setAttribute("class", gov.aps.jca.JCALibrary.CHANNEL_ACCESS_JAVA)
configuration.setAttribute("addr_list", properties.getAddressList())
configuration.setAttribute("auto_addr_list", str(properties.isAutoAddressList()));
if properties.getMaxArrayBytes() is not None:
    configuration.setAttribute("max_array_bytes", properties.getMaxArrayBytes())
if properties.getServerPort() is not None:
    configuration.setAttribute("server_port", properties.getServerPort())

def initialize():    
    global jca
    global context
    global configuration

    if context is not None:
        context.destroy()

    context= jca.createContext(configuration)

initialize()


class PutListener(gov.aps.jca.event.PutListener):
    def __init__(self, callback):
        self.callback = callback
    def putCompleted(self, put_ev):
        if  put_ev is None:
            self.callback(None , None , None)
        else:
            count = put_ev.getCount() 
            status = put_ev.getStatus() 
            dbr_type = put_ev.getType()        
            self.callback([count, status, dbr_type]) #TODO: Check these: status must be second par

def formatCbArgs(status, dbr):
    dbrType = dbr.getType()
    if status <> gov.aps.jca.CAStatus.NORMAL:
        cb_args=[None, status.getValue()]
    else:
        try:
            val = dbr.getValue()
            if val is not None:
                val = val.tolist()
                if len(val) == 1:
                    val = val[0]
            if dbr.isSTS():
                cb_args = [val, dbr.getSeverity().getValue(), status.getValue()]
            else:
                cb_args = [val, gov.aps.jca.dbr.Severity.NO_ALARM, status.getValue()]
            if dbr.isTIME():
                timestamp = dbr.getTimeStamp()
                cb_args.append( timestamp.nsec() if (timestamp is not None) else 0.0)
            else:
                cb_args.append(0.0)        
            if dbr.isENUM():
                cb_args.append([None, None]) #TODO
            elif dbr.isGR():
                gr=[dbr.getUnits(), dbr.getUpperDispLimit(), dbr.getLowerDispLimit(), 
                    dbr.getUpperAlarmLimit(), dbr.getUpperWarningLimit(), 
                    dbr.getLowerAlarmLimit(), dbr.getLowerWarningLimit()]
                if (dbr.isCTRL()):
                    gr.append(dbr.getUpperCtrlLimit())
                    gr.append(dbr.getLowerCtrlLimit())
                if (dbr.isPRECSION()):
                    gr.append(dbr.getPrecision())
                cb_args.append(gr)        
        except:
            traceback.print_exc(file=sys.stderr)
            cb_args=[None, None]            
    return cb_args


class GetListener(gov.aps.jca.event.GetListener):
    def __init__(self, callback):
        self.callback = callback
    def getCompleted(self, get_ev):
        status = get_ev.getStatus() 
        dbr = get_ev.getDBR()   
        self.callback(formatCbArgs(status, dbr))

class MonitorListener(gov.aps.jca.event.MonitorListener):
    def __init__(self, callback):
        self.callback = callback
    def monitorChanged(self, monitor_ev):        
        status = monitor_ev.getStatus() 
        dbr = monitor_ev.getDBR()   
        self.callback(formatCbArgs(status, dbr))
        #print dbr.getValue()

def search(name, callback): #returns channel
    ch= context.createChannel(name)
    context.pendIO(1.0)
    return ch    

def name(channel):
    return channel.getName() 

def clear(channel):
    channel.destroy() 
    flush()

def put(channel, val, not_used, put_callback, req_type):
    type = gov.aps.jca.dbr.DBRType.forValue(req_type) 
    if put_callback is not None:
        listener = PutListener(put_callback)
        channel.put(val, listener)
    else:
        channel.put(val)
    flush()  

def get(channel, get_callback, req_type, count, *args):
    listener = GetListener(get_callback)
    type = gov.aps.jca.dbr.DBRType.forValue(req_type) 
    channel.get(type, count, listener)
    flush() 


def monitor(channel, event_callback, count, mask, req_type, *args):
    listener = MonitorListener(event_callback)
    type = gov.aps.jca.dbr.DBRType.forValue(req_type)
    monitor = channel.addMonitor(type, count, mask, listener) 
    flush()
    return monitor

def clear_monitor(event_id):
    event_id.removeMonitorListener(event_id.getMonitorListener())
    flush()

def ch_info(channel):
    return (channel.getFieldType().getValue(), channel.getElementCount() ,
         None, channel.getConnectionState().getValue(), channel.getHostName(), 
         channel.getReadAccess() , channel.getWriteAccess())


def pend_io(timeout):
    context.pendIO(timeout)
    _checkInterrupted()
    return 0 #for OK


def pend_event(timeout):
    context.pendEvent(timeout) 
    _checkInterrupted()
    return 80 #C library always returns  ECA_TIMEOUT
    
def poll():
    return pend_event(1e-12)

def flush():
    context.flushIO()
    _checkInterrupted()
    return 0


def _checkInterrupted():    
    java.lang.Thread.currentThread().sleep(0)
    if java.lang.Thread.currentThread().isInterrupted():
        raise java.lang.InterruptedException()

