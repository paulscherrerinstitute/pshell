
package ch.psi.pshell.camserver;

import ch.psi.pshell.bs.AddressableDevice;
import ch.psi.pshell.bs.Stream;
import java.io.IOException;
import java.util.Map;

public interface CamServerStream extends AddressableDevice{
     public String getInstance();
     public ProxyClient getProxy();
     public void setInstanceConfig(Map<String, Object> config) throws IOException;
     public Map<String, Object> getInstanceConfig() throws IOException;
    public Stream getStream();     
}