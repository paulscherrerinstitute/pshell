package ch.psi.pshell.xscan.core;

import ch.psi.utils.EventBus;
import java.util.List;

/**
 * Loop of actions to accomplish a task. Depending on the loop actions may be executed in a different way.
 */
public interface ActionLoop extends Action {

    /**
     * Prepare ActionLoop for execution.
     */
    public void prepare();

    /**
     * Cleanup resources used by this ActionLoop while it was executed.
     */
    public void cleanup();

    /**
     * Get the pre actions of the Loop
     *
     * @return	pre actions
     */
    public List<Action> getPreActions();

    /**
     * Get the post actions of the loop
     *
     * @return post actions
     */
    public List<Action> getPostActions();

    /**
     * @return is a datagroup
     */
    public boolean isDataGroup();

    /**
     * Set whether data of the loop belongs to a own data group
     *
     * @param dataGroup
     */
    public void setDataGroup(boolean dataGroup);

    public EventBus getEventBus();
}
