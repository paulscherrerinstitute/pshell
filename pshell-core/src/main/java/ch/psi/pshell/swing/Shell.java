package ch.psi.pshell.swing;

import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.CommandSource;
import ch.psi.pshell.sequencer.ControlCommand;
import ch.psi.pshell.sequencer.Sequencer;
import ch.psi.pshell.sequencer.InterpreterUtils;
import ch.psi.pshell.sequencer.ScriptStdio;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.NamedThreadFactory;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Sys.OSFamily;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import ch.psi.pshell.sequencer.SequencerListener;

/**
 *
 */
public class Shell extends MonitoredPanel {

    static Color colorOutput;
    static Color colorError;
    static Color colorInput;
    static Color colorRemote;

    static Color colorStdout;
    static Color colorStderr;
    static Color colorStdin;
    
    int historyIndex = -1;

    ScrollPopupMenu popupAutoComp;
    boolean truncateMenuContents;
    final ExecutorService commandExecutor;
    Future foregroundTask;
    public static volatile char keyChar;
    public static volatile int keyCode;
    public static volatile boolean waitingKey;

    public Shell() {
        initComponents();
        input.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        commandExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("Shell command executor"));
        onLafChange();
    }

    Sequencer getInterpreter(){
        return Sequencer.getInstance();
    }
    @Override
    protected final void onLafChange() {  
        if (MainFrame.isDark()){
            colorOutput = Color.LIGHT_GRAY ;
            colorError = new Color(255, 70, 70);
            colorInput = new Color(100, 100, 255);
            colorRemote = Color.GRAY;
            colorStdout = new Color(187, 187, 187);
            colorStderr = new Color(255, 0, 255);
            colorStdin = new Color(0, 200, 0);               
        } else {
            colorOutput = Color.DARK_GRAY;
            colorError =  Color.RED;
            colorInput = Color.BLUE;
            colorRemote = Color.GRAY;
            colorStdout = Color.BLACK;
            colorStderr = Color.MAGENTA;
            colorStdin = Color.GREEN;  
        }
     
    }        
    
    public static Color getColorOutput(){
        return colorOutput;
    }
    
    public static Color getColorError(){
        return colorError;
    }

    public static Color getColorInput(){
        return colorInput;
    }

    public static Color getColorRemote(){
        return colorRemote;
    }

    public static Color getColorStdout(){
        return colorStdout;
    }

    public static Color getColorStderr(){
        return colorStderr;
    }    

    public static Color getColorStdin(){
        return colorStdin;
    }
    
    public void initialize() {
        clear();
        input.setEditable(true);
        input.requestFocus();
        getInterpreter().addListener(sequencerListener);
    }

    boolean propagateVariableEvaluation = false;

    public void setPropagateVariableEvaluation(boolean value) {
        propagateVariableEvaluation = value;
    }

    public boolean getPropagateVariableEvaluation() {
        return propagateVariableEvaluation;
    }

    final SequencerListener sequencerListener = new SequencerListener() {
        @Override
        public void onShellCommand(CommandSource source, String command) {
            if (source.isDisplayable()){
                output.append(getInterpreter().getCursor(command) + command + "\n", (source == CommandSource.ui) ? colorInput : colorRemote);
            }
        }

        @Override
        public void onShellResult(CommandSource source, Object result) {
            if (result != null) {
                if (source.isDisplayable()){               
                    if (result instanceof Throwable throwable) {
                        output.append(InterpreterResult.getPrintableMessage(throwable) + "\n", colorError);
                    } else {
                        output.append(getInterpreter().interpreterVariableToString(result) + "\n", (source == CommandSource.ui) ? colorOutput : colorRemote);
                    }
                }
            }
        }

        @Override
        public void onShellStdout(String str) {
            output.append(str + "\n", colorStdout);
        }

        @Override
        public void onShellStderr(String str) {
            output.append(str + "\n", colorStderr);
        }

        @Override
        public void onShellStdin(String str) {
            output.append(str + "\n", colorStdin);
        }

        @Override
        public void onStateChanged(State state, State former) {
            if (state == State.Initializing) {
                if (foregroundTask != null) {
                    foregroundTask.cancel(true);
                    foregroundTask = null;
                }
            }
        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {
            if (preference == ViewPreference.PRINT_SCAN) {
                setPrintScan((value == null) ? defaultPrintScan : (Boolean) value);
            }
        }

    };

    public void setTextPaneFont(Font font) {
        output.setFont(font);
    }

    public Font getTextPaneFont() {
        return output.getFont();
    }

    public void setTextInputFont(Font font) {
        input.setFont(font);
    }

    public Font getTextInputFont() {
        return input.getFont();
    }

    //Properties     
    public int getTextLength() {
        return output.getTextLength();
    }

    public void setTextLength(int size) {
        output.setTextLength(size);
    }

    //Utilities
    public void clear() {
        input.setText("");
        output.setText("");
    }

    int getPopupAutoCompIndex() {
        for (int i = 0; i < popupAutoComp.getMenuComponentCount(); i++) {
            if (((JMenuItem) popupAutoComp.getMenuComponent(i)).isArmed()) {
                return i;
            }
        }
        return -1;
    }

    void setPopupAutoCompIndex(int index) {
        int listSize = popupAutoComp.getMenuComponentCount();
        if ((popupAutoComp != null) && (listSize > 0)) {
            if (index >= listSize) {
                index = 0;
            } else if (index < 0) {
                index = listSize - 1;
            }
            if (index >= 0) {
                for (int i = 0; i < listSize; i++) {
                    JMenuItem item = (JMenuItem) popupAutoComp.getMenuComponent(i);
                    //item.setFont(item.getFont().deriveFont((i==index)?Font.BOLD:Font.PLAIN));
                    item.setArmed((i == index));
                    item.setSelected((i == index));
                }
                popupAutoComp.ensureIndexIsVisible(index);
            }
        }
    }

    void onPopupAutoComp(String command) {
        command = command.trim();
        String limitToken=" ";
        if (truncateMenuContents && (command.lastIndexOf(limitToken) >= 0)) {
            command = command.substring(0, command.lastIndexOf(limitToken));
        }
        int position = input.getCaretPosition();
        String text = input.getText();
        input.setText(text.substring(0, position) + command + text.substring(position));
        closePopupAutoComp();
    }

    void setupPopupAutoComp(List<String> list) {
        closePopupAutoComp();
        if (list.size() > 0) {
            popupAutoComp = new ScrollPopupMenu();
            popupAutoComp.setMaximumVisibleRows(18);
            JMenuItem sel = null;
            for (String signature : list) {
                JMenuItem item = new JMenuItem(signature) {
                    @Override
                    public void setArmed(boolean armed) {
                        super.setArmed(armed);
                        if (armed) {
                            for (int i = 0; i < popupAutoComp.getMenuComponentCount(); i++) {
                                JMenuItem item = (JMenuItem) popupAutoComp.getMenuComponent(i);
                                if ((item != this) && (item.isArmed())) {
                                    item.setArmed(false);
                                }
                            }
                        }
                    }
                };
                sel = item;
                item.addActionListener((ActionEvent e) -> {
                    onPopupAutoComp(e.getActionCommand().trim());
                });
                popupAutoComp.add(item);
            }

            popupAutoComp.show(input, 0, +input.getHeight());

            //In order to keep the focus on the text box
            input.requestFocus();
            setPopupAutoCompIndex(0);
        }
    }

    void closePopupAutoComp() {
        if (popupAutoComp != null) {
            popupAutoComp.setVisible(false);
            popupAutoComp = null;
            //Strange behavior on Mac, when popup goes away and caret is in the last position,  all the text gets selected.
            if (Sys.getOSFamily() == OSFamily.Mac) {
                input.setSelectionStart(input.getCaretPosition());
                input.setSelectionEnd(input.getCaretPosition());
            }
        }
    }

    final ScanListener printScanListener = new ScanListener() {
        @Override
        public void onScanStarted(Scan scan, final String plotTitle) {
            if (!getInterpreter().getExecutionPars().isScanDisplayed(scan)){
                return;
            }            
            output.append(scan.getHeader("\t") + "\n", colorStdout);
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (!getInterpreter().getExecutionPars().isScanDisplayed(scan)){
                return;
            }            
            output.append(record.print("\t") + "\n", colorStdout);
        }

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
        }
    };

    static boolean defaultPrintScan;

    public boolean getPrintScan(boolean value) {
        return printScan;
    }

    public static void setDefaultPrintScan(boolean value) {
        defaultPrintScan = value;
    }

    boolean printScan=defaultPrintScan;

    public void setPrintScan(boolean value) {
        printScan = value;
        if (value) {
            getInterpreter().addScanListener(printScanListener);
        } else {
            getInterpreter().removeScanListener(printScanListener);
        }
    }
    
    static final Object keyWaitLock = new Object();
    public static int waitKey(int timeout) throws InterruptedException{
        timeout = Math.max(timeout, 0);
        keyChar = 0;
        keyCode = 0;
        waitingKey = true;        
        try{
            synchronized (keyWaitLock) {
                keyWaitLock.wait(timeout);
            }
        } finally {
            waitingKey = false;
        }
        return keyCode;
    }
   
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        input = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        output = new ch.psi.pshell.swing.OutputTextPane();

        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        input.setEditable(false);
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        input.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                inputKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                inputKeyTyped(evt);
            }
        });

        output.setTextLength(50000);
        output.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                outputKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(output);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(input, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    boolean ignoreTyped;

    private void inputKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inputKeyPressed
        ignoreTyped = false;
        List<String> history = getInterpreter().getHistoryEntries();
        keyChar = evt.getKeyChar();
        keyCode = evt.getKeyCode();
        
        try {
            if (keyCode == KeyEvent.VK_UP) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(currentIndex - 1);
                } else {
                    if (!waitingKey){
                        if (historyIndex < (history.size() - 1)) {
                            historyIndex++;
                        }
                        if ((historyIndex >= 0) && (historyIndex < history.size())) {
                            input.setText(history.get(history.size() - historyIndex - 1));
                            input.setCaretPosition(input.getText().length());
                        }
                    }
                }
                evt.consume();
            } else if (keyCode == KeyEvent.VK_DOWN) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(currentIndex + 1);
                } else {
                    if (!waitingKey){
                        if (historyIndex > 0) {
                            historyIndex--;
                        }
                        if ((historyIndex >= 0) && (historyIndex < history.size())) {
                            input.setText(history.get(history.size() - historyIndex - 1));
                            input.setCaretPosition(input.getText().length());
                        }
                    }
                }
                evt.consume();
            } else if (keyCode == KeyEvent.VK_PAGE_UP) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(Math.max(currentIndex - popupAutoComp.getMaximumVisibleRows(), 0));
                }
            } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(Math.min(currentIndex + popupAutoComp.getMaximumVisibleRows(), popupAutoComp.getSubElements().length - 1));
                }
            } else {
                if (popupAutoComp != null) {
                    if (keyCode == KeyEvent.VK_ENTER) {
                        int currentIndex = getPopupAutoCompIndex();
                        if (currentIndex >= 0) {
                            onPopupAutoComp(((JMenuItem) popupAutoComp.getSubElements()[currentIndex]).getText());
                        }
                        ignoreTyped = true;
                    }
                    closePopupAutoComp();
                }
                if ((keyCode == KeyEvent.VK_X) && (evt.isControlDown())) {
                    getInterpreter().abort();
                } else if ((keyCode == KeyEvent.VK_P) && (evt.isControlDown())) {
                    getInterpreter().pause();
                } else if ((keyCode == KeyEvent.VK_R) && (evt.isControlDown())) {
                    getInterpreter().resume();
                } else if ((keyCode == KeyEvent.VK_Z) && (evt.isControlDown())) {
                    if (getInterpreter().waitingStdin()) {
                        try {
                            submit(ScriptStdio.END_OF_LINES, true);
                        } catch (Exception ex) {
                            output.append(ex.getMessage() + "\n", colorError);
                        }
                    }
                    input.setText("");
                } else if (keyCode == KeyEvent.VK_PERIOD) {
                    List<String> signatures = InterpreterUtils.getSignatures(input.getText(), input.getCaretPosition(), propagateVariableEvaluation);
                    if (signatures != null) {
                        truncateMenuContents = true;
                        setupPopupAutoComp(signatures);
                    }
                } else if ((keyChar == ControlCommand.CONTROL_COMMAND_PREFIX) && (input.getText().length() == 0)) {
                    List<String> controlCommands = new ArrayList<>();
                    for (ControlCommand cmd : ControlCommand.values()) {
                        controlCommands.add(cmd.toString());
                    }
                    truncateMenuContents = true;
                    setupPopupAutoComp(controlCommands);
                } else if ((keyCode == KeyEvent.VK_SPACE) && (evt.isControlDown())) {
                    ArrayList<String> entries = new ArrayList<>();
                    if (evt.isShiftDown()) {
                        for (String function : getInterpreter().getBuiltinFunctionsNames()) {
                            entries.add(getInterpreter().getBuiltinFunctionDoc(function).split("\n")[0]);
                        }
                        truncateMenuContents = false;
                    } else {
                        if (DevicePool.hasInstance()){
                            for (GenericDevice dev : DevicePool.getInstance().getAllDevices()) {
                                entries.add(dev.getName() + " (" + Nameable.getShortClassName(dev.getClass()) + ")");
                            }
                        }
                        truncateMenuContents = true;
                    }
                    setupPopupAutoComp(entries);
                } else if (keyChar == KeyEvent.VK_TAB) {
                    evt.consume();
                    if (evt.isControlDown()) {
                        Component next = getNextFocusableComponent();
                        if (next != null) {
                            next.requestFocus();
                        }
                    } else {
                        int caretPos = input.getCaretPosition();
                        input.getDocument().insertString(caretPos, "    ", null);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Shell.class.getName()).log(Level.WARNING, null, ex);
        }
        
        synchronized (keyWaitLock) {
            keyWaitLock.notifyAll();
        }         

    }//GEN-LAST:event_inputKeyPressed

    private void inputKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inputKeyTyped
        if (ignoreTyped) {
            ignoreTyped = false;
            return;
        }
        char c = evt.getKeyChar();
        int modifiers = evt.getModifiers();

        if (c == KeyEvent.VK_ESCAPE) {

        }
        if (c == KeyEvent.VK_ENTER) {
            String command = input.getText();
            try {
                //Control commands are send also during execution of statements
                if ((ControlCommand.match(command)) || (getInterpreter().waitingStdin())) {
                    submit(command, true);
                } else {
                    submit(command, false);
                }
            } catch (Exception ex) {
                output.append("Cannot execute command: " + ex.getMessage() + "\n", colorError);
            }
            input.setText("");
        } else if ((modifiers & KeyEvent.CTRL_MASK) != 0) {
            //TODO
        }
        historyIndex = -1;
    }//GEN-LAST:event_inputKeyTyped

    private void inputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_inputFocusLost
        //closePopupAutoComp();
    }//GEN-LAST:event_inputFocusLost

    private void outputKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_outputKeyPressed
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_TAB) {
            input.requestFocus();
        }
    }//GEN-LAST:event_outputKeyPressed

    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        input.requestFocus();
    }//GEN-LAST:event_formFocusGained

    void submit(String command, boolean background) throws Exception {
        if (!background) {
            getInterpreter().assertReady(); //Won't pile requests to executor.
            foregroundTask = commandExecutor.submit(() -> {
                interpret(command);
            });
        } else {
            new Thread(() -> {
                interpret(command);
            }).start();
        }
    }

    void interpret(String command) {
        if (command != null) {
            try {
                getInterpreter().evalLine(command);
            } catch (Exception ex) {
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField input;
    private javax.swing.JScrollPane jScrollPane1;
    private ch.psi.pshell.swing.OutputTextPane output;
    // End of variables declaration//GEN-END:variables
}
