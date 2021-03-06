package ch.psi.pshell.epics;

import ch.psi.pshell.imaging.CameraSource;

/**
 * Image source based on Area Detector.
 */
public class AreaDetectorSource extends CameraSource {

    public AreaDetectorSource(String name, String channelPrefix) {
        super(name, new AreaDetector(name + " detector", channelPrefix));
    }

    public AreaDetectorSource(String name, String channelCtrl, String channelData) {
        super(name, new AreaDetector(name + " detector", channelCtrl, channelData));
    }
}
