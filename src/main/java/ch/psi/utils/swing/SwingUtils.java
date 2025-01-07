package ch.psi.utils.swing;

import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Sys;
import ch.psi.utils.Sys.OSFamily;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Generic Swing utilities.
 */
public class SwingUtils {

    public static void invokeDelayed(final Runnable runnable, final int delayMillis) {
        Timer timer=new Timer(delayMillis, null);
        timer.addActionListener((ActionEvent ae) -> {
            timer.stop();            
            try{
                runnable.run();
            } catch (Exception ex){                
            }
        });
        timer.start();       
    }
    
    public static <T> T invokeAndWait(final Callable<T> callable) throws Exception {   
        if (!SwingUtilities.isEventDispatchThread()){
            class CallReturn {
                T ret;
                Exception ex;
            }                    
            CallReturn ret = new CallReturn();
            SwingUtilities.invokeAndWait(()->{
                try {
                    ret.ret = callable.call();
                } catch (Exception ex) {
                    ret.ex=ex;
                }
            });
            if (ret.ex!=null){
                throw ret.ex;
            }    
            return ret.ret;
        }
        return callable.call();
    }

    /**
     * If parent is null then center in the screen
     */
    public static void centerComponent(Component parent, Component component) {
        if ((Sys.getOSFamily() == OSFamily.Mac) && (component instanceof Window)) {
            ((Window) component).setLocationRelativeTo(parent);
        } else {
            if (parent == null) {
                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                component.setLocation(dim.width / 2 - component.getSize().width / 2, dim.height / 2 - component.getSize().height / 2);
            } else {
                Rectangle r = parent.getBounds();
                Dimension w = component.getSize();
                int x = Math.max(r.x + (r.width - w.width) / 2, 4);
                int y = Math.max(r.y + (r.height - w.height) / 2, 4);
                component.setLocation(x, y);
            }
        }
    }

    public static BufferedImage createImage(Component panel) {
        if (panel == null) {
            return null;
        }
        Dimension size = panel.getSize();
        BufferedImage image = new BufferedImage(
                size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        return image;
    }

    public static boolean hasFont(String name) {
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String font : g.getAvailableFontFamilyNames()) {
            if (font.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static Dimension getTextSize(String text, FontMetrics fontMetrics) {
        int height = fontMetrics.getFont().getSize();
        int width = SwingUtilities.computeStringWidth(fontMetrics, text);
        return new Dimension(width, height);
    }

    static public Frame getFrame(Component c) {
        if (c!=null) {
            while (c.getParent() != null) {
                c = c.getParent();
            }

            while ((c != null) && (c instanceof Window)) {
                if (c instanceof Frame) {
                    return (Frame) c;
                } else if (c instanceof Dialog) {
                    c = (((Dialog) c).getOwner());
                } else {
                    break;
                }
            }
        }
        return null;
    }

    static public Window getWindow(Component c) {
        if (c!=null) {
            while (c.getParent() != null) {
                c = c.getParent();
                if (c instanceof Window) {
                    return (Window) c;
                }
            }
        }
        return null;
    }

    public static Window[] getVisibleWindows() {
        ArrayList<Window> windows = new ArrayList<>();
        for (Window w : Window.getWindows()) {
            if ((w.isVisible())
                    && ((w instanceof JFrame) || (w instanceof JDialog) || (w instanceof JWindow))) {
                windows.add(w);
            }
        }
        return windows.toArray(new Window[0]);
    }

    public static boolean isResizable(Window window) {
        boolean resizable = true;
        if (window instanceof Frame) {
            resizable = ((Frame) window).isResizable();
        } else if (window instanceof Dialog) {
            resizable = ((Dialog) window).isResizable();
        }
        return resizable;
    }

    public static void setMacScreenMenuBar(String title) {
        if (Sys.getOSFamily() == OSFamily.Mac) {
            if (title != null) {
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", title);
            }
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("com.apple.mrj.application.live-resize", "true");
        }
    }

    public static void adjustMacMenuBarAccelerator(JMenuItem item) {
        if (Sys.getOSFamily() == OSFamily.Mac) {
            if ((item.getAccelerator() != null) && (item.getAccelerator().getModifiers() & InputEvent.CTRL_MASK) != 0) {
                int modifiers = (item.getAccelerator().getModifiers() & ~InputEvent.CTRL_MASK & ~InputEvent.CTRL_DOWN_MASK) | InputEvent.META_MASK;
                item.setAccelerator(KeyStroke.getKeyStroke(item.getAccelerator().getKeyCode(), modifiers));
            }
        }
    }        
        
    public static void adjustMacMenuBarAccelerators(JMenuBar menuBar) {
        if (Sys.getOSFamily() == OSFamily.Mac) {
            for (Component c : SwingUtils.getComponentsByType(menuBar, JMenuItem.class)) {
                adjustMacMenuBarAccelerator((JMenuItem) c);
            }
        }
    }

    public static boolean isFullScreen(Frame f) {
        return (f.getExtendedState() == JFrame.MAXIMIZED_BOTH) && f.isUndecorated();
    }

    public static void setFullScreen(final Frame f, final boolean value) {
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (value != isFullScreen(f)) {
            boolean displayable = f.isDisplayable();
            if (value) {
                if (displayable) {
                    f.dispose();
                }
                f.setUndecorated(true);
                f.setExtendedState(JFrame.MAXIMIZED_BOTH);

            } else { // back to windowed mode
                if (displayable) {
                    f.dispose();
                }
                f.setUndecorated(false);
                f.setExtendedState(JFrame.NORMAL);
                //gd.setFullScreenWindow(null);
            }
            if (displayable) {
                f.setVisible(true);
                f.invalidate();
            }
        }
    }

    public static void enableFullScreen(Window window) {
        try {
            switch (Sys.getOSFamily()) {
                case Mac:
                    //On Java>=11 it is windows can be full screen by default 
                    if (Sys.getJavaVersion() < 11.0){
                        Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
                        Class params[] = new Class[]{Window.class, Boolean.TYPE};
                        Method method = util.getMethod("setWindowCanFullScreen", params);
                        method.invoke(util, window, true);
                    }
                    break;
            }
        } catch (ClassNotFoundException e1) {
        } catch (Exception ex) {
            Logger.getLogger(SwingUtils.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    public static boolean isMaximized(Frame frame) {
        try {
            return (frame.getExtendedState() == Frame.MAXIMIZED_BOTH);
        } catch (Exception ex) {
        }
        return false;
    }

    public static Component getComponentByName(Container parent, String name) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (name.equals(parent.getComponent(i).getName())) {
                return parent.getComponent(i);
            }
        }
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component subcomponent = parent.getComponent(i);
            if (subcomponent instanceof Container) {
                Component c = getComponentByName((Container) subcomponent, name);
                if (c != null) {
                    return c;
                }
            }
        }
        if (parent instanceof JMenu) {
            JMenu menu = (JMenu) parent;
            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem item = menu.getItem(i);
                if ((item != null) && (name.equals(item.getName()))) {
                    return item;
                }
            }
            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem item = menu.getItem(i);
                if ((item != null) && (item instanceof JMenu)) {
                    Component c = getComponentByName((JMenu) item, name);
                    if (c != null) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    public static int getComponentIndex(Container parent, Component component) {
        if ((component == null) || (parent == null)) {
            return -1;
        }
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == component) {
                return i;
            }
        }
        if (parent instanceof JMenu) {
            JMenu menu = (JMenu) parent;
            for (int i = 0; i < menu.getItemCount(); i++) {
                if (menu.getItem(i) == component) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static boolean containsComponent(Container parent, Component component) {
        return (getComponentIndex(parent, component) >= 0);
    }

    public static Component[] getComponentsByType(Container parent, Class type) {
        boolean is_menu = (parent instanceof JMenu);
        int componentCount = is_menu ? ((JMenu) parent).getMenuComponentCount() : parent.getComponentCount();
        ArrayList<Component> ret = new ArrayList<>();
        for (int i = 0; i < componentCount; i++) {
            Component component = is_menu ? ((JMenu) parent).getMenuComponent(i) : parent.getComponent(i);
            if (type.isAssignableFrom(component.getClass())) {
                ret.add(component);
            }
        }

        for (int i = 0; i < componentCount; i++) {
            Component component = is_menu ? ((JMenu) parent).getMenuComponent(i) : parent.getComponent(i);
            if (component instanceof Container) {
                ret.addAll(Arrays.asList(getComponentsByType((Container) component, type)));
            }
        }
        return ret.toArray(new Component[0]);
    }

    public static void updateAllFrames() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                updateAllFrames();
            });
            return;
        }
        for (Window window : JFrame.getWindows()) {
            try {
                SwingUtilities.updateComponentTreeUI(window);
            } catch (Exception ex) {
            }
            window.validate();
        }
    }

    public static void showMessageBlocking(final Component parent, final String title, final String msg) {
        showMessageBlocking(parent, title, msg, -1);
    }

    public static void showMessageBlocking(final Component parent, final String title, final String msg, int autoCloseTimer) {
        showMessageBlocking(parent, title, msg, autoCloseTimer, Integer.MIN_VALUE);
    }

    public static void showMessageBlocking(final Component parent, final String title, final String msg, int autoCloseTimer, int messageType) {
        if (autoCloseTimer > 0) {
            final JOptionPane pane = new JOptionPane(msg, JOptionPane.INFORMATION_MESSAGE);
            final JDialog dialog = pane.createDialog(parent, (title == null) ? "Message" : title);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            Timer timer = new Timer(autoCloseTimer, (ActionEvent e) -> {
                dialog.setVisible(false);
                ((Timer) e.getSource()).stop();
            });
            timer.start();
            dialog.setVisible(true);
        } else {
            if (messageType == Integer.MIN_VALUE) {
                messageType = JOptionPane.INFORMATION_MESSAGE;
            }
            JOptionPane.showMessageDialog(parent, msg, (title == null) ? "Message" : title, messageType, null);
        }
    }

    /**
     * Safe to call from any thread
     */
    public static void showMessage(final Component parent, final String title, final String msg) {
        showMessage(parent, title, msg, -1);
    }

    public static void showMessage(final Component parent, final String title, final String msg, final int autoCloseTimer) {
        showMessage(parent, title, msg, autoCloseTimer, Integer.MIN_VALUE);
    }

    public static void showMessage(final Component parent, final String title, final String msg, final int autoCloseTimer, int messageType) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                showMessageBlocking(parent, title, msg, autoCloseTimer, messageType);
            });
            return;
        }
        showMessageBlocking(parent, title, msg, autoCloseTimer, messageType);
    }

    public static void showExceptionBlocking(final Component parent, final Exception ex) {
        showExceptionBlocking(parent, ex, null);
    }

    public static void showExceptionBlocking(final Component parent, final Exception ex, final String title) {
        //JOptionPane.showMessageDialog(parent, message, (title == null) ? "Exception" : title, JOptionPane.WARNING_MESSAGE, null);

        int max_width = 1000;
        int max_msg_height = 300;
        int min_width = 400;
        String message = String.valueOf(ExceptionUtils.getMessage(ex)).trim();
        if (message.endsWith(":")) {
            message = message.substring(0, message.length() - 1);
        }
        BorderLayout layout = new BorderLayout();
        layout.setVgap(6);
        JPanel panel = new JPanel(layout);
        Border padding = BorderFactory.createEmptyBorder(8, 0, 2, 0);
        panel.setBorder(padding);
        JTextArea textMessage = new JTextArea(message);
        textMessage.setEnabled(false);
        textMessage.setFont(new JLabel().getFont().deriveFont(Font.BOLD));
        textMessage.setLineWrap(false);
        textMessage.setBorder(null);
        textMessage.setBackground(null);
        textMessage.setDisabledTextColor(textMessage.getForeground());
        textMessage.setAutoscrolls(true);

        if (textMessage.getPreferredSize().width > max_width) {
            textMessage.setPreferredSize(new Dimension(max_width, textMessage.getPreferredSize().height));
        } else if (textMessage.getPreferredSize().width < min_width) {
            textMessage.setPreferredSize(new Dimension(min_width, textMessage.getPreferredSize().height));
        }
        if (textMessage.getPreferredSize().height > max_msg_height) {
            textMessage.setPreferredSize(new Dimension(textMessage.getPreferredSize().width, max_msg_height));
        }
        panel.add(textMessage, BorderLayout.NORTH);

        JScrollPane scrollDetails = new javax.swing.JScrollPane();
        JButton button = new JButton("Details");
        button.setFont(button.getFont().deriveFont(Font.PLAIN));
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(button, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.remove(buttonPanel);
                panel.add(scrollDetails, BorderLayout.SOUTH);
                //scrollDetails.setSize(250, scrollDetails.getHeight());
                if (scrollDetails.getPreferredSize().width > max_width) {
                    scrollDetails.setPreferredSize(new Dimension(max_width, scrollDetails.getPreferredSize().height));
                }
                ((JDialog) panel.getTopLevelAncestor()).pack();
            }
        });

        JTextArea textDetails = new JTextArea(ExceptionUtils.getStackTrace(ex));
        textDetails.setEditable(false);
        textDetails.setLineWrap(false);
        textDetails.setRows(12);
        scrollDetails.setViewportView(textDetails);
        scrollDetails.setAutoscrolls(true);

        JOptionPane.showMessageDialog(parent, panel, (title == null) ? "Exception" : title, JOptionPane.WARNING_MESSAGE, null);
    }

    /**
     * Safe to call from any thread
     */
    public static void showException(final Component parent, final Exception ex) {
        showException(parent, ex, null);
    }

    public static void showException(final Component parent, final Exception ex, final String title) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                showExceptionBlocking(parent, ex, title);
            });
            return;
        }
        showExceptionBlocking(parent, ex, title);
    }

    public static void showScrollableMessageBlocking(final Component parent, final String title, final String description, final String message) {
        BorderLayout layout = new BorderLayout();
        layout.setVgap(8);        
        JPanel panel = new JPanel(layout);        
        panel.setPreferredSize(new Dimension(600,300));
        Border padding = BorderFactory.createEmptyBorder(8, 8, 8, 8);
        panel.setBorder(padding);
        if (description!=null){
            JTextArea textDescription = new JTextArea(description);
            textDescription.setEnabled(false);
            textDescription.setFont(new JLabel().getFont().deriveFont(Font.BOLD));
            textDescription.setLineWrap(false);
            textDescription.setBorder(null);
            textDescription.setBackground(null);
            textDescription.setDisabledTextColor(textDescription.getForeground());
            textDescription.setAutoscrolls(true);
            panel.add(textDescription, BorderLayout.NORTH);
        }
        if (message!=null){
            JScrollPane scrollDetails = new javax.swing.JScrollPane();
            JTextArea textMessage = new JTextArea(message);
            textMessage.setEditable(false);
            textMessage.setLineWrap(false);
            textMessage.setRows(12);
            scrollDetails.setViewportView(textMessage);
            scrollDetails.setAutoscrolls(true);                        
            panel.add(scrollDetails, BorderLayout.CENTER);
        }
        JOptionPane.showMessageDialog(parent, panel, (title == null) ? "Message" : title, JOptionPane.WARNING_MESSAGE, null);
        //SwingUtils.showDialog(SwingUtils.getWindow(parent), (title == null) ? "Message" : title, new Dimension(600,400), panel);
    }

    /**
     * Safe to call from any thread
     */
    public static void showScrollableMessage(final Component parent, final String title, final String description, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                showScrollableMessageBlocking(parent, title, description, message);
            });
            return;
        }
        showScrollableMessageBlocking(parent, title, description, message);
    }
        
    public static JDialog showSplash(final Component parent, String title,  Dimension size, String message){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel(message);        
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);        
        return showSplash(parent, title, size,panel);
    }    
    
    public static JDialog showSplash(final Component parent, String title,  Dimension size, JPanel panel){
        JDialog dialogMessage = SwingUtils.showDialog(parent, title, size, panel);
        dialogMessage.setAlwaysOnTop(true);
        panel.paintImmediately(0,0,panel.getWidth(),panel.getHeight());
        return dialogMessage;
    }    
    
    public enum OptionType {
        
        Default,
        YesNo,
        YesNoCancel,
        OkCancel;

        int toJOptionPaneType() {
            switch (this) {
                case YesNo:
                    return JOptionPane.YES_NO_OPTION;
                case YesNoCancel:
                    return JOptionPane.YES_NO_CANCEL_OPTION;
                case OkCancel:
                    return JOptionPane.OK_CANCEL_OPTION;
                default:
                    return JOptionPane.DEFAULT_OPTION;
            }
        }
    }

    public enum OptionResult {

        Yes,
        No,
        Cancel,
        Closed;

        static OptionResult fromJOptionPaneResult(int result) {
            switch (result) {
                case JOptionPane.YES_OPTION:
                    return OptionResult.Yes;   //Equals OK
                case JOptionPane.NO_OPTION:
                    return OptionResult.No;
                case JOptionPane.CANCEL_OPTION:
                    return OptionResult.Cancel;
                default:
                    return OptionResult.Closed;
            }
        }
    }

    public static OptionResult showOption(final Component parent, final String title, final String msg, OptionType type) {
        int ret = JOptionPane.showOptionDialog(parent, msg, (title == null) ? "Input" : title, type.toJOptionPaneType(), JOptionPane.QUESTION_MESSAGE, null, null, null);
        return OptionResult.fromJOptionPaneResult(ret);
    }

    public static int showOption(final Component parent, final String title, final String msg, Object[] options, Object initialValue) {
        return JOptionPane.showOptionDialog(parent, msg, (title == null) ? "Input" : title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, initialValue);        
    }

    public static OptionResult showOption(final Component parent, final String title, final Component component, OptionType type) {
        int ret = JOptionPane.showOptionDialog(parent, component, (title == null) ? "Input" : title, type.toJOptionPaneType(), JOptionPane.QUESTION_MESSAGE, null, null, null);
        return OptionResult.fromJOptionPaneResult(ret);
    }

    public static int showOption(final Component parent, final String title, final Component component, Object[] options, Object initialValue) {
        return JOptionPane.showOptionDialog(parent, component, (title == null) ? "Input" : title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, initialValue);    
    }

    public static String getString(final Component parent, final String msg, final Object current) {
        return JOptionPane.showInputDialog(parent, msg, (current == null) ? "" : String.valueOf(current));
    }

    public static String getString(final Component parent, final String msg, final String[] options, final String current) {
        JComboBox comboBox = new JComboBox();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (String option : options) {
            if (option != null) {
                model.addElement(option);
            }
        }
        if (current != null) {
            model.setSelectedItem(current);
        }
        comboBox.setModel(model);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 4));
        panel.add(new JLabel(msg), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.CENTER);
        int ret = JOptionPane.showOptionDialog(parent, panel, "Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (ret == JOptionPane.OK_OPTION) {
            if (comboBox.getItemCount() > 0) {
                return (String) comboBox.getSelectedItem();
            }
        }
        return null;
    }

    /**
     * Requests the focus of a JComponent at the moment it is shown. Useful to
     * set focus of components used in JOptionPane.
     */
    public static void requestFocusDeferred(JComponent c) {
        c.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent event) {
                final JComponent component = event.getComponent();
                component.removeAncestorListener(this);
                //The combination wait in theread wait + invoke delayed was added because on Linux
                //JOptionPane sets focus on buttons after this ancestorAdded was called
                new Thread(() -> {
                    Chrono chrono = new Chrono();
                    try {
                        chrono.waitCondition(() -> component.isShowing(), 10000);
                    } catch (Exception ex) {
                    }
                    invokeDelayed(() -> {
                        component.requestFocusInWindow();
                    }, 100);
                }).start();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    public static String getPassword(final Component parent, final String title, final String msg) {
        JPanel panel = new JPanel();
        JPasswordField password = new JPasswordField(18);
        panel.add(new JLabel(msg));
        panel.add(password);
        requestFocusDeferred(password);
        int ret = JOptionPane.showOptionDialog(parent, panel, (title == null) ? "Input" : title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (ret == JOptionPane.OK_OPTION) {
            return new String(password.getPassword());
        }
        return null;
    }

    public static Object getEnum(final Component parent, final String msg, final Class type, final Object current) {
        if ((!type.isEnum()) || (current == null)) {
            return null;
        }
        Object[] enumVals = type.getEnumConstants();
        String[] enumStrs = new String[enumVals.length];

        for (int i = 0; i < enumVals.length; i++) {
            enumStrs[i] = enumVals[i].toString();
        }
        String ret = getString(parent, msg, enumStrs, current.toString());
        if (ret == null) {
            return null;
        }
        return Enum.valueOf(type, ret);
    }

    //Automates dialog& frame creation for displaying a component
    public static JDialog showDialog(final Component parent, final String title, Dimension size, final JComponent content) {
        JDialog dialog = null;
        if ((content.getTopLevelAncestor() != null) && (content.getTopLevelAncestor() instanceof JDialog)) {
            dialog = (JDialog) content.getTopLevelAncestor();
        }

        if ((dialog == null) || (!dialog.isShowing())) {
            if (size == null) {
                Dimension preferedSize = content.getPreferredSize();
                if ((preferedSize == null) || (preferedSize.width == 0)) {
                    size = new Dimension(480, 240);
                }
            }
            Window parentWindow = null;
            if ((parent != null) && (parent instanceof Window)){
                parentWindow = (Window) parent;
            }
            dialog = new JDialog(parentWindow);
            JPanel panel = null;
            if (content instanceof JPanel) {
                panel = (JPanel) content;
            } else {
                panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(content, BorderLayout.CENTER);
            }

            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(panel, BorderLayout.CENTER);
            if (size != null) {
                dialog.setSize(size);
            } else {
                dialog.pack();
            }
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent evt) {
                    if (content instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) content).close();
                        } catch (Exception ex) {
                        }
                    }
                }
            });
            dialog.setTitle(title);
        }
        dialog.setVisible(true);

        if (parent != null) {
            centerComponent(parent, dialog);
        }
        dialog.requestFocus();
        return dialog;
    }

    public static JFrame showFrame(final Window parent, final String title, Dimension size, final JComponent content) {
        JFrame frame = null;
        if ((content.getTopLevelAncestor() != null) && (content.getTopLevelAncestor() instanceof JFrame)) {
            frame = (JFrame) content.getTopLevelAncestor();
        }

        if ((frame == null) || (!frame.isShowing())) {
            if (size == null) {
                Dimension preferedSize = content.getPreferredSize();
                if ((preferedSize == null) || (preferedSize.width == 0)) {
                    size = new Dimension(480, 240);
                }
            }
            frame = new JFrame();
            JPanel panel = null;
            if (content instanceof JPanel) {
                panel = (JPanel) content;
            } else {
                panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(content, BorderLayout.CENTER);
            }

            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            if (size != null) {
                frame.setSize(size);
            } else {
                frame.pack();
            }
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent evt) {
                    if (content instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) content).close();
                        } catch (Exception ex) {
                        }
                    }
                }
            });
            frame.setTitle(title);
        }
        frame.setVisible(true);

        if (parent != null) {
            frame.setIconImages(parent.getIconImages());
            centerComponent(parent, frame);
        }
        frame.requestFocus();
        return frame;
    }

    // Random Colors
    public static Color generateRandomColor(long seed) {
        Random random = new Random(seed);
        return generateRandomColor(random);
    }

    public static Color generateRandomColor() {
        Random random = new Random();
        return generateRandomColor(random);
    }

    public static Color generateRandomColor(Random random) {

        Color mix = Color.WHITE;
        int red = (random.nextInt(256) + mix.getRed()) / 2;
        int green = (random.nextInt(256) + mix.getGreen()) / 2;
        int blue = (random.nextInt(256) + mix.getBlue()) / 2;

        Color color = new Color(red, green, blue);
        return color;
    }

    public static String colorToString(Color color) {
        if (color.getAlpha() == 255) {
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        } else {
            return String.format("#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
    }    
    
    public static Color stringToColor(String str) {
        str = str.startsWith("#") ? str.substring(1) : str;
        switch (str.length()) {
            case 6: 
                return new Color(
                    Integer.parseInt(str.substring(0, 2), 16), // Red
                    Integer.parseInt(str.substring(2, 4), 16), // Green
                    Integer.parseInt(str.substring(4, 6), 16)  // Blue
                );
            case 8:
                return new Color(
                    Integer.parseInt(str.substring(0, 2), 16), // Red
                    Integer.parseInt(str.substring(2, 4), 16), // Green
                    Integer.parseInt(str.substring(4, 6), 16), // Blue
                    Integer.parseInt(str.substring(6, 8), 16)  // Alpha
                );
            default:
                throw new IllegalArgumentException("Invalid color format: " + str);
        }
    }    
    
    //JComboBox
    public static void setEnumCombo(JComboBox combo, Class cls) {
        setEnumCombo(combo, cls, false);
    }
    
    public static void setEnumCombo(JComboBox combo, Class cls, boolean asString) {
        if ((combo != null) & (cls != null) & (cls.isEnum())) {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            for (Object object : cls.getEnumConstants()) {
                model.addElement(asString ? String.valueOf(object): object);
            }
            combo.setModel(model);
        }
    }
    
    public static void setEnumCombo(JComboBox combo, Class cls, boolean asString, boolean addEmptyString) {
        if ((combo != null) & (cls != null) & (cls.isEnum())) {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            for (Object object : cls.getEnumConstants()) {
                model.addElement(asString ? String.valueOf(object): object);
            }
            combo.setModel(model);
        }
    }    

    public static void insertCombo(JComboBox combo, Object obj, int index) {
        if (combo != null) {
            DefaultComboBoxModel model = (DefaultComboBoxModel) combo.getModel();
            model.insertElementAt(obj, index);
        }
    }        

    //JTextPane
    public static void appendToTextPane(final JTextPane textPane, final String msg, final Color c, final int textLength) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                appendToTextPane(textPane, msg, c, textLength);
            });
            return;
        }
        String text = msg;
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        int len = textPane.getDocument().getLength();
        if (textLength > 0) {
            int textLen = len + text.length();
            if (textLen > textLength) {
                int finalLength = (int) (textLength * 0.9);
                int delete = textLen - finalLength;
                if (delete > len) {
                    text = text.substring(delete - len);
                    delete = len;
                }
                textPane.setSelectionStart(0);
                textPane.setSelectionEnd(delete);
                textPane.replaceSelection("");
                len = textPane.getDocument().getLength();
            }

        }
        textPane.setCaretPosition(len);
        textPane.setCharacterAttributes(aset, false);
        textPane.replaceSelection(text);
    }

    //JTextComponent
    public static void scrollToVisible(final JTextComponent component, final int line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Element root = component.getDocument().getDefaultRootElement();
                    int startOffset = root.getElement(Math.min(Math.max(line, 1), root.getElementCount()) - 1).getStartOffset();
                    component.setCaretPosition(startOffset);
                    component.select(startOffset, startOffset);
                    JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, component);
                    if (viewport != null) {
                        Rectangle caretViewRect = component.modelToView(component.getCaretPosition());
                        int y = caretViewRect.y - ((viewport.getExtentSize().height - caretViewRect.height) / 2);
                        y = Math.min(Math.max(0, y), viewport.getViewSize().height - viewport.getExtentSize().height);
                        viewport.setViewPosition(new Point(0, y));
                    }
                } catch (Exception ex) {
                }
            }
        });
    }

    //JTabbedPane
    /**
     * A closable tab element. An object of this type is created when called
     * SwingUtils.setTabClosable().
     */
    public static class CloseButtonTabComponent extends JPanel {

        private final JLabel label;
        private final JButton button;
        private final JTabbedPane pane;
        private final CloseableTabListener listener;

        public CloseButtonTabComponent(final JTabbedPane pane, CloseableTabListener listener) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            if (pane == null) {
                throw new NullPointerException("TabbedPane is null");
            }
            this.pane = pane;
            this.listener = listener;
            setOpaque(false);

            label = new JLabel() {
                @Override
                public String getText() {
                    int i = pane.indexOfTabComponent(CloseButtonTabComponent.this);
                    if (i != -1) {
                        return pane.getTitleAt(i);
                    }
                    return null;
                }
            };
            add(label);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            button = new TabButton();
            add(button);
            setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        }

        public JButton getButton() {
            return button;
        }

        public JLabel getLabel() {
            return label;
        }

        private class TabButton extends JButton implements ActionListener {

            public TabButton() {
                int size = 17;
                setPreferredSize(new Dimension(size, size));
                setToolTipText("Close this tab");
                setUI(new BasicButtonUI());
                setContentAreaFilled(false);
                setFocusable(false);
                setBorder(BorderFactory.createEtchedBorder());
                setBorderPainted(false);
                addMouseListener(buttonMouseListener);
                setRolloverEnabled(true);
                addActionListener(this);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int i = pane.indexOfTabComponent(CloseButtonTabComponent.this);
                if (i != -1) {
                    if (listener != null) {
                        if (!listener.canClose(pane, i)) {
                            return;
                        }
                    }
                    pane.remove(i);
                }
            }

            @Override
            public void updateUI() {
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isPressed()) {
                    g2.translate(1, 1);
                }
                g2.setStroke(new BasicStroke(2));
                g2.setColor(MainFrame.isDark() ? Color.GRAY : Color.BLACK);
                if (getModel().isRollover()) {
                    g2.setColor(MainFrame.isDark() ? new Color(255, 40, 40) : new Color(180, 0, 0));
                }
                int delta = 6;
                g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
                g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
                g2.dispose();
            }
        }

        private final static MouseListener buttonMouseListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(false);
                }
            }
        };
    }

    /**
     * The listener interface for receiving closeable tab events.
     */
    public interface CloseableTabListener {

        boolean canClose(JTabbedPane tabbedPane, int index);
    }

    public static void setTabClosable(final JTabbedPane tabbedPane, int index) {
        setTabClosable(tabbedPane, index, null);
    }

    public static void setTabClosable(final JTabbedPane tabbedPane, int index, CloseableTabListener listener) {
        tabbedPane.setTabComponentAt(index, new CloseButtonTabComponent(tabbedPane, listener));
    }
    
    public static boolean isTabClosable(final JTabbedPane tabbedPane, int index) {
        return (tabbedPane.getTabComponentAt(index) instanceof CloseButtonTabComponent);
    }    

    //JTable
    public static boolean isCellVisible(JTable table, int rowIndex, int colIndex) {
        if (!(table.getParent() instanceof JViewport)) {
            return false;
        }
        JViewport viewport = (JViewport) table.getParent();
        Rectangle rect = table.getCellRect(rowIndex, colIndex, true);
        Point pt = viewport.getViewPosition();
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);
        return new Rectangle(viewport.getExtentSize()).contains(rect);
    }

    public static void scrollToVisible(final JTable table, final int rowIndex, final int colIndex) {
        SwingUtilities.invokeLater(() -> {
            int rows = table.getRowCount();
            int cols = table.getColumnCount();
            if (rows > 0) {
                int row = (rowIndex < 0) ? rows : rowIndex;
                int col = (colIndex < 0) ? cols : colIndex;
                if ((row <= rows) && (col <= cols)) {
                    table.scrollRectToVisible(table.getCellRect(row, col, true));
                }
            }
        });
    }

    public static Object[][] getTableData(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Object[][] ret = new Object[model.getRowCount()][model.getColumnCount()];
        for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                ret[i][j] = model.getValueAt(i, j);
            }
        }
        return ret;
    }

    public static Object[] getTableColumnNames(JTable table) {
        ArrayList cols = new ArrayList<>();
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            cols.add(table.getModel().getColumnName(i));
        }
        return cols.toArray();
    }

    public static void setupTableClipboardTransfer(final JTable table) {
        int modifier = (Sys.getOSFamily() == Sys.OSFamily.Mac) ? InputEvent.META_MASK : ActionEvent.CTRL_MASK;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        table.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    StringBuilder sb = new StringBuilder();
                    int cols = table.getSelectedColumnCount();
                    int rows = table.getSelectedRowCount();
                    int[] selRows = table.getSelectedRows();
                    int[] selCols = table.getSelectedColumns();
                    if (!   ((rows - 1 == selRows[selRows.length - 1] - selRows[0]  && rows == selRows.length) 
                          && (cols - 1 == selCols[selCols.length - 1] - selCols[0]  && cols == selCols.length))) {
                        throw new Exception(new Exception("Invalid copy selection"));
                    }
                    for (int i = 0; i < rows; i++) {
                        for (int j = 0; j < cols; j++) {
                            sb.append(table.getValueAt(selRows[i], selCols[j]));
                            if (j < cols - 1) {
                                sb.append("\t");
                            }
                        }
                        sb.append("\n");
                    }
                    clipboard.setContents(new StringSelection(sb.toString()), null);
                } catch (Exception ex) {
                    showException(table, ex);
                }
            }
        }, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, modifier, false), JComponent.WHEN_FOCUSED);
        table.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int row = table.getSelectedRows()[0];
                    int col = table.getSelectedColumns()[0];
                        String str = (String) (clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor));
                    String[] rows = str.split("\n");
                    for (int i=0; i< rows.length; i++) {
                        String[] cols = rows[i].split("\\s+");
                        for (int j=0; j< cols.length; j++) {
                            if (((row + i) < table.getRowCount())  && ((col + j) < table.getColumnCount())) {
                                table.setValueAt(cols[j], row + i, col + j);
                                if (table.getEditingRow() == row  +i && table.getEditingColumn() == col+j) {
                                    table.getCellEditor().cancelCellEditing();                        
                                }                              
                            }
                        }
                    }
                    table.repaint();
                } catch (Exception ex) {
                    showException(table, ex);
                }
            }
        }, "Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, modifier, false), JComponent.WHEN_FOCUSED);
    }
    
    
    public static void setEnumTableColum(JTable table, int columnIndex, Class cls) {
        TableColumn col = table.getColumnModel().getColumn(columnIndex);
        if ((col != null) & (cls != null) & (cls.isEnum())) {  
            JComboBox combo = new JComboBox();
            table.setRowHeight(Math.max(table.getRowHeight(), combo.getPreferredSize().height - 3));
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            for (Object object : cls.getEnumConstants()) {
                model.addElement(object);
            }
            combo.setModel(model);
            DefaultCellEditor cellEditor = new DefaultCellEditor(combo);
            cellEditor.setClickCountToStart(2);
            col.setCellEditor(cellEditor);            
        }
    }    
    
    public static void setColumTitle(JTable table, int columnIndex, String title) {
        String[] names = getColumnNames(table);
        names[columnIndex] = title;
        
        if (table.getModel() instanceof DefaultTableModel){
            ((DefaultTableModel)table.getModel()).setColumnIdentifiers(names);
        }
        JTableHeader th = table.getTableHeader();
        TableColumn tc = th.getColumnModel().getColumn(columnIndex);
        tc.setHeaderValue(title);
        th.repaint();    
    }
    
    public static String[] getColumnNames(JTable table) {
        int columnCount = table.getColumnCount();
        String[] columnNamesArray = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNamesArray[i] = table.getColumnName(i); 
        }
        return columnNamesArray;
    }    
    
    public static void removeColumn(DefaultTableModel model, JTable table, int index) {
        // Create a new custom table model to preserve attributes
        DefaultTableModel newModel = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= index) {
                    return model.getColumnClass(columnIndex + 1); // Adjust for removed column
                }
                return model.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (columnIndex >= index) {
                    return model.isCellEditable(rowIndex, columnIndex + 1); // Adjust for removed column
                }
                return model.isCellEditable(rowIndex, columnIndex);
            }
        };

        // Add columns to the new model, excluding the removed column
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (col != index) {
                newModel.addColumn(model.getColumnName(col));
            }
        }

        // Add rows to the new model
        for (int row = 0; row < model.getRowCount(); row++) {
            Object[] rowData = new Object[newModel.getColumnCount()];
            int newColIndex = 0;

            for (int col = 0; col < model.getColumnCount(); col++) {
                if (col != index) {
                    rowData[newColIndex++] = model.getValueAt(row, col);
                }
            }
            newModel.addRow(rowData);
        }

        // Set the new model to the table
        table.setModel(newModel);    }       
        
    //JTree
    public static void expandAll(JTree tree) {
        setAllTreeItemState(tree, new TreePath(tree.getModel().getRoot()), true);
    }

    public static void collapseAll(JTree tree) {
        setAllTreeItemState(tree, new TreePath(tree.getModel().getRoot()), false);
    }

    static void setAllTreeItemState(JTree tree, TreePath parent, boolean expanded) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                setAllTreeItemState(tree, path, expanded);
            }
        }
        if (expanded) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    public static TreePath getTreePath(JTree tree, String path){
        try{
            List selectionPath = new ArrayList<>();
            path = path.replaceFirst("^/",""); //remove first / if present
            path = path.replaceAll("/$", ""); //remove last / if present
            String[] tokens = path.split("/");
            TreeModel model = tree.getModel();
            Object cur = model.getRoot();
            selectionPath.add(cur);
            for (String name:tokens){
                Object next=null;
                for (int i=0; i<model.getChildCount(cur); i++){
                    Object child = model.getChild(cur, i);
                    if (child.toString().equals(name)){
                        next = child;
                        break;
                    }                    
                }
                if (next==null){
                    return null;
                }                
                cur=next;
                selectionPath.add(cur);
            }
            return new TreePath(selectionPath.toArray());        
        } catch (Exception ex){
            return null;
        }
    }

    
    public static boolean selectTreePath(JTree tree,String path){
        TreePath treePath = getTreePath(tree, path);
        if (treePath!=null){
            tree.setSelectionPath(treePath);
        }
        return (treePath!=null);
    }
    
    
    /**
     * Implement table change listener that provide new and former value
     */
    public static abstract class TableChangeListener implements PropertyChangeListener {

        final private JTable table;
        private int row;
        private int column;
        private Object former;
        private Object value;

        public TableChangeListener(JTable table) {
            this.table = table;
            this.table.addPropertyChangeListener("tableCellEditor", this);
        }

        public int getColumn() {
            return column;
        }

        public Object getValue() {
            return value;
        }

        public Object getFormer() {
            return former;
        }

        public int getRow() {
            return row;
        }

        public JTable getTable() {
            return table;
        }

        void processEditingStopped() {
            value = table.getModel().getValueAt(row, column);
            if (!value.equals(former)) {
                onTableChange(row, column, value, former);
            }
        }    

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (table.isEditing()) {
                SwingUtilities.invokeLater(() -> {
                    row = table.convertRowIndexToModel(table.getEditingRow());
                    column = table.convertColumnIndexToModel(table.getEditingColumn());
                    former = table.getModel().getValueAt(row, column);
                    value = null;
                });
            } else {
                processEditingStopped();
            }
        }  
        
        abstract protected void onTableChange(int row, int column, Object value, Object former);
    }
    

    //Layout
    //Based on SpringUtilities.makeCompactGrid
    public static void makeGridSpringLayout(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout) parent.getLayout();
        } catch (ClassCastException exc) {
            throw new java.lang.IllegalArgumentException("Container must have SpringLayout");
        }

        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, layout.getConstraints(parent.getComponent(r * cols + c)).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = layout.getConstraints(parent.getComponent(r * cols + c));
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height, layout.getConstraints(parent.getComponent(r * cols + c)).getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints = layout.getConstraints(parent.getComponent(r * cols + c));
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

    //Color 
    public static int getLuminance(Color color) {
        return (int) (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue());
    }

    public static int getPerceivedLuminance(Color color) {
        return (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
    }
    
 
    //JColorChooser
    public static final Color DEFAULT_COLOR = new Color(0);
    public static Color getColorWithDefault(Component parent, String title, Color currentColor){       
        List<Color> selection=new ArrayList<>();
        JColorChooser colorChooser = new JColorChooser();
        if (currentColor!=null){
            colorChooser.setColor(currentColor);
        }
        JDialog dialog = JColorChooser.createDialog(parent, title, true, colorChooser, 
                (e)->{
                    selection.add(colorChooser.getColor()); 
                }, 
        null);
        
        String reset = UIManager.getString("ColorChooser.resetText", parent.getLocale());
        for (Component component : SwingUtils.getComponentsByType(dialog, JButton.class)){            
            JButton button = (JButton)component;
            if (!(button instanceof BasicArrowButton)){                
                if (reset.equals(button.getText())) {
                    button.setText("Default");
                    for (ActionListener al : button.getActionListeners()) {
                        button.removeActionListener(al);
                    }
                    button.addActionListener(e -> {
                        selection.add(DEFAULT_COLOR); 
                        dialog.setVisible(false);
                    });
                }            
            }
        }
        
        dialog.setVisible(true);
        return (selection.isEmpty()) ? null : selection.get(0);
    }
    

    //Image    
    static class ImageInvertFilter extends RGBImageFilter {

        @Override
        public int filterRGB(int x, int y, int color) {
            float brightness_enhance = 0.1f;
            int alpha = color & 0xff000000;
            int red = (color & 0x00ff0000) >> 16;
            int green = (color & 0x0000ff00) >> 8;
            int blue = color & 0x000000ff;

            //Inverting color
            red = 255 - red;
            green = 255 - green;
            blue = 255 - blue;

            //Invertung hue and brightness
            float hsb[] = Color.RGBtoHSB(red, green, blue, null);
            float hue = hsb[0];
            float saturation = hsb[1];
            float brightness = hsb[2];
            hue += (hue > 0.5) ? -0.5 : 0.5;
            brightness += (1.0f - brightness) * brightness_enhance;

            color = Color.HSBtoRGB(hue, saturation, brightness);
            return alpha | (color & 0xFFFFFF);
        }
    }

    public static Image invert(Image image) {
        return Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), new ImageInvertFilter()));
    }
    
    //LAF
    
    public static String getNimbusLookAndFeel() {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("nimbus".equalsIgnoreCase(info.getName())) {
                return info.getClassName();
            }
        }
        return null;
    }

    public static String getDarculaLookAndFeel() {
        return "com.bulenkov.darcula.DarculaLaf";
    }

    public static String getDarkLookAndFeel() {
        return "com.formdev.flatlaf.FlatDarculaLaf";
    }

    public static String getFlatLookAndFeel() {
        return "com.formdev.flatlaf.FlatIntelliJLaf";
    }
    
    public static String getBlackLookAndFeel() {
            return "com.formdev.flatlaf.themes.FlatMacDarkLaf";
    }
    
    
    public static boolean isDark() {
        return isDark(UIManager.getLookAndFeel().getClass());            
    }    
    
    public static boolean isBlack() {
        return UIManager.getLookAndFeel().getClass().getName().equals(getBlackLookAndFeel());            
    }
      
    public static boolean isDark(Class laf) {
        return isDark(laf.getName());            
    }      
    
    public static boolean isDark(String lafClassName) {
        if (lafClassName.contains("Dark")){
            return true;
        }
        if (lafClassName.contains("Darcula")){
            return true;
        }
        return false;
    }
    
    public static boolean isFlatLaf(String className){
        return className.contains("flatlaf");
    }      

    public static boolean isNimbus() {
        return UIManager.getLookAndFeel().getClass().getName().equals(getNimbusLookAndFeel());
    }
    
    public static void setLookAndFeel(String className) {
        if (className == null) {
            return;
        }
        try {
            if (className.equals(getDarculaLookAndFeel())){
               //Darcula LAF does not work on Java 14, it is deprecated
                if (Sys.getJavaVersion()>=14){
                    System.err.println("Darcula LAF is deprecated in Java>=14: use Dark instead");
                }
                if (Sys.getOSFamily() == Sys.OSFamily.Linux) {
                    //TODO: workaround to https://github.com/bulenkov/Darcula/issues/29
                    //Not needed with netbeans darcula
                    UIManager.getFont("Label.font");
                }
            }
            UIManager.setLookAndFeel(className);   
            
            
            for (Window window : JFrame.getWindows()) {
                if (window instanceof MainFrame){
                    try {
                        ((MainFrame)window).onLafChange();
                    } catch (Exception ex) {
                    } 
                }                  
                if (window instanceof StandardDialog){
                    try {
                        ((StandardDialog)window).onLafChange();
                    } catch (Exception ex) {
                    } 
                }                  
                for (Component panel : SwingUtils.getComponentsByType(window, MonitoredPanel.class)){
                    try {
                        if (panel.isDisplayable()){
                            ((MonitoredPanel)panel).onLafChange();
                        }
                    } catch (Exception ex) {
                    } 
                }
                for (Component panel : SwingUtils.getComponentsByType(window, StandardDialog.class)){
                    try {
                        if (panel.isDisplayable()){
                            ((StandardDialog)panel).onLafChange();
                        }
                    } catch (Exception ex) {
                    } 
                }  
            }            
            SwingUtils.updateAllFrames();
        } catch (Exception ex) {
            Logger.getLogger(SwingUtils.class.getName()).log(Level.WARNING, null, ex);
        }
    }
    
}
