""" @package cadefs
contstants and enumerated conststant

This defines constants  and classes useful to inteprete code returned from CA library.
"""
#
CA_OP_GET = 0
CA_OP_PUT = 1
CA_OP_CREATE_CHANNEL = 2
CA_OP_ADD_EVENT = 3
CA_OP_CLEAR_EVENT = 4
CA_OP_OTHER = 5
# used with connection callbacks
CA_OP_CONN_UP = 6
CA_OP_CONN_DOWN = 7
# imported from caeventmask.h
DBE_VALUE =(1<<0)
DBE_LOG   =(1<<1)
DBE_ALARM =(1<<2)
DBE_PROPERTY=(1<<3)
# also chekc ECA_IODONE/ECA_IOINPROGRESS in caError.py
IODONE = 42
IOINPROGRESS = 43
#
DBF_NATIVE=-1
DBF_STRING=0
DBF_INT = 1
DBF_SHORT =1
DBF_FLOAT =2
DBF_ENUM  =3
DBF_CHAR  =4
DBF_LONG  = 5
DBF_DOUBLE = 6
DBF_NO_ACCES = 	7
LAST_TYPE = DBF_DOUBLE

def VALID_DB_FIELD(x):
    return ((x >= 0) and (x <= LAST_TYPE))
def INVALID_DB_FIELD(x):
    return ((x < 0) or (x > LAST_TYPE))

#/* data request buffer types */
DBR_NATIVE= DBF_NATIVE
DBR_STRING = 	DBF_STRING
DBR_INT	 =	DBF_INT
DBR_SHORT = DBF_INT
DBR_FLOAT = DBF_FLOAT
DBR_ENUM = DBF_ENUM
DBR_CHAR = DBF_CHAR
DBR_LONG = DBF_LONG
DBR_DOUBLE = DBF_DOUBLE
DBR_STS_STRING = 7
DBR_STS_SHORT = 8
DBR_STS_INT = DBR_STS_SHORT
DBR_STS_FLOAT = 9
DBR_STS_ENUM = 10
DBR_STS_CHAR = 11
DBR_STS_LONG = 12
DBR_STS_DOUBLE = 13
DBR_TIME_STRING = 14
DBR_TIME_INT = 15
DBR_TIME_SHORT = 15
DBR_TIME_FLOAT = 16
DBR_TIME_ENUM = 17
DBR_TIME_CHAR = 18
DBR_TIME_LONG = 19
DBR_TIME_DOUBLE = 20
DBR_GR_STRING = 21
DBR_GR_SHORT = 22
DBR_GR_INT = DBR_GR_SHORT
DBR_GR_FLOAT = 23
DBR_GR_ENUM = 24
DBR_GR_CHAR = 25
DBR_GR_LONG = 26
DBR_GR_DOUBLE = 27
DBR_CTRL_STRING = 28
DBR_CTRL_SHORT = 	29
DBR_CTRL_INT = 	DBR_CTRL_SHORT
DBR_CTRL_FLOAT = 30
DBR_CTRL_ENUM = 31
DBR_CTRL_CHAR = 32
DBR_CTRL_LONG = 33
DBR_CTRL_DOUBLE	 = 34
DBR_PUT_ACKT = 	DBR_CTRL_DOUBLE + 1
DBR_PUT_ACKS = DBR_PUT_ACKT + 1
DBR_STSACK_STRING = DBR_PUT_ACKS + 1
LAST_BUFFER_TYPE = DBR_STSACK_STRING

def VALID_DB_REQ(x):
    return ((x >= 0) and (x <= LAST_BUFFER_TYPE))
def INVALID_DB_REQ(x):
    return ((x < 0) or (x > LAST_BUFFER_TYPE))

class AlarmSeverity:
    """Alarm Severity class

    AlarmSeverity class is provided to keep constants representing EPICS channel severity status.
    It also keeps strings and colors for each severity states.
    """
    NO_ALARM  =0x0
    MINOR_ALARM=0x1
    MAJOR_ALARM=0x2
    INVALID_ALARM=0x3
    ALARM_NSEV=INVALID_ALARM+1
    Strings=(
	"NO_ALARM",
	"MINOR",
	"MAJOR",
	"INVALID",
        )
    Colors=("green","yellow","red","grey")

class AlarmStatus:
    """!
    AlarmStatus class provides constants returned by EPICS Channe Access library as channel status code.
    It also gives you strings for corresponding channel status.
    """
    NO_ALARM = 0
    READ_ALARM = 1
    WRITE_ALARM = 2
    #/* ANALOG ALARMS */
    HIHI_ALARM = 3
    HIGH_ALARM = 4
    LOLO_ALARM = 5
    LOW_ALARM = 6
    #/* BINARY ALARMS */
    STATE_ALARM = 7
    COS_ALARM = 8
    #/* other alarms */
    COMM_ALARM = 9
    TIMEOUT_ALARM = 10
    HW_LIMIT_ALARM = 11
    CALC_ALARM = 12
    SCAN_ALARM = 13
    LINK_ALARM = 14
    SOFT_ALARM = 15
    BAD_SUB_ALARM = 16
    UDF_ALARM = 17
    DISABLE_ALARM = 18
    SIMM_ALARM = 19
    READ_ACCESS_ALARM = 20
    WRITE_ACCESS_ALARM = 21
    Strings=(
        "NO_ALARM",
        "READ",
        "WRITE",
        "HIHI",
        "HIGH",
        "LOLO",
        "LOW",
        "STATE",
        "COS",
        "COMM",
        "TIMEOUT",
        "HWLIMIT",
        "CALC",
        "SCAN",
        "LINK",
        "SOFT",
        "BAD_SUB",
        "UDF",
        "DISABLE",
        "SIMM",
        "READ_ACCESS",
        "WRITE_ACCESS",
        )


# ch_state={cs_never_conn=0, cs_prev_conn, cs_conn, cs_closed}

cs_never_conn= 0
cs_prev_conn = 1
cs_conn      = 2
cs_closed    = 3

class ch_state:
    """
    ch_state class provides constants representing channel connection status.
    """
    cs_never_conn= 0
    cs_prev_conn = 1
    cs_conn      = 2
    cs_closed    = 3
    Strings=(
        "channel never connected",
        "channel previously connected",
        "channel connected",
        "channel already closed",
        )
