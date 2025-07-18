package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Task;
import ch.psi.pshell.sequencer.CommandSource;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public abstract class VersioningTasks{

    /**
     * Task to commit changes to the local Git repository.
     */
    public static class Commit extends Task {
        

        final String commitMessage;

        public Commit(String commitMessage) {
            super();
            this.commitMessage = commitMessage;
        }

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Commiting";
            setMessage(msg);            
            setProgress(0);
            try {
                Context.getApp().sendTaskInit(msg);
                Context.getVersioningManager().addCommitAll(commitMessage, false);
                msg = "Success commiting";
                Context.getApp().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {     
                Logger.getLogger(Commit.class.getName()).log(Level.WARNING, null, ex);
                setMessage("Error commiting");
                Context.getApp().sendError(ex.toString());                
                throw ex;
            } finally {
                Context.getApp().sendTaskFinish(msg);
            }
        }
    }
    /**
     * Task to checkout another brunch in the local Git repository.
     */
    public static class Checkout extends Task {

        final boolean branch;
        final String name;

        public Checkout(boolean branch, String name) {
            super();
            this.branch = branch;
            this.name = name;
        }

        @Override
        protected String doInBackground() throws Exception {
            String msg;
            if (branch) {
                msg = "Checking out branch: " + name;
            } else {
                msg = "Checking out tag: " + name;
            }
            setMessage(msg);
            setProgress(0);
            try {
                Context.getApp().sendTaskInit(msg);

                if (branch) {
                    Context.getVersioningManager().checkoutLocalBranch(name);
                } else {
                    Context.getVersioningManager().checkoutTag(name);
                }

                msg = "Success checking out";
                Context.getApp().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                Logger.getLogger(Checkout.class.getName()).log(Level.WARNING, null, ex);
                setMessage("Error checking out");
                Context.getApp().sendError(ex.toString());
                throw ex;
            } finally {
                Context.getApp().sendTaskFinish(msg);
            }
        }
    }

    /**
     * Task to pull from remote Git repository.
     */
    public static class PullUpstream extends Task {

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Pulling history";
            setMessage(msg);
            setProgress(0);
            try {
                Context.getApp().sendTaskInit(msg);
                Context.getVersioningManager().assertHasRemoteRepo();
                Context.getInterpreter().setSourceUI(CommandSource.ui); //Ensure authentication dialog comes to local interface
                Context.getVersioningManager().pullFromUpstream();
                msg = "Success pulling from upstream";
                Context.getApp().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                Logger.getLogger(PullUpstream.class.getName()).log(Level.WARNING, null, ex);
                setMessage("Error pulling from upstream");
                Context.getApp().sendError(ex.toString());
                throw ex;
            } finally {
                Context.getApp().sendTaskFinish(msg);
            }
        }
    }

    /**
     * Task to push to remote Git repository.
     */
    public static class PushUpstream extends Task {

        final boolean allBranches;
        final boolean force;
        final boolean tags;

        public PushUpstream(boolean allBranches, boolean force, boolean tags) {
            super();
            this.allBranches = allBranches;
            this.force = force;
            this.tags = tags;
        }

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Pushing history";
            setMessage(msg);
            setProgress(0);
            try {
                Context.getApp().sendTaskInit(msg);
                Context.getVersioningManager().assertHasRemoteRepo();
                Context.getInterpreter().setSourceUI(CommandSource.ui); //Ensure authentication dialog comes to local interface
                Context.getVersioningManager().pushToUpstream(allBranches, force, tags);
                msg = "Success pushing to upstream";
                Context.getApp().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                Logger.getLogger(PushUpstream.class.getName()).log(Level.WARNING, null, ex);
                setMessage("Error pushing to upstream");
                Context.getApp().sendError(ex.toString());
                throw ex;
            } finally {
                Context.getApp().sendTaskFinish(msg);
            }
        }
    }

}
