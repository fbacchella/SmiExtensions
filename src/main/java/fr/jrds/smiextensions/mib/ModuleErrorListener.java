package fr.jrds.smiextensions.mib;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ModuleErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
            Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        String sourceFileName;
        if (e != null) {
            sourceFileName = e.getInputStream().getSourceName();
        } else if (recognizer != null) {
            sourceFileName = recognizer.getInputStream().getSourceName();
        } else {
            sourceFileName = "UNKNOWN FILE";
        }
        //throw new ModuleException(msg, sourceFileName, line, charPositionInLine);
    }

}
