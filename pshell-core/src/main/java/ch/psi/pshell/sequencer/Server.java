package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.utils.Processing;
import jakarta.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
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
        if (url.contains("://0.0.0.0")) {
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
            
            String configPath = Setup.getCachePath("keystore");
            Path keystoreJks =Paths.get(configPath, "keystore.jks");
            Path keystorePkcs12 =Paths.get(configPath, "keystore.p12");
            Path passFile =Paths.get(configPath, "pass");
            Path pemFile = Paths.get(configPath, "cert.pem");
            Path keyFile = Paths.get(configPath, "sec.key");
            
            if (!passFile.toFile().isFile()){
                 Files.writeString(passFile, "changeit");
            }
            String pass = Files.readString(passFile).trim();
            if (pass.length() > 0) {
                sslConfig.setKeyStorePass(pass);
            }            
            

            if(pemFile.toFile().isFile() && !keystorePkcs12.toFile().isFile() && !keystoreJks.toFile().isFile()){
                logger.info("Generating PKCS12 file from PEM");
                try {           
                    Processing.run(new File(configPath), "openssl", "pkcs12", "-export",
                        "-in", pemFile.toFile().getName(),
                        "-inkey", keyFile.toFile().getName(),
                        "-out", keystorePkcs12.toFile().getName(),
                        "-name", "myalias",
                        "-passout", "file:" + passFile.toFile().getName());
                    if (!keystorePkcs12.toFile().isFile()){
                        throw new Exception("Error generating PKCS12 file from PEM");
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            } 
            
            if(keystoreJks.toFile().isFile()){
                sslConfig.setKeyStoreFile(keystoreJks.toString());
            } else if (keystorePkcs12.toFile().isFile()){
                sslConfig.setKeyStoreType("PKCS12");
                sslConfig.setKeyStoreFile(keystorePkcs12.toString());
            } else {
                throw new IOException("Undefined SSL keystore file");    
            } 
               

            SSLEngineConfigurator sslEngineConfig = new SSLEngineConfigurator(sslConfig)
                                                    .setClientMode(false)
                                                    .setNeedClientAuth(false);
                    
            server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, true, sslEngineConfig, false);                        
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
        if (Setup.isDebug()) {
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


    private static PrivateKey loadPrivateKey(Path pemPath) throws Exception {
        String pem = Files.readString(pemPath)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] der = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static X509Certificate loadCertificate(Path pemPath) throws Exception {
        try (InputStream in = Files.newInputStream(pemPath)) {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(in);
        }
    }


    private static SSLContext createSSLContext(Path certPem, Path keyPem) throws Exception {
        X509Certificate cert = loadCertificate(certPem);
        PrivateKey key = loadPrivateKey(keyPem);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("alias", key, "changeit".toCharArray(), new Certificate[]{cert});

        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "changeit".toCharArray());

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }
}