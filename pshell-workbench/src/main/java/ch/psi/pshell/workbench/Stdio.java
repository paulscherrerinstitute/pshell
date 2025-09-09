package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.ScriptExecution;

/**
 * An optional interface for the workbench to exchange data through the stdio.
 */
public class Stdio extends ch.psi.pshell.app.Stdio{

    /**
     * Override to implement the console behavior
     */
    protected void onConsoleCommand(String name, String[] pars, String trimming) throws Exception {
        StringBuilder sb;

        switch (name) {
            case "exit":
                Context.getApp().exit(this);
                break;
            case "run":
                if (pars.length > 0) {
                    String file = pars[0];
                    Context.getApp().startTask(new ScriptExecution(file, null, null, false, false));
                }
                break;
            case "eval":
                if (!trimming.isEmpty()) {
                    System.out.println(Context.getSequencer().evalLine(trimming));
                }
                break;
            case "evalb":
                if (!trimming.isEmpty()) {
                    System.out.println(Context.getSequencer().evalLineBackground(trimming));
                }
                break;
            case "show":
                if (Context.hasView()) {
                    Context.getView().setVisible(true);
                }
                break;
            case "state":
                System.out.println(Context.getApp().getState());
                break;
            case "hide":
                if (Context.hasView()) {
                    Context.getView().setVisible(false);
                }

                break;
            case "about":
                sb = new StringBuilder();
                sb.append(App.getApplicationTitle());
                sb.append("\n").append(App.getApplicationCopyright());
                sb.append("\nVersion ").append(App.getApplicationBuildInfo());
                sb.append("\n").append(App.getApplicationDescription());
                sb.append("\n");
                System.out.println(sb.toString());
                break;
            case "plugins":
                sb = new StringBuilder();
                for (ch.psi.pshell.plugin.Plugin p : Context.getPlugins()) {
                    sb.append("Name: ").append(p.toString()).append(" Started: ").append(p.isStarted()).append(" Class: ").append(p.getClass().getName());
                }
                System.out.println(sb.toString());
                break;
            default:
                System.out.println("Invalid Command");
                break;
        }
    }
}
