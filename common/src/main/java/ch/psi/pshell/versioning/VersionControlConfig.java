package ch.psi.pshell.versioning;

/**
 *
 */
public record VersionControlConfig(
        String localPath, 
        String remotePath, 
        String remoteLogin, 
        boolean autoCommit,
        String[] addedFolders) {
    
};
