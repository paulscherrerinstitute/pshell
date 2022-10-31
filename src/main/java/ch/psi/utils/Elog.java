package ch.psi.utils;

/**
 *
 */
public class Elog {
    public static void log(String logbook, String title, String message, String[] attachments) throws Exception {
        String domain = "";
        String category = "Info";
        String entry = "";
        StringBuffer cmd = new StringBuffer();

        cmd.append("G_CS_ELOG_add -l \"").append(logbook).append("\" ");
        cmd.append("-a \"Author=%s\" ".formatted(Sys.getUserName()));
        cmd.append("-a \"Type=pshell\" ");
        cmd.append("-a \"Entry=").append(entry).append("\" ");
        cmd.append("-a \"Title=").append(title).append("\" ");
        cmd.append("-a \"Category=").append(category).append("\" ");
        cmd.append("-a \"Domain=").append(domain).append("\" ");
        for (String attachment : attachments) {
            cmd.append("-f \"").append(attachment).append("\" ");
        }
        cmd.append("-n 1 ");
        cmd.append("\"").append(message).append("\" ");
        System.out.println(cmd.toString());

        final Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd.toString()});
        new Thread(() -> {
            try {
                process.waitFor();
                int bytes = process.getInputStream().available();
                if (bytes>0){
                    byte[] arr = new byte[bytes];
                    process.getInputStream().read(arr, 0, bytes);
                    System.out.println(new String(arr));
                }
                bytes = process.getErrorStream().available();
                if (bytes>0){
                    byte[] arr = new byte[bytes];
                    process.getErrorStream().read(arr, 0, bytes);
                    System.err.println(new String(arr));
                }
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }).start();
    }    
}
