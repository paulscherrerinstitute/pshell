package ch.psi.pshell.devices;

import ch.psi.pshell.device.ReadableWritable;
import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.IO;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 *
 */
public class Snapshot {

    final String name;
    final List<ReadableWritable> devices;
    final List<Object> state;

    public enum Mode {
        SERIES,
        PARALLEL,
        STOP_ON_ERROR
    }

    public Snapshot(String name) {
        this(name, null);
    }

    public Snapshot(List<ReadableWritable> devices) {
        this(null, devices);
    }

    public Snapshot(String name, List<ReadableWritable> devices) {
        this.name = ((name == null) || (name.isBlank())) ? "default" : name.trim();
        this.devices = new ArrayList<>();
        this.state = new ArrayList<>();
        setDevices(devices);
    }

    public String getName() {
        return name;
    }

    public List<ReadableWritable> getDevices() {
        return List.copyOf(devices);
    }

    public List<Object> getState() {
        //List.copyOf(state) does not allow null elements.
        return Collections.unmodifiableList(new ArrayList<>(state));
    }

    public void setDevices(List<ReadableWritable> devices) {
        this.state.clear();
        this.devices.clear();
        if (devices != null) {
            devices.forEach(Objects::requireNonNull);
            this.devices.addAll(devices);
        }
    }

    public boolean isTaken() {
        return state.size() > 0;
    }
    
    public void assertTaken() {
        if (!isTaken()){
            throw new IllegalStateException("Snapshot hasn't been taken");
        }
    }

    public void assertValid() {
        assertTaken();
        if (state.size() != devices.size()){
            throw new IllegalStateException("Invalid state size");
        }
        state.forEach(Objects::requireNonNull);        
    }
                
    public void clear() {
        state.clear();
    }

    public List<Exception> take() throws InterruptedException {
        state.clear();
        state.addAll(Collections.nCopies(devices.size(), null));
        return take(Mode.PARALLEL);
    }

    public List<Exception> restore() throws InterruptedException {
        return restore(Mode.PARALLEL);
    }

    public List<Exception> take(Mode mode) throws InterruptedException {
        return execute(mode, false);
    }

    public List<Exception> restore(Mode mode) throws InterruptedException {
        assertTaken();
        return execute(mode, true);
    }

    protected List<Exception> execute(Mode mode, boolean set) throws InterruptedException {
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        Function<Integer, Exception> exec = (i) -> {
            try {
                if (set) {
                    if (state.get(i) != null) {
                        devices.get(i).write(state.get(i));
                    } else {
                        java.util.logging.Logger.getLogger(Snapshot.class.getName()).warning("Error restoring: " + devices.get(i) + " - null value");
                    }
                } else {
                    state.set(i, devices.get(i).read());
                }
                return null;
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(Snapshot.class.getName()).warning((set ? "Error restoring: " : "Error taking: ") + devices.get(i) + " - " + e.toString());
                errors.add(e);
                return e;
            }
        };            

        switch (mode) {
            case SERIES, STOP_ON_ERROR -> {
                for (int i = 0; i < devices.size(); i++) {
                    Exception e = exec.apply(i);
                    if (e instanceof InterruptedException ie) {
                        throw ie;
                    } else if (e != null) {
                        errors.add(e);
                        if (mode == Mode.STOP_ON_ERROR) {
                            // Fail fast
                            break;
                        }
                    }
                }
            }

            case PARALLEL -> {
                ExecutorService pool = Executors.newFixedThreadPool(
                        Math.min(devices.size(), Runtime.getRuntime().availableProcessors())
                );
                List<Future<?>> futures = new ArrayList<>();
                List<InterruptedException> interruptedException = new ArrayList<>();
                for (int i = 0; i < devices.size(); i++) {
                    final int idx = i;
                    futures.add(pool.submit(() -> {
                        Exception e = exec.apply(idx);
                        if (e instanceof InterruptedException ie) {
                            interruptedException.add(ie);
                        }
                    }));
                }
                try {
                    for (Future<?> f : futures) {
                        try {
                            f.get();
                        } catch (Exception ignored) {
                        }
                        if (!interruptedException.isEmpty()) {
                            throw interruptedException.get(0);
                        }
                    }
                } finally {
                    pool.shutdownNow();
                }
            }
        }
        return errors;
    }

    // Persistence
    protected Path getPath() {
        String cache =  Setup.getCachePath("snapshot");
        return Paths.get(cache, name);        
    }

    public String save() throws IOException {
        assertTaken();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path dir = getPath();
        Files.createDirectories(dir);

        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", devices.get(i).getName());
            entry.put("value", state.get(i));
            entries.add(entry);
        }

        String json = EncoderJson.encode(entries, true);
        Path file = dir.resolve(timestamp + ".json");
        Files.writeString(file, json);
        return timestamp;
    }

    public void load() throws IOException {
        load(null);
    }

    public void load(String timestamp) throws IOException {
        state.clear();
        List<Map<String, Object>> entries = getEntries(timestamp);
        state.addAll(Collections.nCopies(devices.size(), null));
        //Update state
        for (int i = 0; i < devices.size(); i++) {
            Map<?, ?> entry = entries.get(i);
            state.set(i, entry.get("value"));
        }
    }
    
    public void del() throws IOException {
        Path dir = getPath();
        IO.deleteRecursive(dir);
    }

    private List<Map<String, Object>> getEntries(String timestamp) throws IOException {
        if ((timestamp == null) || (timestamp.isBlank())) {
            Path dir = getPath();
            if (!Files.exists(dir)) {
                throw new FileNotFoundException("Snapshot directory not found: " + dir);
            }
            Optional<Path> latest = Files.list(dir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

            if (latest.isEmpty()) {
                throw new FileNotFoundException("No snapshot files found in: " + dir);
            }
            timestamp = latest.get().getFileName().toString().replace(".json", "");
        }
        Path file = Paths.get(getPath().toString(), timestamp + ".json");
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Snapshot file not found: " + file);
        }

        String json = Files.readString(file);
        List<Map<String, Object>> entries = (List<Map<String, Object>>) EncoderJson.decode(json, List.class);
        checkEntries(entries);
        return entries;
    }

    private void checkEntries(List<Map<String, Object>> entries) {
        //Check compatibility of device list
        if (entries.size() != devices.size()) {
            throw new IllegalStateException("Snapshot device count mismatch.");
        }
        for (int i = 0; i < devices.size(); i++) {
            Map<?, ?> entry = entries.get(i);
            String deviceName = (String) entry.get("name");
            if (!Objects.equals(devices.get(i).getName(), deviceName)) {
                throw new IllegalStateException("Device order mismatch: expected "+ devices.get(i).getName() + " but found " + deviceName);
            }
        }

    }

}
