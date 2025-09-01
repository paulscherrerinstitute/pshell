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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Python {    
    public static final String BASE_URL = "https://www.python.org/ftp/python/";
    public static final String DEFAULT_VERSION = "3.13.7";
    
    
    static Logger logger = Logger.getLogger(Python.class.getName());

    public static String getInstallationPath(Path folder){
        return Setup.expandPath(folder.toString());
    }

    public static String[] getDefaultPackages(){
        return new String[] {"numpy", "scipy","pyarrow", "pandas", "numba", "matplotlib", "pillow", "requests", "scikit-image", "h5py", "pyepics"};
    }        
     
     
    public static String getInstaller(String version){
        OSFamily os = Sys.getOSFamily();
        Arch arch = Sys.getArch();              
        switch(os){
            case Windows:
                if (arch==Arch.X86_64){
                    return "python-" + version + "-amd64.exe";
                } else if (arch==Arch.X86){
                    return "python-" + version + ".exe";
                }
                break;
            case Linux:
                return "Python-" + version + ".tgz";
            case Mac:
                //return "python-" + VERSION + "-macos11.pkg";
                return "Python-" + version + ".tgz";
        }     
        throw new RuntimeException("Unsupported system - os:" + os + " arch:" + arch);
    }
    
    public static void install(Path folder) throws IOException, InterruptedException{        
        install(DEFAULT_VERSION, folder);
    }

    
    public static void install(String version, Path folder) throws IOException, InterruptedException{        
        String installationPath = getInstallationPath(folder);
        String currentInstall = getVersion(folder);
        if (!currentInstall.isEmpty()){
            throw new IOException(currentInstall + " already installed in " + folder);
        }
        new File(installationPath).mkdirs();
        
        String baseUrl = BASE_URL + version + "/";                       
        String filename = getInstaller(version);
        String url = baseUrl + filename;

        Path downloadsFolder = Paths.get(Setup.getCachePathDownloads());
        downloadsFolder.toFile().mkdirs();

        
        Path installationFile = Paths.get(Setup.getCachePathDownloads(), filename);        
        logger.log(Level.INFO, "Downloading Python from: " + url + " to: " + installationFile);        
        if (!installationFile.toFile().isFile()){            
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
        
        String prefix = IO.getPrefix(installationFile.toFile());
        Path sourceFolder  = downloadsFolder.resolve(prefix);
        Path buildFolder = downloadsFolder.resolve(prefix+ "-build");        
        if (buildFolder.toFile().exists()){
            IO.deleteRecursive(buildFolder);
        }
        Files.createDirectories(buildFolder);
        try{
            String installerFileName = installationFile.toAbsolutePath().toString();              

            OSFamily os = Sys.getOSFamily();
            Arch arch = Sys.getArch();    

            switch(os){
                case Windows:
                    logger.log(Level.INFO, "Installing " + installerFileName + " to: " + installationPath);        
                    runProcess(installerFileName, "/quiet", "TargetDir=" + installationPath);
                    break;
                case Mac:
                case Linux:
                    logger.log(Level.INFO, "Extracting " + installerFileName + " to: " + downloadsFolder);        
                    runProcess("tar", "-xzf", installerFileName, "-C", downloadsFolder.toString());
                    
                    List<String> configureCmd = new ArrayList<>();
                    configureCmd.add(sourceFolder.resolve("configure").toString());
                    configureCmd.add("--prefix=" + installationPath);
                    configureCmd.add("--enable-optimizations");                    
                    if  (!hasOpenSSLHeaders()){
                        logger.log(Level.INFO, "OpenSSL headers not detected");
                        Path sslFodler = installOpenSSL(Paths.get(installationPath));
                        configureCmd.add("--with-openssl=" + sslFodler.toString());
                    }
                    configureCmd.add("--enable-shared"); // <<-- needed for JEP                     
                    String ldFlags = "";
                    if (os==Linux){                        
                        // Check for zlib
                        if (!hasZlibHeaders()) {
                            logger.log(Level.INFO, "Zlib headers not detected");
                            Path zlibFolder = installZlib(Paths.get(installationPath));
                            // Tell configure where to find it
                            configureCmd.add("CPPFLAGS=-I" + zlibFolder.resolve("include"));
                            if (arch != Arch.ARM64){                                
                                ldFlags = "-L" + zlibFolder.resolve("lib") + " ";
                            }
                        }                                                                                                    
                        
                    }
                    configureCmd.add("--enable-shared"); // <<-- needed for JEP                                         
                    ldFlags += "-Wl,-rpath," + installationPath + "/lib";
                    //ldFlags += "-Wl,-rpath,@loader_path/../lib";
                    configureCmd.add("LDFLAGS=" + ldFlags);                    
                    
                    logger.log(Level.INFO, "Building Python to:" + installationPath + " with config: " + Str.toString(configureCmd));                
                    //runProcess(buildFolder, sourceFolder.resolve("configure").toString(), "--prefix=" + installationPath, "--enable-optimizations");
                    runProcess(buildFolder, configureCmd.toArray(new String[0]));                    
                    runProcess(buildFolder, "make");
                    runProcess(buildFolder, "make", "install"); 
                                        
                    try{
                        //Make sure there is a python link to python3
                        Path binDir = Paths.get(installationPath).resolve("bin");
                        Path target = binDir.resolve("python3");   // the real executable
                        Path link = binDir.resolve("python");      // the symlink you want to create
                        if (!Files.exists(link)){
                            logger.log(Level.INFO, "Creating python link to python3");  
                            try {
                                Files.createSymbolicLink(link, target.getFileName());            

                            } catch (Exception ex){
                                logger.log(Level.WARNING, null, ex); 
                            }        
                        }
                    } catch (Exception ex){
                        logger.log(Level.WARNING, null, ex); 
                    }       
                    break;
                }
            try{
                //Update pip
                logger.log(Level.INFO, "Trying to upgarde pip");  
                execute (Paths.get(installationPath), "python -m pip install --upgrade pip");                                                            
            } catch (Exception ex){
                logger.log(Level.WARNING, null, ex); 
            }                    

            Files.delete(Paths.get(installerFileName));
            IO.deleteRecursive(sourceFolder);
            IO.deleteRecursive(buildFolder);
        } finally {
        }
    }
    
    private static boolean hasOpenSSLHeaders() {
        Path[] possibleDirs = {
            Paths.get("/usr/include/openssl"),
            Paths.get("/usr/local/include/openssl")
        };
        for (Path p : possibleDirs) {
            if (Files.isDirectory(p)) return true;
        }
        return false;
    }    
    
    
    public static Path installOpenSSL(Path folder) throws IOException, InterruptedException{        
        return installOpenSSL("3.3.2", folder);
    }
    
    public static Path installOpenSSL(String version, Path folder) throws IOException, InterruptedException{   
        folder = Paths.get(getInstallationPath(folder));
        String fileName = "openssl-" + version;
        Path opensslSrc = folder.resolve(fileName);
        Files.createDirectories(opensslSrc.getParent());

        // Download tarball
        Path installationFile = Paths.get(Setup.getCachePathDownloads(), fileName + ".tar.gz");                
        if (!Files.exists(installationFile)) {
            String url = "https://www.openssl.org/source/" + fileName + ".tar.gz";
            logger.log(Level.INFO, "Downloading OpenSSL from: " + url + " to: " + installationFile);        
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.copy(in, installationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        folder.toFile().mkdirs();

        // Extract
        runProcess(folder, "tar", "-xzf", installationFile.toString());

        // Build in user folder        
        Path installFolder = folder.resolve("openssl");
        logger.log(Level.INFO, "Building OpenSSL to:" + installFolder);    
        runProcess(opensslSrc, "./Configure", "--prefix=" + installFolder.toString(), "no-shared");
        runProcess(opensslSrc, "make", "-j" + Runtime.getRuntime().availableProcessors());
        runProcess(opensslSrc, "make", "install_sw");
        Files.delete(installationFile);
        IO.deleteRecursive(opensslSrc);
        return installFolder; // contains include/ and lib/
    }
    
    
private static boolean hasZlibHeaders() {
    Path[] possibleDirs = {
        Paths.get("/usr/include"),
        Paths.get("/usr/local/include")
    };
    for (Path dir : possibleDirs) {
        if (Files.exists(dir.resolve("zlib.h"))) {
            return true;
        }
    }
    return false;
}

    public static Path installZlib(Path folder) throws IOException, InterruptedException {
        return installZlib("1.3.1", folder);
    }

    public static Path installZlib(String version, Path folder) throws IOException, InterruptedException {
        folder = Paths.get(getInstallationPath(folder));
        String fileName = "zlib-" + version;
        Path zlibSrc = folder.resolve(fileName);
        Files.createDirectories(zlibSrc.getParent());

        // Download tarball
        Path installationFile = Paths.get(Setup.getCachePathDownloads(), fileName + ".tar.gz");
        if (!Files.exists(installationFile)) {
            String url = "https://zlib.net/" + fileName + ".tar.gz";
            logger.log(Level.INFO, "Downloading zlib from: " + url + " to: " + installationFile);
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.copy(in, installationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        folder.toFile().mkdirs();

        // Extract
        runProcess(folder, "tar", "-xzf", installationFile.toString());

        // Build in user folder (static only)
        Path installFolder = folder.resolve("zlib");
        logger.log(Level.INFO, "Building static zlib to:" + installFolder);
   
        Map env = new HashMap();
        if (Sys.getArch() == Arch.ARM64){
            env.put("CFLAGS", "-fPIC");  // set PIC for ARM64, required for Linux           
        }
        runProcess(zlibSrc, env, "./configure", "--prefix=" + installFolder.toString(), "--static");                
        runProcess(zlibSrc, env, "make", "-j" + Runtime.getRuntime().availableProcessors());
        runProcess(zlibSrc, env, "make", "install");

        // Cleanup
        Files.delete(installationFile);
        IO.deleteRecursive(zlibSrc.toFile());

        return installFolder; // contains include/ and lib/
    }  
    
    
    private static void runProcess(String... command) throws IOException, InterruptedException {
        runProcess (null, command);
    }    
    
    private static void runProcess(Path workingDir, String... command) throws IOException, InterruptedException {
        runProcess (workingDir, null, command);
    }
    
    private static void runProcess(Path workingDir, Map<String,String> env, String... command) throws IOException, InterruptedException {
        logger.log(Level.INFO, "Running: " + String.join(" ", command) + " on folder: " + Str.toString(workingDir));
        ProcessBuilder pb = new ProcessBuilder(command);
        if (env!=null){
            logger.log(Level.INFO, "With env: " + Str.toString(env));
            Map<String, String> environment = pb.environment();
            environment.putAll(env);            
        }
        if (workingDir!=null){
            pb.directory(workingDir.toFile());  // <-- set working directory
        }
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Process failed with exit code " + exit);
        }
    }    
    
    public static String execute(Path folder,String cmd) throws IOException, InterruptedException{
        return execute(folder,null, cmd);
    }
    
    public static String execute(Path folder, Map<String,String> env, String cmd) throws IOException, InterruptedException{
        logger.log(Level.INFO, "Executing: {0}", cmd);
        String installationPath = getInstallationPath(folder);
        ProcessBuilder processBuilder;       
        if (Sys.isWindows()) {            
            String path = installationPath + "\\";
            if (!cmd.startsWith("python")){
                path +=  "Scripts\\";
            }
            processBuilder = new ProcessBuilder("cmd.exe", "/c", path + cmd);
        } else {            
            if (cmd.startsWith("python ")) {
                cmd = cmd.replaceFirst("^python", "python3");
            }            
            processBuilder = new ProcessBuilder("sh", "-c", installationPath + "/bin/" + cmd);
        }   
        if (env!=null){
            logger.log(Level.INFO, "With env: " + Str.toString(env));
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(env);            
        }        
        return Processing.run(processBuilder);
    }
    
    public static String installPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return installPackages(folder, new String[]{pkg});
    }

    public static String installPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
        return installPackages(folder, null, pkgs);
    }

    public static String installPackages(Path folder, Map<String,String> env, String[] pkgs) throws IOException, InterruptedException{
        return installPackages(folder, env, null, pkgs);
    }

    public static String installPackages(Path folder, Map<String,String> env, String options, String[] pkgs) throws IOException, InterruptedException{
        options = (options==null) ? "" : (options + " ");
        String cmd = "pip3 install " +  options + String.join(" ", pkgs);
        return execute(folder, env, cmd);
    }

    public static String uninstallPackage(Path folder, String pkg) throws IOException, InterruptedException{
        return uninstallPackages(folder, new String[]{pkg});
    }

    public static String uninstallPackages(Path folder, String[] pkgs) throws IOException, InterruptedException{
        return uninstallPackages(folder, null, pkgs);
     }

    public static String uninstallPackages(Path folder, Map<String,String> env, String[] pkgs) throws IOException, InterruptedException{
        String cmd = "pip3 uninstall " +  String.join(" ", pkgs);
        return execute(folder, env, cmd);
     }
    
    public static String installJep(Path folder, String java_home) throws IOException, InterruptedException{
        installPackages(folder, new String[]{"setuptools", "wheel"});
        Map env = new HashMap();
        env.put("JAVA_HOME", java_home); 
        env.put("PYTHONHOME",  folder.toString());                
        
        String includeDir = folder.resolve("include").toString();

        String libDir = folder.resolve("lib").toString();
        env.put("LDFLAGS", "-Wl,-rpath," + libDir);
        //env.put("LDFLAGS", "-Wl,-rpath,@loader_path/../lib");                
        env.put("CPPFLAGS", "-I" + includeDir);        
        
        String options = "--no-build-isolation --force-reinstall --no-cache-dir";
    
        return installPackages(folder, env, options, new String[]{"jep"});
    }    

    public static String getVersion(Path folder) {        
        try {
            String cmd = "python --version";
            String version = execute(folder, cmd);        
            return (version == null) ? "" : version;
        } catch (Exception ex) {
            return "";
        }
    }
    
    public static String[] getInstalledPackages(Path folder) {        
        var list = new ArrayList<String>();
        try {            
            String cmd = "pip3 list";
            String ret = execute(folder, cmd);        
            if (ret != null){
                String[] rows = ret.split("\\r?\\n");
                for (int i=2; i< rows.length; i++){
                    String row = rows[i].trim();
                    if (!row.isEmpty()){           
                        list.add(row);
                        /*
                        if (!row.contains(" ")){
                            list.add(row);
                        } else {
                            String name = row.substring(0, row.indexOf(" "));
                            String ver = row.substring(row.lastIndexOf(" ")).trim();
                            list.add(name+"=="+ver);
                        }
                        */
                    }
                }
            }            
        } catch (Exception ex) {            
        }
        return list.toArray(new String[0]);
    }
    
    
    public static boolean isInstalled(Path folder){
        try{
            if (folder.toFile().isDirectory()){
                String version = getVersion(folder);
                return (version == null) ? false : true;                
            }
        } catch (Exception ex){            
        }
        return false;
    }     
           
    public static void main(String[] args) throws Exception {
        //String folder = ((args.length > 0) && (args[0] != null) & (!args[0].isBlank())) ?  args[0] : "~";
        String folder = "~/temp/cpython";
        Path path = Paths.get(folder);
        if (!hasZlibHeaders()){
            Path zlibPath  = installZlib(path);
             System.out.println(zlibPath);
        }
        
        install(path);
        installPackages(path, getDefaultPackages());              
        installPackage(path, "jep");             
        System.out.println(getVersion(path));
    }
}
