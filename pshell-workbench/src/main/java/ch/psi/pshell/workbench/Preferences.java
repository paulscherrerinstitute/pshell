package ch.psi.pshell.workbench;

import ch.psi.pshell.devices.DefaultPanel;
import ch.psi.pshell.devices.DevicePanelManager;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.MainFrame.ScriptEditorPreferences;
import ch.psi.pshell.framework.MainFrame.ScriptPopupMode;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.PlotLayout;
import ch.psi.pshell.swing.CodeEditor;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.Terminal;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Serializer;
import ch.psi.pshell.utils.Str;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.awt.Color;
import java.awt.Font;
import java.beans.Transient;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity class holder user Workbench displaying preferences.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Preferences {

    static final long serialVersionUID = 1;

    public static  final String PROPERTY_PREFERENCES = "ch.psi.pshell.preferences";
    static final String PREFERENCES_FILENAME = "preferences.json";
    
    static final PanelLocation DEFAULT_CONSOLE_LOCATION = PanelLocation.Document;
    static final PanelLocation DEFAULT_DATA_PANEL_LOCATION = PanelLocation.Status;

    public enum PanelLocation {

        Document,
        Plot,
        Status,
        Detached,
        Hidden,
        Left
        
    }

    public static class FontSpec implements Serializable {
        private static final long serialVersionUID = 1L;    
        public String name;
        public int style;
        public int size;
        public FontSpec(){
        }
        public FontSpec(String name, int style, int size){
            this.name=name;
            this.style=style;
            this.size=size;
        }
        public Font toFont(){
            return new Font(name, style, size);
        }
        public static FontSpec fromFont(Font font){
            return new FontSpec(font.getName(), font.getStyle(), font.getSize());
        }
        @Override
        public String toString(){
            return "Font - name:" + name + " style:" + style + " size:" + size;
        }
    }   
    

    public FontSpec fontShellPanel;
    public FontSpec fontShellCommand;
    public FontSpec fontOutput;
    public FontSpec fontEditor;
    public FontSpec fontPlotLabel;
    public FontSpec fontPlotTick;
    public FontSpec fontPlotTitle;
    public FontSpec fontTerminal;
    public int tabSize = 4;
    public int contentWidth;
    public Integer editorBackground;
    public Integer editorForeground;
    public boolean simpleEditor;
    public boolean hideEditorLineNumbers;
    public boolean hideEditorContextMenu;

    public PanelLocation consoleLocation;
    public PanelLocation dataPanelLocation;
    public boolean openDataFilesInDocTab;
    public boolean noVariableEvaluationPropagation;
    public String[] processingScripts;

    public boolean asyncViewersUpdate;
    public boolean asyncHistoryPlotsUpdate;
    public Integer defaultPanelPrecision; 
    public boolean scanPlotDisabled;
    public boolean scanTableDisabled;
    public boolean cachedDataPanel;
    public String dataExtensions;
    public String dataSubFiles;
    public boolean showEmergencyStop;
    public boolean showHomingButtons;
    public boolean showJogButtons;
    public boolean hideScanPanel;
    public boolean hideOutputPanel;
    public boolean showXScanFileBrowser;
    //public boolean showXScanDataViewer;
    public boolean showQueueBrowser;
    
    public boolean backgroundRendering;
    public boolean showImageStatusBar = true;
    public boolean persistRendererWindows;
    public Colormap defaultRendererColormap;

    public String linePlot = ch.psi.pshell.plot.LinePlotJFree.class.getName();
    public String matrixPlot = ch.psi.pshell.plot.MatrixPlotJFree.class.getName();
    public String surfacePlot = ch.psi.pshell.plot.SurfacePlotJzy3d.class.getName();
    public String timePlot = ch.psi.pshell.plot.TimePlotJFree.class.getName();
    public boolean plotsDetached;
    public boolean plotsHidden;
    public PlotLayout plotLayout = PlotLayout.Vertical;
    public Quality quality = Quality.High;
    public Colormap defaultPlotColormap;
    public int markerSize;
    public Integer plotBackground = null; 
    public Integer gridColor = null;  
    public Integer outlineColor = null; 
    public boolean disableOffscreenBuffer;

    public DefaultPanel[] defaultPanels;

    public ScriptPopupMode scriptPopupMode = ScriptPopupMode.None;

    Path file;
    
    public static Path getFile(){ 
        String pref = Options.PREFERENCES.getString(null);
        if ((pref!=null) && (!pref.isBlank())){
            return Paths.get(Setup.expandPath(pref.trim()));
        } else {
            return Paths.get(Setup.getConfigPath(), PREFERENCES_FILENAME);
        }    
    }

    public static Preferences load() {
        Preferences preferences = new Preferences();
        Path file = getFile();

        try {
           preferences = (Preferences) Serializer.decode(file, Preferences.class);

        } catch (Exception ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.FINE, null, ex);
            //Defaults
            FontSpec[] fonts = getDefaultFonts();
            preferences.fontShellPanel = fonts[0];
            preferences.fontEditor = fonts[1];
            preferences.fontOutput = fonts[2];
            preferences.fontShellCommand = fonts[3];
            preferences.fontPlotLabel = fonts[4];
            preferences.fontPlotTick = fonts[5];
            preferences.fontPlotTitle= fonts[6];
            preferences.fontTerminal= fonts[7];
            preferences.tabSize = 4;
            preferences.processingScripts = new String[0];
            preferences.defaultPanels = DevicePanelManager.getDefaultPanels();
            preferences.consoleLocation = DEFAULT_CONSOLE_LOCATION;
            preferences.dataPanelLocation = DEFAULT_DATA_PANEL_LOCATION;
            preferences.defaultPlotColormap = Colormap.Temperature;
            preferences.defaultRendererColormap = Colormap.Grayscale;        
            preferences.plotBackground = null;
            preferences.gridColor = null;  
            preferences.outlineColor = null; 
            preferences.editorForeground= null;    
            preferences.editorBackground = null;
            preferences.showImageStatusBar = true;
            preferences.linePlot = ch.psi.pshell.plot.LinePlotJFree.class.getName();
            preferences.matrixPlot = ch.psi.pshell.plot.MatrixPlotJFree.class.getName();
            preferences.timePlot = ch.psi.pshell.plot.TimePlotJFree.class.getName();
            preferences.plotLayout = PlotLayout.Vertical;
            preferences.quality = Quality.High;
        }
        if (preferences.fontTerminal==null){
            preferences.fontTerminal=getDefaultFonts()[7];
        }
        preferences.file = file;
        
        //Not removing invalid device and panel classes, otherwise dynamic classes won't work
        /*
        DefaultPanel[] invalid = new DefaultPanel[0];
        for (DefaultPanel defaultPanel : preferences.defaultPanels) {
            try {
                defaultPanel.getDeviceClass();
                defaultPanel.getPanelClass();
            } catch (Exception ex) {
                    Logger.getLogger(Preferences.class.getName()).log(Level.FINE, null, ex);
                    invalid = Arr.append(invalid, defaultPanel);
            }
        }
        preferences.defaultPanels = Arr.remove(preferences.defaultPanels, invalid);
        */
        
        return preferences;
    }
    
    public static FontSpec getDefaultEditorFont(){
        return SwingUtils.hasFont("Lucida Console")
            ? new FontSpec("Lucida Console", 0, 11)
            : new FontSpec(Font.MONOSPACED, 0, 13);
    }

    @Transient
    public static FontSpec[] getDefaultFonts() {
        FontSpec editorFont = getDefaultEditorFont();
        FontSpec commandFont = new FontSpec(Font.SANS_SERIF, 0, 13);
        FontSpec plotLabelFont = new FontSpec(Font.SANS_SERIF, 0, 11);        
        FontSpec plotTickFont = new FontSpec(Font.SANS_SERIF, 0, 10); 
        FontSpec plotTitleFont =  new FontSpec(Font.SANS_SERIF, Font.BOLD, 13);
        FontSpec terminalFont = null;

        try{
            //Terminal is not present in light jar
            terminalFont = FontSpec.fromFont(Terminal.getDefaultFont());
        } catch (Throwable t){
            terminalFont = new FontSpec(Font.MONOSPACED, 0, 13);
        }
        
        return new FontSpec[]{
            editorFont,
            editorFont,
            editorFont,
            commandFont,
            plotLabelFont,
            plotTickFont,
            plotTitleFont,
            terminalFont
        };
    }
   

    public void save() {
        try {
            Serializer.encode(this, file);
            IO.setFilePermissions(file.toFile(), Context.getConfigFilePermissions());
        } catch (Exception ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Transient
    public Color getEditorBackgroundColor() {
        if (editorBackground == null) {
            return CodeEditor.getBackgroundColor();
        }
        return new Color(editorBackground);
    }

    @Transient
    public Color getEditorForegroundColor() {
        if (editorForeground == null) {
            return CodeEditor.getForegroundColor();
        }
       return new Color(editorForeground);
    }
    
    @Transient
    public Color getPlotBackgroundColor() {
        if (plotBackground == null) {
            return null;
        }
       return new Color(plotBackground);
    }

    @Transient
    public Color getPlotGridColor() {
        if (gridColor == null) {
            return null;
        }
       return new Color(gridColor);
    }

    @Transient
    public Color getPlotOutlineColor() {
        if (outlineColor == null) {
            return null;
        }
       return new Color(outlineColor);
    }    
    
    @Transient
    public ScriptPopupMode getScriptPopupMode() {
        if (scriptPopupMode == null) {
            return ScriptPopupMode.None;
        }
        return scriptPopupMode;
    }
    
    @Transient
    public String[] getDataPanelAdditionalExtensions(){
        if (dataExtensions!=null){
            String[] ret = Str.split(dataExtensions.trim(), new String[]{"|", ";", ",", " "});
            ret = Arr.removeEquals(ret, "");
            return ret;
        }
        return new String[0];
    }
    
    @Transient
    public String[] getDataPanelAdditionalFiles(){
        if (dataSubFiles!=null){
            String[] ret = Str.split(dataSubFiles.trim(), new String[]{"|", ";", ",", " "});
            ret = Arr.removeEquals(ret, "");
            return ret;
        }
        return new String[0];
    }    
    
    @Transient
    public int getDefaultPanelPrecision(){
        return (defaultPanelPrecision==null) ? 6 : defaultPanelPrecision;
    }
    
    @Transient
    public ScriptEditorPreferences getScriptEditorPreferences(){
        return new ScriptEditorPreferences( 
            simpleEditor,
            hideEditorLineNumbers,
            hideEditorContextMenu,
            tabSize,
            fontEditor.toFont(),
            getEditorBackgroundColor(),
            getEditorForegroundColor(),
            contentWidth);
    }
}
