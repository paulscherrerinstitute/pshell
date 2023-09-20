package ch.psi.pshell.camserver;

import ch.psi.pshell.device.DeviceBase;

/**
 * Interface to a CamServer cluster manager
 */
public class CamServerService extends DeviceBase {

    public static enum Type{
        Camera,
        Pipeline
    }

    final ProxyClient proxy;
    final Type type;
    final InstanceManagerClient client;
    
    
    public CamServerService(String name, String url) {
        this(name, url, (Type)null);
    }

    public CamServerService(String name, String url, String type) {
        this(name, url, Type.valueOf(type));
    }
    
    public CamServerService(String name, String url, Type type) {
        super(name);
        this.type = (type==null) ? Type.Pipeline : type;
        proxy = new ProxyClient(url);        
        if (this.type == CamServerService.Type.Camera){
            client =  new CameraClient(url);
        } else {
            client =  new PipelineClient(url);        
        }
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
    
    public InstanceManagerClient getClient(){        
        return client;
    }
}
