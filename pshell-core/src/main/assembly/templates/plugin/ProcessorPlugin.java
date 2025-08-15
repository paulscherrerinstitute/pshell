
import ch.psi.pshell.framework.PanelProcessor;
import ch.psi.pshell.framework.Task;
import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.State;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Template for a processor plugin backed by json files.
 */
public class ProcessorPlugin extends PanelProcessor {

    public static final String TYPE = "ProcessorPlugin";
    public static final String FILE_EXTENSION = "json";
    public static final String HOME_PATH = "{home}/" + TYPE;

    File currentFile;

    public ProcessorPlugin() {
        initComponents();
    }

    //Overridable callbacks
    @Override
    public void onInitialize(int runCount) {

    }

    @Override
    public void onStateChange(State state, State former) {

    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }

    @Override
    public void onTaskFinished(Task task) {
    }

    @Override
    protected void onTimer() {
    }

    @Override
    protected void onLoaded() {

    }

    @Override
    protected void onUnloaded() {

    }

    //Invoked by 'update()' to update components in the event thread
    @Override
    protected void doUpdate() {
    }

    //Processor Configuration
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getDescription() {
        return getType() + " definition file (*." + FILE_EXTENSION + ")";
    }

    @Override
    public String getHomePath() {
        return HOME_PATH;
    }

    @Override
    public String[] getExtensions() {
        return new String[]{FILE_EXTENSION};
    }

    @Override
    public String getFileName() {
        return (currentFile == null) ? null : currentFile.toString();
    }

    @Override
    public boolean createFilePanel() {
        return true;
    }

    @Override
    public boolean createMenuNew() {
        return true;
    }

    @Override
    public boolean canStep() {
        return false;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean isTabNameUpdated() {
        return false;
    }

    @Override
    public void saveAs(String fileName) throws IOException {
        currentFile = new File(fileName);
        Map config = getConfig();
        String json = EncoderJson.encode(config, true);
        Files.write(currentFile.toPath(), json.getBytes());
    }

    @Override
    public void open(String fileName) throws IOException {
        clear();
        if (fileName != null) {
            Path path = Paths.get(fileName);
            String json = new String(Files.readAllBytes(path));
            Map config = (Map) EncoderJson.decode(json, Map.class);
            currentFile = path.toFile();
            setConfig(config);
        }
    }

    //Component update 
    public void clear() throws IOException {
        currentFile=null;        
        //TODO
    }

    Map getConfig() throws IOException {
        //TODO
        return new HashMap();
    }

    void setConfig(Map config) throws IOException {
        //TODO
    }

    //Workbench actions
    @Override
    public void plotDataFile(File file) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public void step() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void abort() throws InterruptedException {
        super.abort();
    }

    @Override
    public void execute() throws Exception {
        //TODO
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 449, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 137, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
