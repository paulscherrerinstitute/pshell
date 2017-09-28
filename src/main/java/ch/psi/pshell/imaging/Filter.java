package ch.psi.pshell.imaging;

import ch.psi.utils.Chrono;
import ch.psi.utils.State;
import java.awt.image.BufferedImage;

/**
 *
 */
abstract public class Filter extends SourceBase implements ImageListener {

    public Filter(String name) {
        super(name, null);
        setState(State.Ready);
    }

    public Filter() {
        this(null);
    }

    boolean passive;
    volatile Chrono chrono;

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public boolean isPassive() {
        return passive;
    }

    BufferedImage image;
    Data data;

    @Override
    public void onImage(Object origin, BufferedImage image, Data data) {
        if (passive) {
            this.image = image;
            this.data = data;
            chrono = new Chrono();
        } else {
            execute(image, data);
        }
    }

    @Override
    public void refresh() {
        if (passive) {
            if ((image != null) || (data != null)) {
                //Don't repeat  pushed images
                if ((getAge() == null) || (chrono.getEllapsed() < getAge())) {
                    execute(image, data);
                }
            }
        } else {
            super.refresh();
        }
    }

    void execute(BufferedImage image, Data data) {
        data = processData(data);
        image = process(image, data);
        pushImage(image, data);
    }

    @Override
    public void onError(Object origin, Exception ex) {
        pushError(ex);
    }

    protected Data processData(Data data) {
        return data;
    }

    abstract public BufferedImage process(BufferedImage image, Data data);
}
