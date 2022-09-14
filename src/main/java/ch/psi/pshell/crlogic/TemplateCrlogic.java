package ch.psi.pshell.crlogic;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelException;
import ch.psi.jcae.annotation.CaChannel;
import ch.psi.pshell.epics.Epics;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TemplateCrlogic {

    /**
     * Ticks per second - IOC setting ATTENTION - This field must only be set bu the IOC - ATTENTION
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}:TPS")
    private Channel<Integer> ticksPerSecond;

    /**
     * Status of the OTFSCAN IOC logic
     */
    public enum Status {
        SETUP, INACTIVE, INITIALIZE, ACTIVE, STOP, FAULT, ERROR
    };
    @CaChannel(type = String.class, name = "${PREFIX}:STATUS")
    private Channel<String> status;

    /**
     * Message from the OTFSCAN IOC logic
     */
    @CaChannel(type = String.class, name = "${PREFIX}:MSG")
    private Channel<String> message;

    /**
     * IOC ticks between data acquisition interrupts
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}:TBINT")
    private Channel<Integer> ticksBetweenInterrupts;

    /**
     * Name or ip address of the NFS server to save the data to (depending on the IOC setup)
     */
    @CaChannel(type = String.class, name = "${PREFIX}:NFSSE")
    private Channel<String> nfsServer;

    /**
     * Name of the NFS share on the NFS server
     */
    @CaChannel(type = String.class, name = "${PREFIX}:NFSSH")
    private Channel<String> nfsShare;

    /**
     * Name of the data file
     */
    @CaChannel(type = String.class, name = "${PREFIX}:DFNAM")
    private Channel<String> dataFile;

    /**
     * Flag to identify whether the data file should be appended
     */
    @CaChannel(type = Boolean.class, name = "${PREFIX}:FAPPE")
    private Channel<Boolean> appendFile;

    /**
     * Readout resources
     */
    @CaChannel(type = String[].class, name = "${PREFIX}:RRES")
    private Channel<String[]> readoutResources;

    public Channel<Integer> getTicksPerSecond() {
        return ticksPerSecond;
    }

    public Channel<String> getStatus() {
        return status;
    }

    public Channel<String> getMessage() {
        return message;
    }

    public Channel<Integer> getTicksBetweenInterrupts() {
        return ticksBetweenInterrupts;
    }

    public Channel<String> getNfsServer() {
        return nfsServer;
    }

    public Channel<String> getNfsShare() {
        return nfsShare;
    }

    public Channel<String> getDataFile() {
        return dataFile;
    }

    public Channel<Boolean> getAppendFile() {
        return appendFile;
    }

    public Channel<String[]> getReadoutResources() {
        return readoutResources;
    }

    public void waitStatus(TemplateCrlogic.Status status, long timeout) throws InterruptedException, ExecutionException, ChannelException, TimeoutException {
        //getStatus().waitForValue(status.toString(), timeout);    
        Epics.waitValue(getStatus(), status.toString(), (int) timeout); //Extra check in the end of temporization
    }

    public void setStatus(TemplateCrlogic.Status status) throws InterruptedException, ExecutionException, ChannelException {
        getStatus().setValue(status.toString());
    }
}
