################################################################################################### 
# Demonstrate the use of fft function in mathutils.py
###################################################################################################

from mathutils import fft, get_modulus
import random

#The signal is composed by 3 sinusoids (100Hz, 200Hz, 400Hz) and a background noise (a sin waveform is A.sin(2.pi.f.t))
def signal_generator(t):
    return  math.sin(100*2*math.pi*t)  + 0.5 * math.sin(200*2*math.pi*t) + 0.25 * math.sin(400*2*math.pi*t) + 0.5* random.random()

sampling_frequency = 1024.0         
number_of_samples = 1024

time_vector = [x / sampling_frequency for x in frange(0, number_of_samples, 1)]
signal = [signal_generator(x) for x in time_vector]

tranform = fft(signal)
two_side_spectrum = [x / number_of_samples for x in get_modulus(tranform)]
spectrum = [two_side_spectrum[0],] + [x * 2 for x in two_side_spectrum[1:len(two_side_spectrum)/2 + 1] ]

number_of_samples = len(tranform)    # Signal may have been padded to next power of two
freq_vector = [x * sampling_frequency / float(number_of_samples) for x in frange(0, len(spectrum) , 1)]

plot([signal,spectrum], ["signal", "spectrum"],[time_vector, freq_vector])
    