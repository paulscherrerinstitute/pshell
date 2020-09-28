################################################################################################### 
# Example of using ImageJ to get image measurements
################################################################################################### 

from ijutils import get_measurement, load_array
import ch.psi.pshell.imaging.Filter as Filter
from ch.psi.pshell.imaging.Overlays import Text
import ch.psi.pshell.imaging.Pen as Pen

class MeasurementsFilter(Filter):
    def __init__(self, measurements):
        self.overlay = Text(Pen(java.awt.Color.GREEN.darker()), "", \
            java.awt.Font("Verdana", java.awt.Font.PLAIN, 12), java.awt.Point(20,20))
        self.measurements = measurements
        self.source = None
        self.renderer = None

    def process(self, image, data):
        try:
            ip = load_array(data.array, data.width, data.height)
            msg = ""
            if self.measurements is not None:
                for measurement in self.measurements:
                    val = get_measurement(ip,measurement)
                    msg = msg + "%s = %1.4f\n" % (measurement,val)
            self.overlay.update(msg)
        except:
            self.overlay.update(str(sys.exc_info()[1]))
        return image

    def start(self, source, renderer=None):
        self.stop()
        self.source = source
        self.renderer = renderer if (renderer is not None) else show_panel(source)
        self.source.setFilter(self)
        self.renderer.addOverlay(self.overlay)

    def stop(self):
        if self.renderer is not None:
            self.renderer.removeOverlay(self.overlay)
        if self.source is not None:
            self.source.setFilter(None)
        self.source = None
        self.renderer = None

filter_measurements = None

def start_measurements(measurements, source, renderer=None):
    global filter_measurements
    stop_measurements()
    filter_measurements = MeasurementsFilter(measurements)
    filter_measurements.start(source, renderer)

def stop_measurements():
    global filter_measurements
    if filter_measurements is not None:
        filter_measurements.stop()
    filter_measurements = None

measurements = ["Area", "Mean", "StdDev", "Mode", "Min", "Max", "XM", "YM"]
start_measurements (measurements, src1)
time.sleep(5.0)
stop_measurements()