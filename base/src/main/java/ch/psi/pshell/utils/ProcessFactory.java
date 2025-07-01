package ch.psi.pshell.utils;

import ch.psi.pshell.utils.Sys.OSFamily;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs a class containing a main method in a different process.
 */
public class ProcessFactory {

    public static Process createProcess(Class c) {
        return createProcess(c, null, null);
    }

    public static Process createProcess(Class c, String process_name) {
        return createProcess(c, null, process_name);
    }

    public static Process createProcess(Class c, String[] pars) {
        return createProcess(c, pars, null);
    }

    public static Process createProcess(Class c, String[] pars, String process_name) {
        try {
            String java_exe = Paths.get(System.getProperty("java.home"), "bin", "java").toString();

            String executable = java_exe;
            boolean windows = Sys.getOSFamily() == OSFamily.Windows;

            if (windows) {
                java_exe += ".exe";
                if ((executable.contains(" ")) && (!executable.startsWith("\""))) {
                    executable = "\"" + java_exe + "\"";
                }
            }
            if (pars == null) {
                pars = new String[0];
            }

            if (process_name != null) {
                if (windows) {
                    executable = process_name + ".exe";
                    Files.copy(new File(java_exe).toPath(), new File(executable).toPath());
                } else {
                    executable += " -Dname=" + process_name;
                }
            }
            String cmd = windows
                    ? executable + " -classpath \"" + System.getProperty("java.class.path") + "\" " + c.getName()
                    : executable + " -classpath " + System.getProperty("java.class.path") + " " + c.getName();
            for (String par : pars) {
                cmd += (" " + par);
            }
            return Runtime.getRuntime().exec(cmd, null, new File(new File(".").getCanonicalPath()));
        } catch (IOException ex) {
            Logger.getLogger(ProcessFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
