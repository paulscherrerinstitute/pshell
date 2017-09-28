package ch.psi.pshell.core;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

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
        return String.format("%sstatic/", getBaseURL());
    }

    public String getBaseURL() {
        return baseUri.toString();
    }

    public Server(String hostname, Integer port) throws UnknownHostException {
        logger.info("Initializing " + getClass().getSimpleName());
        if (hostname == null) {
            hostname = InetAddress.getLocalHost().getHostName();
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
        logger.info(String.format("Interface available at %s", getInterfaceURL()));
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }
}
