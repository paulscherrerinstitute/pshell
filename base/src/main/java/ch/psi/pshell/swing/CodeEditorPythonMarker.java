package ch.psi.pshell.swing;

import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.PythonTokenMaker;

/**
 *
 */
public class CodeEditorPythonMarker extends PythonTokenMaker {

    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset, boolean hyperlink) {
        if (tokenType == TokenTypes.IDENTIFIER) {
            int newType = CodeEditor.getExtraTokens().get(array, start, end);
            if (newType > -1) {
                tokenType = newType;
            }
        }
        super.addToken(array, start, end, tokenType, startOffset, hyperlink);
    }

}
