package ch.psi.pshell.data;

/**
 *
 */
public class FormatTextEmbeddedAttrs  extends FormatText{
    @Override
    public String getId() {
        return "txte";
    }
    
    protected boolean getEmbeddedAttributes(){
        return true;
    }            
}
