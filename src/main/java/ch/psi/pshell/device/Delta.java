package ch.psi.pshell.device;

import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Performs differential read on values of the source device.
 */
public class Delta extends ReadonlyRegisterBase<Double> implements ReadonlyRegisterNumber<Double> {

    final Readable source;
    private Double lastValue = null;

    public Delta(String name, Readable source) {
        super(name);
        if (source instanceof ReadonlyRegister) {
            setPrecision(((ReadonlyRegister) source).getPrecision());
        }
        this.source = source;
        if (source instanceof DeviceBase) {
            setParent(((DeviceBase) source));
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    public Delta(String name, Readable source, int precision) {
        this(name, source);
        setPrecision(precision);
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        lastValue = null;
    }

    ArrayList<Number> samples;

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        Double value = ((Number) source.read()).doubleValue();
        Double ret = (lastValue == null) ? Double.NaN : (value - lastValue);
        lastValue = value;
        return ret;
    }
}
