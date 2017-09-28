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
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.Epics;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that manages the instantiation and disposal of the global device list.
 */
public class DevicePool extends ObservableBase<DevicePoolListener> implements AutoCloseable {

    final static Logger logger = Logger.getLogger(DevicePool.class.getName());
    static final HashMap<String, GenericDevice> deviceList = new HashMap<>();

    public static final String SIMULATED_FLAG = "$";

    public void initialize() throws FileNotFoundException, IOException, InterruptedException {
        logger.info("Initializing " + getClass().getSimpleName());
        load();
        initializeDevices();
        applyDeviceAttributes();
        logger.info("Finished " + getClass().getSimpleName() + " initialization");
    }

    final Collection<String> orderedDeviceNames = new ArrayList<>();

    /**
     * Entity class holding the configuration attributes of a device in the global device pool.
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

        //Retrive constructor arguments
        //Managing string with spaces: ""
        ArrayList<String> args = new ArrayList<>();
        if ((pars.length > 1) && (pars[1].trim().length() > 0)) {
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(pars[1].trim());
            while (m.find()) {
                //Does not remove surrounding "" here in order to be displayed correctly: must be removed when instantiating device                
                args.add(m.group(1));
            }
        }
        attr.arguments = args.toArray(new String[0]);

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
        System.setProperty(Epics.PROPERTY_JCAE_CONFIG_FILE, Paths.get(configPath, "jcae.properties").toString());
        Epics.create();
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
                    Class[] parClasses = new Class[arguments.length];
                    for (int i = 0; i < arguments.length; i++) {
                        parClasses[i] = String.class;
                        if ((arguments[i].length() >= 2) && arguments[i].startsWith("\"") && arguments[i].endsWith("\"")) {
                            arguments[i] = arguments[i].substring(1, arguments[i].length() - 1);
                        }
                    }
                    GenericDevice device;
                    try {
                        Constructor constructor = cls.getConstructor(parClasses);
                        device = (GenericDevice) constructor.newInstance((Object[]) arguments);
                        add(device);
                    } catch (NoSuchMethodException ex) {
                        //Try to convert strings to actual parameters
                        Constructor constructor = null;
                        Object[] newTokens = new Object[arguments.length];
                        for (Constructor c : cls.getConstructors()) {
                            boolean ok = false;
                            if (c.getParameterCount() == arguments.length) {
                                ok = true;
                                for (int i = 0; i < arguments.length; i++) {
                                    Class type = c.getParameterTypes()[i];
                                    if (GenericDevice.class.isAssignableFrom(type)
                                            || Readable.class.isAssignableFrom(type)
                                            || Writable.class.isAssignableFrom(type)) {
                                        newTokens[i] = getByName(arguments[i], type);
                                    } else if (type.isArray()) {
                                        newTokens[i] = null;
                                    } else {
                                        newTokens[i] = Config.fromString(type, arguments[i]);
                                    }
                                    if (newTokens[i] == null) {
                                        ok = false;
                                        break;
                                    }
                                }
                            }
                            if (ok) {
                                constructor = c;
                                break;
                            }
                        }
                        if (constructor != null) {
                            device = (GenericDevice) constructor.newInstance(newTokens);
                            add(device);
                        } else {
                            throw ex;
                        }
                    }
                    if (device != null) {
                        if (attr.simulated) {
                            device.setSimulated();
                        }
                        deviceAttributes.put(device, attr);
                    }

                } catch (Exception ex) {
                    orderedDeviceNames.remove(deviceName);
                    logger.log(Level.SEVERE, "Error instantiating device: " + deviceName + " (" + config + ")");
                }
            }
        }
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

    public void initializeDevices() throws InterruptedException {
        for (GenericDevice dev : getAllDevices()) {
            try {
                logger.log(Level.INFO, "Initializing: " + dev.getName());
                dev.initialize();
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error initializing: " + dev.getName());
            }
        }
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
        if (contains(device)) {
            return false;
        }
        logger.log(Level.INFO, "Adding device: " + device.getName());
        add(device);
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
        if (getByName(device.getName()) == null) {
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

        if (device.getState() != State.Closing) {
            try {
                device.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error closing device: " + ex.getMessage());
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
