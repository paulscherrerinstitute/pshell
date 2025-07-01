package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Configuration;
import ch.psi.pshell.xscan.model.Notification;
import ch.psi.pshell.xscan.model.Recipient;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RecipientListItemProvider implements ListItemProvider<String> {

    private Configuration configuration;

    private final String[] actions = new String[]{"Recipient"};

    public RecipientListItemProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String[] getItemKeys() {
        return (actions);
    }

    @Override
    public Component newItem(String key) {
        if (configuration.getNotification() == null) {
            configuration.setNotification(new Notification());
        }
        if (key.equals(actions[0])) {
            Recipient s = new Recipient();
            configuration.getNotification().getRecipient().add(s);
            return (getItem(s));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        if (configuration.getNotification() != null) {
            for (Recipient s : configuration.getNotification().getRecipient()) {
                l.add(getItem(s));
            }
        }
        return l;
    }

    private Component getItem(Recipient object) {
        RecipientPanel p = new RecipientPanel(object);
        p.setName("Recipient");
        return (p);
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof RecipientPanel c) {
            if (configuration.getNotification() != null) {
                configuration.getNotification().getRecipient().remove(c);

                // Remove notification object if there are no recipients left
                if (configuration.getNotification().getRecipient().isEmpty()) {
                    configuration.setNotification(null);
                }
            }
            // There is nothing to be removed
        }
    }

    @Override
    public boolean isEmpty() {
        if (configuration.getNotification() != null) {
            return configuration.getNotification().getRecipient().isEmpty();
        }
        return true;
    }

    @Override
    public int size() {
        if (configuration.getNotification() != null) {
            return configuration.getNotification().getRecipient().size();
        }
        return 0;
    }

    @Override
    public void moveItemUp(Component component) {
        ListUtil.moveItemUp(configuration.getNotification().getRecipient(), getObject(component));
    }

    @Override
    public void moveItemDown(Component component) {
        ListUtil.moveItemDown(configuration.getNotification().getRecipient(), getObject(component));
    }

    private Object getObject(Component component) {
        if (component instanceof RecipientPanel panel) {
            return panel.getObject();
        }
        return null;
    }

}
