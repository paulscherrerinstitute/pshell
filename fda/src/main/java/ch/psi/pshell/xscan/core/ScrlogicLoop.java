package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.impl.type.DoubleTimestamp;
import ch.psi.pshell.utils.EventBus;
import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.EndOfStreamMessage;
import ch.psi.pshell.xscan.Metadata;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * Assumptions: - The delay between the monitor writing the value to the monitor queue and the readout of all the queues
 * is sufficient to prevent the situation that some monitors of events close to each other on different IOC's have not
 * arrived yet. - The sequence of monitors fired for one channel is according to the sequence of the causes. No monitor
 * package is overtaking an other package on the network.
 *
 * - No monitor events are lost on the network (while using monitors you cannot guarantee this)
 *
 * The data queue returned by this logic includes two items for the timestamp and nanoseconds offset. These two items
 * are the first two items of a message The id's are: crTimestampMilliseconds crTimestampOffsetNanoseconds
 */
public class ScrlogicLoop implements ActionLoop {

    private static String ID_TIMESTAMP_MILLISECONDS = "crTimestampMilliseconds";
    private static String ID_TIMESTAMP_OFFSET_NANOSECONDS = "crTimestampOffsetNanoseconds";

    private static final Logger logger = Logger.getLogger(ScrlogicLoop.class.getName());

    private boolean dataGroup = false;
    private final List<Action> preActions = new ArrayList<Action>();
    private final List<Action> postActions = new ArrayList<Action>();

    private final List<Channel<DoubleTimestamp>> sensors;
    private final List<String> sensorIds;

    /**
     * List of monitors that were attached to the sensor channels (i.e workaround)
     */
    private final List<PropertyChangeListener> monitors = new ArrayList<>();

    private List<Object> currentValues;

    private CountDownLatch latch;

    private final EventBus eventbus;
    private List<Metadata> metadata;

    public ScrlogicLoop(List<String> sensorIds, List<Channel<DoubleTimestamp>> sensors) {
        this.eventbus = new EventBus();
        this.sensors = sensors;
        this.sensorIds = sensorIds;
    }

    @Override
    public void execute() throws InterruptedException {

        latch = new CountDownLatch(1);

        // Initialize current values
        currentValues = new ArrayList<Object>(sensors.size() + 2);
        // Initialize values
        currentValues.add(0.0);
        currentValues.add(0.0);
        for (Channel<DoubleTimestamp> sensor : sensors) {
            try {
                DoubleTimestamp value = sensor.getValue();
                double timestamp = value.getTimestamp().getTime();
                double noffset = value.getNanosecondOffset();
                if (timestamp > ((Double) currentValues.get(0))) {
                    // We don't care about the nanoseconds offset so far
                    currentValues.set(0, timestamp);
                    currentValues.set(1, noffset);
                }
                currentValues.add(value.getValue());
            } catch (InterruptedException e) {
                throw e; 
            } catch (Exception e) {
                throw new RuntimeException("Unable to retrieve initial value");
            }
            // Initialize current value with NAN
        }

        // Create metadata
        metadata = new ArrayList<>(sensors.size());

        // Build up data message metadata based on the channels registered.
        metadata.add(new Metadata(ID_TIMESTAMP_MILLISECONDS));
        metadata.add(new Metadata(ID_TIMESTAMP_OFFSET_NANOSECONDS));
        for (String id : sensorIds) {
            metadata.add(new Metadata(id));
        }

        // Send a first data message to be sure that there is a message inside
        // the queue
        DataMessage message = new DataMessage(metadata);
        message.getData().addAll(currentValues);

        eventbus.post(message);

        // Attach monitors to the channels (this is actually a workaround)
        int counter = 2;
        for (Channel<DoubleTimestamp> sensor : sensors) {
            final int currentCount = counter;

            PropertyChangeListener listener = new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("value")) {

                        // Merging values this way it is not 100% accurate as
                        // theoretically values does not need to come in
                        // in the correct time sequence. However we tradeoff
                        // this accuracy as we are not precise anyway
                        DoubleTimestamp v = (DoubleTimestamp) evt.getNewValue();
                        double timestamp = v.getTimestamp().getTime();
                        double noffset = v.getNanosecondOffset();
                        currentValues.set(0, timestamp);
                        currentValues.set(1, noffset);
                        currentValues.set(currentCount, v.getValue());

                        DataMessage message = new DataMessage(metadata);
                        message.getData().addAll(currentValues);
                        // logger.info("update"+v.getValue());
                        eventbus.post(message);
                    }
                }
            };
            sensor.addPropertyChangeListener(listener);
            monitors.add(listener);

            counter++;
        }

        logger.info("Start data acquisition");

        latch.await();

        // Remove monitors
        logger.info("Remove monitors");
        for (int i = 0; i < sensors.size(); i++) {
            Channel<DoubleTimestamp> sensor = sensors.get(i);
            sensor.removePropertyChangeListener(monitors.get(i));
        }

        // Put end of stream to the queue
        eventbus.post(new EndOfStreamMessage(dataGroup));

    }

    @Override
    public void abort() {
        latch.countDown();
    }

    @Override
    public void prepare() {
    }

    @Override
    public void cleanup() {
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
    public boolean isDataGroup() {
        return dataGroup;
    }

    @Override
    public void setDataGroup(boolean dataGroup) {
        this.dataGroup = dataGroup;
    }

    @Override
    public EventBus getEventBus() {
        return eventbus;
    }
}
