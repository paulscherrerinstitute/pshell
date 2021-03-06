package ch.psi.pshell.scan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Publisher;
import ch.psi.pshell.ui.App;
import java.util.ArrayList;

/**
 *
 */
public class ScanStreamer extends Publisher {

    final ScanListener listener;

    public static final String ENVELOPE_START = "Start";
    public static final String ENVELOPE_RECORD = "Record";
    public static final String ENVELOPE_END = "End";

    final ArrayList<Event> events = new ArrayList();

    public ScanStreamer(int port) {
        super(port);
        listener = new ScanListener() {
            @Override
            public void onScanStarted(Scan scan, String plotTitle) {
                sendEvent(ENVELOPE_START, scan.getResult().toString());
            }

            @Override
            public void onNewRecord(Scan scan, ScanRecord record) {
                sendEvent(ENVELOPE_RECORD, record.toString());
            }

            @Override
            public void onScanEnded(Scan scan, Exception ex) {
                sendEvent(ENVELOPE_END, scan.getResult().getPath());
            }
        };
        Context.getInstance().addScanListener(listener);
    }

    @Override
    public void close() throws Exception {
        Context.getInstance().removeScanListener(listener);
        super.close();
    }

    public static void main(String[] args) throws Exception {
        App.init(args);
        String server = "localhost:5563";
        if (App.hasArgument("url")) {
            server = App.getArgumentValue("url");
        }
        org.zeromq.ZMQ.Context context = org.zeromq.ZMQ.context(1);
        org.zeromq.ZMQ.Socket subscriber = context.socket(org.zeromq.ZMQ.SUB);

        subscriber.connect("tcp://" + server);
        subscriber.subscribe(ENVELOPE_START.getBytes());
        subscriber.subscribe(ENVELOPE_RECORD.getBytes());
        subscriber.subscribe(ENVELOPE_END.getBytes());
        while (!Thread.currentThread().isInterrupted()) {
            String type = subscriber.recvStr();
            String contents = subscriber.recvStr();
            System.out.println(type + " : " + contents);
        }
        subscriber.close();
        context.term();
    }
}
