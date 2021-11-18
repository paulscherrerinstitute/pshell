package ch.psi.pshell.core;

import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.plot.Plot;
import java.util.Arrays;
import java.util.List;

/**
 * The listener interface for receiving plot events.
 */
public interface PlotListener {
    final public static String DEFAULT_PLOT_TITLE = "Default";

    List<Plot> plot(String title, PlotDescriptor[] plots) throws Exception;

    List<Plot> getPlots(String title);
    
    default List<String> getTitles(){
        return Arrays.asList(new String[]{DEFAULT_PLOT_TITLE});
    }
    
    default void onTitleClosed(String title){}
}
