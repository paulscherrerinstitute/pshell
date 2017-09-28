package ch.psi.pshell.scripting;

import javax.script.ScriptException;

/**
 *
 */
public class StatementException extends ScriptException {

    StatementException(ScriptException ex, Statement statement) {
        this(ex, statement.fileName, statement.lineNumber);
    }

    StatementException(ScriptException ex, String filename, int statementLineNumber) {
        super(getRawMessage(ex.getMessage()),
                filename,
                getStatementExceptionLine(ex.getLineNumber(), statementLineNumber),
                ex.getColumnNumber());
    }

    //Unfoetunatelly cannot get the original message directly, need this hack
    static String getRawMessage(String msg) {
        if (msg != null) {
            String mark = " in ";
            if (msg.contains(mark)) {
                msg = msg.substring(0, msg.lastIndexOf(mark));
            }
        }
        return msg;
    }

    static int getStatementExceptionLine(int exceptionLine, int scriptLine) {
        if (exceptionLine > 0) {
            if (scriptLine <= 0) {
                scriptLine = 1;
            }
            return exceptionLine + scriptLine - 1;
        } else {
            return scriptLine;
        }
    }
}
