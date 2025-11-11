package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Setup;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;


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
        if (url.contains("://0.0.0.0")){
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

    public Server(String hostname, Integer port) throws IOException {
        this(hostname, port, false);
    }
    
    public Server(String hostname, Integer port, boolean https) throws IOException {
        this(hostname, port, https, false);
    }

    public Server(String hostname, Integer port, boolean https, boolean light) throws IOException {
        logger.log(Level.INFO, "Initializing {0}", getClass().getSimpleName());
        if (hostname == null) {
            hostname = "0.0.0.0"; 
        }
        if (port == null) {
            port = 8080;
        }
        this.port = port;

        // Check base directory and create if it does not exist		
        String scheme = https ? "https" : "http";
        baseUri = UriBuilder.fromUri(scheme + "://" + hostname + "/").port(port).build();

        ResourceConfig resourceConfig = new ResourceConfig(SseFeature.class, JacksonFeature.class);
        resourceConfig.register(ServerService.class);
        resourceConfig.register(new ServerResourceBinder());
        HttpServer server = null;
        if (https) {
            //Generate keystore with: keytool -genkeypair -alias server -keyalg RSA -keysize 2048  -keystore keystore.jks -validity 3650  -storepass changeit
            SSLContextConfigurator sslConfig = new SSLContextConfigurator();
            sslConfig.setKeyStoreFile(Paths.get(Setup.getCachePath("keystore"), "keystore.jks").toString());
            try{
                String pass = Files.readString(Paths.get(Setup.getCachePath("keystore"), "pass")).trim();
                if (pass.length()>0){
                    sslConfig.setKeyStorePass(pass);
                }
            } catch (Exception ex){                
            }
            try{
                //JKS (default) or PKCS12
                String type = Files.readString(Paths.get(Setup.getCachePath("keystore"), "type")).trim();
                if (type.length()>0){
                    sslConfig.setKeyStoreType(type);
                }
            } catch (Exception ex){                
            }
            // Optional truststore (if you need client auth)
            // sslConfig.setTrustStoreFile("/path/to/truststore.jks");
            // sslConfig.setTrustStorePass("trustpass");
            SSLEngineConfigurator sslEngineConfig =
                new SSLEngineConfigurator(sslConfig)
                    .setClientMode(false)
                    .setNeedClientAuth(false);
            server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, true, sslEngineConfig);
        } else {
           server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
        }        
        
        if (light){
            ThreadPoolConfig kernelPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(1)
                .setMaxPoolSize(2)
                .setPoolName("grizzly-nio-kernel");

            ThreadPoolConfig workerPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(2)
                .setMaxPoolSize(5)
                .setPoolName("grizzly-http-server");

            TCPNIOTransport transport = server.getListener("grizzly").getTransport();
            transport.setSelectorRunnersCount(1);
            transport.setKernelThreadPoolConfig(kernelPoolConfig);
            transport.setWorkerThreadPoolConfig(workerPoolConfig);
        }
        server.start();

        // Static content
        String home = Setup.getWwwPath();
        server.getServerConfiguration().addHttpHandler(new StaticHttpHandler(home), "/static");
        String msg = String.format("Interface available at %s", getStaticURL());
        logger.info(msg);
        if (Setup.isDebug()){
            ServerService.setEvalLogLevel(Level.INFO);
            ServerService.setRunLogLevel(Level.INFO);
        }
        System.out.println(msg);
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }
}
