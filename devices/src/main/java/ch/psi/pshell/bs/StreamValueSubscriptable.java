package ch.psi.pshell.bs;

import ch.psi.bsread.message.ChannelConfig;
import ch.psi.pshell.scripting.Subscriptable;
import ch.psi.pshell.utils.Reflection.Hidden;
import java.util.List;

/**
 * Entity containing the current value for a stream, including a list of identifiers, their values,
 * a pulse id and a timestamp.
 */
public class StreamValueSubscriptable extends StreamValue implements Subscriptable.MappedList<String, Object>{


    StreamValueSubscriptable(long pulseId, long timestamp, List<String> identifiers, List values, java.util.Map<String, ChannelConfig> config) {
        this(pulseId, timestamp, 0, identifiers, values, config);
    }

    StreamValueSubscriptable(long pulseId, long timestamp, long nanosOffset, List<String> identifiers, List values, java.util.Map<String, ChannelConfig> config) {
        super(pulseId, timestamp, nanosOffset, identifiers, values, config);
    }


    public Object getValue(String id) {
        return __getitem__(id);
    }

    public Object getValue(int index) {
        return __getitem__(index);
    }
    
    @Override
    public final java.util.List<String> keys(){
        return super.getKeys();
    }  
    
    //Override
    @Override
    public final List values() {
        return super.getValues();
    }
    
    @Override
    @Hidden
    public int toItemIndex(String itemKey){
        for (int i = 0; i < identifiers.size(); i++) {
            if (identifiers.get(i).equals(itemKey)) {
                return i;
            }
        }
        return -1;
    }              
}
