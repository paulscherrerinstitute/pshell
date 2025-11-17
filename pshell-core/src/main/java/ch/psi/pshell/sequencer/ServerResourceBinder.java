package ch.psi.pshell.sequencer;

import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.media.sse.SseBroadcaster;

/**
 * Injection binder for the HTTP server.
 */
public class ServerResourceBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(Sequencer.getInstance()).to(Sequencer.class);
        bind(new SseBroadcaster()).to(SseBroadcaster.class);
    }

}
