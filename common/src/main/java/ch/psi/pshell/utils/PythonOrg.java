package ch.psi.pshell.utils;

import ch.psi.pshell.app.Setup;
import ch.psi.pshell.utils.Sys.Arch;
import ch.psi.pshell.utils.Sys.OSFamily;
import static ch.psi.pshell.utils.Sys.OSFamily.Linux;
import static ch.psi.pshell.utils.Sys.OSFamily.Mac;
import static ch.psi.pshell.utils.Sys.OSFamily.Windows;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PythonOrg {    
    private static final String BASE_URL = "https://www.python.org/ftp/python/";
    private static final String VERSION = "3.13.7";
    
    
    static Logger logger = Logger.getLogger(PythonOrg.class.getName());

    public static String getInstallationPath(Path folder){
        if (folder.toString().startsWith("~")){
            folder = Paths.get(folder.toString().replaceFirst("^~", Sys.getUserHome()));
        }
        return folder.toAbsolutePath().toString();
    }

    public static String[] getDefaultPackages(){
        return new String[] {"numpy", "scipy","pyarrow", "pandas", "numba", "matplotlib", "pillow", "requests", "scikit-image", "h5py", "pyepics"};
    }        
     
     
    public static String getStandardInstaller(){
        OSFamily os = Sys.getOSFamily();
        Arch arch = Sys.getArch();              
        switch(os){
            case Windows:
                if (arch==Arch.X86_64){
                    return "python-" + VERSION + "-amd64.exe";
                } else if (arch==Arch.X86){
                    return "python-" + VERSION + ".exe";
                }
                break;
            case Linux:
                return "Python-" + VERSION + ".tgz";
            case Mac:
                return "python-" + VERSION + "-macos11.pkg";
        }     
        throw new RuntimeException("Unsupported system - os:" + os + " arch:" + arch);
    }
    
    public static void install(Path folder) throws IOException, InterruptedException{        
        install(VERSION, folder);
    }

    
    public static void install(String version, Path folder) throws IOException, InterruptedException{        
        String installationPath = getInstallationPath(folder);
        new File(installationPath).mkdirs();
        
        String baseUrl = BASE_URL + version + "/";                       
        String filename = getStandardInstaller();
        String url = baseUrl + filename;
        
        logger.log(Level.INFO, "Downloading Python from: " + baseUrl);        
        
        Path installationFile = Paths.get( Setup.expandPath("{context}"), filename);
        if (!installationFile.toFile().isFile()){
            System.out.println("Downloading Python from " + url + " to " + installationFile);
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.copy(in, installationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!installationFile.toFile().isFile()){
                throw new IOException("Error downloading installer from " + url);
            }
            if (!Sys.isWindows()){
                try {
                    installationFile.toFile().setExecutable(true, false); 
                } catch (Exception e) {
                    throw new IOException("Could not set executable permission: " + e.getMessage());
                }
            }          
        }
        
        String installerFileName = installationFile.toString();       
        logger.log(Level.INFO, "Installing Python on: " + installationPath + " with installer: " + installerFileName);        

        OSFamily os = Sys.getOSFamily();
        Arch arch = Sys.getArch();    
                
        switch(os){
            case Windows:
                runProcess(installerFileName, "/quiet", "TargetDir=" + installationPath);
                break;
            case Mac:
                runProcess( "installer", "-pkg", installerFileName, "-target", installationPath);
                break;
            case Linux:
                runProcess("tar", "-xzf", installerFileName, "-C", installationPath);
                break;
        }     
        
    }
    
    private static void runProcess(String... cmd) throws IOException, InterruptedException {
        System.out.println("Running: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Process failed with exit code " + exit);
        }
    }    
    
    public static String execute(Path folder, String cmd) throws IOException, InterruptedException{
        logger.log(Level.INFO, "Executing: {0}", cmd);
        String installationPath = getInstallationPath(folder);
        ProcessBuilder processBuilder;
        if (Sys.isWindows()) {            
            String path = installationPath + "\\";
            if (!cmd.startsWith("python")){
                path +=  "Scripts\\";
            }
            processBuilder = new ProcessBuilder(
                "cmd.exe", "/c", path + cmd
            );
        } else {
            processBuilder = new ProcessBuilder(
                "sh", "-c", 
               installationPath + "/bin/" + cmd);
        }   
        return Processing.run(processBuilder);
    }
    
    public static String installPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return installPackages(folder, new String[]{pkg});
    }

    public static String installPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
        String cmd = "install " +  String.join(" ", pkgs);
        return execute(folder, cmd);
    }

    public static String uninstallPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return uninstallPackages(folder, new String[]{pkg});
    }

    public static String uninstallPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
        String cmd = "pip uninstall " +  String.join(" ", pkgs);
        return execute(folder, cmd);
     }

    public static String getVersion(Path folder) throws IOException, InterruptedException{        
        try {
            String cmd = "python --version";
            String version = execute(folder, cmd);        
            return (version == null) ? "" : version;
        } catch (Exception ex) {
            return "";
        }
    }
    
    public static void main(String[] args) throws Exception {
        //String folder = ((args.length > 0) && (args[0] != null) & (!args[0].isBlank())) ?  args[0] : "~";
        String folder = "~/temp/cpython";
        Path path = Paths.get(folder);
        install(path);
        installPackages(path, getDefaultPackages());              
        installPackage(path, "jep");
    }
}
