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

    static volatile Clip clip;

    public static void playFile(File file) throws Exception {
        if (clip != null) {
            clip.close();
            clip = null;
        }
        AudioInputStream stream;
        AudioFormat format;
        DataLine.Info info;
        stream = AudioSystem.getAudioInputStream(file);
        format = stream.getFormat();
        info = new DataLine.Info(Clip.class, format);
        clip = (Clip) AudioSystem.getLine(info);
        clip.open(stream);
        clip.start();
        clip.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                if (event.getType() == Type.STOP) {
                    clip.close();
                    clip = null;
                }
            }
        });

    }

}
