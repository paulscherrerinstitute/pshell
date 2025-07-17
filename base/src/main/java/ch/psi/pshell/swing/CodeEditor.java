package ch.psi.pshell.swing;

import ch.psi.pshell.app.App;
import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Sys;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * In order to have matlab file highlighting, add net.imagej:imagej-ui-swing:0.20.0 to the 
 * extensions folder.
 */
public class CodeEditor extends TextEditor {

    final RTextScrollPane scrollPane;
    final RSyntaxTextArea editorPane;
    
    static volatile TokenMap extraTokens;
    
    public static TokenMap getExtraTokens(){
        if (extraTokens==null){
            extraTokens = new TokenMap();
        }
        return extraTokens;
    }
    
    public static void setExtraTokens(TokenMap extraTokens){        
        CodeEditor.extraTokens =  extraTokens;
    }
    
    public static Color getForegroundColor(){
        return TextEditor.getForegroundColor();
    }
    
    public static Color getBackgroundColor(){
        return TextEditor.getBackgroundColor();
    }    
    
    public static Color getDisabledBackgroundColor(){
        return TextEditor.getDisabledBackgroundColor();
    }        
    
    @Override
    protected final void onLafChange() {  
        boolean dark = MainFrame.isDark();
        
        editorPane.setCurrentLineHighlightColor(dark ? new Color(47, 47, 47) : new Color(233, 239, 248));

        Color disabledBackgroundColorMac = new Color(232,232,232);
        Color selectionBackgroundColor = dark ? new Color(66, 70, 80) : new Color(184, 207, 229);     
        Color colorText = getForegroundColor();
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
        if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("Mac OS X")) {
            setEditorDisabledBackground(disabledBackgroundColorMac);
        } else {
            setEditorDisabledBackground(getDisabledBackgroundColor());
        }
        editorPane.setCaretColor(colorText);
        editorPane.setSelectionColor(selectionBackgroundColor);   
        if (isDisplayable()){
            editorPane.setBackground(getBackgroundColor());
            scrollPane.getGutter().setBackground(getBackgroundColor());
        }
    }        

    public CodeEditor() {
        super();        
        editorPane = new RSyntaxTextArea(20, 60);
        editorPane.setAnimateBracketMatching(false);
        editorPane.setHighlightCurrentLine(highlightCurrentLine);
        //editorPane.setCurrentLineHighlightColor(dark ? new Color(30, 30, 35) : new Color(233, 239, 248));
        
        editorPane.setMatchedBracketBorderColor(null);
        editorPane.setMatchedBracketBGColor(new Color(243, 255, 15));
        editorPane.setBracketMatchingEnabled(true);
        editorPane.setTabsEmulated(true);
                
        if (Sys.isLinux()){
            //Comment toggle is disabled in RSyntaxTextArea by default on Linux because it is triggered not only KEY_PRESSED but also KEY_TYPED.
            //This workaround filters the KEY_TYPED event.
            //https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/RSyntaxTextAreaDefaultInputMap.java
            editorPane.addKeyListener(new java.awt.event.KeyAdapter() {              
                @Override
                public void keyTyped(KeyEvent evt) {
                    char c = evt.getKeyChar();
                    if (c == KeyEvent.VK_SLASH) {
                        if (evt.isControlDown() ){
                            evt.consume();
                        }
                    }
                }                
            });
            editorPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, CTRL_DOWN_MASK),RSyntaxTextAreaEditorKit.rstaToggleCommentAction);
        }      
        
        onLafChange();

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



        scrollPane = new RTextScrollPane(editorPane);
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

    boolean highlightCurrentLine=true;
    public void setHighlightCurrentLine(boolean value) {
        if (value !=  highlightCurrentLine){
            highlightCurrentLine = value;
            editorPane.setHighlightCurrentLine(highlightCurrentLine);
        }
    }

    public boolean getHighlightCurrentLine() {
        return highlightCurrentLine;
    }
    
    @Override
    public void setFileName(String fileName) {
        super.setFileName(fileName);
        setType(IO.getExtension(fileName));
    }

    String type;

    public void setType(String value) {
        if (value != null) {
            type = switch (value) {
                case "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA;
                case "py" -> "text/custom_python";
                case "groovy" -> "text/custom_groovy";
                case "js" -> "text/custom_javascript";
                case "c" -> SyntaxConstants.SYNTAX_STYLE_C;
                case "cpp" -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
                case "htm", "html" -> SyntaxConstants.SYNTAX_STYLE_HTML;
                case "json" -> SyntaxConstants.SYNTAX_STYLE_JSON;
                case "sql" -> SyntaxConstants.SYNTAX_STYLE_SQL;
                case "xml" -> SyntaxConstants.SYNTAX_STYLE_XML;
                case "properties" -> SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
                case "vb" -> SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC;
                case "m" -> "text/matlab";
                default -> SyntaxConstants.SYNTAX_STYLE_NONE;
            }; //type = SyntaxConstants.SYNTAX_STYLE_PYTHON;
            //type = SyntaxConstants.SYNTAX_STYLE_GROOVY;
            //type = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
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
        if ((scrollPane!=null) && (scrollPane.getGutter()!=null)){
            scrollPane.getGutter().setBackground(color);
        }
    }

    @Override
    protected void setEditorPaneEnabled(boolean value) {
        super.setEditorPaneEnabled(value);
        editorPane.setBracketMatchingEnabled(value);
        editorPane.setHighlightCurrentLine(highlightCurrentLine && value);
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
