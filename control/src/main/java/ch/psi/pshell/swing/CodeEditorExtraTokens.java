package ch.psi.pshell.swing;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.devices.DevicePoolListener;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.utils.State;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 *
 */
public class CodeEditorExtraTokens {

    static volatile TokenMap extraTokens;
    static String[] functionNames;

    static {
        init();
    }

    static public void init() {
        if (extraTokens == null) {
            extraTokens = new TokenMap();
            new Thread(() -> {
                while ((Context.getState() == State.Invalid) || (Context.getState() == State.Initializing)) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                if ((Context.getState() == State.Closing) || (!Context.hasScriptManager())) {
                    return;
                }
                //!!! TODO 
                //context.addListener(new ContextAdapter() {
                //    @Override
                //    public void onContextInitialized(int runCount) {
                //        functionNames=null;
                //        update();                       
                //    }
                //});                
                update();                
                DevicePool.addStaticListener(devicePoolListener);
            }).start();            
        }
    }

    static final DevicePoolListener devicePoolListener = new DevicePoolListener() {
        @Override
        public void onDeviceAdded(GenericDevice dev) {
            requestUpdate();
        }

        @Override
        public void onDeviceRemoved(GenericDevice dev) {
            requestUpdate();
        }
    };

    static volatile boolean requsting;

    static void requestUpdate() {
        if (!requsting) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            }).start();
        }
    }

    static void update() {
        requsting = false;
        try {
            TokenMap map = new TokenMap();
            if (Context.hasDevicePool()){
                for (GenericDevice dev : Context.getDevicePool().getAllDevices()) {
                    try {
                        map.put(dev.getName(), TokenTypes.VARIABLE);
                    } catch (Exception ex) {
                        Logger.getLogger(CodeEditorExtraTokens.class.getName()).log(Level.INFO, null, ex);
                    }
                }
            }
            if ((functionNames == null) && (Context.hasScriptManager()))
            {
                try{
                    //!!! TODO
                    //functionNames = Context.getBuiltinFunctionsNames();
                } catch (Exception ex) {
                    Logger.getLogger(CodeEditorExtraTokens.class.getName()).log(Level.INFO, null, ex);
                }
            }
            if (functionNames != null) {
                for (String function : functionNames) {
                    map.put(function, TokenTypes.FUNCTION);
                }
            }
            extraTokens = map;
        } catch (Exception ex) {
            Logger.getLogger(CodeEditorExtraTokens.class.getName()).log(Level.WARNING, null, ex);
        }
    }

}
