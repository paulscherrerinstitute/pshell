package ch.psi.pshell.data;

/**
 *
 */
public class FormatTextDetachedAttrs  extends FormatText{
    @Override
    public String getId() {
        return "txtd";
    }
    
    protected boolean getEmbeddedAttributes(){
        return false;
    }            
}
