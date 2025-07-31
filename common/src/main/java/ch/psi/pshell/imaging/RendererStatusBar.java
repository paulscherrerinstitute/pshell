package ch.psi.pshell.imaging;

import ch.psi.pshell.swing.MonitoredPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 */
public class RendererStatusBar extends MonitoredPanel {

    double frameRate;
    Timer timerFrameRate;
    final ImageRenderer renderer;
    final JPopupMenu popupMenuUnits;

    /**
     */
    public RendererStatusBar(ImageRenderer renderer) {
        initComponents();
        this.renderer = renderer;
        //Font font = SwingUtils.hasFont("Lucida Console")? new Font("Lucida Console", 0, 11) : new Font(Font.MONOSPACED, 0, 11));
        Font font = labelX.getFont().deriveFont(Math.min(11.0f, labelX.getFont().getSize()));
        labelX.setFont(font);
        labelY.setFont(font);
        labelZ.setFont(font);
        labelDim.setFont(font);
        labelType.setFont(font);
        labelFps.setFont(font);
        popupMenuUnits = new JPopupMenu();
        JRadioButtonMenuItem menuPixels = new JRadioButtonMenuItem(Units.Pixels.toString());
        JRadioButtonMenuItem menuUnits = new JRadioButtonMenuItem(Units.Units.toString());
        popupMenuUnits.add(menuPixels);
        popupMenuUnits.add(menuUnits);
        menuPixels.addActionListener((ev) -> {
            setUnits(Units.Pixels);
        });
        menuUnits.addActionListener((ev) -> {
            setUnits(Units.Units);
        });

        popupMenuUnits.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menuPixels.setSelected(units == Units.Pixels);
                menuUnits.setSelected(!menuPixels.isSelected());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        popupMenuUnits.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

        };
        labelX.addMouseListener(mouseAdapter);
        labelY.addMouseListener(mouseAdapter);
    }

    public enum Units {
        Pixels,
        Units
    }

    Units units = Units.Units;

    public void setUnits(Units units) {
        this.units = units;
    }

    public Units getUnits() {
        return units;
    }

    boolean showFrameRate = true;

    public void setShowFrameRate(boolean value) {
        showFrameRate = value;
        labelFps.setVisible(value);
    }

    public boolean getShowFrameRate() {
        return showFrameRate;
    }

    void update() {
        updateInfo();
        updatePosition();
        updateFrameRate();
    }

    void updateInfo() {
        if (isVisible()) {
            Data data = renderer.getData();
            String dimStr = " ", typeStr = " ";
            if (data != null) {
                Dimension size = data.getSize(true);
                dimStr = String.format("%dx%dx%d", size.width, size.height, data.getDepth());
                typeStr = String.format("%s%s", data.isUnsigned() ? "unsigned " : "", data.getType().getSimpleName());
            }
            labelDim.setText(dimStr);
            labelType.setText(typeStr);
        }
    }

    void updatePosition() {
        if (isVisible()) {
            Data data = renderer.getData();
            String xStr = " ", yStr = " ", zStr = " ";
            if (data != null) {
                Point location = renderer.currentMouseLocation;
                int x = (location == null) ? 0 : location.x;
                int y = (location == null) ? 0 : location.y;
                Dimension size = data.getSize(true);
                x /= renderer.getScaleX();
                y /= renderer.getScaleY();
                x = Math.min(x, size.width - 1);
                y = Math.min(y, size.height - 1);
                if ((x >= 0) && (y >= 0)) {
                    xStr = "x=" + data.getXStr(x, (getUnits() == Units.Pixels) ? null : renderer.getCalibration());
                    yStr = "y=" + data.getYStr(y, (getUnits() == Units.Pixels) ? null : renderer.getCalibration());
                    zStr = "z=" + data.getElementStr(y, x, true);
                }
            }
            labelX.setText(xStr);
            labelY.setText(yStr);
            labelZ.setText(zStr);
        }
    }

    void updateFrameRate() {
        if (isVisible()) {
            String fpsStr = " ";
            if ((renderer.getData() != null) && showFrameRate) {
                fpsStr = showFrameRate ? String.format("%1.2ffps", frameRate) : "";
            }
            labelFps.setText(fpsStr);
        }
    }

    @Override
    protected void onHide() {
        if (timerFrameRate != null) {
            timerFrameRate.stop();
            timerFrameRate = null;
        }
    }

    @Override
    protected void onShow() {
        if (showFrameRate) {
            timerFrameRate = new Timer(1000, (ActionEvent e) -> {
                frameRate = renderer.getFrameRate();
                updateFrameRate();
            });
            timerFrameRate.setInitialDelay(0);
            timerFrameRate.start();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelX = new javax.swing.JLabel();
        labelY = new javax.swing.JLabel();
        labelZ = new javax.swing.JLabel();
        labelDim = new javax.swing.JLabel();
        labelFps = new javax.swing.JLabel();
        labelType = new javax.swing.JLabel();

        labelX.setText("x=");

        labelY.setText("y=");

        labelZ.setText("z=");
        labelZ.setMinimumSize(new java.awt.Dimension(85, 16));
        labelZ.setPreferredSize(new java.awt.Dimension(85, 16));

        labelDim.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelDim.setText("0x0x0");

        labelFps.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelFps.setText("0.00fps");

        labelType.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelType.setText("unsigned byte");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(labelX, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(labelY, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(labelZ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelDim, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelType, javax.swing.GroupLayout.PREFERRED_SIZE, 80, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelFps, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(labelX)
                .addComponent(labelY)
                .addComponent(labelZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelDim)
                .addComponent(labelFps)
                .addComponent(labelType))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel labelDim;
    private javax.swing.JLabel labelFps;
    private javax.swing.JLabel labelType;
    private javax.swing.JLabel labelX;
    private javax.swing.JLabel labelY;
    private javax.swing.JLabel labelZ;
    // End of variables declaration//GEN-END:variables
}
