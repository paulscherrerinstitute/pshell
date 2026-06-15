package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.utils.Str;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CommandInfo {
    public final CommandInfo parent;
    public final CommandSource source;
    public final String script;
    public final String command;
    public final boolean background;
    public final Object args;
    public final Thread thread;
    public final long id;
    public final long start;
    public long end;
    Object result;
    
    static long commandId = 1;
    private boolean aborted;

    private CommandInfo(CommandSource source, String script, String command, Object args, boolean background, CommandInfo parent) {
        boolean isRun = false;
        //run command sets script name and arguments so standard name 
        if ((script == null) && (command != null)) {
            String aux = command.trim();
            if (aux.startsWith("run(")) {
                aux = aux.substring(4);
                if (aux.contains(")")) {
                    aux = aux.substring(0, aux.indexOf(")"));                    
                    String[] tokens = Str.splitWithQuotes(aux, ",");
                    script = tokens[0].trim();
                    aux = tokens.length>1 ? tokens[1].trim() : null;
                    if ((aux!=null) && (!aux.isBlank())) {
                        if (aux.startsWith("locals") && aux.substring(6).trim().startsWith("=")){
                        } else {
                            if (aux.startsWith("args") && aux.substring(4).trim().startsWith("=")){
                                aux = aux.substring(aux.indexOf("=")+1).trim();
                            }
                            if ((!aux.isBlank()) && !aux.equals("None")){
                                if ((aux.startsWith("[") && aux.endsWith("]")) ||
                                    (aux.startsWith("{") && aux.endsWith("}"))){
                                     aux = aux.substring(1, aux.length() - 1).trim();
                                }                           
                                args = aux;
                            }                             
                        }
                    }
                    script = Str.removeQuotes(script);
                }
            }
        }
        this.parent = parent;
        this.source = source;
        this.script = script;
        this.command = command;
        this.background = background;
        this.args = args;
        this.thread = background ? Thread.currentThread() : Context.getSequencer().getInterpreterThread();
        synchronized (CommandInfo.class) {
            this.id = commandId++;
        }        
        this.start = System.currentTimeMillis();
    }
    
    public CommandInfo(CommandSource source, String script, String command, Object args, boolean background) {
        this(source, script, command, args, background, null);
    }
    
    //Nested command ("run")
    public CommandInfo(CommandInfo parent, String script,Object args) {
        this(CommandSource.script, script, null, args, (parent == null) ? false : parent.background, parent);
    }

    public boolean isRunning() {
        return end == 0;
    }

    public boolean isAborted() {
        return aborted;
    }
       

    protected void setAborted(){
        aborted = true;
        end = System.currentTimeMillis();        
        if ((parent != null) && (!parent.aborted)){
            parent.setAborted();
        }        
        for (CommandInfo child : getChildren()){
            if (!child.aborted){
                child.setAborted();
            }
        }
    }      
        
    protected void setResult(Object result){
        this.result = result;
        end = System.currentTimeMillis();        
    }
    
    public enum Status{
        Running,
        Aborted,
        Error,
        Success
    }
    
    public Status getStatus(){
        if (isRunning()){
            return Status.Running;
        }
        if (isAborted()){
            return Status.Aborted;
        }
        if (isError()){
            return Status.Error;
        }
        return Status.Success;
    }

    public Object getResult() {
        return result;
    }

    public boolean isError() {
        return result instanceof Throwable;
    }

    public void abort() throws InterruptedException {
        setAborted();
        if (background) {
            this.thread.interrupt();
        } else {
            Context.abort();
        }
    }

    public void join() throws InterruptedException {
        while (isRunning()) {
            Context.getCommandBus().waitCommandBusChangeEvent();
        }
    }

    public long getAge() {
        if (end == 0) {
            return 0;
        }
        return System.currentTimeMillis() - end;
    }

    public long getRunningTime() {
        if (end == 0) {
            return System.currentTimeMillis() - start;
        }
        return end - start;
    }
    
    public List<CommandInfo> getChildren(){
        var ret = new ArrayList<CommandInfo>();
        for (CommandInfo ci : Context.getCommandBus().getCommands()){
            if (ci.parent == this){
                ret.add(ci);
            }
        }
        return ret;
    }

    
    @Override
    public String toString() {
        return String.format("%s - %s - %s - %s", background ? String.valueOf(id) : "FG", source.toString(), command, Str.toString(args, 10));
    }

}
