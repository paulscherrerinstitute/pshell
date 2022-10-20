package ch.psi.pshell.bs;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
@Deprecated
public class CameraServer extends ch.psi.pshell.camserver.CameraSource {

    public CameraServer(String name) {
        super(name);
    }

    public CameraServer(String name, String url) {
        super(name, url);
    }

    public CameraServer(String name, String host, int port) {
        super(name, host, port);
    }

    protected CameraServer(String name, String url, ColormapSourceConfig cfg) {
        super(name, url, cfg);
    }
}
