"""
CaChannel class having identical API as of caPython/CaChannel class,
based on PythonCA ( > 1.20.1beta2)

Author:     Xiaoqiang Wang
Created:    Sep. 22, 2008
Changes:
"""
# python 2 -> 3 compatible layer
import sys
if sys.hexversion >= 0x03000000:
    long = int

import ca

ca.cs_never_search = 4

# retrieve numeric waveforms as numpy arrays, default No
USE_NUMPY = False

class CaChannelException(Exception):
    def __init__(self, status):
        self.status = str(status)
    def __str__(self):
        return self.status

class CaChannel:
    """CaChannel: A Python class with identical API as of caPython/CaChannel.

    This class implements the methods to operate on channel access so that you can find
    their  C library counterparts ,
    http://www.aps.anl.gov/epics/base/R3-14/12-docs/CAref.html#Function.
    Therefore an understanding of C API helps much.

    To get started easily, convenient methods are created for often used operations,

    ==========    ======
    Operation     Method
    ==========    ======
    connect       :meth:`searchw`
    read          :meth:`getw`
    write         :meth:`putw`
    ==========    ======

    They have shorter names and default arguments. It is recommended to start with these methods.
    Study the other C alike methods when necessary.

    >>> import CaChannel
    >>> chan = CaChannel.CaChannel('catest')
    >>> chan.searchw()
    >>> chan.putw(12.3)
    >>> chan.getw()
    12.3
    """

    ca_timeout = 3.0

    dbr_d = {}
    dbr_d[ca.DBR_SHORT] = int
    dbr_d[ca.DBR_INT]   = int
    dbr_d[ca.DBR_LONG]  = int
    dbr_d[ca.DBR_FLOAT] = float
    dbr_d[ca.DBR_DOUBLE]= float
    dbr_d[ca.DBR_CHAR]  = int
    dbr_d[ca.DBR_STRING]= str
    dbr_d[ca.DBR_ENUM]  = int

    def __init__(self, pvName=None):
        self.pvname = pvName
        self.__chid = None
        self.__evid = None
        self.__timeout = None
        self._field_type = None
        self._element_count = None
        self._puser = None
        self._conn_state = None
        self._host_name = None
        self._raccess = None
        self._waccess = None

        self._callbacks={}

    def __del__(self):
        try:
            self.clear_event()
            self.clear_channel()
            self.flush_io()
        except:
            pass

    def version(self):
        return "CaChannel, version v28-03-12"
#
# Class helper methods
#
    def setTimeout(self, timeout):
        """Set the timeout for this channel object. It overrides the class timeout.

        :param float timeout:  timeout in seconds

        """
        if (timeout>=0 or timeout is None):
            self.__timeout = timeout
        else:
            raise ValueError
    def getTimeout(self):
        """Retrieve the timeout set for this channel object.

        :return: timeout in seconds for this channel instance

        """
        if self.__timeout is None:
            timeout = CaChannel.ca_timeout
        else:
            timeout = self.__timeout

        return timeout


#
# *************** Channel access medthod ***************
#

#
# Connection methods
#   search_and_connect
#   search
#   clear_channel

    def search_and_connect(self, pvName, callback, *user_args):
        """Attempt to establish a connection to a process variable.

        :param str pvName:          process variable name
        :param callable callback:   function called when connection completes and connection status changes later on.
        :param user_args:           user provided arguments that are passed to callback when it is invoked.
        :raises CaChannelException: if error happens

        The user arguments are returned to the user in a tuple in the callback function.
        The order of the arguments is preserved.

        Each Python callback function is required to have two arguments.
        The first argument is a tuple containing the results of the action.
        The second argument is a tuple containing any user arguments specified by ``user_args``.
        If no arguments were specified then the tuple is empty.


        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (:meth:`pend_io`, :meth:`poll`, :meth:`pend_event`, :meth:`flush_io`)
           is called. This allows several requests to be efficiently sent over the network in one message.

        >>> chan = CaChannel('catest')
        >>> def connCB(epicsArgs, userArgs):
        ...     chid = epicsArgs[0]
        ...     connection_state = epicsArgs[1]
        ...     if connection_state == ca.CA_OP_CONN_UP:
        ...         print('%s is connected' % ca.name(chid))
        >>> chan.search_and_connect(None, connCB, chan)
        >>> status = chan.pend_event(2)
        catest is connected
        """
        if pvName is None:
            pvName = self.pvname
        else:
            self.pvname = pvName
        self._callbacks['connCB']=(callback, user_args)
        try:
            self.__chid = ca.search(pvName, self._conn_callback)
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)

    def search(self, pvName=None):
        """Attempt to establish a connection to a process variable.

        :param str pvName: process variable name
        :raises CaChannelException: if error happens

        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (:meth:`pend_io`, :meth:`poll`, :meth:`pend_event`, :meth:`flush_io`)
           is called. This allows several requests to be efficiently sent over the network in one message.

        >>> chan = CaChannel()
        >>> chan.search('catest')
        >>> status = chan.pend_io(1)
        >>> chan.state()
        2
        """
        if pvName is None:
            pvName = self.pvname
        else:
            self.pvname = pvName
        try:
            self.__chid = ca.search(pvName, None)
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)

    def clear_channel(self):
        """Close a channel created by one of the search functions.

        Clearing a channel does not cause its connection handler to be called.
        Clearing a channel does remove any monitors registered for that channel.
        If the channel is currently connected then resources are freed only some
        time after this request is flushed out to the server.

        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (:meth:`pend_io`, :meth:`poll`, :meth:`pend_event`, :meth:`flush_io`)
           is called. This allows several requests to be efficiently sent over the network in one message.

        """
        if(self.__chid is not None):
            try:
                ca.clear(self.__chid)
            except ca.error:
                msg = sys.exc_info()[1]
                raise CaChannelException(msg)

#
# Write methods
#   array_put
#   array_put_callback
#

    def _setup_put(self,value, req_type, count = None):
        if count is None:
            count = self.element_count()
        else:
            count = max(1, min(self.element_count(), count) )

        if req_type == -1:
            req_type = self.field_type()

        # single numeric value
        if (isinstance(value, int) or
                isinstance(value, long) or
                isinstance(value, float) or
                isinstance(value, bool)):
            pval = (CaChannel.dbr_d[req_type](value),)
        # single string value
        #   if DBR_CHAR, split into chars
        #   otherwise convert to field type
        elif isinstance(value, str):
            if req_type == ca.DBR_CHAR:
                if len(value) < count:
                    count = len(value)
                pval = [ord(x) for x in value[:count]]
            else:
                pval = (CaChannel.dbr_d[req_type](value),)
        # assumes other sequence type
        else:
            if len(value) < count:
                count = len(value)
            pval = [CaChannel.dbr_d[req_type](x) for x in value[:count]]

        return pval

    def array_put(self, value, req_type=None, count=None):
        """Write a value or array of values to a channel

        :param value:           data to be written. For multiple values use a list or tuple
        :param req_type:        database request type (``ca.DBR_XXXX``). Defaults to be the native data type.
        :param int count:       number of data values to write. Defaults to be the native count.

        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.array_put(123)
        >>> chan.flush_io()
        >>> chan.getw()
        123.0
        >>> chan = CaChannel('cabo')
        >>> chan.searchw()
        >>> chan.array_put('Busy', ca.DBR_STRING)
        >>> chan.flush_io()
        >>> chan.getw()
        1
        >>> chan = CaChannel('cawave')
        >>> chan.searchw()
        >>> chan.array_put([1,2,3])
        >>> chan.flush_io()
        >>> chan.getw()
        [1.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        >>> chan.getw(count=3, use_numpy=True)
        array([ 1.,  2.,  3.])
        >>> chan = CaChannel('cawavec')
        >>> chan.searchw()
        >>> chan.array_put('1234',count=3)
        >>> chan.flush_io()
        >>> chan.getw(count=4)
        [49, 50, 51, 0]
        """
        if req_type is None: req_type = -1
        val = self._setup_put(value, req_type, count)
        try:
            ca.put(self.__chid, val, None, None, req_type)
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)

    def array_put_callback(self, value, req_type, count, callback, *user_args):
        """Write a value or array of values to a channel and execute the user
        supplied callback after the put has completed.

        :param value:           data to be written. For multiple values use a list or tuple.
        :param req_type:        database request type (``ca.DBR_XXXX``). Defaults to be the native data type.
        :param int count:       number of data values to write, Defaults to be the native count.
        :param callable callback: function called when the write is completed.
        :param user_args:       user provided arguments that are passed to callback when it is invoked.
        :raises CaChannelException: if error happens

        Each Python callback function is required to have two arguments.
        The first argument is a dictionary containing the results of the action.

        =======  =====  =======
        field    type   comment
        =======  =====  =======
        chid     int    channels id structure
        type     int    database request type (ca.DBR_XXXX)
        count    int    number of values to transfered
        status   int    CA status return code (ca.ECA_XXXX)
        =======  =====  =======

        The second argument is a tuple containing any user arguments specified by ``user_args``.
        If no arguments were specified then the tuple is empty.


        >>> def putCB(epicsArgs, userArgs):
        ...     print('%s put completed' % ca.name(epicsArgs['chid']))
        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.array_put_callback(145, None, None, putCB)
        >>> status = chan.pend_event(1)
        catest put completed
        >>> chan = CaChannel('cabo')
        >>> chan.searchw()
        >>> chan.array_put_callback('Busy', ca.DBR_STRING, None, putCB)
        >>> status = chan.pend_event(1)
        cabo put completed
        >>> chan = CaChannel('cawave')
        >>> chan.searchw()
        >>> chan.array_put_callback([1,2,3], None, None, putCB)
        >>> status = chan.pend_event(1)
        cawave put completed
        >>> chan = CaChannel('cawavec')
        >>> chan.searchw()
        >>> chan.array_put_callback('123', None, None, putCB)
        >>> status = chan.pend_event(1)
        cawavec put completed
        """
        if req_type is None: req_type = 0
        val = self._setup_put(value, req_type, count)
        self._callbacks['putCB']=(callback, user_args)
        try:
            ca.put(self.__chid, val, None, self._put_callback, req_type)
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)
#
# Read methods
#   getValue
#   array_get
#   array_get_callback
#

    # Obtain read value after ECA_NORMAL is returned on an array_get().
    def getValue(self):
        """Return the value(s) after array_get has completed"""
        return self.val

    # Simulate with a synchronous getw function call
    def array_get(self, req_type=None, count=None, **keywords):
        """Read a value or array of values from a channel. The new value is
        retrieved by a call to getValue method.

        :param req_type:    database request type (``ca.DBR_XXXX``). Defaults to be the native data type.
        :param int count:   number of data values to read, Defaults to be the native count.
        :param keywords:    optional arguments assigned by keywords

                            ===========   =====
                            keyword       value
                            ===========   =====
                            use_numpy     True if waveform should be returned as numpy array. Default :data:`CaChannel.USE_NUMPY`.
                            ===========   =====

        :raises CaChannelException: if error happens

        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (``pend_io``, ``poll``, ``pend_event``, ``flush_io``)
           is called. This allows several requests to be efficiently sent over the network in one message.


        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.putw(123)
        >>> chan.array_get()
        >>> chan.getValue()
        123.0
        """
        self.val = self.getw(req_type, count, **keywords)

    def array_get_callback(self, req_type, count, callback, *user_args, **keywords):
        """Read a value or array of values from a channel and execute the user
        supplied callback after the get has completed.

        :param req_type:        database request type (``ca.DBR_XXXX``). Defaults to be the native data type.
        :param int count:       number of data values to read, Defaults to be the native count.
        :param callable callback:  function called when the get is completed.
        :param user_args:       user provided arguments that are passed to callback when it is invoked.
        :param keywords:        optional arguments assigned by keywords

                                ===========   =====
                                keyword       value
                                ===========   =====
                                use_numpy     True if waveform should be returned as numpy array. Default :data:`CaChannel.USE_NUMPY`.
                                ===========   =====

        :raises CaChannelException: if error happens

        Each Python callback function is required to have two arguments.
        The first argument is a dictionary containing the results of the action.

        +-----------------+---------------+------------------------------------+-------------------------+---------------+-------------+---------------+
        | field           |  type         |  comment                           |       request type                                                    |
        |                 |               |                                    +----------+--------------+---------------+-------------+---------------+
        |                 |               |                                    | DBR_XXXX | DBR_STS_XXXX | DBR_TIME_XXXX | DBR_GR_XXXX | DBR_CTRL_XXXX |
        +=================+===============+====================================+==========+==============+===============+=============+===============+
        | chid            |   int         |   channels id number               |    X     |       X      |     X         |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | type            |   int         |   database request type            |    X     |       X      |     X         |   X         | X             |
        |                 |               |   (ca.DBR_XXXX)                    |          |              |               |             |               |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | count           |   int         |   number of values to transfered   |    X     |       X      |     X         |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | status          |   int         |   CA status return code            |    X     |       X      |     X         |   X         | X             |
        |                 |               |   (ca.ECA_XXXX)                    |          |              |               |             |               |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_value        |               |   PV value                         |    X     |       X      |     X         |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_status       |   int         |   PV alarm status                  |          |       X      |     X         |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_severity     |   int         |   PV alarm severity                |          |       X      |     X         |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_seconds      |   float       |   timestamp                        |          |              |     X         |             |               |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_nostrings    |   int         |   ENUM PV's number of states       |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_statestrings |   string list |   ENUM PV's states string          |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_units        |   string      |   units                            |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_precision    |   int         |   precision                        |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_updislim     |   float       |   upper display limit              |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_lodislim     |   float       |   lower display limit              |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_upalarmlim   |   float       |   upper alarm limit                |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_upwarnlim    |   float       |   upper warning limit              |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_loalarmlim   |   float       |   lower alarm limit                |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_lowarnlim    |   float       |   lower warning limit              |          |              |               |   X         | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_upctrllim    |   float       |   upper control limit              |          |              |               |             | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+
        | pv_loctrllim    |   float       |   lower control limit              |          |              |               |             | X             |
        +-----------------+---------------+------------------------------------+----------+--------------+---------------+-------------+---------------+

        The second argument is a tuple containing any user arguments specified by ``user_args``.
        If no arguments were specified then the tuple is empty.

        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (``pend_io``, ``poll``, ``pend_event``, ``flush_io``)
           is called. This allows several requests to be efficiently sent over the network in one message.

        >>> def getCB(epicsArgs, userArgs):
        ...     for item in sorted(epicsArgs.keys()):
        ...         if item.startswith('pv_'):
        ...             print('%s %s' % (item,epicsArgs[item]))
        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.putw(145)
        >>> chan.array_get_callback(ca.DBR_CTRL_DOUBLE, 1, getCB)
        >>> status = chan.pend_event(1)
        pv_loalarmlim -20.0
        pv_loctrllim 0.0
        pv_lodislim 0.0
        pv_lowarnlim -10.0
        pv_precision 3
        pv_severity 2
        pv_status 3
        pv_units mm
        pv_upalarmlim 20.0
        pv_upctrllim 0.0
        pv_updislim 0.0
        pv_upwarnlim 10.0
        pv_value 145.0
        >>> chan = CaChannel('cabo')
        >>> chan.searchw()
        >>> chan.putw(0)
        >>> chan.array_get_callback(ca.DBR_CTRL_ENUM, 1, getCB)
        >>> status = chan.pend_event(1)
        pv_nostrings 2
        pv_severity 0
        pv_statestrings ('Done', 'Busy')
        pv_status 0
        pv_value 0
        """
        if req_type is None: req_type = ca.dbf_type_to_DBR(self.field_type())
        if count is None: count = self.element_count()
        self._callbacks['getCB']=(callback, user_args)
        try:
            ca.get(self.__chid, self._get_callback, req_type, count, keywords.get('use_numpy', USE_NUMPY))
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)

#
# Monitor methods
#   add_masked_array_event
#   clear_event
#

    # Creates a new event id and stores it on self.__evid.  Only one event registered
    # per CaChannel object.  If an event is already registered the event is cleared
    # before registering a new event.
    def add_masked_array_event(self, req_type, count, mask, callback, *user_args, **keywords):
        """Specify a callback function to be executed whenever changes occur to a PV.

        :param req_type:        database request type (``ca.DBR_XXXX``). Defaults to be the native data type.
        :param int  count:      number of data values to read, Defaults to be the native count.
        :param mask:            logical or of ``ca.DBE_VALUE``, ``ca.DBE_LOG``, ``ca.DBE_ALARM``.
                                Defaults to be ``ca.DBE_VALUE|ca.DBE_ALARM``.
        :param callable callback:        function called when the get is completed.
        :param user_args:       user provided arguments that are passed to callback when
                                it is invoked.
        :param keywords:        optional arguments assigned by keywords

                                ===========   =====
                                keyword       value
                                ===========   =====
                                use_numpy     True if waveform should be returned as numpy array. Default :data:`CaChannel.USE_NUMPY`.
                                ===========   =====

        :raises CaChannelException: if error happens

        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (:meth:`pend_io`, :meth:`poll`, :meth:`pend_event`, :meth:`flush_io`)
           is called. This allows several requests to be efficiently sent over the network in one message.

        >>> def eventCB(epicsArgs, userArgs):
        ...     print('pv_value %s' % epicsArgs['pv_value'])
        ...     print('pv_status %d %s' % (epicsArgs['pv_status'], ca.alarmStatusString(epicsArgs['pv_status'])))
        ...     print('pv_severity %d %s' % (epicsArgs['pv_severity'], ca.alarmSeverityString(epicsArgs['pv_severity'])))
        >>> chan = CaChannel('cabo')
        >>> chan.searchw()
        >>> chan.putw(1)
        >>> chan.add_masked_array_event(ca.DBR_STS_ENUM, None, None, eventCB)
        >>> status = chan.pend_event(1)
        pv_value 1
        pv_status 7 STATE
        pv_severity 1 MINOR
        >>> chan.clear_event()
        >>> chan.add_masked_array_event(ca.DBR_STS_STRING, None, None, eventCB)
        >>> status = chan.pend_event(1)
        pv_value Busy
        pv_status 7 STATE
        pv_severity 1 MINOR
        >>> chan.clear_event()
        """
        if req_type is None: req_type = ca.dbf_type_to_DBR(self.field_type())
        if count is None: count = self.element_count()
        if mask is None: mask = ca.DBE_VALUE|ca.DBE_ALARM
        if self.__evid is not None:
            self.clear_event()
            self.flush_io()
        self._callbacks['eventCB']=(callback, user_args)
        try:
            self.__evid = ca.monitor(self.__chid, self._event_callback, count, mask, req_type, keywords.get('use_numpy', USE_NUMPY))
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)

    def clear_event(self):
        """Remove previously installed callback function.

        .. note:: All remote operation requests such as the above are accumulated (buffered)
           and not forwarded to the IOC until one of execution methods (:meth:`pend_io`, :meth:`poll`, :meth:`pend_event`, :meth:`flush_io`)
           is called. This allows several requests to be efficiently sent over the network in one message.
        """
        if self.__evid is not None:
            try:
                ca.clear_monitor(self.__evid)
                self.__evid = None
            except ca.error:
                msg = sys.exc_info()[1]
                raise CaChannelException(msg)

#
# Execute methods
#   pend_io
#   pend_event
#   poll
#   flush_io
#

    def pend_io(self,timeout=None):
        """Flush the send buffer and wait until outstanding queries (``search``, ``array_get``) complete
        or the specified timeout expires.

        :param float timeout:         seconds to wait
        :raises CaChannelException: if timeout or other error happens

        """
        if timeout is None:
            timeout = self.getTimeout()
        status = ca.pend_io(float(timeout))
        if status != 0:
            raise CaChannelException(ca.caError._caErrorMsg[status])

    def pend_event(self,timeout=None):
        """Flush the send buffer and process background activity (connect/get/put/monitor callbacks) for ``timeout`` seconds.

        It will not return before the specified timeout expires and all unfinished channel access labor has been processed.

        :param float timeout:  seconds to wait

        """
        if timeout is None:
            timeout = 0.1
        status = ca.pend_event(timeout)
        # status is always ECA_TIMEOUT
        return status

    def poll(self):
        """Flush the send buffer and execute any outstanding background activity.

        .. note:: It is an alias to ``pend_event(1e-12)``.
        """
        status = ca.poll()
        # status is always ECA_TIMEOUT
        return status

    def flush_io(self):
        """Flush the send buffer and does not execute outstanding background activity."""
        status = ca.flush()
        if status != 0:
            raise CaChannelException(ca.caError._caErrorMsg[status])

#
# Channel Access Macros
#   field_type
#   element_count
#   name
#   state
#   host_name
#   read_access
#   write_access
#
    def get_info(self):
        try:
            info=(self._field_type, self._element_count, self._puser,
                  self._conn_state, self._host_name, self._raccess,
                  self._waccess) = ca.ch_info(self.__chid)
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)
        return info


    def field_type(self):
        """Native type of the PV in the server (``ca.DBF_XXXX``).

        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> ftype = chan.field_type()
        >>> ftype
        6
        >>> ca.dbf_text(ftype)
        'DBF_DOUBLE'
        >>> ca.DBF_DOUBLE == ftype
        True
        """
        self.get_info()
        return self._field_type

    def element_count(self):
        """Maximum array element count of the PV in the server.

        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.element_count()
        1
        """
        self.get_info()
        return self._element_count

    def name(self):
        """Channel name specified when the channel was created.

        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.name()
        'catest'
        """
        return ca.name(self.__chid)

    def state(self):
        """Current state of the CA connection.

            ==================    =============
            States                Meaning
            ==================    =============
            ca.cs_never_conn      PV not found
            ca.cs_prev_conn       PV was found but unavailable
            ca.cs_conn            PV was found and available
            ca.cs_closed          PV not closed
            ca.cs_never_search    PV not searched yet
            ==================    =============

        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.state()
        2
        """
        if self.__chid is None:
            return ca.cs_never_search
        else:
            self.get_info()
            return self._conn_state

    def host_name(self):
        """Host name that hosts the process variable."""
        self.get_info()
        return self._host_name

    def read_access(self):
        """Access right to read the channel.

         :return: True if the channel can be read, False otherwise.

         """
        self.get_info()
        return self._raccess

    def write_access(self):
        """Access right to write the channel.

        :return: True if the channel can be written, False otherwise.

        """
        self.get_info()
        return self._waccess
#
# Wait functions
#
# These functions wait for completion of the requested action.
    def searchw(self, pvName=None):
        """Attempt to establish a connection to a process variable.

        :param str pvName:          process variable name
        :raises CaChannelException: if timeout or error happens

        .. note:: This method waits for connection to be established or fail with exception.

        >>> chan = CaChannel('non-exist-channel')
        >>> chan.searchw()
        Traceback (most recent call last):
            ...
        CaChannelException: User specified timeout on IO operation expired
        """
        if pvName is None:
            pvName = self.pvname
        else:
            self.pvname = pvName
        self.__chid = ca.search(pvName, None)
        timeout = self.getTimeout()
        status = ca.pend_io(timeout)
        if status != 0:
            raise CaChannelException(ca.caError._caErrorMsg[status])

    def putw(self, value, req_type=None):
        """Write a value or array of values to a channel

        If the request type is omitted the data is written as the Python type corresponding to the native format.
        Multi-element data is specified as a tuple or a list.
        Internally the sequence is converted to a list before inserting the values into a C array.
        Access using non-numerical types is restricted to the first element in the data field.
        Mixing character types with numerical types writes bogus results but is not prohibited at this time.
        DBF_ENUM fields can be written using DBR_ENUM and DBR_STRING types.
        DBR_STRING writes of a field of type DBF_ENUM must be accompanied by a valid string out of the possible enumerated values.

        :param value:           data to be written. For multiple values use a list or tuple
        :param req_type:        database request type (``ca.DBR_XXXX``). Defaults to be the native data type.
        :raises CaChannelException: if timeout or error happens

        .. note:: This method does flush the request to the channel access server.

        >>> chan = CaChannel('catest')
        >>> chan.searchw()
        >>> chan.putw(145)
        >>> chan.getw()
        145.0
        >>> chan = CaChannel('cabo')
        >>> chan.searchw()
        >>> chan.putw('Busy', ca.DBR_STRING)
        >>> chan.getw()
        1
        >>> chan.getw(ca.DBR_STRING)
        'Busy'
        >>> chan = CaChannel('cawave')
        >>> chan.searchw()
        >>> chan.putw([1,2,3])
        >>> chan.getw(req_type=ca.DBR_LONG,count=4)
        [1, 2, 3, 0]
        >>> chan = CaChannel('cawavec')
        >>> chan.searchw()
        >>> chan.putw('123')
        >>> chan.getw(count=4)
        [49, 50, 51, 0]
        >>> chan = CaChannel('cawaves')
        >>> chan.searchw()
        >>> chan.putw(['string 1','string 2'])
        >>> chan.getw()
        ['string 1', 'string 2', '']
        """
        if req_type is None: req_type = -1
        val = self._setup_put(value, req_type)
        try:
            ca.put(self.__chid, val, None, None, req_type)
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)
        self.flush_io()

    def getw(self, req_type=None, count=None, **keywords):
        """Read the value from a channel.

        :param req_type:        database request type. Defaults to be the native data type.
        :param int count:       number of data values to read, Defaults to be the native count.
        :param keywords:        optional arguments assigned by keywords

                                ===========   =====
                                keyword       value
                                ===========   =====
                                use_numpy     True if waveform should be returned as numpy array. Default :data:`CaChannel.USE_NUMPY`.
                                ===========   =====
        :return:                If req_type is plain request type, only the value is returned. Otherwise a dict returns
                                with information depending on the request type, same as the first argument passed to user's callback.
                                See :meth:`array_get_callback`.

        :raises CaChannelException: if timeout error happens

        If the request type is omitted the data is returned to the user as the Python type corresponding to the native format.
        Multi-element data has all the elements returned as items in a list and must be accessed using a numerical type.
        Access using non-numerical types is restricted to the first element in the data field.
        DBF_ENUM fields can be read using DBR_ENUM and DBR_STRING types.
        DBR_STRING reads of a field of type DBF_ENUM returns the string corresponding to the current enumerated value.

        """
        updated = [False]
        value = [0]
        def update_value(args):
            if args is None:
                return
            try:
                value[0] = self._format_cb_args(args)
            finally:
                updated[0] = True
        if req_type is None: req_type = ca.dbf_type_to_DBR(self.field_type())
        if count is None: count = self.element_count()
        try:
            ca.get(self.__chid, update_value, req_type, count, keywords.get('use_numpy', USE_NUMPY))
        except ca.error:
            msg = sys.exc_info()[1]
            raise CaChannelException(msg)
        timeout = self.getTimeout()
        self.flush_io()
        n = timeout / 0.001
        while n > 0 and not updated[0]:
            ca.pend_event(0.001)
            n-=1
        if not updated[0]:
            raise CaChannelException(ca.caError._caErrorMsg[10]) # ECA_TIMEOUT
        if ca.dbr_type_is_plain(req_type):
            return value[0]['pv_value']
        else:
            return value[0]

#
# Callback functions
#
# These functions hook user supplied callback functions to CA extension

    def _conn_callback(self):
        callback = self._callbacks.get('connCB')
        if callback is None:
            return
        callbackFunc, userArgs = callback
        if self.state() == 2: OP = 6
        else: OP = 7
        epicsArgs = (self.__chid, OP)
        try:
            callbackFunc(epicsArgs, userArgs)
        except:
            pass

    def _put_callback(self, args):
        callback = self._callbacks.get('putCB')
        if callback is None:
            return
        callbackFunc, userArgs = callback
        epicsArgs={}
        epicsArgs['chid']=self.__chid
        epicsArgs['type']=self.field_type()
        epicsArgs['count']=self.element_count()
        epicsArgs['status']=args[1]
        try:
            callbackFunc(epicsArgs, userArgs)
        except:
            pass

    def _get_callback(self, args):
        callback = self._callbacks.get('getCB')
        if callback is None:
            return
        callbackFunc, userArgs = callback
        epicsArgs = self._format_cb_args(args)
        try:
            callbackFunc(epicsArgs, userArgs)
        except:
            pass

    def _event_callback(self, args):
        callback = self._callbacks.get('eventCB')
        if callback is None:
            return
        callbackFunc, userArgs = callback
        epicsArgs = self._format_cb_args(args)
        try:
            callbackFunc(epicsArgs, userArgs)
        except:
            pass

    def _format_cb_args(self, args):
        epicsArgs={}
        epicsArgs['chid']   = self.__chid
        # dbr_type is not returned
        # use dbf_type instead
        epicsArgs['type']   = self.field_type()
        epicsArgs['count']  = self.element_count()
        # status flag is not returned,
        # args[1] is alarm status
        # assume ECA_NORMAL
        epicsArgs['status'] = 1
        if len(args)==2:          # Error
            epicsArgs['pv_value']   = args[0] # always None
            epicsArgs['status']     = args[1]
        if len(args)>=3:          # DBR_Plain
            epicsArgs['pv_value']   = args[0]
            epicsArgs['pv_severity']= args[1]
            epicsArgs['pv_status']  = args[2]
        if len(args)==4:          # DBR_TIME, 0.0 for others
            epicsArgs['pv_seconds'] = args[3]
        if len(args)==5:
            if len(args[4])==2:   # DBR_CTRL_ENUM
                epicsArgs['pv_nostrings']   = args[4][0]
                epicsArgs['pv_statestrings']= args[4][1]
            if len(args[4])>=7:   # DBR_GR
                epicsArgs['pv_units']       = args[4][0]
                epicsArgs['pv_updislim']    = args[4][1]
                epicsArgs['pv_lodislim']    = args[4][2]
                epicsArgs['pv_upalarmlim']  = args[4][3]
                epicsArgs['pv_upwarnlim']   = args[4][4]
                epicsArgs['pv_loalarmlim']  = args[4][5]
                epicsArgs['pv_lowarnlim']   = args[4][6]
            if len(args[4])==8:   # DBR_GR_FLOAT or DBR_GR_DOUBLE
                epicsArgs['pv_precision']   = args[4][7]
            if len(args[4])>=9:   # DBR_CTRL
                epicsArgs['pv_upctrllim']   = args[4][7]
                epicsArgs['pv_loctrllim']   = args[4][8]
            if len(args[4])==10:  # DBR_CTRL_FLOAT or DBR_CTRL_DOUBLE
                epicsArgs['pv_precision']   = args[4][9]
        return epicsArgs

if __name__ == "__main__":
    import doctest
    doctest.testmod()
