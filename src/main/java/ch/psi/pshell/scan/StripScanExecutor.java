package ch.psi.pshell.scan;

import ch.psi.pshell.core.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StripScanExecutor {
    ExecutorService persistenceExecutor;
    Thread persistenceThread;    
    final Map<String, StripScan> scans = new HashMap<>();
    
    
    public void start(String path, String[] ids, String provider, String layout, boolean flush) {
        if (persistenceExecutor != null){
            persistenceExecutor.shutdown();
        }
        persistenceExecutor = Executors.newSingleThreadExecutor((Runnable runnable) -> {
            persistenceThread = new Thread(Thread.currentThread().getThreadGroup(), runnable, "StripScanExecutor Persistence Thread");
            return persistenceThread;
        });
        persistenceExecutor.submit(() -> {
            Context.getInstance().createExecutionContext();

            scans.clear();

            for (String name : ids) {
                StripScan scan = new StripScan("Value");
                Map pars = new HashMap();
                pars.put("display", false);
                pars.put("save", true);
                pars.put("keep", false);                
                pars.put("flush", flush);
                pars.put("path", Context.getInstance().getSetup().expandPath(path));
                pars.put("layout", layout);
                pars.put("provider", provider);
                pars.put("preserve", false); //Do not now types beforehand, cannot be true
                pars.put("reset", true);
                pars.put("setpoints", false);
                pars.put("tag", name);
                scan.setHidden(true);
                Context.getInstance().setCommandPars(scan, pars);
                scans.put(name, scan);
                try {
                    scan.start();
                } catch (Exception ex) {
                    Logger.getLogger(StripScanExecutor.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        });
    }   
    
    public void append(String id, Double val, long now, long time) {
        if (persistenceExecutor!=null){
            persistenceExecutor.submit(() -> {
                StripScan scan = scans.get(id);
                if (scan!=null){
                    scan.append(val, now, time);
                }
            });
        }
    }

    public void finish() {
        finish(false);
    }
    
    public void finish(boolean wait) {
        if (!scans.isEmpty() && (persistenceExecutor!=null)) {
            Future future = persistenceExecutor.submit(() -> {
                    for (StripScan scan : scans.values()) {
                        try {
                            scan.end();
                        } catch (Exception ex) {
                            Logger.getLogger(StripScanExecutor.class.getName()).log(Level.WARNING, null, ex);
                        }
                    }
                    Context.getInstance().disposeExecutionContext();
                });

            scans.clear();
            persistenceExecutor.shutdown();
            if (wait){
                try {
                   future.get();
                } catch (Exception ex) {
                    Logger.getLogger(StripScanExecutor.class.getName()).log(Level.WARNING, null, ex);
                }
            }            
            persistenceExecutor = null;
        }
    }
}
