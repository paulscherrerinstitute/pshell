package ch.psi.pshell.epics;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBRFactory;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;
import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.GR;
import java.lang.reflect.Array;
import java.util.List;

/**
 *
 */
public class CAS extends ProcessVariable implements AutoCloseable {

    final ReadonlyRegisterBase register;
    final String channelName;
    final DBRType type;

    public CAS(String channelName, ReadonlyRegisterBase register) throws Exception {
        this(channelName, register, null);
    }

    public CAS(String channelName, ReadonlyRegisterBase register, String typeName) throws Exception {
        super(channelName, null);
        if (!isStarted()) {
            start();
        } else {
            //Unregister previous channels having the same name
            server.unregisterProcessVaribale(channelName);
        }
        this.register = register;
        this.channelName = channelName;
        register.addListener(new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                try {
                    postEvent();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        });

        if (typeName != null) {
            type = DBRType.forName("DBR_" + typeName.toUpperCase());
            if (type == null) {
                throw new Exception("Error setting CAS type: bad type name");
            }
            //If not given tries to deduce from cache
        } else {
            Object val = register.take(Integer.MAX_VALUE); //Read if no cache
            if (val == null) {
                throw new Exception("Error setting CAS type: cannot read device value");
            }
            Class t = (val.getClass().isArray()) ? val.getClass().getComponentType() : val.getClass();
            if (t.isPrimitive()) {
                t = Convert.getWrapperClass(t);
            }
            if ((t == Boolean.class) || (t == Integer.class) || (t == Long.class)) {
                type = DBRType.INT;
            } else if (t == Byte.class) {
                type = DBRType.BYTE;
            } else if (t == Double.class) {
                type = DBRType.DOUBLE;
            } else if (t == Float.class) {
                type = DBRType.FLOAT;
            } else if (t == Short.class) {
                type = DBRType.SHORT;
            } else {
                type = DBRType.STRING;
            }
        }
        Logger.getLogger(CAS.class.getName()).info("Adding channel: " + channelName);
        server.registerProcessVaribale(this);
    }

    protected void postEvent() throws InterruptedException {
        if (interest) {
            int mask = Monitor.VALUE | Monitor.LOG;
            //Create DBR
            int count = getDimensionSize(0);
            int controlOffset = DBRType.CTRL_STRING.getValue();
            DBRType type = getType();
            int controlTypeValue = type.getValue() + controlOffset;
            type = DBRType.forValue(controlTypeValue);
            DBR dbr = DBRFactory.create(type, count);
            STS dbrSts = (STS) dbr;
            dbrSts.setSeverity(Severity.NO_ALARM);
            dbrSts.setStatus(Status.NO_ALARM);
            if (dbr.isPRECSION()) {
                ((PRECISION) dbr).setPrecision((short) -1);
            }

            Object ret;
            try {
                synchronized (this) {
                    ret = register.take();
                }
                obj2dbr(dbr, ret);
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
                try {
                    obj2dbr(dbr, null);
                } catch (Exception e) {
                }
                mask |= Monitor.ALARM;
                dbrSts.setSeverity(Severity.INVALID_ALARM);
                dbrSts.setStatus(Status.READ_ALARM);
            }
            getEventCallback().postEvent(mask, dbr);
        }
    }

    @Override
    public String toString() {
        return register.getName() + " CAS";
    }

    @Override
    public DBRType getType() {
        return type;
    }

    @Override
    public int getMaxDimension() {
        return register instanceof ReadonlyRegisterArray ? 1 : 0;
    }

    @Override
    public int getDimensionSize(int dimension) {
        if (dimension > 1) {
            return 0;
        }
        return register instanceof ReadonlyRegisterArray ? ((ReadonlyRegisterArray) register).getSize() : 1;
    }

    @Override
    public CAStatus read(DBR dbr, ProcessVariableReadCallback callback) throws CAException {
        if (register.getAccessType() == AccessType.Write) {
            return CAStatus.NORDACCESS;
        }
        String property = getName();
        try {
            Object ret;
            synchronized (this) {
                ret = register.read();
            }
            obj2dbr(dbr, ret);
            return CAStatus.NORMAL;
        } catch (Exception ex) {
            Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
            try {
                obj2dbr(dbr, null);
            } catch (Exception e) {
            }
            if (dbr instanceof STS) {
                ((STS) dbr).setSeverity(Severity.INVALID_ALARM);
                ((STS) dbr).setStatus(Status.READ_ALARM);
            }
            //TODO: Check JCAE ch.psi.jcae.impl.GetFuture.getCompleted: blocking if didn't receive CAStatus.NORMAL
            return CAStatus.GETFAIL;
        }
    }

    @Override
    public CAStatus write(DBR dbr, ProcessVariableWriteCallback callback) throws CAException {
        if (register.getAccessType() == AccessType.Read) {
            return CAStatus.NOWTACCESS;
        }
        if (!(register instanceof RegisterBase)) {
            return CAStatus.NOWTACCESS;
        }
        try {
            Object obj = null;
            if (dbr.getCount() > 0) {
                if (getMaxDimension() == 0) {
                    obj = Array.get(dbr.getValue(), 0);
                } else {
                    obj = dbr.getValue();
                }
            }
            synchronized (this) {
                ((RegisterBase)register).write(obj);
            }
            //postEvent(); Not posting here, let  value change callback             
            return CAStatus.NORMAL;
        } catch (Exception ex) {
            Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
            if (dbr instanceof STS) {
                ((STS) dbr).setSeverity(Severity.INVALID_ALARM);
                ((STS) dbr).setStatus(Status.WRITE_ALARM);
            }
            //TODO: Check JCAE ch.psi.jcae.impl.SetFuture.putCompleted: blocking if didn't receive CAStatus.NORMAL
            return CAStatus.PUTFAIL;
        }
    }

    void obj2dbr(DBR dbr, Object object) throws Exception {
        if (dbr instanceof TIME) {
            ((TIME) dbr).setTimeStamp(new TimeStamp());
        }
        if (object == null) {
            //TODO: How to return a null value?
            Class c = dbr.getValue().getClass().getComponentType();
            if (c.isPrimitive()) {
                for (int i = 0; i < dbr.getCount(); i++) {
                    Array.set(dbr.getValue(), i, 0);
                }
            } else {
                for (int i = 0; i < dbr.getCount(); i++) {
                    ((Object[]) (dbr.getValue()))[i] = null;
                }
            }

            if (dbr instanceof STS) {
                ((STS) dbr).setStatus(Status.UDF_ALARM);
                ((STS) dbr).setSeverity(Severity.INVALID_ALARM);
            }
        } else {
            if (object instanceof List) {
                object = ((List) object).toArray();
            }
            Class t = object.getClass().isArray() ? object.getClass().getComponentType() : object.getClass();
            if (t.isPrimitive()) {
                t = Convert.getWrapperClass(t);
            }
            Class tbdr = dbr.getValue().getClass().getComponentType();

            if (object.getClass().isArray()) {
                if (tbdr == String.class) {
                    String[] arr = Convert.toStringArray(object);
                    for (int i = 0; i < ((String[]) (dbr.getValue())).length; i++) {
                        if ((arr != null) && (i < arr.length)) {
                            ((String[]) (dbr.getValue()))[i] = arr[i];
                        } else {
                            ((String[]) (dbr.getValue()))[i] = null;
                        }
                    }
                } else {
                    object = Convert.toPrimitiveArray(object, tbdr);
                    System.arraycopy(object, 0, dbr.getValue(), 0, Math.min(Array.getLength(object), dbr.getCount()));
                }
            } else {
                if (tbdr == String.class) {
                    String str = String.valueOf(object);
                    ((String[]) (dbr.getValue()))[0] = str;

                } else {
                    if (t == Boolean.class) {
                        object = ((((Boolean) object) == true) ? 1 : 0);
                    }
                    if (object instanceof Number) {
                        Array.set(dbr.getValue(), 0, Convert.toType((Number) object, tbdr));
                    } else {
                        throw new Exception("Invalid register value");
                    }
                }
            }
            if (dbr instanceof STS) {
                ((STS) dbr).setStatus(Status.NO_ALARM);
                ((STS) dbr).setSeverity(Severity.NO_ALARM);
            }
        }

        if (dbr instanceof PRECISION) {
            ((PRECISION) dbr).setPrecision((short) register.getPrecision());
        }

        if (register instanceof ch.psi.pshell.device.ProcessVariable) {
            ch.psi.pshell.device.ProcessVariable pv = (ch.psi.pshell.device.ProcessVariable) register;
            String units = pv.getUnit();
            double max = pv.getMaxValue();
            double min = pv.getMinValue();
            if (dbr instanceof GR) {
                if (units != null) {
                    ((GR) dbr).setUnits(units);
                }
                if (!Double.isNaN(max)) {
                    ((GR) dbr).setUpperDispLimit(max);
                }
                if (!Double.isNaN(min)) {
                    ((GR) dbr).setLowerDispLimit(min);
                }
            }
            if (dbr instanceof CTRL) {
                if (!Double.isNaN(max)) {
                    ((CTRL) dbr).setUpperCtrlLimit(max);
                }
                if (!Double.isNaN(min)) {
                    ((CTRL) dbr).setLowerCtrlLimit(min);
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (server != null) {
                server.unregisterProcessVaribale(channelName);
            }
        } catch (Exception ex) {
            Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    // A static server object to handle all ProcessVariables
    static DefaultServerImpl server;

    static {
        server = new DefaultServerImpl();
        Context.getInstance().addListener(new ContextAdapter() {
            @Override
            public void onContextStateChanged(State state, State former) {
                if (!state.isInitialized() && isStarted()) {
                    stop();
                }
            }
        });
    }

    static int serverPort = 5064;

    public static void setServerPort(int port) {
        serverPort = port;
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.server_port", String.valueOf(port));
    }

    public static int getServerPort() {
        return serverPort;
    }

    static Thread thread;
    static ServerContext context = null;

    static void start() throws CAException {
        JCALibrary jca = JCALibrary.getInstance();
        context = jca.createServerContext(JCALibrary.CHANNEL_ACCESS_SERVER_JAVA, server);
        thread = new Thread(() -> {
            Logger.getLogger(CAS.class.getName()).info("Starting channel access server: "
                    + ((CAJServerContext) context).getServerInetAddress() + ":" + ((CAJServerContext) context).getServerPort());
            try {
                context.run(0);
            } catch (Exception ex) {
                Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
            } finally {
                Logger.getLogger(CAS.class.getName()).info("Exit channel access server thread");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    static public ServerContext getContext() {
        return context;
    }

    static void stop() {
        try {
            if (context != null) {
                Logger.getLogger(CAS.class.getName()).info("Stopping channel access server");
                context.destroy();
                context = null;
            }
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        } catch (Exception ex) {
            Logger.getLogger(CAS.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    static boolean isStarted() {
        return (context != null);
    }

}
