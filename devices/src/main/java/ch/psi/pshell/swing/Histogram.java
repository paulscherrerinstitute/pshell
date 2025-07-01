
package ch.psi.pshell.swing;

import ch.psi.pshell.imaging.Source;

/**
 *
 */
public class Histogram extends ch.psi.pshell.imaging.Histogram{
    Source source;
    
    public void setSource(Source source) {
        if (isVisible()) {
            onHide();
        }
        this.renderer = null;
        this.source = source;
        if (isVisible()) {
            onShow();
        }
        originalConfig = null;
    }
 
    @Override
    protected void onShow() {
        super.onShow();
        if (source != null) {
            source.addListener(this);
            onImage(null, source.getOutput(), source.getData());
        }
    }

    @Override
    protected void onHide() {
        if (source != null) {
            source.removeListener(this);
        }
        super.onHide();
    }
}
