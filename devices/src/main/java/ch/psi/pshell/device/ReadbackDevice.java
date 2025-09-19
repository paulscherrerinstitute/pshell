package ch.psi.pshell.device;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface for devices having a readback value.
 */
public interface ReadbackDevice<T> extends Cacheable<T>{

    public ReadonlyRegister<T> getReadback();
    
//By default returns a proxy to self - can be used in scans saving data on a different name
    default ReadonlyRegister<T> getSetpoint(){
            ReadonlyRegister setpoint = new ReadonlyRegisterBase<T>(getName() + " setpoint") {
                @Override
                protected T doRead() throws IOException, InterruptedException {
                    return ReadbackDevice.this.read();
                }
                @Override
                public T take() {
                    return ReadbackDevice.this.take();
                }
            };
        try {
            setpoint.initialize();
        } catch (Exception ex) {
            Logger.getLogger(ControlledVariable.class.getName()).log(Level.WARNING, null, ex);
        }
        return setpoint;
    }
    
}
