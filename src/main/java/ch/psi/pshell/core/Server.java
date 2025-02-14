package ch.psi.pshell.core;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import jakarta.ws.rs.core.UriBuilder;
import java.util.logging.Level;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

/**
 * Embedded HTTP server.
 */
public class Server implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    HttpServer server;
    final int port;
    final URI baseUri;

    public int getPort() {
        return port;
    }
    public String getInterfaceURL() {
        String url = getBaseURL();
        if (url.contains("http://0.0.0.0")){
            try {
                url = url.replaceFirst("0.0.0.0", InetAddress.getLocalHost().getHostName());
            } catch (Exception ex) {
            }
        }
        return url;
    }    
    public String getStaticURL() {
        return String.format("%sstatic/", getInterfaceURL());
    }

    public String getBaseURL() {
        return baseUri.toString();
    }

    public Server(String hostname, Integer port) throws UnknownHostException {
        logger.info("Initializing " + getClass().getSimpleName());
        if (hostname == null) {
            hostname = "0.0.0.0"; 
        }
        if (port == null) {
            port = 8080;
        }
        this.port = port;

        // Check base directory and create if it does not exist		
        baseUri = UriBuilder.fromUri("http://" + hostname + "/").port(port).build();

        ResourceConfig resourceConfig = new ResourceConfig(SseFeature.class, JacksonFeature.class);
        resourceConfig.register(ServerService.class);
        resourceConfig.register(new ServerResourceBinder());
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

        // Static content
        String home = Context.getInstance().getSetup().getWwwPath();
        server.getServerConfiguration().addHttpHandler(new StaticHttpHandler(home), "/static");
        String msg = String.format("Interface available at %s", getStaticURL());
        logger.info(msg);
        System.out.println(msg);
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }
}
