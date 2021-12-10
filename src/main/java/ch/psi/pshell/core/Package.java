package ch.psi.pshell.core;

import ch.psi.utils.IO;
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
    
    final List<Plugin> plugins;

    Package(Setup  setup, String path) throws IOException {
        File file = new File(setup.expandPath(path));
        if (!file.isDirectory()){
            throw new IOException("Invalid pachage path: " + path);
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
            Context.getInstance().getPluginManager().loadExtensionsFolder(extensionPath.toString());
        }        
    }

    public void open() throws Exception {
        close();
        if (devicePoolPath.toFile().isFile()){
            String devicesConfigPath= devicesPath.toFile().isDirectory() ? devicesPath.toString() : null;
            Context.getInstance().getDevicePool().initializeExtension(devicePoolPath.toString(), devicesConfigPath);
        }
        if (Context.getInstance().isInterpreterEnabled()) {
            if (scriptPath.toFile().isDirectory()){
                Context.getInstance().getScriptManager().addPythonPath(Context.getInstance().getSetup().expandPath(scriptPath.toString()));                                
                String scriptFile = Context.getInstance().getSetup().getLocalStartupScript();
                Path startupScript = Paths.get(scriptPath.toString(), scriptFile);
                if (startupScript.toFile().isFile()){
                    Context.getInstance().getScriptManager().evalFile(startupScript.toString());
                     Logger.getLogger(Package.class.getName()).info("Executed package startup script: " + startupScript);                    
                }
            }
        }                   
        if (pluginPath.toFile().isDirectory()){
            for (File plugin : IO.listFiles(pluginPath.toFile(), new String[]{"java", "py", "groovy", "jar"})){
                plugins.add(Context.getInstance().getPluginManager().loadInitializePlugin(plugin));
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (Plugin p:plugins){
            try{
                Context.getInstance().getPluginManager().unloadPlugin(p);
            } catch (Exception ex){                
            }
        }
        plugins.clear();
    }
    
}
