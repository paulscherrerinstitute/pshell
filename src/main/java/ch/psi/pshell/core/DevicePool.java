package ch.psi.pshell.core;

import ch.psi.pshell.device.AccessType;
import ch.psi.utils.Arr;
import ch.psi.utils.Config;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.State;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.GenericDeviceBase;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.Epics;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import ch.psi.utils.Threading;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Class that manages the instantiation and disposal of the global device list.
 */
public class DevicePool extends ObservableBase<DevicePoolListener> implements AutoCloseable {

    final static Logger logger = Logger.getLogger(DevicePool.class.getName());
    static final HashMap<String, GenericDevice> deviceList = new HashMap<>();
    static final HashMap<GenericDevice, GenericDevice[]> dependencies = new HashMap<>();
    boolean parallelInitialization;

    public static final String SIMULATED_FLAG = "$";

    public void initialize() throws FileNotFoundException, IOException, InterruptedException {
        logger.info("Initializing " + getClass().getSimpleName());
        parallelInitialization = Context.getInstance().config.isParallelInitialization();
        load();
        initializeDevices();
        applyDeviceAttributes();
        logger.info("Finished " + getClass().getSimpleName() + " initialization");
    }

    final Collection<String> orderedDeviceNames = new ArrayList<>();

    /**
     * Entity class holding the configuration attributes of a device in the
     * global device pool.
     */
    public static class DeviceAttributes {

        private String name;
        private Boolean enabled;
        private String className;
        private String[] arguments;
        private Boolean simulated;
        private AccessType accessType;
        private Boolean monitored;
        private Integer polling;

        public String[] getArguments() {
            //Remove  "" from strings

            return arguments;
        }

        public String[] getParameters() {
            return ((arguments == null) || (arguments.length == 0)) ? null : Arr.remove(arguments, 0);
        }

        public Boolean isSimulated() {
            return simulated;
        }

        public AccessType getAccessType() {
            return accessType;
        }

        public Boolean isMonitored() {
            return monitored;
        }

        public Integer getPolling() {
            return polling;
        }

        public String getName() {
            return name;
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public String getClassName() {
            return className;
        }
    }

    final HashMap<GenericDevice, DeviceAttributes> deviceAttributes = new HashMap<>();

    public DeviceAttributes getDeviceAttributes(GenericDevice dev) {
        return deviceAttributes.get(dev);
    }

    public static DeviceAttributes parseConfigEntry(String line) {
        DeviceAttributes attr = null;
        line = line.trim();
        String[] tokens = line.split("=");
        if (line.contains("=")) {
            String deviceName = line.substring(0, line.indexOf("=")).trim().replace("\\u0020", " ");
            String config = line.substring(line.indexOf("=") + 1).trim();
            try {
                attr = parseConfigEntry(deviceName, config);
            } catch (Exception ex) {
            }
        }
        return attr;
    }

    public static DeviceAttributes parseConfigEntry(String name, String config) {
        DeviceAttributes attr = new DeviceAttributes();
        attr.simulated = false;
        attr.enabled = true;
        if (name.startsWith(IO.PROPERTY_FILE_COMMENTED_FLAG)) {
            name = name.substring(1);
            attr.enabled = false;
        }
        if (name.startsWith(SIMULATED_FLAG)) {
            name = name.substring(1);
            attr.simulated = true;
        }
        attr.name = name;
        //Backward compatibility: Remove in the future
        int end = config.length();
        int aux1 = config.indexOf("|");
        int aux2 = config.indexOf(" ");
        if (aux1 > 0) {
            end = aux1;
        }
        if ((aux2 > 0) && ((aux1 < 0) || (aux2 < aux1))) {
            end = aux2;
        }
        attr.className = config.substring(0, end);
        String[] pars = config.substring(Math.min(end + 1, config.length()), config.length()).split("\\" + Config.ARRAY_SEPARATOR);
        pars = Arr.insert(pars, attr.className, 0);

        attr.accessType = ((pars.length > 2) && (pars[2].trim().length() > 0)) ? AccessType.valueOf(pars[2].trim()) : null;
        attr.polling = ((pars.length > 3) && (pars[3].trim().length() > 0)) ? Integer.valueOf(pars[3].trim()) : null;
        attr.monitored = ((pars.length > 4) && (pars[4].trim().length() > 0)) ? Boolean.valueOf(pars[4].trim()) : null;

        //Retrive constructor arguments managing string delimited with "" separated by spaces
        attr.arguments = ((pars.length > 1) && (pars[1].trim().length() > 0)) ? Str.splitIgnoringQuotesAndMultSpaces(pars[1].trim()) : new String[0];

        //Inject the name in the constructor arguments
        attr.arguments = Arr.insert(attr.arguments, name, 0);
        return attr;
    }

    public static String createConfigEntry(String name, Boolean enabled, Boolean simulated, String className, String args, AccessType access, Integer polling, Boolean monitor) {
        ArrayList<String> list = new ArrayList();
        list.add(className);
        list.add(args);
        list.add((access == null) ? "" : String.valueOf(access));
        list.add((polling == null) ? "" : String.valueOf(polling));
        list.add((monitor == null) ? "" : String.valueOf(monitor));
        String attrs = String.join(Config.ARRAY_SEPARATOR, list);

        if (!name.isEmpty() && !className.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if ((enabled != null) && (!enabled)) {
                sb.append(IO.PROPERTY_FILE_COMMENTED_FLAG);
            }
            if ((simulated != null) && (simulated)) {
                sb.append(DevicePool.SIMULATED_FLAG);
            }
            sb.append(name).append("=");
            sb.append(attrs);
            return sb.toString();
        }
        return null;
    }

    public void load() throws FileNotFoundException, IOException {
        String fileName = Context.getInstance().setup.getDevicePoolFile();
        String configPath = Context.getInstance().setup.getConfigPath();
        close();
        Epics.create(Paths.get(configPath, "jcae.properties").toString(), parallelInitialization);
        if (Context.getInstance().isSimulation()) {
            Epics.getChannelFactory().setDryrun(true);
        }

        if (Context.getInstance().isEmptyMode()) {
            logger.log(Level.INFO, "Empty mode: bypassing device instantiation");
            return;
        }

        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(fileName)) {
            properties.load(in);
            synchronized (deviceList) {
                deviceList.clear();
                orderedDeviceNames.clear();
                dependencies.clear();
            }

            for (String deviceName : IO.getOrderedPropertyKeys(fileName)) {
                String config = "Invalid device name";
                try {
                    config = properties.getProperty(deviceName).trim(); //Raise exception if ':' is used without precedin scape ('\')
                    DeviceAttributes attr = parseConfigEntry(deviceName, config);
                    if (Context.getInstance().isSimulation()) {
                        attr.simulated = true;
                    }
                    Class cls = Context.getInstance().getClassByName(attr.className);
                    String[] arguments = Arr.copy(attr.arguments);
                    GenericDevice device;
                    AdjustedConstructor adjustedConstructor = getAdjustedConstructor(cls, arguments);
                    if (adjustedConstructor != null) {
                        device = (GenericDevice) adjustedConstructor.constructor.newInstance(adjustedConstructor.arguments);
                        GenericDevice[] dependencies = Arr.getSubArray(adjustedConstructor.arguments, GenericDevice.class);
                        if (dependencies.length > 0) {
                            this.dependencies.put(device, dependencies);
                        }
                        add(device);
                    } else {
                        throw new IOException("Invalid constructor parameters for device: " + deviceName);
                    }
                    if (device != null) {
                        if (attr.simulated) {
                            device.setSimulated();
                        }
                        deviceAttributes.put(device, attr);
                    }

                } catch (Exception ex) {
                    orderedDeviceNames.remove(deviceName);
                    logger.log(Level.SEVERE, null, new Exception("Error instantiating device: " + deviceName + " (" + config + ")", ex));
                }
            }
        }
    }

    public static class AdjustedConstructor {

        Constructor constructor;
        Object[] arguments;
    }

    public static ArrayList<Constructor> getConstructors(Class cls) {
        ArrayList<Constructor> ret = new ArrayList<>();
        for (Constructor c : cls.getConstructors()) {
            if (Modifier.isPublic(c.getModifiers())) {
                Parameter[] ps = c.getParameters();
                if ((ps.length > 0) && (ps[0].getType() == String.class)) { //First parameter is name
                    ret.add(c);
                }
            }
        }
        return ret;
    }

    public AdjustedConstructor getAdjustedConstructor(Class cls, String[] arguments) {
        for (Constructor constructor : getConstructors(cls)) {
            Object[] adjustedArguments = getAdjustedArguments(constructor, arguments, null);
            if (adjustedArguments != null) {
                AdjustedConstructor ret = new AdjustedConstructor();
                ret.constructor = constructor;
                ret.arguments = adjustedArguments;
                return ret;
            }
        }
        return null;
    }

    public Object[] getAdjustedArguments(Constructor constructor, String[] arguments, HashMap<String, Class> deviceList) {
        Object[] adjustedArguments = null;
        if (constructor.getParameterCount() == arguments.length) {
            adjustedArguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                String arg = arguments[i];
                boolean enforceString = false;
                boolean enforceDevice = false;
                Class enforceType = null;
                if ((arg.length() >= 2) && arg.startsWith("\"") && arg.endsWith("\"")) {
                    arg = arg.substring(1, arg.length() - 1);
                    enforceString = true;
                } else if ((arg.length() >= 2) && arg.startsWith("<") && arg.endsWith(">")) {
                    arg = arg.substring(1, arg.length() - 1).trim();
                    enforceDevice = true;
                } else if (arg.startsWith("#") && (Str.count(arg, ":") == 1)) {
                    String[] tokens = arg.substring(1, arg.length()).trim().split(":");
                    if ((tokens.length == 2) && (tokens[0].length() > 0) && (tokens[1].length() > 0)) {
                        switch (tokens[1].toLowerCase()) {
                            case "int":
                            case "integer":
                                enforceType = Integer.class;
                                break;
                            case "byte":
                                enforceType = Byte.class;
                                break;
                            case "long":
                                enforceType = Long.class;
                                break;
                            case "short":
                                enforceType = Short.class;
                                break;
                            case "float":
                                enforceType = Float.class;
                                break;
                            case "double":
                                enforceType = Double.class;
                                break;
                            case "boolean":
                                enforceType = Boolean.class;
                                break;
                        }
                        if (enforceType != null) {
                            try {
                                adjustedArguments[i] = Config.fromString(enforceType, tokens[0]); //Check if can convert                                
                                arg = tokens[0];
                            } catch (Exception ex) {
                                enforceType = null;
                            }
                        }

                    }
                }

                Class type = constructor.getParameterTypes()[i];
                if (adjustedArguments[i] == null) {
                    adjustedArguments[i] = arg;
                }
                if (GenericDevice.class.isAssignableFrom(type)
                        || Readable.class.isAssignableFrom(type)
                        || Writable.class.isAssignableFrom(type)) {
                    if (deviceList != null) {
                        Class cls = deviceList.get(arg);
                        if ((cls == null) || !type.isAssignableFrom(cls)) {
                            adjustedArguments[i] = null;
                        }
                    } else {
                        adjustedArguments[i] = getByName(arg, type);
                    }
                } else if (enforceDevice) {
                    adjustedArguments[i] = null;
                } else if (type.isArray()) {
                    adjustedArguments[i] = null;
                } else if ((enforceString) && type != String.class) {
                    adjustedArguments[i] = null;
                } else if (enforceType != null) {
                    if ((enforceType != type) && (Convert.getPrimitiveClass(enforceType) != type)) {
                        return null;
                    }
                } else {
                    adjustedArguments[i] = Config.fromString(type, arg);
                }
                if (adjustedArguments[i] == null) {
                    return null;
                }
            }
        }
        return adjustedArguments;
    }

    public boolean contains(GenericDevice device) {
        synchronized (deviceList) {
            for (String name : orderedDeviceNames) {
                if (device == deviceList.get(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    void add(GenericDevice device) {
        if (device.getName() == null) {
            throw new IllegalArgumentException("Attempting to add device with no name");
        }

        GenericDevice dev = deviceList.get(device.getName());
        if (dev != null) {
            if (dev == device) {
                return;
            }
            logger.severe("Device name clash: " + device.getName());
            throw new IllegalArgumentException(device.getName());
        }

        synchronized (deviceList) {
            orderedDeviceNames.add(device.getName());
            deviceList.put(device.getName(), device);
        }
    }

    public GenericDevice getByName(String name) {
        return getByName(name, null);
    }

    //Ordered as in getAllDeviceNames
    public GenericDevice getByIndex(int index) {
        String[] names = getAllDeviceNames();
        if ((index >= names.length) || (index < 0)) {
            return null;
        }
        return getByName(names[index]);
    }

    public <T extends GenericDevice> T getByIndex(int index, Class<T> type) {
        String[] names = getAllDeviceNames(type);
        if ((index >= names.length) || (index < 0)) {
            return null;
        }
        return getByName(names[index], type);
    }

    public <T extends GenericDevice> T getByName(String name, Class<T> type) {
        synchronized (deviceList) {
            Object obj = deviceList.get(name);
            if (obj == null) {
                return null;
            }
            if ((type != null) && (!type.isAssignableFrom(obj.getClass()))) {
                return null;
            }
            return (T) obj;
        }
    }

    public String[] getAllDeviceNames() {
        synchronized (deviceList) {
            return orderedDeviceNames.toArray(new String[0]);
        }
    }

    public <T extends GenericDevice> String[] getAllDeviceNames(Class<T> type) {
        synchronized (deviceList) {
            ArrayList<String> names = new ArrayList();
            for (String name : orderedDeviceNames) {
                GenericDevice dev = deviceList.get(name);
                if ((type == null) || type.isAssignableFrom(dev.getClass())) {
                    names.add(dev.getName());
                }
            }
            return names.toArray(new String[0]);
        }
    }

    public GenericDevice[] getAllDevices() {
        return getAllDevices(null);
    }

    public <T extends GenericDevice> T[] getAllDevices(Class<T> type) {
        ArrayList<GenericDevice> ret = new ArrayList();
        if (type == null) {
            type = (Class<T>) GenericDevice.class;
        }
        synchronized (deviceList) {
            for (String name : orderedDeviceNames) {
                GenericDevice dev = deviceList.get(name);
                if ((type == GenericDevice.class) || (type.isAssignableFrom(dev.getClass()))) {
                    ret.add(dev);
                }
            }
        }
        return ret.toArray((T[]) Array.newInstance(type, 0));
    }

    public GenericDevice[] getAllDevicesWithState(State state) {
        ArrayList<GenericDevice> ret = new ArrayList<>();
        for (GenericDevice dev : getAllDevices()) {
            if (dev.getState() == state) {
                ret.add(dev);
            }
        }
        return ret.toArray(new GenericDevice[0]);
    }

    public String[] getAllNamesOrderedByName() {
        return getAllDeviceNames(null);
    }

    public <T extends GenericDevice> String[] getAllNamesOrderedByName(Class<T> type) {
        ArrayList<String> names = new ArrayList();
        synchronized (deviceList) {
            for (GenericDevice dev : getAllDevices(type)) {
                names.add(dev.getName());
            }
        }
        String[] ret = names.toArray(new String[0]);
        return Arr.sort(ret);
    }

    public GenericDevice[] getAllDevicesOrderedByName() {
        ArrayList<GenericDevice> ret = new ArrayList<>();
        for (String deviceName : getAllNamesOrderedByName()) {
            GenericDevice dev = getByName(deviceName);
            if (dev != null) {
                ret.add(dev);
            }
        }
        return ret.toArray(new GenericDevice[0]);
    }

    public int getDeviceCount() {
        synchronized (deviceList) {
            return deviceList.size();
        }
    }

    public <T extends GenericDevice> int getDeviceCount(Class<T> type) {
        return getAllDevices(type).length;
    }

    void initializeDevice(GenericDevice dev) throws InterruptedException {
        try {
            logger.log(Level.INFO, "Initializing " + dev.getName());
            dev.initialize();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error initializing " + dev.getName() + ": " + ex.getMessage());
        }
    }

    public void initializeDevices() throws InterruptedException {
        if (parallelInitialization) {
            ArrayList<GenericDevice> processed = new ArrayList<>();
            ArrayList<Callable> callables = new ArrayList();
            for (GenericDevice dev : getAllDevices()) {
                callables.add((Callable) () -> {
                    try {
                        waitDependenciesInit(dev, processed);
                        initializeDevice(dev);
                        return true;
                    } catch (InterruptedException ex) {
                        logger.log(Level.SEVERE, "Error initializing " + dev.getName() + ": interrupted");
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error initializing " + dev.getName() + ": " + ex.getMessage());
                    } finally{
                        synchronized(processed){
                            processed.add(dev);
                        }
                    }
                    return false;
                });
            }
            try {
                Threading.parallelize(callables.toArray(new Callable[0]), "Device Initialization");
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        } else {
            for (GenericDevice dev : getAllDevices()) {
                initializeDevice(dev);
            }
        }
    }

    public ArrayList<GenericDevice> retryInitializeDevices(){
        ArrayList<GenericDevice> ret = new ArrayList<>();
        if (parallelInitialization) {
            ArrayList<Callable> callables = new ArrayList();
            ArrayList<GenericDevice> processed = new ArrayList<>();
            for (GenericDevice dev : getAllDevicesWithState(State.Invalid)) {
                callables.add((Callable) () -> {
                    try {
                        waitDependenciesInit(dev, processed);
                        retryInitializeDevice(dev);
                        return true;
                    } catch (Exception ex) {
                        synchronized(ret){
                            ret.add(dev);
                        }
                    } finally{
                        synchronized(processed){
                            processed.add(dev);
                        }
                    }
                    return false;
                });
            }
            try {
                Threading.parallelize(callables.toArray(new Callable[0]), "Device Reinit");
            } catch (Exception ex) {                
            }

        } else {
            for (GenericDevice dev : getAllDevicesWithState(State.Invalid)) {
                try {
                    retryInitializeDevice(dev);
                } catch (Exception ex) {
                    ret.add(dev);
                }
            }
        }
        return ret;
    }

    void waitDependenciesInit(GenericDevice device, final ArrayList<GenericDevice> processed) throws IOException, InterruptedException{
        GenericDevice[] deps = dependencies.get(device);
        if (deps != null) {
            for (GenericDevice dep : deps) {
                //dep.waitInitialized(-1);            
                while (true) {
                    synchronized (processed) {                        
                        if (processed.contains(dep)){
                            break;
                        }                        
                    }
                    Thread.sleep(device.getWaitSleep());
                }
            }
        }        
    }
    
    void retryInitializeDevice(GenericDevice device) throws IOException, InterruptedException {
        device.initialize();
        applyDeviceAttributes(device);
    }

    public void applyDeviceAttributes() throws FileNotFoundException, IOException, InterruptedException {
        for (GenericDevice dev : getAllDevices()) {
            applyDeviceAttributes(dev);
        }
    }

    public void applyDeviceAttributes(GenericDevice dev) throws FileNotFoundException, IOException, InterruptedException {
        try {
            DeviceAttributes attrs = getDeviceAttributes(dev);
            if (attrs != null) {
                if ((attrs.monitored != null) && (attrs.monitored)) {
                    dev.setMonitored(true);
                }
                if (attrs.polling != null) {
                    dev.setPolling(attrs.polling);
                }
                if (attrs.accessType != null) {
                    dev.setAccessType(attrs.accessType);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    public boolean addDevice(GenericDevice device) {
        return addDevice(device, true);
    }

    public boolean addDevice(GenericDevice device, boolean initialize) {
        if (contains(device)) {
            return false;
        }
        logger.log(Level.INFO, "Adding device: " + device.getName());
        add(device);

        if (initialize) {
            if (!device.isInitialized()) {
                try {
                    if (Context.getInstance().isSimulation()) {
                        device.setSimulated();
                    }
                    device.initialize();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error initializing device: " + ex.getMessage());
                }
            }
        }

        for (DevicePoolListener listener : getListeners()) {
            try {
                listener.onDeviceAdded(device);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        return true;
    }

    public boolean removeDevice(GenericDevice device) {
        return removeDevice(device, true);
    }

    public boolean removeDevice(GenericDevice device, boolean close) {
        if ((device==null) || (getByName(device.getName()) == null)) {
            return false;
        }
        logger.log(Level.WARNING, "Removing device: " + device.getName());

        synchronized (deviceList) {
            orderedDeviceNames.remove(device.getName());
            deviceList.remove(device.getName(), device);
        }

        for (DevicePoolListener listener : getListeners()) {
            try {
                listener.onDeviceRemoved(device);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        if (close) {
            if (device.getState() != State.Closing) {
                try {
                    device.close();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error closing device: " + ex.getMessage());
                }
            }
        }
        return true;
    }

    @Override
    public void close() {
        super.close();
        for (GenericDevice dev : getAllDevices()) {
            try {
                if (dev == null) {
                    logger.warning("Closing null device");
                } else {
                    dev.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        synchronized (deviceList) {
            deviceAttributes.clear();
            deviceList.clear();
            orderedDeviceNames.clear();
        }
        Epics.destroy();
    }
}
