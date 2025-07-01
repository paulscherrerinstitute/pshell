package ch.psi.pshell.device;


/**
 * Base class for ProcessVariable implementations.
 */
public abstract class ReadonlyProcessVariableBase extends ReadonlyRegisterBase<Double> implements ReadonlyProcessVariable {

    protected ReadonlyProcessVariableBase(String name, ReadonlyProcessVariableConfig cfg) {
        super(name, cfg);
    }

    @Override
    public ReadonlyProcessVariableConfig getConfig() {
        return (ReadonlyProcessVariableConfig) super.getConfig();
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
    public String getUnit() {
        if (!getConfig().hasDefinedUnit()) {
            return "units";
        }
        return getConfig().unit;
    }
    
    @Override
    public String getDescription(){
        if (!getConfig().hasDefinedDescription()) {
            return "";
        }
        return getConfig().description;
    }
    
    @Override
    public int getSignBit(){
        return getConfig().sign_bit;
    }

    @Override
    protected boolean hasChanged(Object value, Object former) {
        if (former == null) {
            return (value != null);
        }
        if (value == null){
            return true;
        }
        double resolution = (getPrecision() < 0) ? Math.pow(10.0, -6) : Math.pow(10.0, -getPrecision());      
        return (Math.abs((Double) value - (Double) former) > resolution / 2);
    }


    @Override
    protected Double convertFromRead(Double value) {
        if (value != null) {
            value = getConfig().applySign(value);
            value *= getScale();
            value += getOffset();
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
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0.0);
    }
}
