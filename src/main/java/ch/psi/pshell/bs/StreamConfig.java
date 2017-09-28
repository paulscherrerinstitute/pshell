package ch.psi.pshell.bs;

import ch.psi.pshell.device.DeviceConfig;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration structure for streams statically defined.
 */
public class StreamConfig extends DeviceConfig {

    public String filter;

    public String channel01;
    public String channel02;
    public String channel03;
    public String channel04;
    public String channel05;
    public String channel06;
    public String channel07;
    public String channel08;
    public String channel09;
    public String channel10;
    public String channel11;
    public String channel12;
    public String channel13;
    public String channel14;
    public String channel15;
    public String channel16;
    public String channel17;
    public String channel18;
    public String channel19;
    public String channel20;
    public String channel21;
    public String channel22;
    public String channel23;
    public String channel24;
    public String channel25;
    public String channel26;
    public String channel27;
    public String channel28;
    public String channel29;
    public String channel30;
    public String channel31;
    public String channel32;
    public String channel33;
    public String channel34;
    public String channel35;
    public String channel36;
    public String channel37;
    public String channel38;
    public String channel39;
    public String channel40;
    public String channel41;
    public String channel42;
    public String channel43;
    public String channel44;
    public String channel45;
    public String channel46;
    public String channel47;
    public String channel48;
    public String channel49;
    public String channel50;

    ArrayList<ScalarConfig> getChannels() {
        ArrayList<ScalarConfig> ret = new ArrayList<>();
        try {
            for (int i = 1; i <= 1000; i++) {
                Field f = StreamConfig.class.getField("channel" + String.format("%02d", i));
                ScalarConfig cc = null;
                String val = ((String) (f.get(this)));
                if (val != null) {
                    String[] tokens = val.split(" ");
                    if (DeviceConfig.isStringDefined(tokens[0].trim())) {
                        cc = new ScalarConfig();
                        cc.id = tokens[0];
                        if (tokens.length > 0) {
                            try {
                                cc.modulo = Integer.valueOf(tokens[1]);
                            } catch (Exception ex) {
                                Logger.getLogger(StreamConfig.class.getName()).log(Level.WARNING, null, ex);
                            }
                        }
                        if (tokens.length > 1) {
                            try {
                                cc.offset = Integer.valueOf(tokens[2]);
                            } catch (Exception ex) {
                                Logger.getLogger(StreamConfig.class.getName()).log(Level.WARNING, null, ex);
                            }
                        }
                    }
                }
                ret.add(cc);
            }
        } catch (Exception ex) {
        }
        return ret;
    }
}
