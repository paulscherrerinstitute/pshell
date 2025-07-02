package ch.psi.pshell.pkg;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.pkg.Package;
import ch.psi.pshell.utils.IO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class Package implements AutoCloseable {
    
    final String path;
    final Path scriptPath;
    final Path pluginPath;
    final Path extensionPath;
    final Path configPath;
    final Path devicesPath;    
    final Path devicePoolPath;  
    
    final List<ch.psi.pshell.plugin.Plugin> plugins;

    Package(String path) throws IOException {
        File file = new File(Setup.expandPath(path));
        if (!file.isDirectory()){
            throw new IOException("Invalid package path: " + path);
        }
        this.path = file.getCanonicalPath();
        scriptPath = Paths.get(path, "script");
        devicesPath = Paths.get(path, "devices");
        pluginPath = Paths.get(path, "plugins");
        extensionPath = Paths.get(path, "extensions");
        configPath = Paths.get(path, "config");                                      
        devicePoolPath = Paths.get(configPath.toString(), "devices.properties");          
        plugins = new ArrayList<>();
    }

    public void loadExtensionsFolder() {
        if (extensionPath.toFile().isDirectory()){
            Context.getPluginManager().loadExtensionsFolder(extensionPath.toString());
        }        
    }

    public void open() throws Exception {
        close();
        if (devicePoolPath.toFile().isFile()){
            String devicesConfigPath= devicesPath.toFile().isDirectory() ? devicesPath.toString() : null;
            Context.getDevicePool().initializeExtension(devicePoolPath.toString(), devicesConfigPath);
        }
        if (Context.hasInterpreter() && Context.getInterpreter().isInterpreterEnabled()) {
            if (scriptPath.toFile().isDirectory()){
                Context.getInterpreter().addLibraryPath(scriptPath.toString());                                
                String scriptFile = Setup.getLocalStartupScript();
                if (scriptFile!=null){
                    Path startupScript = Paths.get(scriptFile);
                    if (!startupScript.toFile().isFile()){
                        startupScript = Paths.get(scriptPath.toString(), scriptFile);
                    }
                    if (startupScript.toFile().isFile()){
                        Context.getInterpreter().getScriptManager().evalFile(startupScript.toString());
                        Logger.getLogger(Package.class.getName()).info("Executed package startup script: " + startupScript);                    
                    } else {
                        Logger.getLogger(Package.class.getName()).warning("Invalud package startup script: " + scriptFile);                    
                    }
                }
            }
        }                   
        if (pluginPath.toFile().isDirectory()){
            for (File plugin : IO.listFiles(pluginPath.toFile(), new String[]{"java", "py", "groovy", "jar"})){
                plugins.add(Context.getPluginManager().loadInitializePlugin(plugin));
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (ch.psi.pshell.plugin.Plugin p:plugins){
            try{
                Context.getPluginManager().unloadPlugin(p);
            } catch (Exception ex){                
            }
        }
        plugins.clear();
    }
    
}
