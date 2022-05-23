package ch.psi.pshell.swing;

import ch.psi.pshell.core.Console;
import ch.psi.utils.swing.Document;
import ch.psi.utils.swing.DocumentListener;
import ch.psi.utils.swing.TextEditor;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.script.ScriptException;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Utilities;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.scripting.Statement;
import ch.psi.utils.IO;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.ScrollPopupMenu;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;

/**
 *
 */
public class ScriptEditor extends MonitoredPanel implements Executor {

    static final Color STATEMENT_HIGHLIGHT = MainFrame.isDark() ? new Color(84, 84, 84) : new Color(200, 200, 200);
    static final Color STATEMENT_HIGHLIGHT_MAC = new Color(208, 208, 208);
    boolean isScript = true;
    final boolean syntaxHighlight;

    public ScriptEditor(boolean syntaxHighlight, boolean showLineNumbers, boolean showContextMenu) {
        initComponents();
        this.syntaxHighlight = syntaxHighlight;
        if (syntaxHighlight) {
            CodeEditor codeEditor = new CodeEditor();
            codeEditor.setShowLineNumbers(showLineNumbers);
            codeEditor.setType(Context.getInstance().getScriptType().getExtension());
            javax.swing.GroupLayout layout = (javax.swing.GroupLayout) getLayout();
            layout.replace(editor, codeEditor);
            editor = codeEditor;
        } else {
            editor.getEditor().addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    char c = evt.getKeyChar();
                    if (c == KeyEvent.VK_TAB) {
                        evt.consume();
                        int caretPos = editor.getEditor().getCaretPosition();
                        try {
                            editor.getEditor().getDocument().insertString(caretPos, getTabStr(), null);
                        } catch (BadLocationException ex) {
                            Logger.getLogger(ScriptEditor.class.getName()).log(Level.WARNING, null, ex);
                        }
                    }
                }
            });            
        }
        editor.getEditor().addCaretListener((CaretEvent e) -> {
            int row = getRow(e.getDot(), (JTextComponent) e.getSource());
            int col = getColumn(e.getDot(), (JTextComponent) e.getSource());
            setPosition(row, col);
        });
        
        editor.setFileChooserFolder(Context.getInstance().getSetup().getScriptPath());

        if (showContextMenu) {
            editor.getEditor().setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
            editor.getEditor().addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    if (isScript) {
                        editorKeyPressed(evt);
                    }
                }
            });
        }
    }
    
    public boolean hasSyntaxHighlight(){
        return syntaxHighlight;
    }

    //When changing parent the disabled color is reset (seen in Nimbus)
    @Override
    protected void onActive() {
        textFilename.setDisabledTextColor(UIManager.getDefaults().getColor("TextField.foreground"));
        textCol.setDisabledTextColor(UIManager.getDefaults().getColor("TextField.foreground"));
    }

    //TODO: Is showing the phisical row, and not the row in editor.getDocument.getLines()
    //They are different if the content width is smaller than the maximum line size
    void setPosition(int row, int col) {
        textCol.setText(String.valueOf(row) + ":" + String.valueOf(col));
    }

    public void startExecution() {
        //setEnabled(false);
        getTextEditor().enableManualHighligting();
        setPosition(1, 1);
    }

    public void stopExecution() {
        getTextEditor().disableManualHighligting();
        //setEnabled(true);
    }

    @Override
    public boolean isExecuting() {
        return getTextEditor().isManualHighligting();
    }

    public void onExecutingStatement(final Statement statement) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                onExecutingStatement(statement);
            });
            return;
        }
        try {
            if (getTextEditor().isManualHighligting()) {
                getTextEditor().removeAllHighlights();
                Color highlight = (UIManager.getLookAndFeel().getName().equalsIgnoreCase("Mac OS X")) ?
                        STATEMENT_HIGHLIGHT_MAC : STATEMENT_HIGHLIGHT;
                getTextEditor().highlightLines(statement.lineNumber, statement.finalLineNumber, highlight);
                setPosition(statement.lineNumber, 1);
            }
        } catch (Exception ex) {
        }

    }

    String name = "Unknown";

    public String getScriptName() {
        return name;
    }

    public void setScriptName(String name) {
        this.name = name;
    }

    public void setFileName(String filename) {
        editor.setFileName(filename);
        setScriptName(new File(filename).getName());
    }

    public void addDocumentChangeListener(DocumentListener listener) {
        editor.getDocument().addListener(listener);
    }

    public Document getDocument() {
        return editor.getDocument();
    }

    public TextEditor getTextEditor() {
        return editor;
    }

    Statement[] statements = null;

    public Statement[] getStatements() {
        return statements;
    }

    public Statement[] parse() throws ScriptException, IOException, InterruptedException {
        statements = Context.getInstance().parseString(getText(), getScriptName());
        return statements;
    }

    public static int getRow(int pos, JTextComponent editor) {
        int ret = 0;
        if (pos == 0) {
            ret++;
        } else {
            //TODO: Is this a bug in Utilities.getRowStart? If first line is empty lines are offset by one...
            try {
                if (editor.getText(0, 1).charAt(0) == '\n') {
                    ret++;
                }
            } catch (BadLocationException ex) {
            }
        }

        try {
            int offs = pos;
            while (offs > 0) {
                offs = Utilities.getRowStart(editor, offs) - 1;
                ret++;
            }
        } catch (BadLocationException e) {
        }

        return ret;
    }

    public static int getColumn(int pos, JTextComponent editor) {
        try {
            return pos - Utilities.getRowStart(editor, pos) + 1;
        } catch (BadLocationException e) {
        }
        return -1;
    }

    public void clear() {
        editor.clear();
    }

    public void save() throws IOException {
        editor.save();
    }

    public void saveAs(String filename) throws IOException {
        textFilename.setText(filename);
        editor.saveAs(filename);
        setScriptName(new File(filename).getName());
        isScript = Context.getInstance().getScriptType().getExtension().equals(IO.getExtension(filename));
    }

    public void load(String filename) throws IOException {
        textFilename.setText(filename);
        editor.load(filename);
        setScriptName(new File(filename).getName());
        isScript = Context.getInstance().getScriptType().getExtension().equals(IO.getExtension(filename));
    }

    public void reload() throws IOException {
        editor.reload();
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        editor.setEnabled(value);
    }

    public void setReadOnly(boolean value) {
        editor.setReadOnly(value);
    }

    public boolean isReadOnly() {
        return editor.isReadOnly();
    }

    @Override
    public String getFileName() {
        return editor.getFileName();
    }

    public String getText() {
        return editor.getText();
    }

    public void setText(String text) {
        editor.setText(text);
    }

    public void setTabSize(int size) {
        editor.getEditor().getDocument().putProperty(PlainDocument.tabSizeAttribute, size);
    }

    public int getTabSize() {
        Object o = editor.getEditor().getDocument().getProperty(PlainDocument.tabSizeAttribute);
        return (o != null) ? ((Integer) o) : 8;
    }

    public void setContentWidth(int size) {
        editor.setContentWidth(size);
    }

    public int getContentWidth() {
        return editor.getContentWidth();
    }

    public void setEditorBackground(Color color) {
        editor.setEditorBackground(color);
    }

    public Color getEditorBackground() {
        return editor.getEditorBackground();
    }

    public void setEditorForeground(Color color) {
        editor.setEditorForeground(color);
    }

    public Color getEditorForeground() {
        return editor.getEditorForeground();
    }

    String getTabStr() {
        int size = getTabSize();
        StringBuffer sb = new StringBuffer(getTabSize());
        for (int i = 0; i < getTabSize(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public void setTextPaneFont(Font font) {
        editor.getEditor().setFont(font);
    }

    public Font getTextPaneFont() {
        return editor.getEditor().getFont();
    }
    
    @Override
    public boolean hasChanged(){
        return editor.hasChanged();
    }

    //Context menu
    static boolean propagateVariableEvaluation = false;

    static public void setPropagateVariableEvaluation(boolean value) {
        propagateVariableEvaluation = value;
    }

    static public boolean getPropagateVariableEvaluation() {
        return propagateVariableEvaluation;
    }

    ScrollPopupMenu popupAutoComp;
    boolean truncateMenuContents;

    private void editorKeyPressed(java.awt.event.KeyEvent evt) {
        try {
            List<String> history = Context.getInstance().getHistory();
            char keyChar = evt.getKeyChar();
            int key = evt.getKeyCode();
            if (key == KeyEvent.VK_UP) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(currentIndex - 1);
                    evt.consume();
                }
            } else if (key == KeyEvent.VK_DOWN) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(currentIndex + 1);
                    evt.consume();
                }
            } else if (key == KeyEvent.VK_PAGE_UP) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(Math.max(currentIndex - popupAutoComp.getMaximumVisibleRows(), 0));
                    evt.consume();
                }
            } else if (key == KeyEvent.VK_PAGE_DOWN) {
                if (popupAutoComp != null) {
                    int currentIndex = getPopupAutoCompIndex();
                    setPopupAutoCompIndex(Math.min(currentIndex + popupAutoComp.getMaximumVisibleRows(), popupAutoComp.getSubElements().length - 1));
                    evt.consume();
                }
            } else {
                if (popupAutoComp != null) {
                    if (key == KeyEvent.VK_ENTER) {
                        int currentIndex = getPopupAutoCompIndex();
                        if (currentIndex >= 0) {
                            onPopupAutoComp(((JMenuItem) popupAutoComp.getSubElements()[currentIndex]).getText());
                            evt.consume();
                        }
                    }
                    closePopupAutoComp();
                } else if (key == KeyEvent.VK_PERIOD) {
                    List<String> signatures = Console.getSignatures(editor.getCanonicalText(), editor.getEditor().getCaretPosition(), propagateVariableEvaluation);
                    if (signatures != null) {
                        truncateMenuContents = true;
                        setupPopupAutoComp(signatures);
                    }
                } else if ((key == KeyEvent.VK_SPACE) && (evt.isControlDown())) {
                    ArrayList<String> entries = new ArrayList<>();
                    if (evt.isShiftDown()) {
                        for (String function : Context.getInstance().getBuiltinFunctionsNames()) {
                            entries.add(Context.getInstance().getBuiltinFunctionDoc(function).split("\n")[0]);
                        }
                        truncateMenuContents = false;
                    } else {
                        for (GenericDevice dev : Context.getInstance().getDevicePool().getAllDevices()) {
                            entries.add(dev.getName() + " (" + Nameable.getShortClassName(dev.getClass()) + ")");
                        }
                        truncateMenuContents = true;
                    }
                    setupPopupAutoComp(entries);
                    evt.consume();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ScriptEditor.class.getName()).log(Level.WARNING, null, ex);
        }
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
        int position = editor.getEditor().getCaretPosition();
        try {
            editor.getEditor().getDocument().insertString(position, command, null);
        } catch (Exception ex) {
        }
        closePopupAutoComp();
    }

    void setupPopupAutoComp(List<String> list) {
        int caret = editor.getEditor().getCaretPosition();
        Point point = editor.getEditor().getCaret().getMagicCaretPosition();
        if (point == null) {
            if (getText().isEmpty()) {
                point = new Point(0, 0);
            }
        }
        closePopupAutoComp();
        if ((point != null) && (list.size() > 0)) {
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

            popupAutoComp.show(this, point.x, (int) (point.y + getGraphics().getFontMetrics(editor.getEditor().getFont()).getHeight() * 1.2 + 5));
            setPopupAutoCompIndex(0);

            //In order to keep the focus on the text box
            editor.getEditor().requestFocus();
            editor.getEditor().setCaretPosition(caret);
        }
    }

    void closePopupAutoComp() {
        if (popupAutoComp != null) {
            popupAutoComp.setVisible(false);
            popupAutoComp = null;
        }
    }

    public void refresh() {
        if (editor.getEditor() instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) {
            editor.getEditor().paint(editor.getEditor().getGraphics());
        }
    }
    
    public void indent(){ 
        if (hasSyntaxHighlight()){
            new RSyntaxTextAreaEditorKit.InsertTabAction().actionPerformed(null);
        } else {

            String tab = "";
            for (int i = 0; i < getTabSize(); i++) {
                tab += " ";
            }
            getTextEditor().addPrefixToSelection(tab);
        }    
    }
    
    public void unindent(){ 
        if (hasSyntaxHighlight()){
            new RSyntaxTextAreaEditorKit.DecreaseIndentAction().actionPerformed(null);
        } else {

            for (int i = 0; i < getTabSize(); i++) {
                getTextEditor().removePrefixFromSelection(" ");
            }
            getTextEditor().removePrefixFromSelection("\t");
        }       
    }    
    
    public void comment(){ 
        getTextEditor().addPrefixToSelection(Context.getInstance().getScriptType().getLineCommentMarker());   
    }    
    
    public void uncomment(){ 
        getTextEditor().removePrefixFromSelection(Context.getInstance().getScriptType().getLineCommentMarker());
    }        
    
    public void toggleComment(){ 
        if (hasSyntaxHighlight()){
            new RSyntaxTextAreaEditorKit.ToggleCommentAction().actionPerformed(null);
        }                   
    }          


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editor = new ch.psi.utils.swing.TextEditor();
        textFilename = new javax.swing.JTextField();
        textCol = new javax.swing.JTextField();

        textFilename.setEditable(false);
        textFilename.setDisabledTextColor(javax.swing.UIManager.getDefaults().getColor("TextField.foreground"));
        textFilename.setEnabled(false);

        textCol.setEditable(false);
        textCol.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        textCol.setText("1");
        textCol.setDisabledTextColor(javax.swing.UIManager.getDefaults().getColor("TextField.foreground"));
        textCol.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(textFilename)
                .addGap(0, 0, 0)
                .addComponent(textCol, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textCol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.utils.swing.TextEditor editor;
    private javax.swing.JTextField textCol;
    private javax.swing.JTextField textFilename;
    // End of variables declaration//GEN-END:variables
}
