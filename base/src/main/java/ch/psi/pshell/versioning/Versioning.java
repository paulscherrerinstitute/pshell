
package ch.psi.pshell.versioning;

import ch.psi.pshell.app.Setup;
import ch.psi.pshell.utils.IO;
import java.io.IOException;

/**
 *
 */
public class Versioning {
    public static String getFileContents(String fileName, String revisionId) throws IOException, InterruptedException {
        fileName = Setup.expandPath(fileName);
        if (IO.isSubPath(fileName, Setup.getHomePath())) {
            fileName = IO.getRelativePath(fileName, Setup.getHomePath());
            try {
                return VersioningManager.getInstance().fetch(fileName, revisionId);
            } catch (IOException | InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
        return null;
    }

    public static Revision getFileRevision(String fileName) throws IOException, InterruptedException{
        Revision ret = null;
        fileName = Setup.expandPath(fileName);
        if (IO.isSubPath(fileName, Setup.getHomePath())) {
            fileName = IO.getRelativePath(fileName, Setup.getHomePath());
            try {
                ret = VersioningManager.getInstance().getRevision(fileName);
                if (VersioningManager.getInstance().getDiff(fileName).length() > 0) {
                    ret.id += " *";
                }
            } catch (IOException | InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
        return ret;
    }
    
    public static String getFileRevisionId(String fileName) throws IOException {
        if (fileName != null) {
            try{
                ch.psi.pshell.versioning.Revision rev =  getFileRevision(fileName);
                if (rev != null) {
                    return rev.id;
                }            
            } catch (Exception ex) {
            }
        }
        return null;
    }
    

}
