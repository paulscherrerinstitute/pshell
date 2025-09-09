package ch.psi.pshell.versioning;

/**
 *
 */
public interface VersionControlListener {
    default void onCheckout(String branchOrTag) {}
}
