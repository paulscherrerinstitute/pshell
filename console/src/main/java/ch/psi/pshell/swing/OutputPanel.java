package ch.psi.pshell.swing;

import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.sequencer.Interpreter;
import ch.psi.pshell.sequencer.InterpreterListener;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 */
public class OutputPanel extends MonitoredPanel {

    public OutputPanel() {
        initComponents();

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuClear = new JMenuItem("Clear");
        menuClear.addActionListener((ActionEvent e) -> {
            outputTextPane.clear();
        });
        popupMenu.add(menuClear);

        outputTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

        });
//      outputTextPane.setBackground(Color.red);
    }

    public void initialize() {
        
    }
    
    @Override
    protected void onActive() {
        Interpreter.getInstance().addListener(interpreterListener);
    }

    @Override
    protected void onDesactive() {
        outputTextPane.clear();
        Interpreter.getInstance().removeListener(interpreterListener);        
    }    

    final InterpreterListener interpreterListener = new InterpreterListener() {
        @Override
        public void onShellStdout(String str) {
            outputTextPane.append(str + "\n", SwingUtils.getColorStdout());
        }

        @Override
        public void onShellStderr(String str) {
            outputTextPane.append(str + "\n", SwingUtils.getColorStderr());
        }

        @Override
        public void onShellStdin(String str) {
            outputTextPane.append(str + "\n", SwingUtils.getColorStdin());
        }

        @Override
        public void onExecutingFile(String fileName) {
            String scriptName = Interpreter.getInstance().getStandardScriptName(fileName);
            outputTextPane.append(getTaskInitMessage(scriptName) + "\n", SwingUtils.getColorOutput());
        }

        @Override
        public void onExecutedFile(String fileName, Object result) {
            if (result != null) {
                if (result instanceof Throwable throwable) {
                    outputTextPane.append(InterpreterResult.getPrintableMessage(throwable) + "\n", SwingUtils.getColorError());
                } else {
                    outputTextPane.append(String.valueOf(result) + "\n", SwingUtils.getColorOutput());
                }
            }

            String scriptName = Interpreter.getInstance().getStandardScriptName(fileName);
            outputTextPane.append(getTaskFinishMessage(scriptName) + "\n", SwingUtils.getColorOutput());
        }
    };

    public static String getTaskInitMessage(String task) {
        return "\n--- Start running: " + task + " ---";
    }

    public static String getTaskFinishMessage(String task) {
        return "--- Finished running: " + task + " ---";
    }

    //TODO: Get from Config
    static int outputMaxLength = -1;

    public void putOutput(String str) {
        outputTextPane.append(str + "\n", SwingUtils.getColorOutput());
    }

    public void putError(String str) {
        outputTextPane.append(str + "\n", SwingUtils.getColorError());
    }

    //Properties     
    public int getTextLength() {
        return outputTextPane.getTextLength();
    }

    public void setTextLength(int size) {
        outputTextPane.setTextLength(size);
    }

    //Utilities
    public void clear() {
        outputTextPane.clear();
    }

    public void setTextPaneFont(Font font) {
        outputTextPane.setFont(font);
    }

    public Font getTextPaneFont() {
        return outputTextPane.getFont();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        outputTextPane = new ch.psi.pshell.swing.OutputTextPane();

        outputTextPane.setTextLength(50000);
        jScrollPane1.setViewportView(outputTextPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private ch.psi.pshell.swing.OutputTextPane outputTextPane;
    // End of variables declaration//GEN-END:variables
}
