package ch.psi.pshell.swing;

import ch.psi.pshell.utils.Config;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 *
 */
public class DefaultUserInterface implements UserInterface{
        final Component parent;
        
        public DefaultUserInterface(){
            this(null);
        }
        public DefaultUserInterface(Component parent){
            this.parent = parent;
        }

        @Override
        public String getString(String message, String defaultValue) {
            return SwingUtils.getString(parent, message, defaultValue);
        }

        @Override
        public String getString(String message, String defaultValue, String[] alternatives) {

            return SwingUtils.getString(parent, message, alternatives, defaultValue);
        }

        @Override
        public String getPassword(String message, String title) {
            return SwingUtils.getPassword(parent, title, message);
        }

        @Override
        public String getOption(String message, String type) {
            SwingUtils.OptionResult ret = SwingUtils.showOption(parent, null, message, SwingUtils.OptionType.valueOf(type));
            if (ret == SwingUtils.OptionResult.Closed) {
                ret = SwingUtils.OptionResult.Cancel;
            }
            return ret.toString();
        }

        @Override
        public void showMessage(String message, String title, boolean blocking) {
            if (blocking) {
                SwingUtils.showMessageBlocking(parent, title, message);
            } else {
                SwingUtils.showMessage(parent, title, message);
            }
        }

        @Override
        public ConfigDialog showConfig(Config config) throws InterruptedException {
            return ConfigDialog.showConfigEditor(parent, config, false, false);
        }

        @Override
        public MonitoredPanel showPanel(String text, String title) throws InterruptedException{
                TextEditor editor = new TextEditor();
                editor.setText(text);
                editor.setReadOnly(true);
                editor.setFont(new Font("Courrier New", 1, 13));
                SwingUtils.showDialog(parent,title, new Dimension(1000, 600), editor);                        
                return editor;
        }
}
