package ch.psi.pshell.swing;

import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.GroovyTokenMaker;

/**
 *
 */
public class CodeEditorGroovyMarker extends GroovyTokenMaker {

    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset, boolean hyperlink) {
        if (tokenType == TokenTypes.IDENTIFIER) {
            int newType = CodeEditorExtraTokens.extraTokens.get(array, start, end);
            if (newType > -1) {
                tokenType = newType;
            }
        }
        super.addToken(array, start, end, tokenType, startOffset, hyperlink);
    }

}
