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
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Terminal extends MonitoredPanel implements AutoCloseable{

    final String home;
    final JediTermWidget widget;
    final PtyProcess process;
    final TtyConnector connector;
    volatile boolean closed;
    
    public static Font getDefaultFont(){
        return new DefaultSettingsProvider().getTerminalFont();
    }
    
    public Terminal(String home) throws IOException {
        this(home, getDefaultFont());
    }

    public Terminal(String home, Font font) throws IOException {
        this.home = (home == null) ? Sys.getCurDir() : home;
        widget = new JediTermWidget(80, 24, new DefaultSettingsProvider() {
            @Override
            public Font getTerminalFont() {
                return font;
            }

            @Override
            public TextStyle getDefaultStyle() {
                return MainFrame.isDark() ? new TextStyle(TerminalColor.WHITE, TerminalColor.rgb(43,43,43)) : super.getDefaultStyle();
            }
        });
        
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
       
        widget.setTtyConnector(connector);        
        widget.start();        
        this.setLayout(new BorderLayout());
        add(widget);
    }


    
    @Override
    public void close(){
        if (! closed){
            try{
                widget.stop();
            } catch (Exception ex){                
            }
            try{
                connector.close();
            } catch (Exception ex){                
            }
            try{
                process.destroy();
            } catch (Exception ex){                
            }
            closed = true;
        }
    }    
    
    @Override
    protected void onDesactive() {
        close();
    }    

    public static void main(String[] args) throws IOException {
        SwingUtils.showDialog(null, "Terminal", new Dimension(800, 600), new Terminal(null));
    }

}
