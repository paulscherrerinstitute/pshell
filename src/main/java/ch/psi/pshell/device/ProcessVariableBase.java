package ch.psi.pshell.device;

import ch.psi.utils.Reflection.Hidden;

/**
 * Base class for ProcessVariable implementations.
 */
public abstract class ProcessVariableBase extends RegisterBase<Double> implements ProcessVariable {

    protected ProcessVariableBase(String name, ProcessVariableConfig cfg) {
        super(name, cfg);
    }

    @Override
    public ProcessVariableConfig getConfig() {
        return (ProcessVariableConfig) super.getConfig();
    }

    @Override
    public double getOffset() {
        return getConfig().offset;
    }

    @Override
    public double getScale() {
        return getConfig().scale;
    }
    
    @Override
    public double getResolution() {
        if (Double.isNaN(getConfig().resolution) || (getConfig().resolution < 0)) {
            double precision = getPrecision();
            if (precision < 0) {
                return Math.pow(10.0, -6);
            }
            return Math.pow(10.0, -precision);
        }
        return getConfig().resolution;
    }

    @Override
    public String getUnit() {
        if (!getConfig().hasDefinedUnit()) {
            return "units";
        }
        return getConfig().unit;
    }

    @Override
    public double getMinValue() {
        return adjustPrecision(getConfig().minValue);
    }

    @Override
    public double getMaxValue() {
        return adjustPrecision(getConfig().maxValue);
    }

    @Override
    public boolean isValidValue(Double value) {
        double min = getMinValue();
        double max = getMaxValue();
        if ((value == null) || (value.isNaN())) {
            return false;
        }
        if (!Double.isNaN(min)) {
            if (value < (min - getResolution() / 2.0)) {
                return false;
            }
        }
        if (!Double.isNaN(max)) {
            if (value > (max + getResolution() / 2.0)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean hasChanged(Object value, Object former) {
        if (former == null) {
            return (value != null);
        }
        if (value == null){
            return true;
        }
        return (Math.abs((Double) value - (Double) former) > getResolution() / 2);
    }

    boolean forceReadInRange;

    @Override
    protected Double convertFromRead(Double value) {
        if (value != null) {
            value *= getScale();
            value += getOffset();
            if (forceReadInRange) {
                double min = getMinValue();
                double max = getMaxValue();
                if (max <= min) {
                    return null;
                }
                if (!Double.isNaN(min)) {
                    value = Math.max(value, min);
                }
                if (!Double.isNaN(max)) {
                    value = Math.min(value, max);
                }
            }
        }
        return value;
    }

    @Override
    protected Double convertForWrite(Double value) {
        if (value != null) {
            value -= getOffset();
            value /= getScale();
        }
        return value;

    }

    @Override
    protected Double enforceRange(Double value) {
        if (value != null) {
            //Do not check range: if has already been cheecked by isValidValue
            if (!Double.isNaN(getMinValue())) {
                value = Math.max(value, getMinValue());
            }
            if (!Double.isNaN(getMaxValue())) {
                value = Math.min(value, getMaxValue());
            }
        }
        return value;
    }

    @Hidden
    @Override
    public void assertValidValue(Double value) throws InvalidValueException {
        if (!isValidValue(value)) {
            throw new InvalidValueException(value, getMinValue(), getMaxValue());
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0.0);
    }
}
