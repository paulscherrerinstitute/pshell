import sys
if sys.hexversion >= 0x03000000:
    intern = sys.intern

from ca import _ca
class caError(_ca.error):
  """ EPICS ca.py Errors"""
  pass

__caErrorMsg=(
"Normal successful completion",
"Maximum simultaneous IOC connections exceeded",
"Unknown internet host",
"Unknown internet service",
"Unable to allocate a new socket",
"Unable to connect to internet host or service",
"Unable to allocate additional dynamic memory",
"Unknown IO channel",
"Record field specified inappropriate for channel specified",
"The requested data transfer is greater than available memory or EPICS_CA_MAX_ARRAY_BYTES",
"User specified timeout on IO operation expired",
"Sorry, that feature is planned but not supported at this time",
"The supplied string is unusually large",
"The request was ignored because the specified channel is disconnected",
"The data type specifed is invalid",
"Remote Channel not found",
"Unable to locate all user specified channels",
"Channel Access Internal Failure",
"The requested local DB operation failed",
"Channel read request failed",
"Channel write request failed",
"Channel subscription request failed",
"Invalid element count requested",
"Invalid string",
"Virtual circuit disconnect",
"Identical process variable names on multiple servers",
"Request inappropriate within subscription (monitor) update callback",
"Database value get for that channel failed during channel search",
"Unable to initialize without the vxWorks VX_FP_TASK task option set",
"Event queue overflow has prevented first pass event after event add",
"Bad event subscription (monitor) identifier",
"Remote channel has new network address",
"New or resumed network connection",
"Specified task isnt a member of a CA context",
"Attempt to use defunct CA feature failed",
"The supplied string is empty",
"Unable to spawn the CA repeater thread- auto reconnect will fail",
"No channel id match for search reply- search reply ignored",
"Reseting dead connection- will try to reconnect",
"Server (IOC) has fallen behind or is not responding- still waiting",
"No internet interface with broadcast available",
"Invalid event selection mask",
"IO operations have completed",
"IO operations are in progress",
"Invalid synchronous group identifier",
"Put callback timed out",
"Read access denied",
"Write access denied",
"Requested feature is no longer supported",
"Empty PV search address list",
"No reasonable data conversion between client and server types",
"Invalid channel identifier",
"Invalid function pointer",
"Thread is already attached to a client context",
"Not supported by attached service",
"User destroyed channel",
"Invalid channel priority",
"Preemptive callback not enabled - additional threads may not join context",
"Client's protocol revision does not support transfers exceeding 16k bytes",
"Virtual circuit connection sequence aborted",
"Virtual circuit unresponsive",
)
_caErrorMsg=map(intern,__caErrorMsg)
if sys.hexversion >= 0x03000000:
    _caErrorMsg = list(_caErrorMsg)

ErrCode2Class={}
class PyCa_NoCallback(caError):
    __doc__="Null callback routine"
CA_M_MSG_NO    = 0x0000FFF8
CA_M_SEVERITY  = 0x00000007
CA_M_LEVEL     = 0x00000003
CA_M_SUCCESS   = 0x00000001
CA_M_ERROR     = 0x00000002
CA_M_SEVERE    = 0x00000004
CA_S_MSG_NO= 0x0D
CA_S_SEVERITY=0x03
CA_V_MSG_NO=     0x03
CA_V_SEVERITY=   0x00
CA_V_SUCCESS=    0x00

def CA_EXTRACT_MSG_NO(code): return ( ( (code) & CA_M_MSG_NO )  >> CA_V_MSG_NO )
def CA_EXTRACT_SEVERITY(code): return ( ( (code) & CA_M_SEVERITY )    >> CA_V_SEVERITY) 
def CA_EXTRACT_SUCCESS(code): ( ( (code) & CA_M_SUCCESS )     >> CA_V_SUCCESS )

class ECA_NORMAL(caError):
    __doc__=_caErrorMsg[0]
    __errcode__=1

ErrCode2Class[1]=ECA_NORMAL

class ECA_MAXIOC(caError):
    __doc__=_caErrorMsg[1]
    __errcode__=10

ErrCode2Class[10]=ECA_MAXIOC

class ECA_UKNHOST(caError):
    __doc__=_caErrorMsg[2]
    __errcode__=18

ErrCode2Class[18]=ECA_UKNHOST

class ECA_UKNSERV(caError):
    __doc__=_caErrorMsg[3]
    __errcode__=26

ErrCode2Class[26]=ECA_UKNSERV

class ECA_SOCK(caError):
    __doc__=_caErrorMsg[4]
    __errcode__=34

ErrCode2Class[34]=ECA_SOCK

class ECA_CONN(caError):
    __doc__=_caErrorMsg[5]
    __errcode__=40

ErrCode2Class[40]=ECA_CONN

class ECA_ALLOCMEM(caError):
    __doc__=_caErrorMsg[6]
    __errcode__=48

ErrCode2Class[48]=ECA_ALLOCMEM

class ECA_UKNCHAN(caError):
    __doc__=_caErrorMsg[7]
    __errcode__=56

ErrCode2Class[56]=ECA_UKNCHAN

class ECA_UKNFIELD(caError):
    __doc__=_caErrorMsg[8]
    __errcode__=64

ErrCode2Class[64]=ECA_UKNFIELD

class ECA_TOLARGE(caError):
    __doc__=_caErrorMsg[9]
    __errcode__=72

ErrCode2Class[72]=ECA_TOLARGE

class ECA_TIMEOUT(caError):
    __doc__=_caErrorMsg[10]
    __errcode__=80

ErrCode2Class[80]=ECA_TIMEOUT

class ECA_NOSUPPORT(caError):
    __doc__=_caErrorMsg[11]
    __errcode__=88

ErrCode2Class[88]=ECA_NOSUPPORT

class ECA_STRTOBIG(caError):
    __doc__=_caErrorMsg[12]
    __errcode__=96

ErrCode2Class[96]=ECA_STRTOBIG

class ECA_DISCONNCHID(caError):
    __doc__=_caErrorMsg[13]
    __errcode__=106

ErrCode2Class[106]=ECA_DISCONNCHID

class ECA_BADTYPE(caError):
    __doc__=_caErrorMsg[14]
    __errcode__=114

ErrCode2Class[114]=ECA_BADTYPE

class ECA_CHIDNOTFND(caError):
    __doc__=_caErrorMsg[15]
    __errcode__=123

ErrCode2Class[123]=ECA_CHIDNOTFND

class ECA_CHIDRETRY(caError):
    __doc__=_caErrorMsg[16]
    __errcode__=131

ErrCode2Class[131]=ECA_CHIDRETRY

class ECA_INTERNAL(caError):
    __doc__=_caErrorMsg[17]
    __errcode__=142

ErrCode2Class[142]=ECA_INTERNAL

class ECA_DBLCLFAIL(caError):
    __doc__=_caErrorMsg[18]
    __errcode__=144

ErrCode2Class[144]=ECA_DBLCLFAIL

class ECA_GETFAIL(caError):
    __doc__=_caErrorMsg[19]
    __errcode__=152

ErrCode2Class[152]=ECA_GETFAIL

class ECA_PUTFAIL(caError):
    __doc__=_caErrorMsg[20]
    __errcode__=160

ErrCode2Class[160]=ECA_PUTFAIL

class ECA_ADDFAIL(caError):
    __doc__=_caErrorMsg[21]
    __errcode__=168

ErrCode2Class[168]=ECA_ADDFAIL

class ECA_BADCOUNT(caError):
    __doc__=_caErrorMsg[22]
    __errcode__=176

ErrCode2Class[176]=ECA_BADCOUNT

class ECA_BADSTR(caError):
    __doc__=_caErrorMsg[23]
    __errcode__=186

ErrCode2Class[186]=ECA_BADSTR

class ECA_DISCONN(caError):
    __doc__=_caErrorMsg[24]
    __errcode__=192

ErrCode2Class[192]=ECA_DISCONN

class ECA_DBLCHNL(caError):
    __doc__=_caErrorMsg[25]
    __errcode__=200

ErrCode2Class[200]=ECA_DBLCHNL

class ECA_EVDISALLOW(caError):
    __doc__=_caErrorMsg[26]
    __errcode__=210

ErrCode2Class[210]=ECA_EVDISALLOW

class ECA_BUILDGET(caError):
    __doc__=_caErrorMsg[27]
    __errcode__=216

ErrCode2Class[216]=ECA_BUILDGET

class ECA_NEEDSFP(caError):
    __doc__=_caErrorMsg[28]
    __errcode__=224

ErrCode2Class[224]=ECA_NEEDSFP

class ECA_OVEVFAIL(caError):
    __doc__=_caErrorMsg[29]
    __errcode__=232

ErrCode2Class[232]=ECA_OVEVFAIL

class ECA_BADMONID(caError):
    __doc__=_caErrorMsg[30]
    __errcode__=242

ErrCode2Class[242]=ECA_BADMONID

class ECA_NEWADDR(caError):
    __doc__=_caErrorMsg[31]
    __errcode__=248

ErrCode2Class[248]=ECA_NEWADDR

class ECA_NEWCONN(caError):
    __doc__=_caErrorMsg[32]
    __errcode__=259

ErrCode2Class[259]=ECA_NEWCONN

class ECA_NOCACTX(caError):
    __doc__=_caErrorMsg[33]
    __errcode__=264

ErrCode2Class[264]=ECA_NOCACTX

class ECA_DEFUNCT(caError):
    __doc__=_caErrorMsg[34]
    __errcode__=278

ErrCode2Class[278]=ECA_DEFUNCT

class ECA_EMPTYSTR(caError):
    __doc__=_caErrorMsg[35]
    __errcode__=280

ErrCode2Class[280]=ECA_EMPTYSTR

class ECA_NOREPEATER(caError):
    __doc__=_caErrorMsg[36]
    __errcode__=288

ErrCode2Class[288]=ECA_NOREPEATER

class ECA_NOCHANMSG(caError):
    __doc__=_caErrorMsg[37]
    __errcode__=296

ErrCode2Class[296]=ECA_NOCHANMSG

class ECA_DLCKREST(caError):
    __doc__=_caErrorMsg[38]
    __errcode__=304

ErrCode2Class[304]=ECA_DLCKREST

class ECA_SERVBEHIND(caError):
    __doc__=_caErrorMsg[39]
    __errcode__=312

ErrCode2Class[312]=ECA_SERVBEHIND

class ECA_NOCAST(caError):
    __doc__=_caErrorMsg[40]
    __errcode__=320

ErrCode2Class[320]=ECA_NOCAST

class ECA_BADMASK(caError):
    __doc__=_caErrorMsg[41]
    __errcode__=330

ErrCode2Class[330]=ECA_BADMASK

class ECA_IODONE(caError):
    __doc__=_caErrorMsg[42]
    __errcode__=339

ErrCode2Class[339]=ECA_IODONE

class ECA_IOINPROGRESS(caError):
    __doc__=_caErrorMsg[43]
    __errcode__=347

ErrCode2Class[347]=ECA_IOINPROGRESS

class ECA_BADSYNCGRP(caError):
    __doc__=_caErrorMsg[44]
    __errcode__=354

ErrCode2Class[354]=ECA_BADSYNCGRP

class ECA_PUTCBINPROG(caError):
    __doc__=_caErrorMsg[45]
    __errcode__=362

ErrCode2Class[362]=ECA_PUTCBINPROG

class ECA_NORDACCESS(caError):
    __doc__=_caErrorMsg[46]
    __errcode__=368

ErrCode2Class[368]=ECA_NORDACCESS

class ECA_NOWTACCESS(caError):
    __doc__=_caErrorMsg[47]
    __errcode__=376

ErrCode2Class[376]=ECA_NOWTACCESS

class ECA_ANACHRONISM(caError):
    __doc__=_caErrorMsg[48]
    __errcode__=386

ErrCode2Class[386]=ECA_ANACHRONISM

class ECA_NOSEARCHADDR(caError):
    __doc__=_caErrorMsg[49]
    __errcode__=392

ErrCode2Class[392]=ECA_NOSEARCHADDR

class ECA_NOCONVERT(caError):
    __doc__=_caErrorMsg[50]
    __errcode__=400

ErrCode2Class[400]=ECA_NOCONVERT

class ECA_BADCHID(caError):
    __doc__=_caErrorMsg[51]
    __errcode__=410

ErrCode2Class[410]=ECA_BADCHID

class ECA_BADFUNCPTR(caError):
    __doc__=_caErrorMsg[52]
    __errcode__=418

ErrCode2Class[418]=ECA_BADFUNCPTR

class ECA_ISATTACHED(caError):
    __doc__=_caErrorMsg[53]
    __errcode__=424

ErrCode2Class[424]=ECA_ISATTACHED

class ECA_UNAVAILINSERV(caError):
    __doc__=_caErrorMsg[54]
    __errcode__=432

ErrCode2Class[432]=ECA_UNAVAILINSERV

class ECA_CHANDESTROY(caError):
    __doc__=_caErrorMsg[55]
    __errcode__=440

ErrCode2Class[440]=ECA_CHANDESTROY

class ECA_BADPRIORITY(caError):
    __doc__=_caErrorMsg[56]
    __errcode__=450

ErrCode2Class[450]=ECA_BADPRIORITY

class ECA_NOTTHREADED(caError):
    __doc__=_caErrorMsg[57]
    __errcode__=458

ErrCode2Class[458]=ECA_NOTTHREADED

class ECA_16KARRAYCLIENT(caError):
    __doc__=_caErrorMsg[58]
    __errcode__=464

ErrCode2Class[464]=ECA_16KARRAYCLIENT

class ECA_CONNSEQTMO(caError):
    __doc__=_caErrorMsg[59]
    __errcode__=472

ErrCode2Class[472]=ECA_CONNSEQTMO

class ECA_UNRESPTMO(caError):
    __doc__=_caErrorMsg[60]
    __errcode__=480

ErrCode2Class[480]=ECA_UNRESPTMO

