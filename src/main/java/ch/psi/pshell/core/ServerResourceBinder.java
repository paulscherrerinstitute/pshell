package ch.psi.pshell.core;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.sse.SseBroadcaster;

/**
 * Injection binder for the HTTP server.
 */
public class ServerResourceBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(Context.getInstance()).to(Context.class);
        bind(new SseBroadcaster()).to(SseBroadcaster.class);
    }

}
