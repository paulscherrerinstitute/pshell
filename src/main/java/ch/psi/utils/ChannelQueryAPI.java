
package ch.psi.utils;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface ChannelQueryAPI {
    public List<String> queryChannels(String text, String backend, int limit) throws IOException;
}
