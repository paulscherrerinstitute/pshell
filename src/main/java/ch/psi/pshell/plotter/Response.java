package ch.psi.pshell.plotter;

/**
 *
 */
public class Response {
    public static String UNKNOWN_ERROR = "{\"ret\":null,\"error\":\"Unknown\"}";
    
    public String ret;
    public String error;

    public Response() {
    }

    public Response(String ret, String error) {
        this.ret = ret;
        this.error = error;
    }
}
