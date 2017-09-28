package ch.psi.pshell.plot;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class PlotSeries<T extends Plot> implements Serializable {

    private static final long serialVersionUID = 1L;

    final AtomicBoolean updating;
    volatile int id;

    protected PlotSeries() {
        this("");
    }

    protected PlotSeries(String name) {
        setName((name == null) ? "" : name);
        updating = new AtomicBoolean(false);
    }

    transient Object token;

    void setToken(Object token) {
        this.token = token;
    }

    public Object getToken() {
        return token;
    }

    transient private T plot;

    void setPlot(T plot) {
        this.plot = plot;
    }

    public T getPlot() {
        return plot;
    }

    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clear() {

    }
}
