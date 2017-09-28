################################################################################################### 
# Create an online spectrum analyser uysing mathutils.py
###################################################################################################


from mathutils import fft, get_modulus
import random

def plot_spectrum(signal, sampling_freq, plots = None):
    signal = to_list(signal) 
    number_of_samples = len(signal)
    time_vector = [x / sampling_freq for x in frange(0, number_of_samples, 1)]    
    tranform = fft(signal)
    two_side_spectrum = [x / number_of_samples for x in get_modulus(tranform)]
    spectrum = [two_side_spectrum[0],] + [x * 2 for x in two_side_spectrum[1:len(two_side_spectrum)/2 + 1] ]
    number_of_samples = len(tranform)    # Signal may have been padded to next power of two
    freq_vector = [x * sampling_freq / float(number_of_samples) for x in frange(0, len(spectrum) , 1)]
    if plots is None:
        plots = plot([signal,spectrum], ["signal", "spectrum"],[time_vector, freq_vector], title = "Spectrum")
        plots[0].getAxis(plots[0].AxisId.Y).setRange(-1.5,2.5)
        plots[1].getAxis(plots[1].AxisId.Y).setRange(0.0,1.1)
    else:
        plots[0].getSeries(0).setData(time_vector,signal)
        plots[1].getSeries(0).setData(freq_vector,spectrum)
    return plots

def get_sample(samples, sampling_freq):
    return map(lambda t:math.sin(200*2*math.pi*t/sampling_freq) + random.random(), frange(0,samples,1))
    #Simple example sampling a device:
    #set_exec_pars(persist=False)
    #setup_plotting(enable_plots = False, enable_table = False)
    #return tscan(ai1, samples, 1/sampling_freq).getReadable(0)

plots = None
samples = 1024; sampling_freq = 1024.0
while(True):
    signal = get_sample(samples, sampling_freq)
    plots = plot_spectrum(signal, sampling_freq, plots)
    time.sleep(0.1)
    if plots[0].displayable == False: 
        break
