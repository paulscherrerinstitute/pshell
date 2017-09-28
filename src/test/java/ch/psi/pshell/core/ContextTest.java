package ch.psi.pshell.core;

import ch.psi.pshell.scripting.ScriptType;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 */
public class ContextTest {
    
    @Test
    public void testInterpreter() throws Exception {
        Context.createInstance().start();
        try{
            System.out.println(Context.getInstance().evalLine("1+1")); 
        } finally {
            Context.getInstance().close();
        }
    }

    @Test
    public void listScriptEngines() {
        ScriptEngineManager mgr = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = mgr.getEngineFactories();
        for (ScriptEngineFactory factory : factories) {
            try {
                ScriptEngine engine = factory.getScriptEngine();
            } catch (Throwable t) {
                System.err.println(t);
            }
            System.out.println("ScriptEngineFactory Info");
            String engName = factory.getEngineName();
            String engVersion = factory.getEngineVersion();
            String langName = factory.getLanguageName();
            String langVersion = factory.getLanguageVersion();
            List<String> extensions = factory.getExtensions();
            System.out.printf("\tScript Engine: %s (%s)%n", engName, engVersion);
            List<String> engNames = factory.getNames();
            for (String name : engNames) {
                System.out.printf("\tEngine Alias: %s%n", name);
            }
            System.out.printf("\tLanguage: %s (%s)%n", langName, langVersion);

            for (String ext : extensions) {
                System.out.printf("\tExtension: %s", ext);
            }
            System.out.println();
        }

    }

}
