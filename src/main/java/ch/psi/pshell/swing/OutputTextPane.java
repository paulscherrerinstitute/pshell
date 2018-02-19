package ch.psi.pshell.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 *
 */
public class OutputTextPane extends JTextPane {

    public OutputTextPane() {
        setFont(new java.awt.Font("Lucida Console", 0, 11)); // NOI18N
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textPanelKeyPressed(evt);
            }

            @Override
            public void keyTyped(java.awt.event.KeyEvent evt) {
                textPanelKeyTyped(evt);
            }
        });

        if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("nimbus")) {
            //TODO: This is to compensate the fact Nimbus does not honor background color. 
            //Check if may be fixed in the future
            UIDefaults defaults = new UIDefaults();
            defaults.put("TextPane[Enabled].backgroundPainter", nimbusTextPanePainter);
            defaults.put("TextPane[Selected].backgroundPainter", nimbusTextPanePainter);
            putClientProperty("Nimbus.Overrides", defaults);
            putClientProperty("Nimbus.Overrides.InheritDefaults", true);

            //To make sure that is executed  after default calls following the constructors
            SwingUtilities.invokeLater(() -> {
                setBackground(getNimbusReadonlyColor());
            });
        } else if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("Mac OS X")) {
            //setEditable(false);
            SwingUtilities.invokeLater(() -> {
                setBackground(getMacOSReadonlyColor());
            });
        }
    }

    static final Painter nimbusTextPanePainter = (Painter<JComponent>) (Graphics2D g, JComponent comp, int width1, int height1) -> {
        g.setColor(comp.getBackground());
        g.fillRect(0, 0, width1, height1);
    };

    public static Color getNimbusReadonlyColor() {
        return javax.swing.UIManager.getDefaults().getColor("menu");
    }
    
    public static Color getMacOSReadonlyColor() {
        return new Color(244,244,244);
    }    

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        //Invoking otherwise not working on mac os
        SwingUtilities.invokeLater(() -> {
            setCaretColor(bg);
        });
    }

    int textLength = -1;        //Unlimited
    //Properties     

    public int getTextLength() {
        return textLength;
    }

    public void setTextLength(int size) {
        textLength = size;
    }

    //Utilities
    public void clear() {
        setText("");
    }

    class AppendedText {

        StringBuilder sb;
        Color c;

        AppendedText(String msg, Color c) {
            sb = new StringBuilder(msg);
            this.c = c;
        }
    }

    final ArrayList<AppendedText> appendedText = new ArrayList<>();
    int currentLength = 0;

    public void append(final String msg, final Color c) {
        synchronized (appendedText) {
            currentLength += msg.length();
            if ((appendedText.size() > 0) && (appendedText.get(appendedText.size() - 1)).c == c) {
                appendedText.get(appendedText.size() - 1).sb.append(msg);
            } else {
                appendedText.add(new AppendedText(msg, c));
            }
            if (getTextLength() > 0) {
                int overload = currentLength - getTextLength();
                while ((overload > 0) && (appendedText.size() > 0)) {
                    StringBuilder sb = appendedText.get(0).sb;
                    /*
                     if (sb.length() > overload) {
                     for (;overload<sb.length();overload++){
                     if (sb.charAt(overload)=='\n'){
                     overload++;
                     break;
                     }
                     }
                     }
                     */
                    if (sb.length() > overload) {
                        sb.replace(0, overload, "");
                        overload = 0;
                        currentLength -= overload;
                    } else {
                        currentLength -= appendedText.get(0).sb.length();
                        appendedText.remove(0);
                    }
                }
            }
        }

        if (SwingUtilities.isEventDispatchThread()) {
            update();
        } else {
            SwingUtilities.invokeLater(() -> {
                update();
            });
            return;
        }
    }

    void update() {
        ArrayList<AppendedText> list = null;
        synchronized (appendedText) {
            list = (ArrayList<AppendedText>) appendedText.clone();
            appendedText.clear();
            currentLength = 0;
        }
        for (AppendedText txt : list) {
            String text = txt.sb.toString();
            StyleContext sc = StyleContext.getDefaultStyleContext();
            AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, txt.c);
            int len = getDocument().getLength();
            if (getTextLength() > 0) {
                int textLength = len + text.length();
                if (textLength > getTextLength()) {

                    int finalLength = (int) (getTextLength() * 0.9);
                    int delete = textLength - finalLength;
                    try {
                        //Only remove full rows
                        while ((delete < len) && (getDocument().getText(delete, 1).charAt(0) != '\n')) {
                            delete++;
                        }
                        delete++;
                    } catch (BadLocationException ex) {
                        Logger.getLogger(OutputTextPane.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (delete > len) {
                        text = text.substring(delete - len);
                        delete = len;
                    }
                    setSelectionStart(0);
                    setSelectionEnd(delete);
                    replaceSelection("");
                    len = getDocument().getLength();
                }

            }
            setCaretPosition(len);
            setCharacterAttributes(aset, false);
            replaceSelection(text);
        }
    }

    private void textPanelKeyTyped(java.awt.event.KeyEvent evt) {
        evt.consume();
    }

    private void textPanelKeyPressed(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        int code = evt.getKeyCode();

        if ((c == KeyEvent.VK_BACK_SPACE)
                || (c == KeyEvent.VK_ENTER)
                || (c == KeyEvent.VK_TAB)
                || (c == KeyEvent.VK_CONTROL)
                || ((code == KeyEvent.VK_V) && evt.isControlDown())
                || ((code == KeyEvent.VK_X) && evt.isControlDown())
                || ((code == KeyEvent.VK_V) && evt.isMetaDown())
                || ((code == KeyEvent.VK_X) && evt.isMetaDown())
                || (c == KeyEvent.VK_DELETE)) {
            evt.consume();
        }
    }

}
