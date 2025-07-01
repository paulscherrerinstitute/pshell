
package ch.psi.pshell.bs;

import ch.psi.pshell.device.Device;

/**
 *
 */
public interface AddressableDevice extends Device{
    String getAddress();   
    default String getChannelPrefix(){
        return getAddress();
    }
}
