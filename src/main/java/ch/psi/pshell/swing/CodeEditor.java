package ch.psi.pshell.swing;

import ch.psi.pshell.ui.App;
import ch.psi.utils.IO;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.TextEditor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * In order to have matlab file highlighting, add net.imagej:imagej-ui-swing:0.20.0 to the 
 * extensions folder.
 */
public class CodeEditor extends TextEditor {

    public final static Color TEXT_FOREGROUND_COLOR = MainFrame.isDark() ? new Color(187, 187, 187) : Color.BLACK;
    public final static Color TEXT_BACKGROUND_COLOR = DevicePanel.TEXT_EDIT_BACKGROUND_COLOR;
    public final static Color TEXT_DISABLED_BACKGROUND_COLOR = MainFrame.isDark() ? new Color(60, 63, 65) : new Color(222, 225, 229);
    public final static Color TEXT_DISABLED_BACKGROUND_COLOR_MAC = new Color(232,232,232);
    public final static Color TEXT_SELECTION_BACKGROUND_COLOR = MainFrame.isDark() ? new Color(66, 70, 80) : new Color(184, 207, 229);

    RTextScrollPane scrollPane;
    RSyntaxTextArea editorPane;

    public CodeEditor() {
        super();
        boolean dark = MainFrame.isDark();
        editorPane = new RSyntaxTextArea(20, 60);
        editorPane.setAnimateBracketMatching(false);
        editorPane.setHighlightCurrentLine(true);
        //editorPane.setCurrentLineHighlightColor(dark ? new Color(30, 30, 35) : new Color(233, 239, 248));
        editorPane.setCurrentLineHighlightColor(dark ? new Color(47, 47, 47) : new Color(233, 239, 248));
        editorPane.setMatchedBracketBorderColor(null);
        editorPane.setMatchedBracketBGColor(new Color(243, 255, 15));
        editorPane.setBracketMatchingEnabled(true);
        editorPane.setTabsEmulated(true);

        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/custom_python", "ch.psi.pshell.swing.CodeEditorPythonMarker");
        atmf.putMapping("text/custom_javascript", "ch.psi.pshell.swing.CodeEditorJavaScriptMarker");
        atmf.putMapping("text/custom_groovy", "ch.psi.pshell.swing.CodeEditorGroovyMarker");
        try{
            String matlabMarker = "net.imagej.ui.swing.script.highliters.MatlabTokenMaker";
            Class.forName(matlabMarker);
            atmf.putMapping("text/matlab", "net.imagej.ui.swing.script.highliters.MatlabTokenMaker");
        } catch (Exception ex){            
        }

        Color colorText = TEXT_FOREGROUND_COLOR;
        Color colorString = new Color(206, 123, 0);
        Color colorComment = dark ? new Color(128, 128, 128) : new Color(150, 150, 150);
        Color colorReserved = dark ? new Color(70, 70, 255) : new Color(0, 0, 230);
        Color colorNumber = dark ? new Color(155, 105, 50) : new Color(125, 75, 0);
        Color colorSeparator = colorText; //Brackets, parenthesis...
        Color colorOperator = colorText;
        Color colorField = dark ? new Color(0, 200, 0) : new Color(0, 140, 0);
        Color colorFunction = dark ? new Color(220, 0, 220) : new Color(128, 0, 128);

        boolean italicComments = true;
        boolean boldReserverd = false;

        SyntaxScheme scheme = editorPane.getSyntaxScheme();
        scheme.getStyle(Token.ANNOTATION).foreground = colorComment;
        scheme.getStyle(Token.COMMENT_EOL).foreground = colorComment;
        scheme.getStyle(Token.COMMENT_MULTILINE).foreground = colorComment;
        scheme.getStyle(Token.COMMENT_MARKUP).foreground = colorComment;
        scheme.getStyle(Token.COMMENT_KEYWORD).foreground = colorComment;
        scheme.getStyle(Token.COMMENT_DOCUMENTATION).foreground = colorComment;
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = colorString;
        scheme.getStyle(Token.LITERAL_CHAR).foreground = colorString;
        scheme.getStyle(Token.RESERVED_WORD).foreground = colorReserved;
        scheme.getStyle(Token.RESERVED_WORD_2).foreground = colorReserved;
        scheme.getStyle(Token.IDENTIFIER).foreground = colorText;
        scheme.getStyle(Token.OPERATOR).foreground = colorOperator;
        scheme.getStyle(Token.SEPARATOR).foreground = colorSeparator;//new Color(30,30,30);      
        scheme.getStyle(Token.LITERAL_BACKQUOTE).foreground = colorNumber;
        scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = colorNumber;
        scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = colorNumber;
        scheme.getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = colorNumber;
        scheme.getStyle(Token.LITERAL_NUMBER_HEXADECIMAL).foreground = colorNumber;

        scheme.getStyle(Token.VARIABLE).foreground = colorField;
        scheme.getStyle(Token.FUNCTION).foreground = colorFunction;
        scheme.getStyle(Token.DATA_TYPE).foreground = colorFunction;

        scheme.getStyle(Token.DATA_TYPE).font = scheme.getStyle(Token.FUNCTION).font;

        scheme.getStyle(Token.MARKUP_CDATA).foreground = colorNumber;
        scheme.getStyle(Token.MARKUP_CDATA_DELIMITER).foreground = colorNumber;
        scheme.getStyle(Token.MARKUP_COMMENT).foreground = colorComment;
        scheme.getStyle(Token.MARKUP_DTD).foreground = colorNumber;
        scheme.getStyle(Token.MARKUP_ENTITY_REFERENCE).foreground = colorNumber;
        scheme.getStyle(Token.MARKUP_PROCESSING_INSTRUCTION).foreground = colorNumber;
        scheme.getStyle(Token.MARKUP_TAG_NAME).foreground = colorReserved;
        scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE).foreground = colorField;
        scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE_VALUE).foreground = colorString;
        scheme.getStyle(Token.MARKUP_TAG_DELIMITER).foreground = colorReserved;

        //Let GUI choose a single font (therefore not making reeserver fonts bold)
        if (!italicComments) {
            scheme.getStyle(Token.COMMENT_EOL).font = null;
            scheme.getStyle(Token.COMMENT_MULTILINE).font = null;
            scheme.getStyle(Token.COMMENT_MARKUP).font = null;
            scheme.getStyle(Token.COMMENT_KEYWORD).font = null;
            scheme.getStyle(Token.COMMENT_DOCUMENTATION).font = null;
        }
        if (!boldReserverd) {
            scheme.getStyle(Token.RESERVED_WORD).font = null;
            scheme.getStyle(Token.RESERVED_WORD_2).font = null;
        }
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).font = null;
        scheme.getStyle(Token.LITERAL_CHAR).font = null;
        scheme.getStyle(Token.IDENTIFIER).font = null;
        scheme.getStyle(Token.OPERATOR).font = null;
        scheme.getStyle(Token.SEPARATOR).font = null;
        scheme.getStyle(Token.LITERAL_BACKQUOTE).font = null;
        scheme.getStyle(Token.LITERAL_BOOLEAN).font = null;
        scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).font = null;
        scheme.getStyle(Token.LITERAL_NUMBER_FLOAT).font = null;
        scheme.getStyle(Token.LITERAL_NUMBER_HEXADECIMAL).font = null;
        scheme.getStyle(Token.VARIABLE).font = null;
        scheme.getStyle(Token.FUNCTION).font = null;
        scheme.getStyle(Token.DATA_TYPE).font = null;

        scrollPane = new RTextScrollPane(editorPane);

        if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("Mac OS X")) {
            setEditorDisabledBackground(TEXT_DISABLED_BACKGROUND_COLOR_MAC);
        } else {
            setEditorDisabledBackground(TEXT_DISABLED_BACKGROUND_COLOR);
        }
        editorPane.setCaretColor(colorText);
        editorPane.setSelectionColor(TEXT_SELECTION_BACKGROUND_COLOR);

        editorPane.getMargin().left = 3;
        setShowLineNumbers(true);
        setScrollPane(scrollPane);
        setEditor(editorPane);
    }

    boolean showLineNumbers;

    public void setShowLineNumbers(boolean value) {
        showLineNumbers = value;
        scrollPane.setLineNumbersEnabled(showLineNumbers);
    }

    public boolean getShowLineNumbers() {
        return showLineNumbers;
    }

    @Override
    public void setFileName(String fileName) {
        super.setFileName(fileName);
        setType(IO.getExtension(fileName));
    }

    String type;

    public void setType(String value) {
        if (value != null) {
            switch (value) {
                case "java":
                    type = SyntaxConstants.SYNTAX_STYLE_JAVA;
                    break;
                case "py":
                    //type = SyntaxConstants.SYNTAX_STYLE_PYTHON;
                    type = "text/custom_python";
                    break;
                case "groovy":
                    //type = SyntaxConstants.SYNTAX_STYLE_GROOVY;
                    type = "text/custom_groovy";
                    break;
                case "js":
                    //type = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
                    type = "text/custom_javascript";
                    break;
                case "c":
                    type = SyntaxConstants.SYNTAX_STYLE_C;
                    break;
                case "cpp":
                    type = SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
                    break;
                case "htm":
                case "html":
                    type = SyntaxConstants.SYNTAX_STYLE_HTML;
                    break;
                case "json":
                    type = SyntaxConstants.SYNTAX_STYLE_JSON;
                    break;
                case "sql":
                    type = SyntaxConstants.SYNTAX_STYLE_SQL;
                    break;
                case "xml":
                    type = SyntaxConstants.SYNTAX_STYLE_XML;
                    break;
                case "properties":
                    type = SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
                    break;
                case "vb":
                    type = SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC;
                    break;
                case "m":
                    type = "text/matlab";
                    break;
                default:
                    type = SyntaxConstants.SYNTAX_STYLE_NONE;
            }
            editorPane.setSyntaxEditingStyle(type);
        }
    }

    public String getType() {
        return type;
    }

    @Override
    protected DefaultHighlightPainter getDefaultHighlightPainter(Color color) {
        return new RSyntaxTextAreaHighlighter.DefaultHighlightPainter(color);
    }

    @Override
    protected Highlighter newLineHighlighter() {
        return new LineHighlighter();
    }

    @Override
    protected void doSetEditorBackground(Color color) {
        super.doSetEditorBackground(color);
        scrollPane.getGutter().setBackground(color);
    }

    @Override
    protected void setEditorPaneEnabled(boolean value) {
        super.setEditorPaneEnabled(value);
        editorPane.setBracketMatchingEnabled(value);
        editorPane.setHighlightCurrentLine(value);
    }

    class LineHighlighter extends RSyntaxTextAreaHighlighter {

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
                    if (component != null) {
                        final Rectangle a = component.getBounds();
                        final Insets insets = component.getInsets();
                        if ((a != null) && (insets != null)) {
                            a.x = insets.left;
                            a.y = insets.top;
                            a.height -= insets.top + insets.bottom;
                            for (; i < len; i++) {
                                info = highlights[i];
                                if (info.getClass().getName().indexOf("LayeredHighlightInfo") > -1) {
                                    final Highlighter.HighlightPainter p = info.getPainter();
                                    p.paint(g, info.getStartOffset(), info.getEndOffset(), a, component);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    public static void main(String[] args) throws InterruptedException, IOException {
        App.init(args);
        CodeEditor editor = new CodeEditor();
        if (args.length > 0) {
            try{
                editor.load(args[0]);
            } catch (Exception ex){                
            }
        }
        javax.swing.JFrame frame = editor.getFrame();
        frame.setSize(600, 400);
        frame.setVisible(true);
    }        
}
