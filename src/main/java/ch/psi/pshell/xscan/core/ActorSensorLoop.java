package ch.psi.pshell.xscan.core;

import ch.psi.utils.EventBus;
import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.EndOfStreamMessage;
import ch.psi.pshell.xscan.Metadata;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loop of actions to accomplish a task or test.
 */
public class ActorSensorLoop implements ActionLoop {

    private static Logger logger = Logger.getLogger(ActorSensorLoop.class.getName());

    /**
     * Flag to indicate whether the data of this loop will be grouped According to this flag the dataGroup flag in
     * EndOfStream will be set.
     */
    private boolean dataGroup = false;

    /**
     * List of additional action loops that are executed at the end of each iteration of the loop
     */
    private List<ActionLoop> actionLoops;
    /**
     * List of actions that are executed at the beginning of the loop.
     */
    private List<Action> preActions;
    /**
     * List of actions that are executed at the end of the loop.
     */
    private List<Action> postActions;

    /**
     * List of actions that are executed before the actors are set
     */
    private List<Action> preActorActions;

    /**
     * List of actions that are executed after all actors have been set
     */
    private List<Action> postActorActions;

    /**
     * List of actions that are executed before the sensors are read out
     */
    private List<Action> preSensorActions;
    /**
     * List of actions that are executed after all sensors have been read out
     */
    private List<Action> postSensorActions;

    /**
     * List of actors of this loop
     */
    private List<Actor> actors;

    /**
     * List of sensors of this loop
     */
    private List<Sensor> sensors;

    /**
     * Guard used to check whether the environment was ok while reading out the sensors
     */
    private Guard guard = null;

    private volatile boolean loop = false;
    private volatile boolean abort = false;
    private volatile boolean pause = false;

    private final boolean zigZag;

    private List<ActorSetCallable> pactors;

    private EventBus eventBus = new EventBus();

    public ActorSensorLoop() {
        this(false);
    }

    public ActorSensorLoop(boolean zigZag) {
        this.zigZag = zigZag;
        this.actionLoops = new ArrayList<ActionLoop>();
        this.preActions = new ArrayList<Action>();
        this.postActions = new ArrayList<Action>();
        this.preActorActions = new ArrayList<Action>();
        this.postActorActions = new ArrayList<Action>();
        this.preSensorActions = new ArrayList<Action>();
        this.postSensorActions = new ArrayList<Action>();

        this.actors = new ArrayList<Actor>();
        this.pactors = new ArrayList<ActorSetCallable>();
        this.sensors = new ArrayList<Sensor>();
    }

    /**
     * Executes the actor sensor loop. The actor sensor loop is build up as follows: preActions loop{ check actors -
     * abort if there are no new steps pre actor actions set actors post actor actions pre sensor actions read sensors
     * post sensor actions execute additional registered action loops } postActions
     *
     * @throws InterruptedException
     */
    @Override
    public void execute() throws InterruptedException {
        abort = false;

        List<Metadata> metadata = new ArrayList<>();

        // Build up data message metadata based on the sensors currently registered.
        for (Sensor s : sensors) {
            metadata.add(new Metadata(s.getId()));
        }

        /**
         * Thread pool for parallel execution of tasks
         */
        ExecutorService executorService = Executors.newCachedThreadPool();

        loop = true;

        // Execute pre actions
        for (Action action : preActions) {
            action.execute();
        }

        // Initialize actors of Loop
        for (Actor actor : actors) {
            actor.init();
        }

        // Variable to store the last guard status
        boolean guardOK = true;

        try {

            // Execute loop logic
            while (loop) {

                if (guardOK) {

                    // If actors are defined for the loop check whether all of them
                    // have a next step defined if there is no actor defined only run this loop once
                    if (actors.size() > 0) {
                        // Check whether the actors of this loop have a next step. If not 
                        // abort the loop
                        boolean hasNext = true;
                        for (Actor actor : actors) {
                            if (!actor.hasNext()) {
                                hasNext = false;
                                break; // Stop actor check loop
                            }
                        }

                        // If not all actors have a next step abort the loop
                        if (!hasNext) {
                            break; // Stop action loop
                        }
                    } else {
                        // No actors defined, only run loop once
                        loop = false;
                    }

                    // Execute pre actor actions
                    for (Action action : preActorActions) {
                        action.execute();
                    }

                    waitPauseDone();
                    if (abort) { // End loop if abort was issued
                        break;
                    }

                    // Set actors
                    //				for(Actor actor: actors){
                    //					actor.set();
                    //				}
                    // Parallel set of the actors
                    try {
                        for (Future<Object> f : executorService.invokeAll(pactors)) {
                            f.get(); //Blocks until the async set() is finished
                        }
                    } catch (ExecutionException e) {
                        throw new RuntimeException("Setting the actors failed", e);
                    }

                    // Execute post actor actions
                    for (Action action : postActorActions) {
                        action.execute();
                    }

                    waitPauseDone();
                    if (abort) { // End loop if abort was issued
                        break;
                    }

                }

                if (guard != null) {
                    // Initialize guard
                    guard.init();
                    guardOK = guard.check();

                    // Wait until guard is ok
                    while (!guardOK) {
                        logger.info("Waiting for guard condition(s) to be met");
                        // Sleep 100 milliseconds before next check
                        Thread.sleep(1000);

                        // Check whether the loop is not aborted, if it is aborted
                        // break the wait loop (afterwards also the loop loop is aborted)
                        if (!loop) {
                            break;
                        }

                        guard.init();
                        guardOK = guard.check();
                    }

                    // If loop is aborted proceed to next iteration an abort loop
                    if (!loop) {
                        continue;
                    }
                }

                // Execute pre sensor actions
                for (Action action : preSensorActions) {
                    action.execute();
                }

                waitPauseDone();
                if (abort) { // End loop if abort was issued
                    break;
                }

                // Read sensors
                DataMessage message = new DataMessage(metadata);
                for (Sensor sensor : sensors) {
                    // Readout sensor
                    Object o = sensor.read();
                    // Add sensor data item to message
                    message.getData().add(o);
                }

                // Execute post sensor actions
                for (Action action : postSensorActions) {
                    action.execute();
                }

                waitPauseDone();
                if (abort) { // End loop if abort was issued
                    break;
                }

                // Check guard if one is registered
                if (guard != null) {
                    guardOK = guard.check();
                }

                if (guardOK) {

                    // Post a message with the sensor data
                    eventBus.post(message);

                    // Loop all configured ActionLoop objects
                    for (ActionLoop actionLoop : actionLoops) {
                        actionLoop.execute();
                    }
                }
            }

            // Execute post actions
            for (Action action : postActions) {
                action.execute();
            }

        } finally {
            // Ensure that data stream is terminated ...

            // Issue end of loop control message
            // Set iflag of the EndOfStreamMessage according to dataGroup flag of this loop
            eventBus.post(new EndOfStreamMessage(dataGroup));
        }

        if (zigZag) {
            // Reverse actors for the next run
            for (Actor actor : actors) {
                actor.reverse();
            }
        }

        //executorService.shutdownNow();
        //TODO: Before it was shutdownNow, why?
        executorService.shutdown();

    }

    @Override
    public void abort() {
        abort = true;
        loop = false;

        // To abort all wait actions interrupt this thread
        for (Action a : preSensorActions) {
            try {
                a.abort();
            } catch (Exception ex) {
                Logger.getLogger(ActorSensorLoop.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        // Recursively abort all registered action loops
        for (ActionLoop actionLoop : actionLoops) {
            try {
                actionLoop.abort();
            } catch (Exception ex) {
                Logger.getLogger(ActorSensorLoop.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    @Override
    public void pause() {
        pause = true;
    }

    @Override
    public void resume() {
        pause = false;
    }

    public boolean isPaused() {
        return pause;
    }

    protected void waitPauseDone() throws InterruptedException {
        while (pause && !abort) {
            Thread.sleep(10);
        }
    }

    @Override
    public void prepare() {
        // Create callable for all actors
        pactors.clear();
        for (final Actor a : actors) {
            pactors.add(new ActorSetCallable(a));
        }

        // Reset all actuators
        for (Actor a : actors) {
            a.reset();
        }

        // Recursively call prepare() method of all registered action loops
        for (ActionLoop actionLoop : actionLoops) {
            actionLoop.prepare();
        }
    }

    @Override
    public void cleanup() {
        // Recursively call cleanup() method of all registered action loops
        for (ActionLoop actionLoop : actionLoops) {
            actionLoop.cleanup();
        }
    }

    @Override
    public List<Action> getPreActions() {
        return preActions;
    }

    @Override
    public List<Action> getPostActions() {
        return postActions;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public List<ActionLoop> getActionLoops() {
        return actionLoops;
    }

    public List<Action> getPreActorActions() {
        return preActorActions;
    }

    public List<Action> getPostActorActions() {
        return postActorActions;
    }

    public List<Action> getPreSensorActions() {
        return preSensorActions;
    }

    public List<Action> getPostSensorActions() {
        return postSensorActions;
    }

    public List<Actor> getActors() {
        return actors;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public Guard getGuard() {
        return guard;
    }

    public void setGuard(Guard guard) {
        this.guard = guard;
    }

    public boolean isDataGroup() {
        return dataGroup;
    }

    public void setDataGroup(boolean dataGroup) {
        this.dataGroup = dataGroup;
    }
}
