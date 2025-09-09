package ch.psi.pshell.framework;

import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.Shell;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.TextEditor;
import ch.psi.pshell.utils.ControlChar;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

/**
 *
 */
public class StatusBar extends ch.psi.pshell.app.StatusBar{
    DataPanel dataPanel;
    
    public StatusBar() {
        super();
        if (App.hasInstance() && App.getInstance().hasStatusBarMenu()) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem openLogs = new JMenuItem("Open logs");
            openLogs.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        TextEditor editor = new TextEditor();
                        editor.load(Context.getLogging().getFileName());
                        editor.setReadOnly(true);
                        SwingUtils.showDialog((Frame) getTopLevelAncestor(), Context.getLogging().getFileName(), new Dimension(800, 400), editor);
                    } catch (Exception ex) {
                    }
                }
            });

            OutputPanel outputPanel = new OutputPanel();
            outputPanel.initialize();
            if (Setup.isOutputRedirected()) {
                class ConsoleStream extends OutputStream {

                    final boolean is_err;
                    StringBuilder sb;

                    ConsoleStream(boolean is_err) {
                        this.is_err = is_err;
                        sb = new StringBuilder();
                    }

                    @Override
                    public void write(int b) throws IOException {
                        if (b == ControlChar.LF) {
                            //if (outputPanel.isDisplayable()) {
                            if (is_err) {
                                outputPanel.putError(sb.toString());
                            } else {
                                outputPanel.putOutput(sb.toString());
                            }
                            //}
                            sb = new StringBuilder();
                        } else {
                            sb.append((char) b);
                        }
                    }
                }
                System.setOut(new PrintStream(new ConsoleStream(false)));
                System.setErr(new PrintStream(new ConsoleStream(true)));
            }

            JMenuItem showData = new JMenuItem("Browse data");

            dataPanel = null;
            showData.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if ((dataPanel == null) || (!dataPanel.isDisplayable())) {
                            dataPanel = DataPanel.create(null);
                            SwingUtils.showDialog((Frame) getTopLevelAncestor(), "Data Panel", new Dimension(800, 400), dataPanel);
                        }
                    } catch (Exception ex) {
                    }
                }
            });
            
            JMenuItem showOutput = new JMenuItem("Show output");

            showOutput.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (!outputPanel.isDisplayable()) {
                            SwingUtils.showDialog((Frame) getTopLevelAncestor(), "Output", new Dimension(800, 400), outputPanel);
                        }
                    } catch (Exception ex) {
                    }
                }
            });

            JMenuItem showConsole = new JMenuItem("Show console");
            Shell shell = new Shell();
            shell.initialize();
            showConsole.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (!shell.isDisplayable()) {
                            SwingUtils.showDialog((Frame) getTopLevelAncestor(), "Console", new Dimension(800, 400), shell);
                        }
                    } catch (Exception ex) {
                    }
                }
            });

            popupMenu.add(openLogs);
            popupMenu.addSeparator();
            popupMenu.add(showData);
            popupMenu.add(showOutput);
            popupMenu.add(showConsole);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    checkPopupMenu(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    checkPopupMenu(e);
                }

                void checkPopupMenu(MouseEvent e) {
                    try {
                        if (e.isPopupTrigger()) {
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    } catch (Exception ex) {
                    }
                }
            };

            addMouseListener(mouseAdapter);
        }
        
    }
    
    Timer timerDataFileName;

    public void setShowDataFileName(boolean value) {
        if ((timerDataFileName != null) != value) {
            if (value) {
                getAuxLabel().setForeground(new java.awt.Color(100, 100, 100));
                timerDataFileName = new Timer(2000, (ActionEvent e) -> {
                    try {
                        //Limioting the size of aux message, otherwise will expand window.
                        int maxFilenameLenght = Math.max((getVisibleRect().width - 200) / 8, 10);
                        String file = Context.getDataManager().getLastOutput();
                        if (Context.getState().isNormal() && (file != null)) {
                            if (file.length() > maxFilenameLenght) {
                                file = file.substring(0, maxFilenameLenght - 3) + "...";
                            }
                            setAuxMessage(file);
                        } else {
                            setAuxMessage("");
                        }
                    } catch (Exception ex) {
                        setAuxMessage(ex.getMessage());
                    }
                });
                timerDataFileName.start();

            } else {
                timerDataFileName.stop();
                timerDataFileName = null;
                setAuxMessage("");
            }
        }
    }
    
    
    ScanListener scanListener = new ScanListener() {
        Scan progressScan;

        @Override
        public void onScanStarted(Scan scan, final String plotTitle) {
            setProgress(0);
            int records = scan.getNumberOfRecords();
            if (records > 0) {
                setProgressStep(1.0 / (double) records);
            } else {
                setProgressStep(0);
            }
            progressScan = scan;
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (scan == progressScan) {
                stepProgress();
            }
        }

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
            if (scan == progressScan) {
                if (Context.getApp().getRunningTask() == null) {
                    setProgress(-1);
                }
            }
        }
    };
    

    @Override
    protected void onShow() {
        super.onShow();
        if (Context.hasSequencer()){
            Context.getSequencer().addScanListener(scanListener);        
        }
    }    
    
    @Override
    protected void onHide() {
        super.onHide();
        if (Context.hasSequencer()){
            Context.getSequencer().removeScanListener(scanListener);
        }
    }
    
    
}
