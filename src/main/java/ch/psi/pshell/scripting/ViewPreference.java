package ch.psi.pshell.scripting;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.plotter.PlotLayout;
import ch.psi.utils.Range;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hints to graphical layer
 */
public enum ViewPreference {

    PLOT_DISABLED, //enable/disable scan plot (True/False)
    PLOT_LAYOUT, //  "Horizontal", "Vertical" or "Grid"
    TABLE_DISABLED, //enable/disable scan table (True/False)
    ENABLED_PLOTS, //select Readables to be plotted (list of Readable or String (Readable names))
    PLOT_TYPES, //dictionary or (Readable or String):(String or int) pairs where the key is a plot name and the value is the desired plot type
    PRINT_SCAN, //print scan records to console    
    AUTO_RANGE, //Automatic range scan plots x-axis
    MANUAL_RANGE, //Manually set scan plots x-axis
    MANUAL_RANGE_Y, //Manually set scan plots y-axis
    DOMAIN_AXIS, //set the domain axis source: "Time", "Index", or a readable name. Default(None): first positioner
    DEFAULTS, //Restore defaults preferences
    STATUS; //set application status        
    
    static public String DOMAIN_AXIS_TIME = "Time";
    static public String DOMAIN_AXIS_INDEX = "Index";

    static public class PlotPreferences {

        public List<String> enabledPlots; //If null then plot all readables
        public Map<String, Object> plotTypes;
        public Boolean autoRange;
        public Range range;
        public Range rangeY;
        public Integer steps;
        public String domainAxis;
        public PlotLayout plotLayout;

        public void init() {
            enabledPlots = null;
            plotTypes = null;
            autoRange = null;
            range = null;
            rangeY = null;
            domainAxis = null;
            plotLayout = null;
        }

        public void setFixedRange() {
            autoRange = null;
            range = null;
        }

        public void setManualRange(Object[] range) {
            if (range==null){
                setFixedRange();
            } else {
                if (!(Double.isNaN(((Number) range[0]).doubleValue())) && !(Double.isNaN(((Number) range[1]).doubleValue()))){
                    double min = Math.min(((Number) range[0]).doubleValue(), ((Number) range[1]).doubleValue());
                    double max = Math.max(((Number) range[0]).doubleValue(), ((Number) range[1]).doubleValue());
                    autoRange = null;
                    this.range = new Range(min, max);
                }
            }
        }

        public void setAutoRange(boolean value) {
            autoRange = value;
            range = null;
        }
        
        public void setFixedRangeY() {
            rangeY = null;
        }

        public void setManualRangeY(Object[] range) {
            if (range==null){
                setFixedRangeY();
            } else {            
                if (!(Double.isNaN(((Number) range[0]).doubleValue())) && !(Double.isNaN(((Number) range[1]).doubleValue()))){
                    double min = Math.min(((Number) range[0]).doubleValue(), ((Number) range[1]).doubleValue());
                    double max = Math.max(((Number) range[0]).doubleValue(), ((Number) range[1]).doubleValue());
                    this.rangeY = new Range(min, max);
                }
            }
        }

        public void resetPlotTypes() {
            plotTypes = null;
        }

        public void setPlotTypes(Map types) {
            plotTypes = new HashMap<>();
            for (Object plot : types.keySet()) {
                Object type = types.get(plot);

                if (plot instanceof Nameable) {
                    plot =  ((Nameable) plot).getAlias();
                } else {
                    plot = (String.valueOf(plot));
                }
                plotTypes.put((String) plot, type);
            }
        }

        public void setDomainAxis(String source) {
            domainAxis = source;
        }

        public void setEnabledPlots(ArrayList<String> plots) {
            enabledPlots = plots;
        }
        
        public void setPlotLayout(PlotLayout layout){
            plotLayout = layout;
        }

        public PlotPreferences clone() {
            PlotPreferences ret = new PlotPreferences();
            ret.autoRange = autoRange;
            ret.domainAxis = domainAxis;
            ret.enabledPlots = enabledPlots;
            ret.plotTypes = plotTypes;
            ret.range = range;
            ret.rangeY = rangeY;
            ret.plotLayout = plotLayout;
            return ret;
        }
    }

}
