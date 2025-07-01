package ch.psi.pshell.xscan.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guard checking channels to meet a certain condition
 */
public class ChannelAccessGuard implements Guard {

    private static Logger logger = Logger.getLogger(ChannelAccessGuard.class.getName());

    /**
     * Flag to indicate whether a guard condition failed since the last init call true: all conditions met, false: at
     * least one condition failed
     */
    private volatile boolean check = true;

    private final List<ChannelAccessGuardCondition<?>> conditions;

    public ChannelAccessGuard(List<ChannelAccessGuardCondition<?>> conditions) {

        this.conditions = conditions;

        for (final ChannelAccessGuardCondition<?> condition : conditions) {
            condition.getChannel().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!evt.getNewValue().equals(condition.getValue())) {
                        check = false;
                    }
                }
            });
        }
    }

    @Override
    public void init() {
        check = true;

        // Check one time if all conditions are met
        for (ChannelAccessGuardCondition<?> condition : conditions) {
            try {
                if (!(condition.getChannel().getValue(true)).equals(condition.getValue())) {
                    check = false;
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Guard interrupted ", e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable ", e);
                check = false;
            }
        }
    }

    @Override
    public boolean check() {
        return check;
    }
}
