package ch.psi.pshell.versioning;

/**
 *
 */
public interface VersioningListener {
    default void onCheckout(String branchOrTag) {}
}
