package ch.psi.pshell.sequencer;

import ch.psi.pshell.scripting.Interpreter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;

/**
 * Management of standard input and standard output of the interpreter.
 */
public class ScriptStdio implements AutoCloseable {

    ScriptStdioListener listener;
    final static Logger logger = Logger.getLogger(ScriptStdio.class.getName());
    public static final String END_OF_LINES = Character.toString((char) 0);

    public ScriptStdioListener getListener() {
        return listener;
    }

    public void setListener(ScriptStdioListener listener) {
        this.listener = listener;
    }
    Writer stdoutWriter;
    Writer stderrWriter;
    BufferedReader stdinReader;

    class StdioWriter extends Writer {

        final boolean stderr;
        String lineSeparator = "\n";

        StdioWriter(boolean stderr) {
            this.stderr = stderr;
        }

        StringBuilder sb = new StringBuilder();

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if ((off < 0) || (off > cbuf.length) || (len < 0)
                    || ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            sb.append(cbuf, off, len);
            if (sb.indexOf(lineSeparator) >= 0) {
                int finalCompletedIndex = sb.lastIndexOf(lineSeparator);

                String[] lines = sb.toString().substring(0, finalCompletedIndex).split(lineSeparator);
                if (listener != null) {
                    for (String line : lines) {
                        if (stderr) {
                            listener.onStderr(line);
                        } else {
                            listener.onStdout(line);
                        }
                    }
                }
                sb.delete(0, finalCompletedIndex + 1);
            }
        }

        void push() {
            if (sb.length() > 0) {
                String[] lines = sb.toString().split(lineSeparator);
                if (listener != null) {
                    for (String line : lines) {
                        if (stderr) {
                            listener.onStderr(line);
                        } else {
                            listener.onStdout(line);
                        }
                    }
                }
                sb.delete(0, sb.length());
            }
        }

        @Override
        public void flush() throws IOException {
            //Not calling push systematically because it is being printed line by line only
            if (sb.length() >= 80) {
                //In case multiple print, with no carriage return print the line anyway so
                // buffer does not grow indefinitely
                push();
            }
        }

        @Override
        public void close() throws IOException {
        }
    }

    public ScriptStdio(final Interpreter interpreter) {
        try {

            stdinReader = new BufferedReader(new InputStreamReader(new InputStream() {
                String buf = null;

                @Override
                public int read() throws IOException {
                    if (listener != null) {
                        if (buf == null) {
                            try {
                                if (stdoutWriter instanceof StdioWriter stdioWriter) {
                                    stdioWriter.push();
                                }
                                buf = listener.readStdin();
                                if (buf.equals(END_OF_LINES)) {
                                    buf = null;
                                    return -1;
                                } else {
                                    buf += '\n';
                                }

                            } catch (InterruptedException ex) {
                                throw new IOException(ex);
                            }
                        }
                        if ((buf != null) && (!buf.isEmpty())) {
                            int ret = buf.charAt(0);
                            buf = buf.substring(1);
                            return ret;
                        }
                    }
                    buf = null;
                    return -1;
                }
            }));
            interpreter.setReader(stdinReader);

            stdoutWriter = new StdioWriter(false);
            interpreter.setWriter(stdoutWriter);

            stderrWriter = new StdioWriter(true);
            interpreter.setErrorWriter(stderrWriter);

        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    public ScriptStdio(final ScriptEngine engine) {
        try {
            stdoutWriter = new StdioWriter(false);
            engine.getContext().setWriter(stdoutWriter);

            stderrWriter = new StdioWriter(true);
            engine.getContext().setErrorWriter(stderrWriter);

        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        try {
            listener = null;
            if (stdoutWriter != null) {
                stdoutWriter.close();
            }
            if (stderrWriter != null) {
                stderrWriter.close();
            }
            if (stdinReader != null) {
                stdinReader.close();
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }
}
