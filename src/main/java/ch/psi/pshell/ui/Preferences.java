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
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity class holder user Workbench displaying preferences.
 */
public class Preferences {

    static final long serialVersionUID = 1;

    static final String PREFERENCES_FILENAME = "Preferences.dat";
    
    static final PanelLocation DEFAULT_CONSOLE_LOCATION = PanelLocation.Document;

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

    public Font fontShellPanel;
    public Font fontShellCommand;
    public Font fontOutput;
    public Font fontEditor;
    public Font fontPlotLabel;
    public Font fontPlotTick;
    public Font fontPlotTitle;
    public Float terminalFontSize=10.0f;
    public int tabSize = 4;
    public int contentWidth;
    public Color editorBackground;
    public Color editorForeground;
    public boolean simpleEditor;
    public boolean hideEditorLineNumbers;
    public boolean hideEditorContextMenu;

    public PanelLocation consoleLocation;
    public boolean noVariableEvaluationPropagation;
    public String[] processingScripts;

    public boolean asyncViewersUpdate;
    public boolean scanPlotDisabled;
    public boolean scanTableDisabled;
    public boolean cachedDataPanel;
    public boolean hideFileName;
    public boolean showEmergencyStop;
    public boolean showHomingButtons;
    public boolean showJogButtons;
    public boolean showXScanDataViewer;
    public boolean showXScanFileBrowser;
    
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
    public Color plotBackground = null; //Default
    public Color gridColor = null;  //Default
    public Color outlineColor = null; //Default
    public boolean disableOffscreenBuffer;

    public DefaultPanel[] defaultPanels;

    public ScriptPopupDialog scriptPopupDialog = ScriptPopupDialog.None;

    Path file;

    public static Preferences load(String path) {
        Preferences preferences = new Preferences();
        //Defaults
        Font[] fonts = getDefaultFonts();
        preferences.fontShellPanel = fonts[0];
        preferences.fontEditor = fonts[1];
        preferences.fontOutput = fonts[2];
        preferences.fontShellCommand = fonts[3];
        preferences.fontPlotLabel = fonts[4];
        preferences.fontPlotTick = fonts[5];
        preferences.fontPlotTitle= fonts[6];
        preferences.processingScripts = new String[0];
        preferences.defaultPanels = getDefaultPanels();
        preferences.consoleLocation = DEFAULT_CONSOLE_LOCATION;
        //preferences.propagateVariableEvaluation = true;
        preferences.defaultPlotColormap = Colormap.Temperature;
        preferences.defaultRendererColormap = Colormap.Grayscale;
        Path file = Paths.get(path, PREFERENCES_FILENAME);
        preferences.file = file;

        try {
            HashMap<String, Object> map = (HashMap) Serializer.decode(Files.readAllBytes(file));
            for (String name : map.keySet()) {
                try {
                    Field f = Preferences.class.getField(name);
                    f.set(preferences, map.get(name));
                } catch (Exception ex) {
                    Logger.getLogger(Preferences.class.getName()).log(Level.FINE, null, ex);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.FINE, null, ex);
        }
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

    public static Font[] getDefaultFonts() {
        Font editorFont = SwingUtils.hasFont("Lucida Console")
                ? new Font("Lucida Console", 0, 11)
                : new Font(Font.MONOSPACED, 0, 13);
        Font commandFont = new Font(Font.SANS_SERIF, 0, 13);
        Font plotLabelFont = new Font(Font.SANS_SERIF, 0, 11);        
        Font plotTickFont = new Font(Font.SANS_SERIF, 0, 10); 
        Font plotTitleFont =  new Font(Font.SANS_SERIF, Font.BOLD, 13);

        return new Font[]{
            editorFont,
            editorFont,
            editorFont,
            commandFont,
            plotLabelFont,
            plotTickFont,
            plotTitleFont
        };
    }

    public void save() {
        try {
            HashMap<String, Object> map = new HashMap<>();
            for (Field f : Preferences.class.getFields()) {
                if ((Modifier.isPublic(f.getModifiers()))
                        && (!Modifier.isFinal(f.getModifiers()))
                        && (!Modifier.isStatic(f.getModifiers()))) {
                    try {
                        map.put(f.getName(), f.get(this));
                    } catch (Exception ex) {
                        Logger.getLogger(Preferences.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
            }
            Files.write(file, Serializer.encode(map, Serializer.EncoderType.bin));
            IO.setFilePermissions(file.toFile(), Context.getInstance().getConfig().filePermissionsConfig);
        } catch (Exception ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    public Color getEditorBackground() {
        if (editorBackground == null) {
            return CodeEditor.TEXT_BACKGROUND_COLOR;
        }
        return editorBackground;
    }

    public Color getEditorForeground() {
        if (editorForeground == null) {
            return CodeEditor.TEXT_FOREGROUND_COLOR;
        }
        return editorForeground;
    }

    public ScriptPopupDialog getScriptPopupDialog() {
        if (scriptPopupDialog == null) {
            return ScriptPopupDialog.None;
        }
        return scriptPopupDialog;
    }
}
