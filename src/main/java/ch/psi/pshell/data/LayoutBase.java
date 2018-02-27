package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ExecutionParameters;
import static ch.psi.pshell.data.Layout.ATTR_END_TIMESTAMP;
import static ch.psi.pshell.data.Layout.ATTR_FILE;
import static ch.psi.pshell.data.Layout.ATTR_NAME;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_DOMAIN;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_ENABLE;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_RANGE;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_TYPES;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_TYPES_SEPARATOR;
import static ch.psi.pshell.data.Layout.ATTR_START_TIMESTAMP;
import static ch.psi.pshell.data.Layout.ATTR_VERSION;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Common layouts utilities
 */
public abstract class LayoutBase implements Layout {
     

    boolean persistSetpoints;

    public boolean getPersistSetpoints() {
        return persistSetpoints;
    }

    public void setPersistSetpoints(boolean value) {
        persistSetpoints = value;
    }
    
    
    @Override
    public void onOpened(File output) throws IOException {
        setLayoutAttribute();
        setNameAttribute();
        setScriptFileAttibute();
    } 
    
    @Override
    public void onClosed(File output) throws IOException {
         setScriptVersionAttibute(); //Doing on close because file is commited asynchronously on start of scan.
    }        

    //Set common attributes as expected by DataManager (can be ommited).
    protected void setStartTimestampAttibute(Scan scan) throws IOException {
        String scanPath = getDataManager().getScanPath(scan);
        if (scanPath != null) {
            getDataManager().setAttribute(scanPath, ATTR_START_TIMESTAMP, System.currentTimeMillis());
        }
    }

    protected void setEndTimestampAttibute(Scan scan) throws IOException {
        String scanPath = getDataManager().getScanPath(scan);
        if (scanPath != null) {
            getDataManager().setAttribute(scanPath, ATTR_END_TIMESTAMP, System.currentTimeMillis());
        }
    }

    protected void setPlotPreferencesAttibutes(Scan scan) throws IOException {
        String scanPath = getDataManager().getScanPath(scan);
        if (scanPath != null) {
            ViewPreference.PlotPreferences pp = Context.getInstance().getPlotPreferences();
            if (pp.enabledPlots != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_ENABLE, pp.enabledPlots.toArray(new String[0]));
            }
            if (pp.autoRange != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_RANGE, new double[]{Double.NaN, Double.NaN});
            }
            if (pp.range != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_RANGE, new double[]{pp.range.min, pp.range.max});
            }
            if (pp.domainAxis != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_DOMAIN, pp.domainAxis);
            }
            if (pp.plotTypes != null) {
                ArrayList<String> list = new ArrayList<>();
                for (String key : pp.plotTypes.keySet()) {
                    list.add(key + "=" + pp.plotTypes.get(key));
                }
                getDataManager().setAttribute(scanPath, ATTR_PLOT_TYPES, String.join(ATTR_PLOT_TYPES_SEPARATOR, list));
            }
        }
    }

    protected void setLayoutAttribute() throws IOException {
        getDataManager().setAttribute("/", ATTR_LAYOUT, getClass().getName());
    }    
    
    protected void setNameAttribute() throws IOException {
        String name = getDataManager().getExecutionPars().getName();
        getDataManager().setAttribute("/", ATTR_NAME, (name == null) ? "" : name);
    }            

    protected void setScriptFileAttibute() throws IOException {
        ExecutionParameters pars =  getDataManager().getExecutionPars();
        File file = pars.getScriptFile();
        if (file != null) {
            String fileName = file.getPath();
            getDataManager().setAttribute("/", ATTR_FILE, fileName);
        } else {
            String command = pars.getStatement();
            if (command!=null){
                getDataManager().setAttribute("/", ATTR_COMMAND, command);
            }
        }
    }

    protected void setScriptVersionAttibute() throws IOException {
        String version = getDataManager().getExecutionPars().getScriptVersion();
        if (version != null) {
            getDataManager().setAttribute("/", ATTR_VERSION, version);
        }
    }
}
