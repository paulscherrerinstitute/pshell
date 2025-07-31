package ch.psi.pshell.security;

import ch.psi.pshell.sequencer.CommandSource;
import java.io.IOException;

/**
 *
 */
public interface PasswordProvider {

    String getPassword(CommandSource source) throws IOException;

}
