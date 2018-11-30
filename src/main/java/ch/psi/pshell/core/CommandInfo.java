package ch.psi.pshell.core;

import ch.psi.utils.Str;

/**
 *
 */
public class CommandInfo {

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
    boolean aborted;
    static long commandId = 1;

    CommandInfo(CommandSource source, String script, String command, Object args, boolean background) {
        //run command sets script name and arguments so standard name 
        if ((script == null) && (command != null)) {
            String aux = command.trim();
            if (aux.startsWith("run(")) {
                aux = aux.substring(4);
                if (aux.contains(")")) {
                    aux = aux.substring(0, aux.indexOf(")"));
                    if (aux.contains(",")) {
                        script = aux.substring(0, aux.indexOf(",")).trim();
                        args = aux.substring(aux.indexOf(",")+1).trim();

                    } else {
                        script = aux.trim();
                    }
                    script = Str.removeQuotes(script);
                }
            }
        }

        this.source = source;
        this.script = script;
        this.command = command;
        this.background = background;
        this.args = args;
        this.thread = background ? Thread.currentThread() : Context.getInstance().interpreterThread;
        synchronized (CommandInfo.class) {
            this.id = commandId++;
        }
        Context.getInstance().commandManager.onNewCommand(this.id);
        this.start = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return end == 0;
    }

    public boolean isAborted() {
        return aborted;
    }

    public Object getResult() {
        return result;
    }

    public boolean isError() {
        return (result != null) && (result instanceof Throwable) ;
    }

    public void abort() throws InterruptedException {
        aborted = true;
        if (background) {
            this.thread.interrupt();
        } else {
            Context.getInstance().abort();
        }
    }

    public void join() throws InterruptedException {
        while (isRunning()) {
            synchronized (Context.getInstance().commandManager.commandInfo) {
                Context.getInstance().commandManager.commandInfo.wait();
            }
        }
    }

    public long getAge() {
        if (end == 0) {
            return 0;
        }
        return System.currentTimeMillis() - end;
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %s - %s", background ? String.valueOf(id) : "FG", source.toString(), command, Str.toString(args, 10));
    }

}
