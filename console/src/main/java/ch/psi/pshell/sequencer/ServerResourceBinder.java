package ch.psi.pshell.sequencer;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.media.sse.SseBroadcaster;

/**
 * Injection binder for the HTTP server.
 */
public class ServerResourceBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(Interpreter.getInstance()).to(Interpreter.class);
        bind(new SseBroadcaster()).to(SseBroadcaster.class);
    }

}
