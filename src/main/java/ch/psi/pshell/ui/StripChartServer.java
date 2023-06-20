package ch.psi.pshell.ui;

import ch.psi.utils.EncoderJson;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.SwingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Window;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;

/**
 * 
 */
public class StripChartServer implements AutoCloseable {
    public  static final int DEFAULT_PORT = 17321;
    public  static final String DEFAULT_URL = "localhost"; //InetAddress.getLocalHost().getHostName()
    private static final Logger logger = Logger.getLogger(StripChartServer.class.getName());

    HttpServer server;
    final int port;
    final URI baseUri;

    public int getPort() {
        return port;
    }

    public String getBaseURL() {
        return baseUri.toString();
    }
    
    public static class ExecutionException extends WebApplicationException {

        public ExecutionException(Exception cause) {
            super(Response.status(Status.INTERNAL_SERVER_ERROR).
                    entity(cause.toString()).type("text/plain").build());
        }
    } 
    
    public StripChartServer() {
        this(DEFAULT_URL, DEFAULT_PORT);
    }

    public StripChartServer(String hostname, Integer port)  {
        logger.info("Initializing " + getClass().getSimpleName());      
        System.out.println("StripChart server created at " + Sys.getProcessName() + "\n");
        this.port = (port == null) ? DEFAULT_PORT : port;
        baseUri = UriBuilder.fromUri("http://" + hostname + "/").port(port).build();
        ResourceConfig resourceConfig = new ResourceConfig(JacksonFeature.class);
        resourceConfig.register(StripChartServerService.class);
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        logger.info("Finished " + getClass().getSimpleName() + " initialization");
    }
    
    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Definition of the REST API.
     */
    @Path("/")
    public static class StripChartServerService {

        final static private ObjectMapper mapper = new ObjectMapper();

        private static final Logger logger = Logger.getLogger(StripChartServerService.class.getName());

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
        @Path("windows")
        @Produces(MediaType.APPLICATION_JSON)
        public List getWindows() {
            List ret = new ArrayList();
            for (Window window : SwingUtils.getVisibleWindows()){
                if (window instanceof JDialog) {
                    ret.add(((JDialog) window).getTitle());
                } else if (window instanceof JFrame) {
                    ret.add(((JFrame) window).getTitle());
                } else {
                    ret.add("");
                }
            }
            return ret;
        }        
        
        @GET
        @Path("close")
        public void close() {
            System.exit(0);
        }        

        
        //Example: curl localhost:17321/open/new
        @GET
        @Path("new")    
        @Produces(MediaType.TEXT_PLAIN)
        public String createNew() throws ExecutionException{
             try {
                //StripChart.create(getFileArg(), getArgumentValue("config"), getStripChartFolderArg(), hasArgument("start"));
                StripChart.create(null, null, App.getStripChartFolderArg(), false, false);
                return Sys.getProcessName();
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }                 
        }        
        
        //Example: curl localhost:17321/open/test.scd
        @GET
        @Path("open/{file : .+}")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        public String open(@PathParam("file") final String file) throws ExecutionException{
            try {
                //StripChart.resolveFile throws exception immediately if file not found
                StripChart.create(StripChart.resolveFile(new File(file), App.getStripChartFolderArg()), null, App.getStripChartFolderArg(), false, false);
                return "";
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }                
        }   
        
        //Example: curl localhost:17321/run/test.scd
        @GET
        @Path("run/{file : .+}")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.TEXT_PLAIN)
        public String run(@PathParam("file") final String file) throws ExecutionException{
            try {
                //StripChart.resolveFile throws exception immediately if file not found
                StripChart.create(StripChart.resolveFile(new File(file), App.getStripChartFolderArg()), null, App.getStripChartFolderArg(), true, false);
                return Sys.getProcessName();
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }                
        }         
        
        //Example: curl -H 'Content-Type: application/json' -X PUT -d '[[[true,"TESTIOC:TESTSINUS:SinCalc","Channel",1,1,null]]]' localhost:17321/open
        @PUT    
        @Path("open")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_JSON)
        public String openJson(final List cfg) throws ExecutionException{
            try {
                StripChart.create(null, EncoderJson.encode(cfg, true), App.getStripChartFolderArg(), false, false);
                return Sys.getProcessName();
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }                
        }          
        
        //Auto-start: curl -H 'Content-Type: application/json' -X PUT -d '[[[true,"TESTIOC:TESTSINUS:SinCalc","Channel",1,1,null]]]' localhost:17321/run
        @PUT    
        @Path("run")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_JSON)
        public String runJson(final List cfg) throws ExecutionException{
            try {
                StripChart.create(null, EncoderJson.encode(cfg, true), App.getStripChartFolderArg(), true, false);
                return Sys.getProcessName();
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }                
        }           
    };    
    
    
    
    public static String create(File file, String config, boolean start) {
        String url = "http://" + DEFAULT_URL + ":" + DEFAULT_PORT + "/";
        ClientConfig cfg = new ClientConfig().register(JacksonFeature.class);
        Client client = ClientBuilder.newClient(cfg);
        String ret = null;
        if ((file==null) &&(config==null)){
            WebTarget resource = client.target(url + "new");
            ret = resource.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        } else if (file!=null){
            WebTarget resource = client.target(url + (start ? "run" : "open") + "/" + file.getPath());
            ret = resource.request().accept(MediaType.TEXT_PLAIN).get(String.class);     
        } else {
            WebTarget resource = client.target(url + (start ? "run" : "open"));
            Response r = resource.request().accept(MediaType.TEXT_PLAIN).put(Entity.json(config));          
            ret = r.readEntity(String.class);
        } 
        return ret;
    }    
}
