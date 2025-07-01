package ch.psi.pshell.swing;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;

/**
 *
 */
public class Led extends JLabel {

    int size;

    public Led() {
        super("•");
        setLedSize(15);
    }

    public void setColor(Color color) {
        setForeground(color);
    }

    public Color getColor() {
        return getForeground();
    }

    public int getLedSize() {
        return size;
    }

    public void setLedSize(int size) {
        this.size = size;
        Font f = new Font(Font.SANS_SERIF, Font.PLAIN, size);
        setFont(f);
    }
}
