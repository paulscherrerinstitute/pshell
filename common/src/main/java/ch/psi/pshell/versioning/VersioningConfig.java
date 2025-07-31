package ch.psi.pshell.versioning;

/**
 *
 */
public record VersioningConfig(
        String localPath, 
        String remotePath, 
        String remoteLogin, 
        boolean autoCommit,
        String[] addedFolders) {
    
};
