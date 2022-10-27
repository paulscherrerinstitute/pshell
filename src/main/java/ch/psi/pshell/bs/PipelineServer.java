package ch.psi.pshell.bs;

import ch.psi.pshell.camserver.PipelineSource;
import java.io.IOException;
import java.util.Map;

/**
 * Imaging Source implementation connecting to a CameraServer.
 */
@Deprecated
public class PipelineServer extends PipelineSource {

    public interface PipelineServerListener{
        void onConfigChanged(Map<String, Object> config);
    }    
    
    public PipelineServer(String name) {
        super(name);
    }

    public PipelineServer(String name, String url) {
        super(name, url);
    }

    public PipelineServer(String name, String host, int port) {
        super(name, host, port);
    }

    protected PipelineServer(String name, String url, ColormapSourceConfig cfg) {
        super(name, url, cfg);
    }
    
    
    private PipelineServerListener listener;
    
    public void setPipelineServerListener(PipelineServerListener listener){
        this.listener = listener;
    }

    public PipelineServerListener getPipelineServerListener(){
        return listener;
    }

    @Override
    public void setInstanceConfig(String instanceId, Map<String, Object> config) throws IOException {
        super.setInstanceConfig(instanceId, config);
        if (listener!=null){
            if (instanceId.equals(getCurrentInstance())){        
                listener.onConfigChanged(config);
            }
        }
    }

    @Override
    protected void doClose() throws IOException {
        listener = null;
        super.doClose();
    }       
}

