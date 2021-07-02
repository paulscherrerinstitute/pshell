package ch.psi.utils.swing;

import ch.psi.utils.Sys;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Terminal extends MonitoredPanel implements AutoCloseable{

    final String home;
    PtyProcess process;
    TtyConnector connector;

    public Terminal(String home, Float size) {
        this.home = (home == null) ? Sys.getCurDir() : home;
        JediTermWidget widget = new JediTermWidget(80, 24, new DefaultSettingsProvider() {
            public float getTerminalFontSize() {
                return (size == null) ? super.getTerminalFontSize() : size;
            }

            @Override
            public TextStyle getDefaultStyle() {
                return MainFrame.isDark() ? new TextStyle(TerminalColor.WHITE, TerminalColor.rgb(43,43,43)) : super.getDefaultStyle();
            }
        });
        widget.setTtyConnector(createTtyConnector());        
        widget.start();
        this.setLayout(new BorderLayout());
        add(widget);
    }

    private TtyConnector createTtyConnector() {
        try {
            Map<String, String> envs = System.getenv();
            String[] command;
            if (Sys.isWindows()) {
                command = new String[]{"cmd.exe"};
            } else if (Sys.isMac()) {
                command = new String[]{"zsh"};
                envs = new HashMap<>(System.getenv());
                envs.put("TERM", "xterm-256color");                
            } else {
                command = new String[]{"bash", "--login"};
                envs = new HashMap<>(System.getenv());
                envs.put("TERM", "xterm-256color");
            }
            process = new PtyProcessBuilder().setCommand(command).setEnvironment(envs).setDirectory(home).start();
            connector = new PtyProcessTtyConnector(process, StandardCharsets.UTF_8);
            return connector;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void close(){
        if (connector!=null){
            try{
                connector.close();
            } catch (Exception ex){                
            }
            connector=null;
        }
        if (process!=null){
            try{
                process.destroy();
            } catch (Exception ex){                
            }
            process=null;
        }
    }    
    
    protected void onDesactive() {
        close();
    }    

    public static void main(String[] args) {
        SwingUtils.showDialog(null, "Terminal", new Dimension(800, 600), new Terminal(null, 10.0f));
    }

}
