package ch.psi.pshell.device;

/**
 * Device access can be read only, write only and read/write. If device method is called with
 * invalid access type, an exception must be thrown.
 */
public enum AccessType {

    Read,
    Write,
    ReadWrite
}
