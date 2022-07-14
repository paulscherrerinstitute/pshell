package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import ch.psi.utils.Serializer;
import ch.psi.pshell.device.Camera;
import ch.psi.pshell.device.DiscretePositioner;
import ch.psi.pshell.device.HistogramGenerator;
import ch.psi.pshell.device.MasterPositioner;
import ch.psi.pshell.device.Motor;
import ch.psi.pshell.device.MotorGroup;
import ch.psi.pshell.device.ProcessVariable;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.device.Slit;
import ch.psi.pshell.epics.Scienta;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.swing.CameraPanel;
import ch.psi.pshell.swing.DevicePoolPanel.DefaultPanel;
import ch.psi.pshell.swing.DiscretePositionerPanel;
import ch.psi.pshell.swing.MotorGroupPanel;
import ch.psi.pshell.swing.MotorPanel;
import ch.psi.pshell.swing.ProcessVariablePanel;
import ch.psi.pshell.plotter.PlotLayout;
import ch.psi.pshell.swing.ScientaPanel;
import ch.psi.pshell.swing.SlitPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.pshell.epics.Scaler;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.swing.CodeEditor;
import ch.psi.pshell.swing.DeviceValueChart;
import ch.psi.pshell.swing.HistogramGeneratorPanel;
import ch.psi.pshell.swing.MasterPositionerPanel;
import ch.psi.pshell.swing.ScalerPanel;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import ch.psi.utils.Arr;
import ch.psi.utils.swing.Terminal;
import java.awt.Color;
import java.beans.Transient;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity class holder user Workbench displaying preferences.
 */
public class Preferences {

    static final long serialVersionUID = 1;

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

    public enum ScriptPopupDialog {
        None,
        Exception,
        Return
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
    public boolean scanPlotDisabled;
    public boolean scanTableDisabled;
    public boolean cachedDataPanel;
    public String dataExtensions;
    public String dataSubFiles;
    public boolean hideFileName;
    public boolean showEmergencyStop;
    public boolean showHomingButtons;
    public boolean showJogButtons;
    public boolean hideScanPanel;
    public boolean hideOutputPanel;
    public boolean showXScanFileBrowser;
    public boolean showXScanDataViewer;
    public boolean showQueueBrowser;
    
    public boolean backgroundRendering;
    public boolean showImageStatusBar = true;
    public boolean persistRendererWindows;
    public Colormap defaultRendererColormap;

    public String linePlot = ch.psi.pshell.plot.LinePlotJFree.class.getName();
    public String matrixPlot = ch.psi.pshell.plot.MatrixPlotJFree.class.getName();
    public String surfacePlot = null;
    public String timePlot = ch.psi.pshell.plot.TimePlotJFree.class.getName();
    public boolean plotsDetached;
    public PlotLayout plotLayout = PlotLayout.Vertical;
    public Quality quality = Quality.High;
    public Colormap defaultPlotColormap;
    public int markerSize;
    public Integer plotBackground = null; 
    public Integer gridColor = null;  
    public Integer outlineColor = null; 
    public boolean disableOffscreenBuffer;

    public DefaultPanel[] defaultPanels;

    public ScriptPopupDialog scriptPopupDialog = ScriptPopupDialog.None;

    Path file;

    public static Preferences load() {
        Preferences preferences = new Preferences();
        String pref = App.getArgumentValue("pref");
        Path file;
        if ((pref!=null) && (!pref.isBlank())){
            file =Paths.get(Context.getInstance().getSetup().expandPath(pref.trim()));
        } else {
            file = Paths.get(Context.getInstance().getSetup().getConfigPath(), PREFERENCES_FILENAME);
            if (!file.toFile().exists()){
                //TODO: Just for backward compatibility - Remove in the future
                Path back_comp = Paths.get(Context.getInstance().getSetup().getContextPath(), "Preferences.dat");
                if (back_comp.toFile().exists()){
                    try {
                        HashMap<String, Object> map = (HashMap) Serializer.decode(Files.readAllBytes(back_comp), Serializer.EncoderType.bin);
                        for (String name : map.keySet()) {
                            try {
                                Field f = Preferences.class.getField(name);
                                Object obj = map.get(name);
                                if (obj instanceof Font){
                                    obj = FontSpec.fromFont((Font)obj);
                                } else if (obj instanceof Color){
                                    obj = ((Color)obj).getRGB();
                                }
                                f.set(preferences,obj);
                            } catch (Exception ex) {
                                Logger.getLogger(Preferences.class.getName()).log(Level.FINE, null, ex);
                            }
                        }
                        Serializer.encode(preferences, file);
                        IO.setFilePermissions(file.toFile(), Context.getInstance().getConfig().filePermissionsConfig);
                    } catch (IOException ex) {
                        Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        

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
            preferences.defaultPanels = getDefaultPanels();
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

    @Transient
    public static DefaultPanel[] getDefaultPanels() {
        return new DefaultPanel[]{
            new DefaultPanel(Scaler.class.getName(), ScalerPanel.class.getName()),
            new DefaultPanel(Scienta.class.getName(), ScientaPanel.class.getName()),
            new DefaultPanel(Motor.class.getName(), MotorPanel.class.getName()),
            new DefaultPanel(MasterPositioner.class.getName(), MasterPositionerPanel.class.getName()),
            new DefaultPanel(ProcessVariable.class.getName(), ProcessVariablePanel.class.getName()),
            new DefaultPanel(MotorGroup.class.getName(), MotorGroupPanel.class.getName()),
            new DefaultPanel(DiscretePositioner.class.getName(), DiscretePositionerPanel.class.getName()),
            new DefaultPanel(Camera.class.getName(), CameraPanel.class.getName()),
            new DefaultPanel(Slit.class.getName(), SlitPanel.class.getName()),
            new DefaultPanel(HistogramGenerator.class.getName(), HistogramGeneratorPanel.class.getName()),
            new DefaultPanel(ReadonlyRegister.ReadonlyRegisterArray.class.getName(), DeviceValueChart.class.getName()),
            new DefaultPanel(ReadonlyRegister.ReadonlyRegisterMatrix.class.getName(), DeviceValueChart.class.getName()),            
        };
    }

    @Transient
    public static FontSpec[] getDefaultFonts() {
        FontSpec editorFont = SwingUtils.hasFont("Lucida Console")
                ? new FontSpec("Lucida Console", 0, 11)
                : new FontSpec(Font.MONOSPACED, 0, 13);
        FontSpec commandFont = new FontSpec(Font.SANS_SERIF, 0, 13);
        FontSpec plotLabelFont = new FontSpec(Font.SANS_SERIF, 0, 11);        
        FontSpec plotTickFont = new FontSpec(Font.SANS_SERIF, 0, 10); 
        FontSpec plotTitleFont =  new FontSpec(Font.SANS_SERIF, Font.BOLD, 13);
        FontSpec terminalFont = FontSpec.fromFont(Terminal.getDefaultFont());

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
            IO.setFilePermissions(file.toFile(), Context.getInstance().getConfig().filePermissionsConfig);
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
        if (gridColor == null) {
            return null;
        }
       return new Color(gridColor);
    }    
    
    @Transient
    public ScriptPopupDialog getScriptPopupDlg() {
        if (scriptPopupDialog == null) {
            return ScriptPopupDialog.None;
        }
        return scriptPopupDialog;
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
    
}
