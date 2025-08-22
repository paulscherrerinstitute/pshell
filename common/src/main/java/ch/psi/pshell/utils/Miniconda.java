package ch.psi.pshell.utils;

import ch.psi.pshell.app.Setup;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys.Arch;
import ch.psi.pshell.utils.Sys.OSFamily;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
Installers:
Miniconda3-latest-Windows-x86_64.exe	
Miniconda3-latest-MacOSX-x86_64.sh	
Miniconda3-latest-MacOSX-x86_64.pkg	
Miniconda3-latest-MacOSX-arm64.sh	
Miniconda3-latest-MacOSX-arm64.pkg	
Miniconda3-latest-Linux-x86_64.sh	
Miniconda3-latest-Linux-s390x.sh	
Miniconda3-latest-Linux-aarch64.sh	
Miniconda3-latest-Linux-ppc64le.sh	
Miniconda3-latest-Windows-x86.exe	
Miniconda3-latest-Linux-x86.sh	
Miniconda3-latest-MacOSX-x86.sh
Miniconda3-latest-Linux-armv7l.sh
*/

public class Miniconda {
    public static String MINICONDA_DOWNLOAD_LINK = "https://repo.anaconda.com/miniconda/";
    static Logger logger = Logger.getLogger(Miniconda.class.getName());

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
        String suffix = null;        
        switch(os){
            case Windows:
                if (arch==Arch.X86_64){
                    suffix = "Windows-x86_64.exe";
                } else if (arch==Arch.X86){
                    suffix = "Windows-x64.exe";
                }
                break;
            case Linux:
                if (arch==Arch.X86_64){
                    suffix = "Linux-x86_64.sh";
                } else if (arch==Arch.ARM64){
                    suffix = "Linux-aarch64.sh";
                } else if (arch==Arch.X86){
                    suffix = "MacOSX-x64.sh";
                }
                break;
            case Mac:
                if (arch==Arch.X86_64){
                    suffix = "MacOSX-x86_64.sh";
                } else if (arch==Arch.ARM64){
                    suffix = "MacOSX-arm64.sh";
                } else if (arch==Arch.X86){
                    suffix = "MacOSX-x64.sh";
                }
                break;
        }        
        if (suffix!=null){
            return "Miniconda3-latest-" + suffix;
        }
        throw new RuntimeException("Unsupported system - os:" + os + " arch:" + arch);
    }
     
    public static String install(Path folder) throws IOException, InterruptedException{        
        return install(null, folder);
    }

    public static String install(String installer, Path folder) throws IOException, InterruptedException{        
        return install(null, installer,folder);
    }
    
    public static String install(String downloadLink, String installer, Path folder) throws IOException, InterruptedException{        
        String installationPath = getInstallationPath(folder);
        new File(installationPath).getParentFile().mkdirs();
        if (downloadLink==null) {
            downloadLink = installer;
        }
        if (installer==null){
            installer= getStandardInstaller();
        }
        
        String minicondaDownloadLink = downloadLink +installer;                   
        
        logger.log(Level.INFO, "Downloading Python from: " + minicondaDownloadLink);        
                
        Path installationFile = Paths.get( Setup.expandPath("{context}"), installer);
        if (!installationFile.toFile().isFile()){
            String installerFileName = installationFile.toString();
            try (InputStream in = new URI(minicondaDownloadLink).toURL().openStream()) {
                Files.copy(in, installationFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (URISyntaxException ex){
                throw new IOException(ex);
            }
        }
        if (!installationFile.toFile().isFile()){
            throw new IOException("Error downloading installer from " + minicondaDownloadLink);
        }
        
        if (!Sys.isWindows()){
            try {
                installationFile.toFile().setExecutable(true, false); 
            } catch (Exception e) {
                throw new IOException("Could not set executable permission: " + e.getMessage());
            }
        }
        
        String installerFileName = installationFile.toString();
        
        logger.log(Level.INFO, "Installing Python on: " + installationPath + " with installer: " + installerFileName);        
        
        ProcessBuilder processBuilder;        
        if (Sys.isWindows()) {
            processBuilder = new ProcessBuilder(installerFileName, "/S", "/D=" + installationPath);
        } else {
            processBuilder = new ProcessBuilder("bash", installerFileName, "-b", "-p", installationPath);
        }

        try{
           String ret =  Processing.run(processBuilder);    
            if (getVersion(new File(installationPath).toPath()).isEmpty()){
                throw new IOException("Python installation failed");
            }           
           return ret;
        } finally{
            try{
                Files.delete(Paths.get(installerFileName));
            } catch (Exception ex){
                ex.printStackTrace();
                logger.severe(ex.getMessage());
            }
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
        return installPackages(folder, "conda-forge", pkgs);
    }

    
    public static String installPackage(Path folder, String channel, String pkg) throws IOException, InterruptedException{
        return installPackages(folder, channel, new String[]{pkg});
    }
    
    public static String installPackages(Path folder, String channel, String[] pkgs) throws IOException, InterruptedException{
        String cmd = "conda install -y -c " + channel + " " +  String.join(" ", pkgs);
        return execute(folder, cmd);
    }

    public static String uninstallPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return uninstallPackages(folder, new String[]{pkg});
    }
    
    public static String uninstallPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
        String cmd = "conda uninstall -y " +  String.join(" ", pkgs);
        return execute(folder, cmd);
     }

    public static String pipInstallPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return pipInstallPackages(folder, new String[]{pkg});
    }

    public static String pipInstallPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
        String cmd = "pip install " +  String.join(" ", pkgs);
        return execute(folder, cmd);
    }

    public static String pipUninstallPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return pipUninstallPackages(folder, new String[]{pkg});
    }

    public static String pipUninstallPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
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
        String folder = ((args.length > 0) && (args[0] != null) & (!args[0].isBlank())) ?  args[0] : "~";
        Path path = Paths.get(folder);
        install(path);
        installPackages(path, getDefaultPackages());              
        pipInstallPackage(path, "jep");
    }
}
