package ch.psi.pshell.framework;

import ch.psi.pshell.scan.ScanConfig;

/**
 *
 */
public class Config extends ch.psi.pshell.utils.Config{
    public ScanConfig getScanConfig(){
        return new ScanConfig();
    }
}
