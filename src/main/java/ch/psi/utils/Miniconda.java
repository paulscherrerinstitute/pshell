package ch.psi.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        switch(Sys.getOSFamily()){
            case Windows:
                return "Miniconda3-latest-Windows-x86_64.exe";
            case Linux:
                return "Miniconda3-latest-Linux-x86_64.sh";
            case Mac:
                return "Miniconda3-latest-MacOSX-arm64.sh";
            default:
                throw new RuntimeException("Invalid platform: " + Sys.getOSFamily());
        }        
    }
     
    public static String install(Path folder) throws IOException, InterruptedException{        
        return install(null, folder);
    }

    public static String install(String installer, Path folder) throws IOException, InterruptedException{        
        return install(null, installer,folder);
    }
    
    public static String install(String downloadLink, String installer, Path folder) throws IOException, InterruptedException{        
        String installationPath = getInstallationPath(folder);
        if (downloadLink==null) {
            downloadLink = MINICONDA_DOWNLOAD_LINK;
        }
        if (installer==null){
            installer= getStandardInstaller();
        }
        logger.info("Installing Python on: " + installationPath);        
        
        
        String minicondaDownloadLink = downloadLink +installer;                   
        ProcessBuilder processBuilder;

        if (Sys.isWindows()) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "curl", "-O", minicondaDownloadLink, "&&", "start", installer, "/S", "/D=" + installationPath);
        } else {
            processBuilder = new ProcessBuilder("sh", "-c", "curl -O " + minicondaDownloadLink + " && bash " + installer + " -b -p " + installationPath);
        }

        try{
           return Processing.run(processBuilder);    
        } finally{
            try{
                Files.delete(Paths.get(installer));
            } catch (Exception ex){
                ex.printStackTrace();
                logger.severe(ex.getMessage());
            }
        }       
    }
     
    public static String execute(Path folder, String cmd) throws IOException, InterruptedException{
        logger.info("Executing: " +cmd);
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
        String cmd = "python --version";
        return execute(folder, cmd);        
        
    }
    
    public static void main(String[] args) throws Exception {
        String folder = ((args.length > 0) && (args[0] != null) & (!args[0].isBlank())) ?  args[0] : "~";
        Path path = Paths.get(folder);
        install(path);
        installPackages(path, getDefaultPackages());              
        pipInstallPackage(path, "jep");
    }
}
