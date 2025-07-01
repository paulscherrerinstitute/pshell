package ch.psi.pshell.imaging;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 *
 */
public class ColormapAdapter extends ColormapSource {

    final SourceBase source;
    final ImageListener listener;

    public SourceBase getSource() {
        return source;
    }

    public ColormapAdapter(String name, SourceBase source) {
        super(name, new ColormapSourceConfig());
        this.source = source;
        listener = new ImageListener() {
            @Override
            public void onImage(Object origin, BufferedImage image, Data data) {
                try {
                    if ((data!=null) || (image!=null)){
                        data = (data==null) ? new Data(image) : data;
                        pushData(data.getRectSelection(null, false));
                    }
                } catch (IOException ex) {
                    pushError(ex);
                }
            }

            @Override
            public void onError(Object origin, Exception ex) {
                pushError(ex);
            }
        };        
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (!source.isInitialized()) {
            source.initialize();
        }
        source.addListener(listener);
        try{
            listener.onImage(source, source.currentImage, source.currentData);
        } catch (Exception ex){
            
        }
    }
    
    

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        source.update();
    }

    @Override
    protected void doClose() throws IOException {
        source.removeListener(listener);
        super.doClose();
    }

    protected void onDataReceived(Object deviceData){}
}
