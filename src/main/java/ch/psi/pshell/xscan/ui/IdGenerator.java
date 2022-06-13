package ch.psi.pshell.xscan.ui;

/**
 *
 */
public class IdGenerator {

    public static String generateId() {
        long millis = System.currentTimeMillis();
        int digits = 1000000;
        String id = String.format("id%06.0f", (millis - (Math.floor(millis / digits) * digits)));
        return (id);
    }

}
