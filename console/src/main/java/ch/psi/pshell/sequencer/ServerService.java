package ch.psi.pshell.sequencer;

import ch.psi.pshell.data.DataServer;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.imaging.ImageBuffer;
import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.scan.DataAccessDummyScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.UsersManagerListener;
import ch.psi.pshell.swing.UserInterface;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Str;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 * Definition of the application REST API.
 */
@Path("/")
public class ServerService {

    final static private ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = Logger.getLogger(ServerService.class.getName());

    @Inject
    private Interpreter interpreter;

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
    public Config getConfig() {        
        return Context.getConfig();
    }

    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    //@Produces(MediaType.TEXT_PLAIN)
    public State getState() {
        return interpreter.getState();
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
            DevicePool dp = Context.getDevicePool();
            for (GenericDevice dev : dp.getAllDevices()) {
                ArrayList<String> entry = new ArrayList<>();
                entry.add(dev.getName());
                entry.add(Nameable.getShortClassName(dev.getClass()));
                entry.add(dev.getState().toString());
                String[] value = DevicePool.getDeviceInfo(dev, 5);
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
            List<String[]> ret = Context.getLogManager().getLastLogs();
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
            File file = Paths.get(Setup.getLogPath(), path).toFile();
            return LogManager.getLogContents(file);
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
            Object ret = interpreter.evalLine(CommandSource.server, cmd.equals("\n") ? "" : cmd); //\n is token for empty string
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
    @GET
    @Path("evalAsync/{statement : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public long evalAsync(@PathParam("statement") final String statement) throws ExecutionException {
        try {
            String cmd = formatIncomingText(statement);
            CompletableFuture cf = interpreter.evalLineAsync(CommandSource.server, cmd.equals("\n") ? "" : cmd); //\n is token for empty string
            return interpreter.waitNewCommand(cf);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
    @GET
    @Path("eval-json/{statement : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String evalJson(@PathParam("statement") final String statement) throws ExecutionException {
        try {
            String cmd = formatIncomingText(statement);
            Object ret = interpreter.evalLine(CommandSource.server, cmd.equals("\n") ? "" : cmd);
            return mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
    @PUT
    @Path("set-var")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setVariable(final Map contents) throws ExecutionException {
        try {
            String name = (String) contents.get("name");
            Object value = contents.get("value");
            interpreter.setInterpreterVariable(name, value);
            return Response.ok().build();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
        

    @GET
    @Path("history/{index}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String history(@PathParam("index") final Integer index) throws ExecutionException {
        List<String> history = interpreter.getHistoryEntries();
        if ((index >= 0) && (index < history.size())) {
            return (history.get(history.size() - index - 1));
        }
        throw new ExecutionException(new IllegalArgumentException("Wrong history index"));
    }

    @GET
    @Path("data")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String dataRequest() throws ExecutionException {
        try {
            Object ret = DataServer.execute("", "txt");
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
            Object ret = DataServer.execute(request, "txt");
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
    @Path("data-info/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String dataInfoRequest(@PathParam("request") final String request) throws ExecutionException {
        try {
            Object ret = Context.getDataManager().getInfo(request);
            return (ret == null) ? "" : mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
    @GET
    @Path("data-attr/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String dataAttrRequest(@PathParam("request") final String request) throws ExecutionException {
        try {
            Object ret = Context.getDataManager().getAttributes(request);
            return (ret == null) ? "" : mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("data-json/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String jsonDataRequest(@PathParam("request") final String request) throws ExecutionException {
        try {
            Object ret = DataServer.execute(request, "json");
            return (ret == null) ? "" : ret.toString();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }

    }

    @GET
    @Path("data-bs/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] bsDataRequest(@PathParam("request") final String request) throws ExecutionException {
        try {
            List<byte[]> req = (List<byte[]>) DataServer.execute(request, "bs");
            int size = 16;
            for (byte[] arr : req) {
                size += arr.length;
            }
            byte[] ret = new byte[size];
            int index = 0;
            for (byte[] arr : req) {
                ByteBuffer b = ByteBuffer.allocate(Integer.BYTES);
                b.putInt(arr.length);
                System.arraycopy(b.array(), 0, ret, index, Integer.BYTES);
                index += Integer.BYTES;
                System.arraycopy(arr, 0, ret, index, arr.length);
                index += arr.length;
            }
            return ret;
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("data-bin/{request : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] binDataRequest(@PathParam("request") final String request) throws ExecutionException {
        try {
            return (byte[]) DataServer.execute(request, "bin");
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
    

    @GET
    @Path("scandata/{layout}/{path}/{group}/{dev}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String scanDataRequest(@PathParam("layout") final String layout,
                                      @PathParam("path") final String path,                                  
                                      @PathParam("group") final String group,                                  
                                      @PathParam("dev") final String dev
                                      ) throws ExecutionException {
        try {
            Layout l = (layout.isBlank() || layout.equals("null")) ? null : (Layout)Class.forName(layout.replace("<br>", ".")).newInstance();
            String p = path.replace("<br>", "/").replace("<p>", "|");
            String g = group.replace("<br>", "/");
            Object ret = DataAccessDummyScan.readScanData(null, l, p, g, dev);            
            return Str.toString(ret, -1);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }    
    
    
    @GET
    @Path("scandata-json/{layout}/{path}/{group}/{dev}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String jsonScanDataRequest(@PathParam("layout") final String layout,
                                      @PathParam("path") final String path,                                  
                                      @PathParam("group") final String group,                                  
                                      @PathParam("dev") final String dev
                                      ) throws ExecutionException {
        try {
            Layout l = (layout.isBlank() || layout.equals("null")) ? null : (Layout)Class.forName(layout.replace("<br>", ".")).newInstance();
            String p = path.replace("<br>", "/").replace("<p>", "|");
            String g = group.replace("<br>", "/");
            Object ret = DataAccessDummyScan.readScanData(null, l, p, g, dev);            
            return (ret == null) ? "" : mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }    

    
    @GET
    @Path("scandata-bin/{layout}/{path}/{group}/{dev}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] binScanDataRequest(@PathParam("layout") final String layout,
                                      @PathParam("path") final String path,                                  
                                      @PathParam("group") final String group,                                  
                                      @PathParam("dev") final String dev
                                      ) throws ExecutionException {
        try {
            Layout l = (layout.isBlank() || layout.equals("null")) ? null : (Layout)Class.forName(layout.replace("<br>", ".")).newInstance();
            String p = path.replace("<br>", "/").replace("<p>", "|");
            String g = group.replace("<br>", "/");
            Object ret = DataAccessDummyScan.readScanData(null, l, p, g, dev);  
            return Convert.toByteArray(ret);  
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }    
        
    @GET
    @Path("/download{path : .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] dataDownload(@PathParam("path") final String path) {
        try{
            File file = new File(Setup.expandPath(((path.startsWith("/")) ? "{data}"+path : "{data}/"+path)).trim());
            if (!file.isFile()){
                throw new Exception("Invalid file name: " + path);
            }
            return Files.readAllBytes(file.toPath());
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }        
    }    
   
    @GET
    @Path("plot/{title}/{index}/{format}/{width}/{height}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] getPlot(@PathParam("title") final String title,
            @PathParam("index") final int index,
            @PathParam("format") final String format,
            @PathParam("width") final int width,
            @PathParam("height") final int height) throws ExecutionException {
        try {
            List<Plot> plots = interpreter.getPlots((title.isBlank() || title.equals("null")) ? null : title);
            Dimension size = ((width > 0) && (height > 0)) ? new Dimension(width, height) : null;
            Plot plot = plots.get(index);
            BufferedImage img = plot.getSnapshot(size);
            return ImageBuffer.getImage(img, (format.isBlank() || format.equals("null")) ? null : format);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("plots/{title}")
    @Produces(MediaType.TEXT_PLAIN)
    public int getNumPlots(@PathParam("title") final String title) throws ExecutionException {
        try {
            List<Plot> plots = interpreter.getPlots((title.isBlank() || title.equals("null")) ? null : title);
            return plots.size();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
  
    @DELETE
    @Path("plots/{title}")
    public Response deletePlotContext(@PathParam("title") final String title) {        
        try {
            if (interpreter.removePlotContext(title)){
                return Response.ok().build();
            } else {
                return Response.notModified().build();
            }
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }       
        
    @GET
    @Path("plots")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPlotTitles() throws ExecutionException {
        try {
            List<String> ret = interpreter.getPlotTitles();
            return (ret == null) ? "" : mapper.writeValueAsString(ret);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }    

    @GET
    @Path("script/{path : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getScript(@PathParam("path") final String path) throws ExecutionException {
        try {
            StringBuffer ret = new StringBuffer();
            File file = Paths.get(Setup.getScriptsPath(), path).toFile();
            if (file.exists()) {
                if (file.isDirectory()) {
                    //Subfolders
                    for (File f : IO.listSubFolders(file)) {
                        if (!Arr.containsEqual(new String[]{"Lib", "cachedir"}, f.getName())) {
                            ret.append(f.getName()).append("/\n");
                        }
                    }
                    for (File f : IO.listFiles(file, "*." + interpreter.getScriptType().getExtension())) {
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

    @PUT
    @Path("script")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setScript(final Map data) throws ExecutionException {
        try {
            String path = (String) data.get("path");
            String contents =  (String) data.get("contents");
            File file = Paths.get(Setup.getScriptsPath(), path).toFile();
            if (file.exists()) {
                if (file.isDirectory()) {
                    throw new Exception("Path is a directory: " + path);
                }
            } else {
                file.getParentFile().mkdirs();
            }
            logger.log(Level.WARNING, "Saving script file: {0}", path);            
            Files.writeString(file.toPath(), contents);
            IO.setFilePermissions(file, Context.getScriptFilePermissions());
            return Response.ok().build();   
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
                    String[] tokens = Str.splitWithQuotes(args, ",");
                    for (String token : tokens) {
                        argList.add(Str.removeQuotes(token).trim());
                    }
                }
            }
            if (background) {
                return String.valueOf(interpreter.evalFileBackground(CommandSource.server, script, argList));
            } else {
                return String.valueOf(interpreter.evalFile(CommandSource.server, script, argList));
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
            Boolean background = contents.containsKey("background") ? (Boolean) contents.get("background") : false;
            Boolean async = contents.containsKey("async") ? (Boolean) contents.get("async") : false;
            Object ret = null;
            CompletableFuture cf = null;
            if (async) {
                if (background) {
                    cf = interpreter.evalFileBackgroundAsync(CommandSource.server, script, pars);

                } else {
                    cf = interpreter.evalFileAsync(CommandSource.server, script, pars);
                }
                return interpreter.waitNewCommand(cf);
            } else {
                if (background) {
                    ret = interpreter.evalFileBackground(CommandSource.server, script, pars);
                } else {
                    ret = interpreter.evalFile(CommandSource.server, script, pars);
                }
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
            Statement[] statements = interpreter.parseString(formatIncomingText(contents), "Unknown");
            return String.valueOf(interpreter.evalStatements(CommandSource.server, statements, false, "Unknown", null));
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
                    for (String function : interpreter.getBuiltinFunctionsNames()) {
                        ret.add(interpreter.getBuiltinFunctionDoc(function).split("\n")[0]);
                    }
                    break;
                case "<devices>":
                    for (GenericDevice dev : Context.getDevicePool().getAllDevices()) {
                        ret.add(dev.getName() + " (" + Nameable.getShortClassName(dev.getClass()) + ")");
                    }
                    break;
                default:
                    if (Arr.containsEqual(interpreter.getBuiltinFunctionsNames(), input)) {
                        ret.add(interpreter.getBuiltinFunctionDoc(input));
                    } else {
                        List<String> signatures = null;
                        if (input.endsWith(".")) {
                            signatures = InterpreterUtils.getSignatures(input, input.length() - 1, true);
                        } else {
                            signatures = InterpreterUtils.getSignatures(input, input.length(), true);
                        }
                        if (signatures != null) {
                            ret.addAll(signatures);
                        }
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
            interpreter.abort(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("pause")
    public void pause() throws ExecutionException {
        try {
            interpreter.pause(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }   
    
    @GET
    @Path("resume")
    public void resume() throws ExecutionException {
        try {
            interpreter.resume(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }    

    @GET
    @Path("abort/{commandId}")
    public boolean abort(@PathParam("commandId") final Integer commandId) throws ExecutionException {
        try {
            return interpreter.abort(CommandSource.server, commandId);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    public Object result() throws ExecutionException {
        try {
            return mapper.writeValueAsString(interpreter.getResult(-1));
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("result/{commandId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object result(@PathParam("commandId") final Integer commandId) throws ExecutionException {
        try {
            return mapper.writeValueAsString(interpreter.getResult(commandId));
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("reinit")
    public void reinit() throws ExecutionException {
        try {
            interpreter.reinit(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("stop")
    public void stop() throws ExecutionException {
        try {
            interpreter.stopAll(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("update")
    public void update() throws ExecutionException {
        try {
            interpreter.updateAll(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    public void sendEvent(String name, Object value) {
        sendEvent(name, value, MediaType.APPLICATION_JSON_TYPE);
    }

    public void sendEventAsText(String name, String value) {
        sendEvent(name, value, MediaType.TEXT_PLAIN_TYPE);
    }

    void sendEvent(String name, Object value, MediaType mediaType) {
        if (broadcaster != null) {
            OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            OutboundEvent event = eventBuilder.name(name)
                .mediaType(mediaType)
                .data(value)
                .build();
        broadcaster.broadcast(event);
        }
    }

    final InterpreterListener contextListener = new InterpreterListener() {
        @Override
        public void onStateChanged(State state, State former) {
            sendEvent("state", interpreter.getState());
        }

        @Override
        public void onShellCommand(CommandSource source, String command) {
            sendShell(source, interpreter.getCursor(command) + command);
        }

        @Override
        public void onShellResult(CommandSource source, Object result) {
            if (result != null) {                
                if (result instanceof Throwable t) {
                    sendShell(source, InterpreterResult.getPrintableMessage(t));
                } else {
                    sendShell(source, interpreter.interpreterVariableToString(result));
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
                case PRINT_SCAN -> printScan = (value == null) ? false : (Boolean) value;
                case PLOT_DISABLED -> plotScan = (value == null) ? true : !((Boolean) value);
            }
        }

    };

    boolean plotScan = true;
    boolean printScan;

    void sendShell(CommandSource source, String line) {
        line = line.replace("&", "&amp;");
        line = line.replace("<", "&lt;");
        line = line.replace(">", "&gt;");
        sendEventAsText("shell", line);
    }

    final EventListener eventListener = (String name, Object value) -> {
        ServerService.this.sendEvent(name, value);
    };

    final ScanListener scanListener = new ScanListener() {

        void sendScanStart(Scan scan) {
            sendEvent("scan", scan.getResult());
        }

        void sendScanRecord(Scan scan, ScanRecord record) {
            sendEvent("record", record);
        }

        void sendProgress(Double progress) {
            this.progress = progress;
            sendEvent("progress", progress);
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
            if (!interpreter.getExecutionPars().isScanDisplayed(scan)) {
                return;
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
            if (!interpreter.getExecutionPars().isScanDisplayed(scan)) {
                return;
            }
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
            interpreter.addListener(contextListener); //If already a listener does nothing
            interpreter.addScanListener(scanListener); //If already a listener does nothing
            interpreter.addEventListener(eventListener);            
            interpreter.remoteUserInterface = remoteUserInterface;
            interpreter.serverPlotListener = plotListener;
            if (Context.hasUsersManager()){
                Context.getUsersManager().addListener(userListener);
            }
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
        Map info = new HashMap();
        info.put("context", context);
        info.put("plots", plots);        
        sendEvent("plot", info);
        return null;
    }

    void sendUser() {
        sendEvent("user", getUserInfo());
    }

    ArrayList getUserInfo() {
        ArrayList data = new ArrayList();
        data.add(Context.getUserName());
        data.add(Context.getRights());
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
        sendEvent("ui", data);
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
    };
}
