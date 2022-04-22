package ch.psi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class Processing {
    
    
    public static String[] run(String... command) throws InterruptedException, IOException{
        return run(List.of(command));
    }
    
    
    public static String[] run(List<String> command) throws InterruptedException, IOException{
        Logger.getLogger(Processing.class.getName()).info("Starting process: " + String.join(" ", command));        
        StringBuilder builderErr = new StringBuilder();
        StringBuilder builderOut= new StringBuilder();        
        String line = null;        
        ProcessBuilder pb = new ProcessBuilder(command);
        //pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader readerErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        BufferedReader readerOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
                
        //p.waitFor();
        while(p.isAlive()){  
            if (readerErr.ready()){
                if ( (line = readerErr.readLine()) != null) {
                    builderErr.append(line).append(Sys.getLineSeparator());
                }                 
            }
            if (readerOut.ready()){
                if ((line = readerOut.readLine()) != null) {
                    builderOut.append(line).append(Sys.getLineSeparator());
                } 
            }
            Thread.sleep(10);
        }

        if (readerErr.ready()){
            while ( (line = readerErr.readLine()) != null) {
                builderErr.append(line).append(Sys.getLineSeparator());
            }                 
        }
        if (readerOut.ready()){
            while ((line = readerOut.readLine()) != null) {
                builderOut.append(line).append(Sys.getLineSeparator());
            }        
        }
        String err = builderErr.toString();
        String out = builderOut.toString();
        Logger.getLogger(Processing.class.getName()).fine("err: " + err);   
        Logger.getLogger(Processing.class.getName()).fine("out: " + out);   
        
        return new String[]{out, err};                
    }
}
