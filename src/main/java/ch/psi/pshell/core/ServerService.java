package ch.psi.pshell.core;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import ch.psi.utils.Arr;
import ch.psi.utils.IO;
import ch.psi.utils.State;
import ch.psi.pshell.data.DataServer;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.UsersManagerListener;
import ch.psi.utils.Str;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Definition of the application REST API.
 */
@Path("/")
public class ServerService {

    final static private ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = Logger.getLogger(ServerService.class.getName());

    @Inject
    private Context context;

    @Inject
    private SseBroadcaster broadcaster;

    public class ExecutionException extends WebApplicationException {

        public ExecutionException(Exception cause) {
            super(Response.status(Status.INTERNAL_SERVER_ERROR).
                    entity(cause.getMessage()).type("text/plain").build());
        }
    }

    @GET
    @Path("version")
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "0.0.0";
        }
        return version;
    }

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Configuration getConfig() {
        return Context.getInstance().getConfig();
    }

    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    //@Produces(MediaType.TEXT_PLAIN)
    public State getState() {
        return context.getState();
    }

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List getUser() {
        return getUserInfo();
    }

    @GET
    @Path("devices")
    @Produces(MediaType.APPLICATION_JSON)
    //@Produces(MediaType.TEXT_PLAIN)
    public List<List<String>> getDevices() {
        ArrayList<List<String>> ret = new ArrayList<>();
        try {
            for (GenericDevice dev : context.getDevicePool().getAllDevices()) {
                ArrayList<String> entry = new ArrayList<>();
                entry.add(dev.getName());
                entry.add(Nameable.getShortClassName(dev.getClass()));
                entry.add(dev.getState().toString());
                String[] value = LogManager.getDeviceInfo(dev, 5);
                entry.add(value[0]);
                entry.add(value[1]);
                ret.add(entry);
            }
        } catch (Exception ex) {
        }
        return ret;
    }

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String[]> getLogs() {
        try {
            List<String[]> ret = context.getLogManager().getLastLogs();
            Collections.reverse(ret);
            return ret;
        } catch (Exception ex) {
        }
        return new ArrayList<>();
    }

    @GET
    @Path("logs/{path : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getLogs(@PathParam("path") final String path) throws ExecutionException {
        try {
            return context.getLogManager().getLogContents(path);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    String formatIncomingText(String text) {
        //TODO: I'm not able to encode backslash directly, %5C is not accepted by server.
        //Workaround replacing by a  double dagger is quite ugly.
        return text.replace("‡", "\\");
    }

    @GET
    @Path("eval/{statement : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String eval(@PathParam("statement") final String statement) throws ExecutionException {
        try {
            String cmd = formatIncomingText(statement);
            Object ret = context.evalLine(CommandSource.server, cmd.equals("\n") ? "" : cmd); //\n is token for empty string
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("history/{index}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String history(@PathParam("index") final Integer index) {
        List<String> history = context.getHistory();
        if ((index >= 0) && (index < history.size())) {
            return (history.get(history.size() - index - 1));
        }
        throw new IllegalArgumentException("Wrong history index");
    }

    @GET
    @Path("data")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String dataRequest() throws ExecutionException {
        try {
            Object ret = DataServer.execute("");
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("data/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String dataRequest(@PathParam("request") final String request) throws ExecutionException {
        try {
            Object ret = DataServer.execute(request);
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("contents")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String dataContents() throws ExecutionException {
        try {
            Object ret = DataServer.getContents("");
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("contents/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String dataContents(@PathParam("request") final String request) throws ExecutionException {
        try {
            Object ret = DataServer.getContents(request);
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("script/{path : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String script(@PathParam("path") final String path) throws ExecutionException {
        try {
            StringBuffer ret = new StringBuffer();
            File file = Paths.get(context.setup.getScriptPath(), path).toFile();
            if (file.exists()) {
                if (file.isDirectory()) {
                    //Subfolders
                    for (File f : IO.listSubFolders(file)) {
                        if (!Arr.containsEqual(new String[]{"Lib", "cachedir"}, f.getName())) {
                            ret.append(f.getName()).append("/\n");
                        }
                    }
                    for (File f : IO.listFiles(file, "*." + Context.getInstance().getScriptType().toString())) {
                        ret.append(f.getName()).append("\n");
                    }
                } else {
                    ret.append(new String(Files.readAllBytes(file.toPath())));
                }
            }
            return ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
    @GET
    @Path("run/{contents : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String run(@PathParam("contents") final String contents) throws ExecutionException {
        try {
            String script = formatIncomingText(contents).trim();
            boolean background = contents.trim().endsWith("&");
            if (background) {
                script = script.substring(0, script.length() - 1);
            }
            List argList = null;
            if (script.contains("(")) {
                String args = script.substring(script.indexOf("(") + 1);
                if (args.contains(")")) {
                    argList = new ArrayList();
                    args = args.substring(0, args.lastIndexOf(")"));
                    script = script.substring(0, script.indexOf("("));
                    String[] tokens = Str.splitIgnoringQuotes(args, ",");                    
                    for (String token : tokens) {
                        argList.add(Str.removeQuotes(token).trim());
                    }
                }
            }
            if (background) {
                return String.valueOf(Context.getInstance().evalFileBackground(CommandSource.server, script, argList));
            } else {
                return String.valueOf(Context.getInstance().evalFile(CommandSource.server, script, argList));
            }
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
        
    @PUT
    @Path("run")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object run(final Map contents) throws ExecutionException {
        try {
            String script = (String) contents.get("script");
            Object pars = contents.get("pars");
            Boolean background = (Boolean) contents.get("background");
            Object ret = null;
            if (background) {
                ret = Context.getInstance().evalFileBackground(CommandSource.server, script, pars);
            } else {
                ret = Context.getInstance().evalFile(CommandSource.server, script, pars);
            }
            return mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("evalScript/{contents : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String evalScript(@PathParam("contents") final String contents) throws ExecutionException {
        try {
            Statement[] statements = Context.getInstance().parseString(formatIncomingText(contents), "Unknown");
            return String.valueOf(Context.getInstance().evalStatements(CommandSource.server, statements, false, "Unknown", null));
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("autocompletion/{input}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAutoCompletion(@PathParam("input") final String input) throws ExecutionException {
        try {
            List<String> ret = new ArrayList<>();
            switch (input) {
                case ":":
                    for (ControlCommand cmd : ControlCommand.values()) {
                        ret.add(cmd.toString());
                    }
                    break;
                case "<builtins>":
                    for (String function : Context.getInstance().getBuiltinFunctionsNames()) {
                        ret.add(Context.getInstance().getBuiltinFunctionDoc(function).split("\n")[0]);
                    }
                    break;
                case "<devices>":
                    for (GenericDevice dev : Context.getInstance().getDevicePool().getAllDevices()) {
                        ret.add(dev.getName() + " (" + Nameable.getShortClassName(dev.getClass()) + ")");
                    }
                    break;
                default:
                    List<String> signatures = null;
                    if (input.endsWith(".")) {
                        signatures = Console.getSignatures(input, input.length() - 1, true);
                    } else {
                        signatures = Console.getSignatures(input, input.length(), true);
                    }
                    if (signatures != null) {
                        ret.addAll(signatures);
                    }
            }
            return ret;
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("abort")
    public void abort() throws ExecutionException {
        try {
            context.abort(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("reinit")
    public void reinit() throws ExecutionException {
        try {
            context.reinit(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("stop")
    public void stop() throws ExecutionException {
        try {
            context.stopAll(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("update")
    public void update() throws ExecutionException {
        try {
            context.updateAll(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onContextStateChanged(State state, State former) {
            if (broadcaster != null) {
                OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                OutboundEvent event = eventBuilder.name("state")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(context.getState())
                        .build();
                broadcaster.broadcast(event);
            }
        }

        @Override
        public void onShellCommand(CommandSource source, String command) {
            sendShell(source, context.getCursor(command) + command);
        }

        @Override
        public void onShellResult(CommandSource source, Object result) {
            if (result != null) {
                if (result instanceof Throwable) {
                    sendShell(source, Console.getPrintableMessage((Throwable) result));
                } else {
                    sendShell(source, String.valueOf(result));
                }
            }
        }

        @Override
        public void onShellStdout(String str) {
            sendShell(null, str);
        }

        @Override
        public void onShellStdin(String str) {
            sendShell(null, str);
        }

        @Override
        public void onShellStderr(String str) {
            sendShell(null, str);
        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {
            switch (preference) {
                case PRINT_SCAN:
                    printScan = (value==null) ? false : (Boolean) value;
                    break;
                case PLOT_DISABLED:
                    plotScan = (value==null) ? true : !((Boolean) value);
                    break;
            }
        }

    };

    boolean plotScan = true;
    boolean printScan;

    void sendShell(CommandSource source, String line) {
        line = line.replace("&", "&amp;");
        line = line.replace("<", "&lt;");
        line = line.replace(">", "&gt;");
        if (broadcaster != null) {
            OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            OutboundEvent event = eventBuilder.name("shell")
                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                    .data(line)
                    .build();
            broadcaster.broadcast(event);
        }
    }

    final ScanListener scanListener = new ScanListener() {

        void sendScanStart(Scan scan) {
            if (broadcaster != null) {
                OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                OutboundEvent event = eventBuilder.name("scan")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(scan.getReadableNames())
                        .build();
                broadcaster.broadcast(event);
            }
        }

        void sendScanRecord(Scan scan, ScanRecord record) {
            if (broadcaster != null) {
                OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                OutboundEvent event = eventBuilder.name("record")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(record)
                        .build();
                broadcaster.broadcast(event);
            }
        }

        void sendProgress(Double progress) {
            this.progress = progress;
            if (broadcaster != null) {
                OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                OutboundEvent event = eventBuilder.name("progress")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(progress)
                        .build();
                broadcaster.broadcast(event);
            }
        }
        double progress;
        double step;

        @Override
        public void onScanStarted(Scan scan, final String plotTitle) {
            sendProgress(0.0);
            try {
                int records = scan.getNumberOfRecords();
                step = (1.0 / records);
            } catch (Exception ex) {
                step = 0.0;
            }
            if (printScan) {
                sendShell(CommandSource.server, scan.getHeader("\t"));
            }
            if (plotScan) {
                sendScanStart(scan);
            }
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            progress = Math.min(progress + step, 1.0);
            sendProgress(progress);

            if (printScan) {
                sendShell(CommandSource.server, record.print("\t"));
            }
            if (plotScan) {
                //Removing 2d data. 
                //TODO: Caution when enabling images: may be very slow and a strategy must be found
                record = record.copy(1);
                sendScanRecord(scan, record);
            }
        }

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
            sendProgress(-1.0);
        }
    };

    static boolean initialized;

    @GET
    @Path("events")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput subscribe() {
        if (!initialized) {
            initialized = true;
            context.addListener(contextListener); //If already a listener does nothing
            context.addScanListener(scanListener); //If already a listener does nothing            
            context.getUsersManager().addListener(userListener);
            context.remoteUserInterface = remoteUserInterface;
            //if (context.serverMode) {
            //    context.setPlotListener(plotListener);
            //} else {
            context.serverPlotListener = plotListener;
            //}
        }
        EventOutput eventOutput = new EventOutput();
        broadcaster.add(eventOutput);
        return eventOutput;
    }

    final UsersManagerListener userListener = new UsersManagerListener() {
        @Override
        public void onUserChange(User user, User former) {
            sendUser();
        }
    };

    final PlotListener plotListener = new PlotListener() {
        @Override
        public List plot(String context, PlotDescriptor[] plots) throws Exception {
            return sendPlot(context, plots);
        }

        @Override
        public List getPlots(String context) {
            return new ArrayList<>();
        }
    };

    ArrayList sendPlot(String context, PlotDescriptor[] plots) {
        if (broadcaster != null) {
            OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            OutboundEvent event = eventBuilder.name("plot")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(plots)
                    .build();
            broadcaster.broadcast(event);
        }
        return null;
    }

    void sendUser() {
        if (broadcaster != null) {
            OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            OutboundEvent event = eventBuilder.name("user")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(getUserInfo())
                    .build();
            broadcaster.broadcast(event);
        }
    }

    ArrayList getUserInfo() {
        ArrayList data = new ArrayList();
        data.add(context.getUser().name);
        data.add(context.getRights());
        return data;
    }

    static volatile String uiReturn;
    static final Object uiWait = new Object();

    @PUT
    @Path("ui/{val}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void ui(@PathParam("val") final String val) throws ExecutionException {
        synchronized (uiWait) {
            uiReturn = formatIncomingText(val);
            if (uiReturn.equals("\\z")) {
                uiReturn = null;
            }
            uiWait.notifyAll();
        }
    }

    String sendUICommand(String type, Object[] pars) throws InterruptedException {
        ArrayList data = new ArrayList<>();
        data.add(type);
        data.addAll(Arrays.asList(pars));
        uiReturn = null;
        if (broadcaster != null) {
            OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            OutboundEvent event = eventBuilder.name("ui")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(data)
                    .build();
            broadcaster.broadcast(event);
        }
        synchronized (uiWait) {
            uiWait.wait();
        }
        return uiReturn;
    }

    UserInterface remoteUserInterface = new UserInterface() {
        @Override
        public String getString(String message, String defaultValue) throws InterruptedException {
            return sendUICommand("getstring", new String[]{message, defaultValue});
        }

        @Override
        public String getString(String message, String defaultValue, String[] alternatives) throws InterruptedException {
            return sendUICommand("selectstring", new Object[]{message, defaultValue, alternatives});
        }

        @Override
        public String getPassword(String message, String title) throws InterruptedException {
            return sendUICommand("getpwd", new String[]{message, title});
        }

        @Override
        public String getOption(String message, String type) throws InterruptedException {
            return sendUICommand("getopt", new String[]{message, type});
        }

        @Override
        public void showMessage(String message, String title, boolean blocking) throws InterruptedException {
            sendUICommand("message", new String[]{message, title, String.valueOf(blocking)});
        }

        @Override
        public Object showPanel(GenericDevice dev) throws InterruptedException {
            throw new java.lang.UnsupportedOperationException();
        }
    };
}
