package ch.psi.pshell.core;

import java.io.File;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
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
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.imaging.ImageBuffer;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.scan.DataAccessDummyScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scan.ScanResult;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.UsersManagerListener;
import ch.psi.utils.Config;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    @Path("evalAsync/{statement : .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public long evalAsync(@PathParam("statement") final String statement) throws ExecutionException {
        try {
            String cmd = formatIncomingText(statement);
            CompletableFuture cf = context.evalLineAsync(CommandSource.server, cmd.equals("\n") ? "" : cmd); //\n is token for empty string
            return Context.getInstance().waitNewCommand(cf);
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
            Object ret = context.evalLine(CommandSource.server, cmd.equals("\n") ? "" : cmd);
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
            context.setInterpreterVariable(name, value);
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
        List<String> history = context.getHistory();
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
            Object ret = context.getDataManager().getInfo(request);
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
            Object ret = context.getDataManager().getAttributes(request);
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
            File file = new File(context.getSetup().expandPath(((path.startsWith("/")) ? "{data}"+path : "{data}/"+path)).trim());
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
            List<Plot> plots = context.getPlots((title.isBlank() || title.equals("null")) ? null : title);
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
            List<Plot> plots = context.getPlots((title.isBlank() || title.equals("null")) ? null : title);
            return plots.size();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
    
  
    @DELETE
    @Path("plots/{title}")
    public Response deletePlotContext(@PathParam("title") final String title) {        
        try {
            if (context.removePlotContext(title)){
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
            List<String> ret = context.getPlotTitles();
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
            File file = Paths.get(context.setup.getScriptPath(), path).toFile();
            if (file.exists()) {
                if (file.isDirectory()) {
                    //Subfolders
                    for (File f : IO.listSubFolders(file)) {
                        if (!Arr.containsEqual(new String[]{"Lib", "cachedir"}, f.getName())) {
                            ret.append(f.getName()).append("/\n");
                        }
                    }
                    for (File f : IO.listFiles(file, "*." + Context.getInstance().getScriptType().getExtension())) {
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
            File file = Paths.get(context.setup.getScriptPath(), path).toFile();
            if (file.exists()) {
                if (file.isDirectory()) {
                    throw new Exception("Path is a directory: " + path);
                }
            } else {
                file.getParentFile().mkdirs();
            }
            logger.warning("Saving script file: " + path);            
            Files.writeString(file.toPath(), contents);
            IO.setFilePermissions(file, context.getConfig().filePermissionsScripts);
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
            Boolean background = contents.containsKey("background") ? (Boolean) contents.get("background") : false;
            Boolean async = contents.containsKey("async") ? (Boolean) contents.get("async") : false;
            Object ret = null;
            CompletableFuture cf = null;
            if (async) {
                if (background) {
                    cf = Context.getInstance().evalFileBackgroundAsync(CommandSource.server, script, pars);

                } else {
                    cf = Context.getInstance().evalFileAsync(CommandSource.server, script, pars);
                }
                return Context.getInstance().waitNewCommand(cf);
            } else {
                if (background) {
                    ret = Context.getInstance().evalFileBackground(CommandSource.server, script, pars);
                } else {
                    ret = Context.getInstance().evalFile(CommandSource.server, script, pars);
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
                    if (Arr.containsEqual(Context.getInstance().getBuiltinFunctionsNames(), input)) {
                        ret.add(Context.getInstance().getBuiltinFunctionDoc(input));
                    } else {
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
    @Path("pause")
    public void pause() throws ExecutionException {
        try {
            context.pause(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }   
    
    @GET
    @Path("resume")
    public void resume() throws ExecutionException {
        try {
            context.resume(CommandSource.server);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }    

    @GET
    @Path("abort/{commandId}")
    public boolean abort(@PathParam("commandId") final Integer commandId) throws ExecutionException {
        try {
            return context.abort(CommandSource.server, commandId);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("result")
    @Produces(MediaType.APPLICATION_JSON)
    public Object result() throws ExecutionException {
        try {
            return mapper.writeValueAsString(context.getResult(-1));
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @GET
    @Path("result/{commandId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object result(@PathParam("commandId") final Integer commandId) throws ExecutionException {
        try {
            return mapper.writeValueAsString(context.getResult(commandId));
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

    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onContextStateChanged(State state, State former) {
            sendEvent("state", context.getState());
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
                    sendShell(source, context.interpreterVariableToString(result));
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
                    printScan = (value == null) ? false : (Boolean) value;
                    break;
                case PLOT_DISABLED:
                    plotScan = (value == null) ? true : !((Boolean) value);
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
            if (!Context.getInstance().getExecutionPars().isScanDisplayed(scan)) {
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
            if (!Context.getInstance().getExecutionPars().isScanDisplayed(scan)) {
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
            context.addListener(contextListener); //If already a listener does nothing
            context.addScanListener(scanListener); //If already a listener does nothing
            context.addEventListener(eventListener);
            context.getUsersManager().addListener(userListener);
            context.remoteUserInterface = remoteUserInterface;
            context.serverPlotListener = plotListener;
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

        @Override
        public Object showPanel(GenericDevice dev) throws InterruptedException {
            throw new java.lang.UnsupportedOperationException();
        }

        @Override
        public Object showPanel(Config config) throws InterruptedException {
            throw new java.lang.UnsupportedOperationException();
        }
        
        @Override
        public Object showPanel(ScanResult result) throws InterruptedException {
            throw new java.lang.UnsupportedOperationException();
        }

        @Override
        public int waitKey(int timeout) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
}
