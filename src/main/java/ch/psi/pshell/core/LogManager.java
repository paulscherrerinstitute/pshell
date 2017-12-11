package ch.psi.pshell.core;

import ch.psi.utils.Chrono;
import ch.psi.utils.Folder;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.ProcessVariable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Management of application logs.
 */
public class LogManager {

    FileHandler logFile;
    public static final String ROOT_LOGGER = "ch.psi";
    public static final String FILE_SEPARATOR = " - ";

    public static int LAST_LOGS_SIZE = 100;
    ArrayList<String[]> lastLogs = new ArrayList();

    public LogManager() {
        Logger globalLogger = Logger.getLogger("");
        Handler[] handlers = globalLogger.getHandlers();
        for (Handler handler : handlers) {
            handler.setFormatter(formatter);
        }
    }

    public static void setLevel(Level level) {
        if (level != null) {
            Logger.getLogger(ROOT_LOGGER).setLevel(level);
        }
    }

    public static Level getLevel() {
        return Logger.getLogger(ROOT_LOGGER).getLevel();
    }

    final Formatter formatter = new Formatter() {
        @Override
        public String format(LogRecord record) {
            String date = Chrono.getTimeStr(record.getMillis(), "dd/MM/YY");
            String time = Chrono.getTimeStr(record.getMillis(), "HH:mm:ss.SSS");
            StringBuilder sb = new StringBuilder();
            sb.append(date).append(FILE_SEPARATOR);
            sb.append(time).append(FILE_SEPARATOR);
            String logger = record.getLoggerName();
            int simpleNameIndex = logger.lastIndexOf(".") + 1;
            if (simpleNameIndex >= (logger.length() - 1)) {
                simpleNameIndex = 0;
            }
            logger = logger.substring(simpleNameIndex); // Only the simple class name to simplify visualization
            sb.append(logger).append(".").append(record.getSourceMethodName()).append(FILE_SEPARATOR);
            sb.append(Str.capitalizeFirst(record.getLevel().toString().toLowerCase())).append(FILE_SEPARATOR);
            if (record.getThrown() != null) {
                Throwable t = record.getThrown();
                sb.append(t.toString());
                sb.append("\nStack trace:");
                StringWriter stackTrace = new StringWriter();
                t.printStackTrace(new PrintWriter(stackTrace));
                sb.append("\n").append(stackTrace.toString());
            } else {
                sb.append(record.getMessage());
            }
            sb.append("\n");
            return sb.toString();
        }
    };

    public void start(String fileName, int daysToLive) {
        try {
            if (fileName != null) {
                String folder = IO.getFolder(fileName);
                //Try cleaning old lock files
                for (File f : IO.listFiles(folder, "*.lck")) {
                    try {
                        f.delete();
                    } catch (Exception ex) {
                        //Don't care about it
                    }
                }

                if (logFile != null) {
                    try {
                        removeHandler(logFile);
                    } catch (Exception ex) {
                        System.err.println(ex);
                    }
                }

                logFile = new FileHandler(fileName);
                logFile.setFormatter(formatter);
                addHandler(logFile);
                if (daysToLive > 0) {
                    new Folder(folder).cleanup(daysToLive * 24 * 3600 * 1000, true, false, false);
                }
            }

            addHandler(lastLogsHandler);

        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    final Handler lastLogsHandler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            synchronized (lastLogs) {
                lastLogs.add(parseLogRecord(record));
                if (lastLogs.size() >= LAST_LOGS_SIZE) {
                    lastLogs.remove(0);
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    };

    //TODO: Understand why not using ROOT_LOGGER
    public static void addHandler(Handler handler) {
        if (!Arr.contains(Logger.getLogger(ROOT_LOGGER).getHandlers(), handler)) {
            Logger.getLogger(ROOT_LOGGER).addHandler(handler);
        }
    }

    public static void removeHandler(Handler handler) {
        Logger.getLogger(ROOT_LOGGER).removeHandler(handler);
    }

    public List<String[]> getLastLogs() {
        synchronized (lastLogs) {
            return (ArrayList<String[]>) lastLogs.clone();
        }
    }

    public static void setConsoleLoggerLevel(Level level) {
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(level);
            }
        }
    }

    public static String[] parseLogRecord(LogRecord record) {
        String date = Chrono.getTimeStr(record.getMillis(), "dd/MM/YY");
        String time = Chrono.getTimeStr(record.getMillis(), "HH:mm:ss.SSS");
        String logger = record.getLoggerName();
        int simpleNameIndex = logger.lastIndexOf(".") + 1;
        if (simpleNameIndex >= (logger.length() - 1)) {
            simpleNameIndex = 0;
        }
        logger = logger.substring(logger.lastIndexOf(".") + 1); // Only the simple class name to simplify visualization                
        logger = logger + "." + record.getSourceMethodName();

        String level = Str.capitalizeFirst(record.getLevel().toString().toLowerCase());
        String description = record.getMessage();
        if (record.getThrown() != null) {
            description = record.getThrown().toString();
        }
        return new String[]{date, time, logger, level, description};
    }

    public static String[] getDeviceInfo(GenericDevice dev, int maxElements) {
        String info = "";
        if (dev.isSimulated()) {
            info = "Simulated";
        } else if (dev.isMonitored()) {
            info = "Monitored";
        } else if (dev.isPolled()) {
            info = "Polled";
        }
        String units = (dev instanceof ProcessVariable) ? " " + ((ProcessVariable) dev).getUnit() : "";
        try {
            if (dev instanceof ReadbackDevice) {
                dev = ((ReadbackDevice) dev).getReadback();
            }
            Object value = dev.take();
            if (value != null) {
                Integer age = dev.getAge();
                return new String[]{Str.toString(value, maxElements) + units, Chrono.getEllapsedStr(age, "HH:mm:ss"), info};
            }
        } catch (Exception ex) {
        }
        return new String[]{"", "", info};
    }

    static String getLogContents(String path) throws IOException {
        File file = Paths.get(Context.getInstance().getSetup().getLogPath(), path).toFile();
        if (file.exists()) {
            if (file.isDirectory()) {
                StringBuilder ret = new StringBuilder();
                for (File f : IO.listSubFolders(file)) {
                    ret.append(f.getName()).append("/\n");
                }
                for (File f : IO.listFiles(file, "*.log")) {
                    ret.append(f.getName()).append("\n");
                }
                return ret.toString();
            } else {
                return new String(Files.readAllBytes(file.toPath()));
            }
        }
        throw new FileNotFoundException(path);
    }

    public static List<String[]> search(List<String> files, Level level, String origin, String text, Calendar start, Calendar end) throws IOException {
        Collections.sort(files);
        List<String[]> ret = new ArrayList<String[]>();
        for (String file : files) {
            search(file, level, origin, text, start, end, ret);
        }
        return ret;
    }

    public static List<String[]> search(String file, Level level, String origin, String text, Calendar start, Calendar end, List<String[]> list) throws IOException {
        origin = origin.trim();
        if (origin.length() == 0) {
            origin = null;
        }
        text = text.trim();
        if (text.length() == 0) {
            text = null;
        }
        List<String[]> ret = list == null ? (new ArrayList<String[]>()) : list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Calendar time = Calendar.getInstance();
            boolean addedLastRecord = false;
            while ((line = br.readLine()) != null) {
                try {
                    String[] tokens = line.split(FILE_SEPARATOR);
                    if (tokens.length >= 5) {
                        if (tokens.length > 5) {
                            ArrayList<String> aux = new ArrayList();
                            for (int i = 4; i < tokens.length; i++) {
                                aux.add(tokens[i]);
                            }
                            tokens[4] = String.join(FILE_SEPARATOR, aux);
                            tokens = new String[]{tokens[0], tokens[1], tokens[2], tokens[3], tokens[4]};
                        }
                        addedLastRecord = false;
                        time.setTime(new SimpleDateFormat("dd/MM/YY HH:mm:ss.SSS").parse(tokens[0] + " " + tokens[1]));
                        if ((origin == null) || (tokens[2].contains(origin))) {
                            if ((level == null) || (level.intValue() <= Level.parse(tokens[3].toUpperCase()).intValue())) {
                                if ((text == null) || (tokens[4].contains(text))) {
                                    if ((start == null) || (time.after(start))) {
                                        if ((end == null) || (time.before(end))) {
                                            ret.add(tokens);
                                            addedLastRecord = true;
                                        }
                                    }
                                }
                            }
                        }
                    } else if (addedLastRecord) {
                        ret.get(ret.size() - 1)[4] = ret.get(ret.size() - 1)[4] + "\n" + line;
                    }
                } catch (Exception ex) {
                }
            }
        }
        return ret;
    }

    public static String getLogForValue(Object value) {
        int[] shape = Arr.getShape(value);
        String log = null;
        if (shape.length >= 2) {
            return "[" + Convert.arrayToString(shape, " x ") + "]";
        }
        return Str.toString(value, 10);
    }
}
