package ch.psi.utils;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;

/**
 *
 */
public class Audio {

    static volatile Clip former;

    public static void cancel() throws Exception {
        if (former!=null){
            former.close();
            former = null;
        }        
    }
    
    public static Clip playFile(File file) throws Exception {
        return playFile(file, true);
    }
    
    public static Clip playFile(File file, boolean cancel) throws Exception {
        if (cancel){
            cancel();
        }
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.open(stream);
        clip.start();
        clip.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                if (event.getType() == Type.STOP) {
                    clip.close();
                    if (clip == former){
                        former = null;
                    }
                } 
            }
        });
        former = clip;
        return clip;
    }
}
