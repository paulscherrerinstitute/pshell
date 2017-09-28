
from _ca import *
from cadefs import *
import caError

def alarmSeverityString(sevr):
    try:
        return AlarmSeverity.Strings[sevr]
    except:
        return "Unkown Severity"

def alarmStatusString(status):
    try:
        return AlarmStatus.Strings[status]
    except:
        return "Unknown Alarm"

def message(status):
    try:
        return caError._caErrorMsg[caError.CA_EXTRACT_MSG_NO(status)]
    except:
        return str(status)

def dbf_type_is_valid(dbftype):
    return dbftype >= 0 and dbftype <= LAST_TYPE
def dbr_type_is_valid(dbrtype):
    return dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE

def dbr_type_is_plain(dbrtype):
    return (dbrtype >= DBR_STRING and dbrtype <= DBR_DOUBLE)
def dbr_type_is_STS(dbrtype):
    return (dbrtype >= DBR_STS_STRING and dbrtype <= DBR_STS_DOUBLE)
def dbr_type_is_TIME(dbrtype):
    return (dbrtype >= DBR_TIME_STRING and dbrtype <= DBR_TIME_DOUBLE)
def dbr_type_is_GR(dbrtype):
    return (dbrtype >= DBR_GR_STRING and dbrtype <= DBR_GR_DOUBLE)
def dbr_type_is_CTRL(dbrtype):
    return (dbrtype >= DBR_CTRL_STRING and dbrtype <= DBR_CTRL_DOUBLE)

def dbr_type_is_STRING(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_STRING)
def dbr_type_is_SHORT(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_SHORT)
def dbr_type_is_FLOAT(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_FLOAT)
def dbr_type_is_ENUM(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_ENUM)
def dbr_type_is_CHAR(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_CHAR)
def dbr_type_is_LONG(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_LONG)
def dbr_type_is_DOUBLE(dbrtype):
    return (dbrtype >= 0 and dbrtype <= LAST_BUFFER_TYPE and
         dbrtype%(LAST_TYPE+1) == DBR_DOUBLE)

def dbf_type_to_DBR(dbftype):
    if dbftype>=0 and dbftype <= LAST_TYPE:
        return dbftype
    else:
        return -1

def dbf_type_to_DBR_STS(dbftype):
    if dbftype>=0 and dbftype <= LAST_TYPE:
        return dbftype + LAST_TYPE+1
    else:
        return -1

def dbf_type_to_DBR_TIME(dbftype):
    if dbftype>=0 and dbftype <= LAST_TYPE:
        return dbftype + (LAST_TYPE+1)*2
    else:
        return -1
def dbf_type_to_DBR_GR(dbftype):
    if dbftype>=0 and dbftype <= LAST_TYPE:
        return dbftype + (LAST_TYPE+1)*3
    else:
        return -1
def dbf_type_to_DBR_CTRL(dbftype):
    if dbftype>=0 and dbftype <= LAST_TYPE:
        return dbftype + (LAST_TYPE+1)*4
    else:
        return -1
