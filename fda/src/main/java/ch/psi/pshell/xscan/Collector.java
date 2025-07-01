package ch.psi.pshell.xscan;

import ch.psi.pshell.utils.EventBus;
import ch.psi.pshell.utils.EventBusListener;
import ch.psi.pshell.utils.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Collector class that is collecting and merging data from different Queues.
 */
public class Collector {

    private EventBus bus;
    private List<MessageListener> listeners = new ArrayList<>();

    private List<Metadata> metadata;
    private boolean first = true;

    public Collector(EventBus b) {
        this.bus = b;
    }

    public void addEventBus(EventBus b) {
        MessageListener l = new MessageListener();
        listeners.add(l);
        b.register(l);
    }

    private class MessageListener implements EventBusListener {

        private DataMessage message;

        //@Subscribe
        @Override
        public void onMessage(final Message message) {
            int level = listeners.indexOf(this);
            if (message instanceof DataMessage dataMessage) {
                this.message = dataMessage;

                if (level == 0) {
                    if (first) {
                        metadata = new ArrayList<>();
                        for (int i = listeners.size() - 1; i >= 0; i--) {
                            // Correct/Add dimension information
                            for (Metadata m : listeners.get(i).getMessage().getMetadata()) {
                                m.setDimension(i);
                            }
                            metadata.addAll(listeners.get(i).getMessage().getMetadata());
                        }

                    }
                    DataMessage m = new DataMessage(metadata);
                    for (int i = listeners.size() - 1; i >= 0; i--) {
                        m.getData().addAll(listeners.get(i).getMessage().getData());
                    }
                    bus.post(m);
                }
            }
            if (message instanceof EndOfStreamMessage endOfStreamMessage) {

                StreamDelimiterMessage ddm = new StreamDelimiterMessage(level, endOfStreamMessage.isIflag());
                bus.post(ddm);
                if (level == (listeners.size() - 1)) { // if highest dimension then send end of stream
                    bus.post(new EndOfStreamMessage());
                }
            }
        }

        public DataMessage getMessage() {
            return message;
        }
    }

}
