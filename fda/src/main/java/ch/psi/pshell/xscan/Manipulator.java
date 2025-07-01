package ch.psi.pshell.xscan;

import ch.psi.pshell.utils.EventBus;
import ch.psi.pshell.utils.EventBusListener;
import ch.psi.pshell.utils.Message;
import ch.psi.pshell.xscan.core.Manipulation;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies manipulations to the data stream
 */
public class Manipulator implements EventBusListener {

    private EventBus bus;

    private final List<Manipulation> manipulations;
    private boolean first = true;
    private List<Metadata> metadata = new ArrayList<>();

    public Manipulator(EventBus b, List<Manipulation> manipulations) {
        this.bus = b;
        this.manipulations = manipulations;
    }

    //@Subscribe
    @Override
    public void onMessage(final Message message) {
        if (message instanceof DataMessage dataMessage) {
            if (first) {
                first = false;

                metadata.addAll(dataMessage.getMetadata());

                for (Manipulation manipulation : this.manipulations) {
                    manipulation.initialize(this.metadata);

                    // Add manipulation id to metadata
                    this.metadata.add(new Metadata(manipulation.getId(), 0)); // Calculated component always belongs to lowes dimension
                }
            }

            DataMessage dm = dataMessage;
//			message = new DataMessage(metadata);
            for (Manipulation manipulation : manipulations) {
//                          ((DataMessage)message).getData().add(manipulation.execute(dm));
                dm.getData().add(manipulation.execute(dm));

                // Need to update the metadata of the message
                dm.setMetadata(this.metadata);
            }
        }
        bus.post(message);
    }
}
