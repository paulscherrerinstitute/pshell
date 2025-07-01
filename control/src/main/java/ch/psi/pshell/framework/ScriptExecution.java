package ch.psi.pshell.framework;

import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.sequencer.InterpreterListener;
import javax.script.ScriptException;

/**
 * Task to run or debug scripts.
 */
public class ScriptExecution extends Task {

    volatile Statement currentStatement;

    public ScriptExecution(String fileName, Statement[] statements, Object args, boolean pauseOnStart, boolean debug) {
        super();
        this.fileName = fileName;
        this.statements = statements;
        this.args = args;
        this.pauseOnStart = pauseOnStart;
        this.debug = debug;
        Context.getInterpreter().addListener(new InterpreterListener() {
            @Override
            public void onNewStatement(final Statement statement) {
                currentStatement = statement;
            }

            @Override
            public void onExecutedStatement(Statement statement) {
                if (Context.getInterpreter().isRunningStatements()) {
                    if (!isDone() && statements != null) {
                        //if ((Context.hasView()) && (!Context.getView().hasOngoingScan())) {
                        //!!! Test
                        if (Context.getInterpreter().getVisibleScans().length==0){  
                            //Scan progress has priority
                            setProgress((int) (((double) statement.number) * 100 / statements.length));
                        }
                    }
                }
            }

            @Override
            public void onExecutedFile(String fileName, Object result) {
                Context.getInterpreter().removeListener(this);
            }
        });
    } //Scan progress has priority

    public Statement getCurrentStatement() {
        return currentStatement;
    }
    String scriptName;
    final String fileName;
    final Statement[] statements;
    final Object args;
    final boolean pauseOnStart;
    final boolean debug;

    public String getScriptName() {
        return scriptName;
    }

    @Override
    protected Object doInBackground() throws Exception {
        currentStatement = null;
        scriptName = Context.getInterpreter().getStandardScriptName(fileName);
        setCommand(scriptName);
        setProgress(0);
        try {
            Object ret = null;
            if (debug) {
                ret = Context.getInterpreter().evalStatements(statements, pauseOnStart, fileName, args);
            } else {
                ret = Context.getInterpreter().evalFile(fileName, args);
            }
            setProgress(100);
            checkShowReturn(ret);
            return ret;
        } catch (ScriptException ex) {
            checkShowException(ex);
            throw ex;
        } catch (Exception ex) {
            sendErrorApp(ex);
            throw ex;
        } finally {
            currentStatement = null;
        }
    }
    
}
