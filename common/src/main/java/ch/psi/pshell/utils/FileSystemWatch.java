package ch.psi.pshell.utils;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSystemWatch implements AutoCloseable {

    Logger logger = Logger.getLogger(FileSystemWatch.class.getName());
    WatchService watchService;
    ScheduledFuture scheduledFuture;
    static ScheduledExecutorService schedulerWatchService;
    
    public final String[] paths;
    public final String[] extensions;
    public final boolean create;
    public final boolean delete;
    public final boolean modify;
    public final boolean registered;
    public final int interval;
    public final FileSystemWatchListener listener;
    
    public interface FileSystemWatchListener {

        //Kind = ENTRY_CREATE, ENTRY_DELETE or ENTRY_MODIFY
        void onEvent(Path path, String kind);
    }
    
    public FileSystemWatch(String[] paths, String[] extensions, boolean create, boolean delete, boolean modify, int interval, FileSystemWatchListener listener) {
        this.paths = paths;
        this.create = create;
        this.delete = delete;
        this.modify = modify;
        this.extensions = extensions;
        this.interval = interval;
        this.listener = listener;
        this.registered = register();
    }
    
    final boolean register() {
        boolean registered = false;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            if (watchService != null) {                
                for (String path : paths) {
                    logger.log(Level.INFO, "Registering lib path: {0}", path);
                    try {
                        List<WatchEvent.Kind> kinds = new ArrayList<>();
                        if (create) {
                            kinds.add(StandardWatchEventKinds.ENTRY_CREATE);
                        }
                        if (delete) {
                            kinds.add(StandardWatchEventKinds.ENTRY_CREATE);
                        }
                        if (modify) {
                            kinds.add(StandardWatchEventKinds.ENTRY_CREATE);
                        }
                        Paths.get(path).register(watchService, kinds.toArray(new WatchEvent.Kind[0]));
                        registered = true;
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
                
                if (registered) {
                    if (schedulerWatchService == null) {
                        schedulerWatchService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Scheduler file watch service"));
                    }
                    if (interval > 0) {
                        scheduledFuture = schedulerWatchService.scheduleWithFixedDelay(() -> {
                            poll();
                        }, interval, interval, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
        return registered;
    }
    
    public void check() {
        check(-1);
    }
        
    private volatile boolean checking;
    public void check(int delay) {
        if (schedulerWatchService != null){
            if (!checking){
                checking = true;
                schedulerWatchService.submit(() -> {
                    try {
                        if (delay>0){
                            Thread.sleep(delay);
                        }
                        poll();         
                    } catch (InterruptedException ex){                        
                    } catch (Exception ex) {                    
                        logger.log(Level.WARNING, null, ex);
                    } finally{
                        checking = false;
                    }                
                });
            }
        }
    }   
    
    void poll() {
        WatchKey watchKey = null;
        while ((watchKey = watchService.poll()) != null) {
            try {
                ArrayList<Path> updatedFolders = new ArrayList<>();
                List<WatchEvent<?>> keys = watchKey.pollEvents();
                for (WatchEvent<?> watchEvent : keys) {
                    WatchEvent.Kind<?> watchEventKind = watchEvent.kind();
                    Path path = ((Path) watchEvent.context());
                    Path dir = (Path) watchKey.watchable();
                    Path fullPath = dir.resolve((Path) watchEvent.context());
                    if (extensions != null) {
                        String ext = IO.getExtension(path.toString());
                        if (!Arr.containsEqual(extensions, ext)) {
                            continue;
                        }
                    }
                    if (listener != null) {
                        try {
                            listener.onEvent(fullPath, watchEventKind.name());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                    }
                    
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
            if (watchKey != null) {
                watchKey.reset();
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        if (watchService != null) {
            try {
                watchService.close();
                watchService = null;
            } catch (Exception ex) {
            }
        }
        if (scheduledFuture != null) {
            try {
                scheduledFuture.cancel(true);
                scheduledFuture = null;
            } catch (Exception ex) {
            }
        }
    }
}
