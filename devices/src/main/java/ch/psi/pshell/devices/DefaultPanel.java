
package ch.psi.pshell.devices;

import ch.psi.pshell.extension.Extensions;
import java.beans.Transient;
import java.io.Serializable;

/**
 *
 */
public class DefaultPanel implements Serializable {

    private static final long serialVersionUID = 1L;

    public DefaultPanel(){
    }

    public DefaultPanel(String deviceClassName, String panelClassName) {
        this.deviceClassName = deviceClassName;
        this.panelClassName = panelClassName;
    }
    public String deviceClassName;
    public String panelClassName;

    @Transient
    public Class getDeviceClass() throws ClassNotFoundException {
        return Extensions.getClass(deviceClassName);
    }

    @Transient
    public Class getPanelClass() throws ClassNotFoundException {
        return Extensions.getClass(panelClassName);
    }
}
