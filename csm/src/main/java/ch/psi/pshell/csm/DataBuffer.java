package ch.psi.pshell.csm;

import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.versioning.VersionControl;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 *
 */
public class DataBuffer {
    static String dataSourcesRepoFolder;
    
    public static Git cloneDataSourcesRepo(boolean imageBuffer) throws GitAPIException, IOException{
        CredentialsProvider credentialsProvider = getCredentialsProvider();
        String path = getDataSourcesRepoFolder(imageBuffer);
        File gitFile =  new File(path + "/.git");
        if (!gitFile.exists()){
            String url = imageBuffer ? App.getImageBufferSourcesRepo() : App.getDataBufferSourcesRepo();
            Logger.getLogger(DataBuffer.class.getName()).info("Cloning data sources repo: " + url + " to " + path);
            Git.cloneRepository()
              .setURI(url)
              .setDirectory(new File(path)) 
              .setCredentialsProvider(credentialsProvider)
              .call();
        }        
        Logger.getLogger(DataBuffer.class.getName()).info("Pulling data sources: " + gitFile);
        Git git = Git.open(gitFile);      
        git.pull().setCredentialsProvider(credentialsProvider).call(); 
        return git;
    }
    
    static CredentialsProvider getCredentialsProvider(){
        return VersionControl.getDefaultCredentialsProvider();
    }
    static CredentialsProvider getCredentialsProvider(String usr, String pwd){
        return new UsernamePasswordCredentialsProvider(usr, pwd);
    }
    
    public static void commitRepo(Git git, String msg) throws GitAPIException, IOException{
        git.commit().setAll(true).setMessage(msg).call();
    }    
    //8f52060a88599bdce87b14658e4d722e90f7e535
    

    
    public static void pushRepo(Git git, String usr, String pwd) throws GitAPIException, IOException{
        git.push().setCredentialsProvider(getCredentialsProvider(usr, pwd)).setForce(true).call();
    }        

    public static void checkCredentials(Git git, String usr, String pwd) throws GitAPIException, IOException{
        git.push().setDryRun(true).setCredentialsProvider(getCredentialsProvider(usr, pwd)).setForce(true).call();
    }        
    
    public static Git updateDataSourcesRepo(boolean imageBuffer) throws GitAPIException, IOException{        
        try{
            return cloneDataSourcesRepo(imageBuffer);
        } catch (Exception ex){
            Logger.getLogger(DataBuffer.class.getName()).log(Level.WARNING, null, ex);
            System.out.println(ex);
            String path = getDataSourcesRepoFolder(imageBuffer);
            Logger.getLogger(DataBuffer.class.getName()).severe("Deleting data sources repo: " + path);
            IO.deleteRecursive(path);
            return cloneDataSourcesRepo(imageBuffer);
        }
    }
    

    public static String getDataSourcesRepoFolder(boolean imageBuffer) throws IOException{
        String path = getDataSourcesRepoFolder();
        return path + "/" + (imageBuffer ? "ib" : "db");
    }
    
    public static String getDataSourcesRepoFolder() throws IOException{
        if (dataSourcesRepoFolder == null){
            Path tempDir = Files.createTempDirectory("csm");
            IO.deleteFolderOnExit(tempDir.toFile());
            dataSourcesRepoFolder = tempDir.toString();
        }
        return dataSourcesRepoFolder;
        //return Sys.getUserHome() + "/.csm";
    }

    public static String reconnectDataBufferCameraSources(String cameraName) throws IOException, InterruptedException, GitAPIException{
        return reconnectCameraSources(cameraName, false);
    }
    public static String reconnectImageBufferCameraSources(String cameraName) throws IOException, InterruptedException, GitAPIException{
        return reconnectCameraSources(cameraName, true);
    }
    
    static String reconnectCameraSources(String cameraName, boolean imageBuffer) throws IOException, InterruptedException, GitAPIException{
        updateDataSourcesRepo(imageBuffer);
        Logger.getLogger(DataBuffer.class.getName()).info("Reconnecting camera  to " +  (imageBuffer ? "ImageBuffer: " : "DataBuffer: ") + cameraName);
        //String command = "./bufferutils restart --label " + cameraName;
        
        List<String> pars = new ArrayList<>();
        
        pars.add("./bufferutils");
        pars.add("restart");
        pars.add("--label");        
        pars.add(cameraName);
                
        ProcessBuilder pb = new ProcessBuilder(pars);
        pb.directory(new File(getDataSourcesRepoFolder(imageBuffer)));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();

        BufferedReader reader;
        StringBuilder builder;
        String line = null;
        
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        builder = new StringBuilder();
        while ( (line = reader.readLine()) != null) {
            builder.append(line).append(Sys.getLineSeparator());
        }
        String output = builder.toString();
        return  output;        
    }
    
    static String uploadSources(boolean imageBuffer) throws IOException, InterruptedException, GitAPIException{        
        try(Git git = updateDataSourcesRepo(imageBuffer)){
            Logger.getLogger(DataBuffer.class.getName()).info("Uploading sources to " +  (imageBuffer ? "ImageBuffer" : "DataBuffer"));

            List<String> pars = new ArrayList<>();

            pars.add("./bufferutils");
            pars.add("upload");

            ProcessBuilder pb = new ProcessBuilder(pars);
            pb.directory(new File(getDataSourcesRepoFolder(imageBuffer)));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();

            BufferedReader reader;
            StringBuilder builder;
            String line = null;

            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            builder = new StringBuilder();
            while ( (line = reader.readLine()) != null) {
                builder.append(line).append(Sys.getLineSeparator());
            }
            String output = builder.toString();
            return  output;        
        }        
    }
    
    
    public static class CameraInfo{
        String name;
        String url;
        String group;
        boolean enabled;     
        
        CameraInfo( String name, String url, String group, boolean enabled){
            this.name=name;
            this.url=url;
            this.enabled=enabled;
            this.group=group;
        }
        
        CameraInfo copy(){
             return new CameraInfo( name,  url, group, enabled);
        }
        
        CameraInfo copyToggleEnable(){
             return new CameraInfo( name,  url, group, !enabled);
        }
    }
    
    public static List<CameraInfo> getImageBufferConfig2() throws IOException, InterruptedException, GitAPIException{
        List<CameraInfo> ret = new ArrayList<>();
        try(Git git = updateDataSourcesRepo(true)){
            Path path = Paths.get(getDataSourcesRepoFolder(true), "sources", "image.sources");
            if (!path.toFile().isFile()){
                throw new IOException("Cannot retrieve the configuration file");
            }
            String str = Files.readString(path);


            Pattern pattern = Pattern.compile("/\\*(?s)(.*?)\\*/");
            Matcher matcher = pattern.matcher(str);
            String json = matcher.replaceAll("");        


            Map config =  (Map) JsonSerializer.decode(json, Map.class);


            List<Map> entries = (List<Map>) config.get("sources");

            for (Map m : entries ){
                try{
                    CameraInfo info = new CameraInfo((String)((List)m.get("labels")).get(0),(String)m.get("stream"), (String)((List)m.get("groups")).get(0), true);
                    ret.add(info);
                } catch (Exception ex){
                    System.out.println(ex);
                }
                System.out.println(m.get("stream"));
            }



            // Create a list to store all /* ... */ comments
             pattern = Pattern.compile("/\\*(.*?)\\*/");
            // Create a list to store matched substrings
            List<String> substrings = new ArrayList<>();
            // Use a Matcher to find and collect all matched substrings
             matcher = pattern.matcher(str);
            while (matcher.find()) {
                substrings.add(matcher.group(1)); 
            }
            for (String s : substrings){
                if (s.startsWith(" ,")){
                    s = s.trim();
                    try{
                        Map m =  (Map) JsonSerializer.decode(s.substring(1), Map.class);
                        CameraInfo info = new CameraInfo((String)((List)m.get("labels")).get(0),(String)m.get("stream"), (String)((List)m.get("groups")).get(0), false);
                        ret.add(info);
                    } catch (Exception ex){
                        System.out.println(ex);
                    }
                }
            }

            return ret;
        }
    }
    
    public static List<CameraInfo> getImageBufferConfig() throws IOException, InterruptedException, GitAPIException{
        List<CameraInfo> ret = new ArrayList<>();
        
        try(Git git = updateDataSourcesRepo(true)){
            Path path = Paths.get(getDataSourcesRepoFolder(true), "sources", "image.sources");
            if (!path.toFile().isFile()){
                throw new IOException("Cannot retrieve the configuration file");
            }
            String str = Files.readString(path);

            String[] lines = str.split("\n");

            for (String line : lines){
                String trimmed = line.trim();
                boolean lineEnabled =!trimmed.startsWith("/*");
                line = line.replace("/*", "").replace("*/", "").trim();
                if (line.startsWith(",")){
                    line = line.substring(1);
                }
                if (line.endsWith(",")){
                    line = line.substring(0,line.length()-1);
                }
                try{
                    Map m =  (Map) JsonSerializer.decode(line, Map.class);
                    CameraInfo info = new CameraInfo((String)((List)m.get("labels")).get(0),(String)m.get("stream"), (String)((List)m.get("groups")).get(0), lineEnabled);
                    ret.add(info);
                } catch (Exception ex){
                    //System.out.println(ex);
                }
            }
        }        
        return ret;        
    }
  
  
    public static String updateImageBufferConfig(List<CameraInfo> cameras, String usr, String pwd, String msg) throws IOException, InterruptedException, GitAPIException{
        try(Git git = updateDataSourcesRepo(true)){
            Path path = Paths.get(getDataSourcesRepoFolder(true), "sources", "image.sources");
            if (!path.toFile().isFile()){
                throw new IOException("Cannot retrieve the configuration file");
            }                        
            checkCredentials(git, usr, pwd);
            
            String str = Files.readString(path);

            String[] lines = str.split("\n");

            for (CameraInfo camera : cameras){
                for (String line : lines){
                    if (line.contains(camera.url)) {
                        String trimmed = line.trim();
                        boolean lineEnabled =!trimmed.startsWith("/*");
                        if (lineEnabled != camera.enabled){
                            String newLine=null;
                            if (camera.enabled){
                                newLine = line.replace("/*", "  ").replace("*/", "");
                            } else {
                                int leadingSpaces = line.indexOf(trimmed);
                                String leadingPadding = (leadingSpaces>4) ? (line.substring(0, leadingSpaces-3) + "/* ") : " /* ";                                                    
                                newLine = leadingPadding + trimmed + " */";
                            }                        
                            str = str.replace(line, newLine);
                            break;
                        }
                    }
                }            
            }

            Files.writeString(path, str);  
           
            String ret = uploadSources(true);
            commitRepo(git,((msg==null)||(msg.isBlank())) ? "Updated by CSM" : msg);
            pushRepo(git, usr, pwd);            
            return ret;
        }
    }    
      
    
    public static void main(String[] args) {
        try{            
        
            //String ret = reconnectCameraSources("test", false);
            //System.out.println(ret);
            List<CameraInfo> ret = getImageBufferConfig();
            System.out.println(ret);
        } catch (Exception ex){
            System.err.println(ex);
        }
    }    
    
}
