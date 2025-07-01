package ch.psi.pshell.xscan.core;

import ch.psi.pshell.utils.EventBus;
import ch.psi.pshell.utils.EventBusListener;
import ch.psi.pshell.utils.Message;
import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.EndOfStreamMessage;
import ch.psi.pshell.xscan.Metadata;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to merge two data streams into one. The secondary queues data is added to the primary queues data. The
 * resulting queue therefor will hold the same amount of elements as the primary queue does. The datagroup flag set in
 * the end of stream message will be set according to the flag set in the primary queue.
 */
public class MergeLogic implements EventBusListener {

    private static final Logger logger = Logger.getLogger(MergeLogic.class.getName());

    private final EventBus primaryEventBus;
    private final EventBus secondaryEventBus;
    private final EventBus outputEventBus;

    private List<Metadata> metadata;

    // Handler of the secondary event bus. We need to keep the reference on this
    // to be able to unregister ...
    private EventBusListener handler;

    boolean firstPrimary = true;
    boolean firstSecondary = true;

    private BlockingQueue<DataMessage> queue = new LinkedBlockingQueue<>();
    private List<Object> currentData = null;

    public MergeLogic(EventBus primaryEventBus, EventBus secondaryEventBus, EventBus outputEventBus) {
        this.primaryEventBus = primaryEventBus;
        this.secondaryEventBus = secondaryEventBus;
        this.outputEventBus = outputEventBus;
    }

    // Enables the merge logic
    public void enable() {

        firstPrimary = true;
        firstSecondary = true;

        queue.clear();

        handler = new EventBusListener() {
            //@Subscribe
            @Override
            public void onMessage(final Message m) {
                if (m instanceof DataMessage dataMessage) {
                    // Only queue data messages
                    queue.add(dataMessage);
                }
            }
        };

        primaryEventBus.register(this);
        secondaryEventBus.register(handler);
    }

    // Disables the merge logic
    public void disable() {
        primaryEventBus.unregister(this);
        secondaryEventBus.unregister(handler);

        queue.clear();
    }

    /**
     * This is the master merging logic
     *
     * @param m	Data message from primary event bus
     */
    //@Subscribe
    @Override
    public void onMessage(Message rawMessage) {
        if (rawMessage instanceof EndOfStreamMessage) {
            outputEventBus.post(rawMessage);

            // Clear queue of secondary event bus 
            queue.clear();
            return;
        } else if (!(rawMessage instanceof DataMessage)) {
            logger.warning("Message type not supported - ignore");
            return;
        }

        DataMessage message = (DataMessage) rawMessage;

        // Hack to remove the synchronization timestamp from the primary event bus
        if (firstPrimary) {
            firstPrimary = false;

            metadata = new ArrayList<>();
            List<Metadata> pqm = message.getMetadata();
            metadata.add(pqm.get(0)); // add first component (this is the actuator)
            // Skip the next component as this is the timestamp used to merge the data
            for (int i = 2; i < pqm.size(); i++) {
                metadata.add(pqm.get(i));
            }
        }

        // Merge logic
        // Get and remove merge timestamp from the data of the message 
        Double timestamp = (Double) message.getData().remove(1);
        long milliseconds = (long) (timestamp * 1000);
        long nanoOffset = (long) ((timestamp * 1000 - milliseconds) * 1000000);

        int count = 10;
        while (true) { // TODO Wait only for a limited amount of time ...
            // Assumption: the secondary Queue holds at least the data up to the timestamp of the message handled by this function
            DataMessage msCheck = queue.peek();

            // No data is available need to wait for some time
            // If still no data is available, throw an exception 
            if (msCheck == null) {
                if (count == 0) {
                    throw new RuntimeException("No data comming in from SCR channels");
                }

                if (currentData == null) {
                    // Need to wait until data comes in ... (this case is for the beginning only)
                    logger.info("No data available");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        logger.log(Level.INFO, "", e);
                    }
                    count--;
                    continue;
                } else {
                    break;
                }
            }

            // If we handle the first message of the secondary event bus, we copy its metadata 
            if (firstSecondary) {
                firstSecondary = false;

                List<Metadata> sqm = msCheck.getMetadata();
                // Skip first two components of the message as this is the timestamp
                for (int i = 2; i < sqm.size(); i++) {
                    metadata.add(sqm.get(i));
                }
            }

            // Check whether timestamp of the next message is bigger than the timestamp of the 
            // message from the primary queue - if the timestamp is bigger do not take message out of the queue
            long currMilliCheck = ((Double) msCheck.getData().get(0)).longValue();
            long currNanoCheck = ((Double) msCheck.getData().get(1)).longValue();

            if (currMilliCheck > milliseconds || (currMilliCheck == milliseconds && currNanoCheck > nanoOffset)) {
                break;
            }

            try {
                DataMessage currentDataMessage = queue.take();
                currentData = currentDataMessage.getData();
                // Remove timestamps used for synchronization
                currentData.remove(0);
                currentData.remove(0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        if (currentData == null) {
            throw new RuntimeException("No SCR data arrived");
        }

        // Add data to primary data queue message and put it into the out queue
        message.getData().addAll(currentData);
        message.setMetadata(metadata);
        outputEventBus.post(message);
    }
}
