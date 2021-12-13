package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.DevicePoolListener;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.utils.State;
import java.io.IOException;
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
                Context context = Context.getInstance();
                while ((context.getState() == State.Invalid) || (context.getState() == State.Initializing)) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                if ((context.getState() == State.Closing) || (context.getScriptManager() == null)) {
                    return;
                }
                context.addListener(new ContextAdapter() {
                    @Override
                    public void onContextInitialized(int runCount) {
                        functionNames=null;
                        update();
                        context.getDevicePool().addListener(devicePoolListener);
                    }
                });
                update();
                context.getDevicePool().addListener(devicePoolListener);
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
            for (GenericDevice dev : Context.getInstance().getDevicePool().getAllDevices()) {
                try {
                    map.put(dev.getName(), TokenTypes.VARIABLE);
                } catch (Exception ex) {
                    Logger.getLogger(CodeEditorExtraTokens.class.getName()).log(Level.INFO, null, ex);
                }
            }
            if ((functionNames == null) && (Context.getInstance().isInterpreterEnabled()))
            {
                try{
                    functionNames = Context.getInstance().getBuiltinFunctionsNames();
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
