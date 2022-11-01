package ch.psi.pshell.camserver;

import ch.psi.pshell.device.DeviceBase;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
public class CamServerService extends DeviceBase {

    public static enum Type{
        Camera,
        Pipeline
    }

    final ProxyClient proxy;
    final Type type;
    
    
    public CamServerService(String name, String url) {
        this(name, url, (Type)null);
    }

    public CamServerService(String name, String url, String type) {
        this(name, url, Type.valueOf(type));
    }
    
    public CamServerService(String name, String url, Type type) {
        super(name);
        proxy = new ProxyClient(url);
        this.type = (type==null) ? Type.Pipeline : type;
    }

    public ProxyClient getProxy() {
        return proxy;
    }

    public Type getType() {
        return type;
    }
    
    public String getUrl() {
        return proxy.getUrl();
    }
}
