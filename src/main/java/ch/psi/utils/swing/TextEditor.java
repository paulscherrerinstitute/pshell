package ch.psi.utils.swing;

import ch.psi.utils.Sys;
import ch.psi.utils.Sys.OSFamily;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.PlainDocument;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * Editor for text files.
 */
public class TextEditor extends Editor {
    
    public final static Color TEXT_EDIT_BACKGROUND_COLOR = MainFrame.isDark() ? new Color(43, 43, 43) : Color.WHITE;    

    //Undo manager knows when document returns to a saved state, so title can be updated
    class MyUndoManager extends UndoManager {

        Object unchangedLastEdit;

        void setUnchanged() {
            unchangedLastEdit = editToBeUndone();//lastEdit();
        }

        boolean isUnchanged() {
            return editToBeUndone() == unchangedLastEdit;
        }
    }

    MyUndoManager undoManager;
    Integer contentWidth;

    public static final Color FOREGROUND_COLOR = MainFrame.isDark() ? new Color(187, 187, 187) : Color.BLACK;

    /**
     * Representation of a text document.
     */
    public static class TextDocument extends Document {

        TextEditor editor;
        boolean updatingText;

        protected TextDocument() {
        }
        DocumentListener documentListener = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!updatingText) {
                    setChanged(true);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!updatingText) {
                    setChanged(true);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!updatingText) {
                    setChanged(true);
                }
            }
        };

        @Override
        public void clear() {
            updatingText = true;
            editor.setText("");
            updatingText = false;
            setChanged(false);
        }

        @Override
        public void load(String fileName) throws IOException {
            String text = new String(Files.readAllBytes(Paths.get(fileName)));
            updatingText = true;
            editor.setText(text);
            updatingText = false;
            setChanged(false);
        }

        @Override
        public void save(String fileName) throws IOException {
            Files.write(Paths.get(fileName), editor.getText().getBytes());
            setChanged(false);
        }

        @Override
        public void setChanged(boolean changed) {
            super.setChanged(changed);
            if (!changed) {
                editor.undoManager.setUnchanged();
            }
        }

        public Line[] getLines() {
            //JTextComponent.getText() JEditorPane.getText() have different implementation
            String text = editor.getCanonicalText();
            ArrayList<Line> ret = new ArrayList<>();

            String[] lines = text.split("\\n");
            int pointer = 0;
            for (int i = 0; i < lines.length; i++) {
                Line line = new Line();
                line.index = i;
                line.text = lines[i];
                line.start = pointer;
                line.end = line.start + line.text.length();
                pointer += (line.text.length() + 1);
                ret.add(line);
            }
            return ret.toArray(new Line[0]);
        }

        @Override
        public String getContents() {
            return editor.getText();
        }

    }

    /**
     * Representation of a line in a text document.
     */
    public static class Line {

        public String text;
        public int index;
        public int start;
        public int end;
    }

    JMenuItem menuUndo;
    JMenuItem menuRedo;
    JMenuItem menuFont;

    JMenuItem menuComment;
    JMenuItem menuUncomment;

    public TextEditor() {
        super(new TextDocument());
        ((TextDocument) getDocument()).editor = this;
        initComponents();
        setEditor(new javax.swing.JEditorPane());
    }

    static final Painter nimbusEditorPaneBackPainter = (Painter<JComponent>) (Graphics2D g, JComponent comp, int width1, int height1) -> {
        g.setColor(comp.getBackground());
        g.fillRect(0, 0, width1, height1);
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        jTextField1 = new javax.swing.JTextField();

        scrollPane.setViewportView(jTextField1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jTextField1;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    public JTextComponent getEditor() {
        return editorPane;
    }

    private JPopupMenu menuPopup;
    JTextComponent editorPane;

    public void setEditor(JTextComponent component) {
        editorPane = component;
        scrollPane.setViewportView(editorPane);

        if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("nimbus")) {
            //TODO: This is to compensate the fact Nimbus does not honor background color. 
            //Check if may be fixed in the future
            UIDefaults defaults = new UIDefaults();
            defaults.put("EditorPane[Enabled].backgroundPainter", nimbusEditorPaneBackPainter);
            defaults.put("EditorPane[Selected].backgroundPainter", nimbusEditorPaneBackPainter);
            editorPane.putClientProperty("Nimbus.Overrides", defaults);
            editorPane.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
            //To make sure that is executed  after default calls following the constructors
            SwingUtilities.invokeLater(() -> {
                doSetEditorBackground(editorPane.getBackground());
            });
        }

        editorPane.getDocument().addDocumentListener(((TextDocument) getDocument()).documentListener);
        undoManager = new MyUndoManager();
        editorPane.getDocument().addUndoableEditListener(undoManager);

        Action searchAction = new SearchAction();
        Action searchAgainAction = new SearchAgainAction();
        Action undoAction = new UndoAction();
        Action redoAction = new RedoAction();

        if (menuPopup == null) {
            menuPopup = getPopupMenu();
            menuUndo = new JMenuItem("Undo");
            menuUndo.addActionListener(undoAction);
            menuRedo = new JMenuItem("Redo");
            menuRedo.addActionListener(redoAction);
            menuFont = new JMenuItem("Set font...");
            menuFont.addActionListener((ActionEvent e) -> {
                Frame owner = getFrame();
                FontDialog dlg = new FontDialog(owner, true, getEditorFont());
                dlg.setVisible(true);
                if (dlg.getResult()) {
                    setEditorFont(dlg.getSelectedFont());
                }
            });
            JMenuItem menuCut = new JMenuItem("Cut");
            JMenuItem menuCopySelection = new JMenuItem("Copy");
            JMenuItem menuPaste = new JMenuItem("Paste");
            JMenuItem menuSelectAll = new JMenuItem("Select All");
            menuCut.addActionListener((ActionEvent e) -> {
                cut();
            });
            menuCopySelection.addActionListener((ActionEvent e) -> {
                copySelection();
            });
            menuPaste.addActionListener((ActionEvent e) -> {
                paste();
            });
            menuSelectAll.addActionListener((ActionEvent e) -> {
                selectAll();
            });

            menuPopup.remove(menuCopy);//Only use selection copy
            getPopupMenu().addSeparator();
            getPopupMenu().add(menuCut);
            getPopupMenu().add(menuCopySelection);
            getPopupMenu().add(menuPaste);
            getPopupMenu().add(menuSelectAll);
            getPopupMenu().addSeparator();
            getPopupMenu().add(menuUndo);
            getPopupMenu().add(menuRedo);
            getPopupMenu().addSeparator();
            getPopupMenu().add(menuFont);

            menuPopup.addPopupMenuListener(new PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    boolean canCopy = canCopySelection();
                    menuCut.setEnabled(canCopy);
                    menuCopySelection.setEnabled(canCopy);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
        editorPane.setComponentPopupMenu(menuPopup);

        int modifiers = (Sys.getOSFamily() == OSFamily.Mac) ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;

        editorPane.registerKeyboardAction(searchAction, KeyStroke.getKeyStroke(
                KeyEvent.VK_F, modifiers), JComponent.WHEN_FOCUSED);
        editorPane.registerKeyboardAction(searchAgainAction, KeyStroke.getKeyStroke(
                KeyEvent.VK_F3, 0), JComponent.WHEN_FOCUSED);
        editorPane.registerKeyboardAction(undoAction, KeyStroke.getKeyStroke(
                KeyEvent.VK_Z, modifiers), JComponent.WHEN_FOCUSED);
        editorPane.registerKeyboardAction(redoAction, KeyStroke.getKeyStroke(
                KeyEvent.VK_Y, modifiers), JComponent.WHEN_FOCUSED);
        editorBackground = editorPane.isEnabled() ? editorPane.getBackground() : TEXT_EDIT_BACKGROUND_COLOR;
    }

    public void setScrollPane(JScrollPane pane) {
        ((javax.swing.GroupLayout) getLayout()).replace(scrollPane, pane);
        scrollPane = pane;
    }

    //Properties
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        setEditorPaneEnabled(value);
        getPopupMenu().setEnabled(value);
    }

    @Override
    public void setReadOnly(boolean value) {
        editorPane.setEditable(!value);
    }

    @Override
    public boolean isReadOnly() {
        return !editorPane.isEditable();
    }

    public void setText(String text) {
        if (editorPane != null) {
            editorPane.setText(text);
            editorPane.setCaretPosition(0);
            undoManager.discardAllEdits();
        }
    }

    @Override
    protected void onShow() {
        if (contentWidth != null) {
            setContentWidth(contentWidth);
        }
    }

    public int getContentWidth() {
        if (contentWidth == null) {
            return 0;
        }
        return contentWidth;
    }

    public void setContentWidth(int width) {
        contentWidth = width;
        if (contentWidth > 0) {
            editorPane.setSize(new java.awt.Dimension(width, editorPane.getHeight()));
        }
    }

    public String getText() {
        return editorPane.getText();
    }

    @Override
    public TextDocument getDocument() {
        return (TextDocument) document;
    }

    public Font getEditorFont() {
        return editorPane.getFont();
    }

    public void setEditorFont(Font font) {
        if (font != null) {
            editorPane.setFont(font);
        }
    }

    public String getCanonicalText() {
        return (getEditor() instanceof JEditorPane) ? getText().replaceAll("\\r?\\n", "\n") : getText();
    }

    public boolean search(String searchText, boolean caseInsensitive) {
        this.searchText = searchText;
        this.caseInsensitive = caseInsensitive;
        int location = editorPane.getCaretPosition();
        if ((searchText != null) && !searchText.isEmpty()) {
            String text = getCanonicalText();
            if (caseInsensitive) {
                text = text.toLowerCase();
                searchText = searchText.toLowerCase();
            }
            int pos = text.indexOf(searchText, location);
            //Search again from beggining
            if ((location > 0) && (pos < 0)) {
                pos = text.indexOf(searchText, 0);
            }
            if (pos >= 0) {
                editorPane.setCaretPosition(pos + searchText.length());
                editorPane.select(pos, pos + searchText.length());
                return true;
            }
            return false;
        } else {
            editorPane.setCaretPosition(location);
        }
        return true;
    }

    public void scrollToVisible(final int line) {
        SwingUtils.scrollToVisible(editorPane, line);
    }

    public final void setSelection(int start, int length) {
        editorPane.requestFocus();
        editorPane.setSelectionStart(start);
        editorPane.setSelectionEnd(start + length - 1);
    }

    public void selectBlockFully() {
        editorPane.setSelectionStart(Utilities.getParagraphElement(editorPane, editorPane.getSelectionStart()).getStartOffset());
        editorPane.setSelectionEnd(Utilities.getParagraphElement(editorPane, editorPane.getSelectionEnd()).getEndOffset());
    }

    public void copySelection() {
        editorPane.copy();
    }

    public void paste() {
        editorPane.paste();
    }

    public void cut() {
        editorPane.cut();
    }

    public void selectAll() {
        editorPane.selectAll();
    }

    public boolean canCopySelection() {
        String selection = editorPane.getSelectedText();
        return (selection != null) && (selection.length() > 0);
    }

    public boolean canFindNext() {
        return (searchText != null) && (searchText.length() > 0);
    }

    static String searchText;
    static boolean caseInsensitive;

    class SearchAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent evt) {
            search();
        }
    }

    public void search() {
        JPanel panel = new JPanel(new BorderLayout());
        ((BorderLayout) panel.getLayout()).setVgap(10);
        JTextField text = new JTextField("");

        JCheckBox caseInsens = new JCheckBox("Case insensitive");
        JPanel p = new JPanel(new BorderLayout());
        ((BorderLayout) p.getLayout()).setHgap(5);
        p.add(new JLabel("Search text: "), BorderLayout.WEST);
        p.add(text, BorderLayout.EAST);
        panel.add(p, BorderLayout.NORTH);
        panel.add(caseInsens, BorderLayout.SOUTH);
        text.setPreferredSize(new Dimension(200, text.getPreferredSize().height));
        SwingUtils.requestFocusDeferred(text);
        if (showOption("Find", panel, OptionType.OkCancel) == OptionResult.Yes) {
            if (!search(text.getText(), caseInsens.isSelected())) {
                showMessage("Find", "Text not found");
            }
        }

    }

    public void searchAgain() {
        search(searchText, caseInsensitive);
    }

    public void undo() {
        try {
            undoManager.undo();
            checkUndoManager();
        } catch (CannotUndoException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void redo() {
        try {
            undoManager.redo();
            checkUndoManager();
        } catch (CannotRedoException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    void checkUndoManager() {
        boolean unchanged = undoManager.isUnchanged();
        if (getDocument().changed == unchanged) {
            getDocument().setChanged(!unchanged);
        }
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }

    class SearchAgainAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent evt) {
            searchAgain();
        }
    }

    class UndoAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent evt) {
            undo();
        }
    }

    class RedoAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent evt) {
            redo();
        }
    }

    public void setEditorBackground(Color color) {
        editorBackground = color == null ? TEXT_EDIT_BACKGROUND_COLOR : color;
        if (editorPane.isEnabled()) {
            doSetEditorBackground(editorBackground);
        }
    }

    public Color getEditorBackground() {
        return editorPane.getBackground();
    }

    Color editorBackground;
    Color editorDisabledBackground;

    public void setEditorDisabledBackground(Color color) {
        editorDisabledBackground = color;
        if (!editorPane.isEnabled()) {
            doSetEditorBackground(color);
        }
    }

    public Color getEditorDisabledBackground() {
        return editorDisabledBackground;
    }

    public void setEditorForeground(Color color) {
        editorPane.setForeground(color == null ? FOREGROUND_COLOR : color);
    }

    public Color getEditorForeground() {
        return editorPane.getForeground();
    }

    protected void setEditorPaneEnabled(boolean value) {
        if (value != editorPane.isEnabled()) {
            editorPane.setEnabled(value);
            if (value) {
                doSetEditorBackground(editorBackground);
            } else {
                doSetEditorBackground(editorDisabledBackground);
            }
        }
    }

    protected void doSetEditorBackground(Color color) {
        editorPane.setBackground(color);
    }

    Color disabledTextColor;

    public void disableMouseSelection() {
        setEditorPaneEnabled(false);
        editorPane.setDisabledTextColor(FOREGROUND_COLOR);
        disabledTextColor = editorPane.getDisabledTextColor();
    }

    public void enableMouseSelection() {
        if (disabledTextColor != null) {
            setEditorPaneEnabled(true);
            editorPane.setDisabledTextColor(FOREGROUND_COLOR);
        }
    }

    Highlighter highliter;
    Line[] highlitingLines;

    protected Line[] getHighlitingLines() {
        return highlitingLines;
    }

    public boolean isManualHighligting() {
        return (highliter != null);
    }

    public void enableManualHighligting() {
        disableMouseSelection();
        highliter = editorPane.getHighlighter();
        Highlighter hilite = newLineHighlighter();
        editorPane.setHighlighter(hilite);

        highlitingLines = getDocument().getLines();
    }

    public void disableManualHighligting() {
        if (isManualHighligting()) {
            editorPane.setHighlighter(highliter);
            enableMouseSelection();
            highliter = null;
        }
    }

    public Object highlightLine(int line, Color color) {
        return highlightLines(line, line, color);
    }

    protected DefaultHighlightPainter getDefaultHighlightPainter(Color color) {
        return new DefaultHighlighter.DefaultHighlightPainter(color);
    }

    protected Highlighter newLineHighlighter() {
        return new LineHighlighter();
    }

    public Object highlightLines(int lineFrom, int lineTo, Color color) {
        if (isManualHighligting()) {
            lineTo = Math.min(lineTo, highlitingLines.length);
            if ((lineFrom > 0) && (lineTo >= lineFrom)) {
                try {
                    DefaultHighlightPainter painter = getDefaultHighlightPainter(color);
                    if ((lineFrom <= highlitingLines.length) && (lineTo >= lineFrom)) {
                        int start = highlitingLines[lineFrom - 1].start;
                        int end = highlitingLines[lineTo - 1].end + 1;
                        return editorPane.getHighlighter().addHighlight(start, end, painter);
                    }
                } catch (BadLocationException ex) {
                }
            }
        }
        return null;
    }

    public void removeHighlight(Object highlight) {
        if (isManualHighligting()) {
            editorPane.getHighlighter().removeHighlight(highlight);
            repaint();
        }
    }

    public void removeAllHighlights() {
        if (isManualHighligting()) {
            editorPane.getHighlighter().removeAllHighlights();
            repaint();
        }
    }

    class LineHighlighter extends DefaultHighlighter {

        private JTextComponent component;

        @Override
        public final void install(final JTextComponent c) {
            super.install(c);
            this.component = c;
        }

        @Override
        public final void deinstall(final JTextComponent c) {
            super.deinstall(c);
            this.component = null;
        }

        @Override
        public final void paint(final Graphics g) {
            final Highlighter.Highlight[] highlights = getHighlights();
            final int len = highlights.length;
            for (int i = 0; i < len; i++) {
                Highlighter.Highlight info = highlights[i];
                if (info.getClass().getName().contains("LayeredHighlightInfo")) {
                    final Rectangle a = this.component.getBounds();
                    final Insets insets = this.component.getInsets();
                    if ((a != null) && (insets != null)) {
                        a.x = insets.left;
                        a.y = insets.top;
                        a.height -= insets.top + insets.bottom;
                        for (; i < len; i++) {
                            info = highlights[i];
                            if (info.getClass().getName().indexOf("LayeredHighlightInfo") > -1) {
                                final Highlighter.HighlightPainter p = info.getPainter();
                                p.paint(g, info.getStartOffset(), info.getEndOffset(), a, this.component);
                            }
                        }
                    }
                }
            }
        }
    }

    public final void addPrefixToSelection(String prefix) {
        try {
            selectBlockFully();
            StringBuilder output = new StringBuilder();
            PlainDocument doc = (PlainDocument) editorPane.getDocument();            
            Element root = doc.getDefaultRootElement();
            int start = editorPane.getSelectionStart();
            int end = editorPane.getSelectionEnd();
            for (int i = root.getElementIndex(start); i < root.getElementIndex(end); i++) {
                Element line = root.getElement(i);
                String text = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
                output.append(prefix).append(text);
            }
            editorPane.replaceSelection(output.toString());
            setSelection(start, output.length());
        } catch (BadLocationException ex) {
            Logger.getLogger(TextEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removePrefixFromSelection(String prefix) {
        try {
            selectBlockFully();
            StringBuilder output = new StringBuilder();
            PlainDocument doc = (PlainDocument) editorPane.getDocument();
            Element root = doc.getDefaultRootElement();
            int start = editorPane.getSelectionStart();
            int end = editorPane.getSelectionEnd();
            for (int i = root.getElementIndex(start); i < root.getElementIndex(end); i++) {
                Element line = root.getElement(i);
                String text = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
                if (text.startsWith(prefix)) {
                    output.append(text.substring(prefix.length(), text.length()));
                } else {
                    output.append(text);
                }
            }
            editorPane.replaceSelection(output.toString());
            setSelection(start, output.length());

        } catch (BadLocationException ex) {
            Logger.getLogger(TextEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        TextEditor editor = new TextEditor();
        if ((args.length > 0) && (args[0] != null) & (!args[0].trim().isEmpty())) {
            editor.load(args[0]);
        }
        javax.swing.JFrame frame = editor.getFrame();
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
}
