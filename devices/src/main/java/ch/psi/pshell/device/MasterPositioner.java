package ch.psi.pshell.device;

import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Range;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master-slave axis .
 */
public class MasterPositioner extends PositionerBase {

    final PositionerBase master;
    final Positioner[] slaves;
    final MotorGroup motorGroup;

    public MasterPositioner(String name, PositionerBase master, Positioner... slaves) {
        super(name, new MasterPositionerConfig());
        this.master = master;
        this.slaves = slaves;
        this.setReadback(master.getReadback());
        if (slaves.length > 6) {
            throw new RuntimeException("Maximum number of slave positioner is 6");
        }
        MotorGroup motorGroup = null;
        ArrayList<Motor> motorList = new ArrayList<>();
        ArrayList<Device> components = new ArrayList<>();

        if (master instanceof Device device) {
            components.add(device);
        }
        for (Positioner p : slaves) {
            if (p instanceof Motor motor) {
                motorList.add(motor);
            }
            if (p instanceof Device device) {
                components.add(device);
            }
        }

        if (motorList.size() > 0) {
            if (master instanceof Motor motor) {
                motorList.add(0, motor);
            }
            motorGroup = new MotorGroupBase(name + " motor group", motorList.toArray(new Motor[0]));
        }

        this.motorGroup = motorGroup;
        setComponents(components.toArray(new Device[0]));
        for (Device d : components) {
            d.addListener(new DeviceListener() {
                @Override
                public void onStateChanged(Device device, State state, State former) {
                    processState();
                }
            });
        }
    }

    public MasterPositioner(String name, PositionerBase master, Positioner m1) {
        this(name, master, new Positioner[]{m1});
    }

    public MasterPositioner(String name, PositionerBase master, Positioner m1, Positioner m2) {
        this(name, master, new Positioner[]{m1, m2});
    }

    public MasterPositioner(String name, PositionerBase master, Positioner m1, Positioner m2, Positioner m3) {
        this(name, master, new Positioner[]{m1, m2, m3});
    }

    public MasterPositioner(String name, PositionerBase master, Positioner m1, Positioner m2, Positioner m3, Positioner m4) {
        this(name, master, new Positioner[]{m1, m2, m3, m4});
    }

    public MasterPositioner(String name, PositionerBase master, Positioner m1, Positioner m2, Positioner m3, Positioner m4, Positioner m5) {
        this(name, master, new Positioner[]{m1, m2, m3, m4, m5});
    }

    public MasterPositioner(String name, PositionerBase master, Positioner m1, Positioner m2, Positioner m3, Positioner m4, Positioner m5, Positioner m6) {
        this(name, master, new Positioner[]{m1, m2, m3, m4, m5, m6});
    }

    @Override
    public MasterPositionerConfig getConfig() {
        return (MasterPositionerConfig) super.getConfig();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        PositionerConfig cfg = ((PositionerBase) master).getConfig();
        getConfig().maxValue = cfg.maxValue;
        getConfig().minValue = cfg.minValue;
        getConfig().offset = cfg.offset;
        getConfig().precision = cfg.precision;
        getConfig().deadband = cfg.deadband;
        getConfig().scale = cfg.scale;
        getConfig().sign_bit = cfg.sign_bit;
        getConfig().unit = cfg.unit;
        getConfig().save();

        super.doInitialize();
        if (motorGroup != null) {
            motorGroup.initialize();
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        for (Device d : getComponents()) {
            d.update();
        }
    }

    public Double[] getSlavePositions(Double masterPosition) throws IOException {
        Double[] ret = new Double[slaves.length];

        ArrayList<double[]> table = getInterpolationTable();

        int segment = getInterpolationSegment(masterPosition);
        double m0 = table.get(segment)[0];
        double m1 = table.get(segment + 1)[0];
        if (m0 > m1) {
            throw new IOException("Invalid interpolation table");
        }
        for (int i = 0; i < slaves.length; i++) {
            double s0 = table.get(segment)[i + 1];
            double s1 = table.get(segment + 1)[i + 1];
            ret[i] = (((s1 - s0) * (masterPosition - m0)) / (m1 - m0) + s0);
        }
        return ret;
    }

    boolean isSlaveInMotorGroup(int i) {
        return ((motorGroup != null) && Arr.contains(motorGroup.getComponents(), slaves[i]));
    }

    boolean isMasterInMotorGroup() {
        return ((motorGroup != null) && Arr.contains(motorGroup.getComponents(), master));
    }

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        Double[] slavePositions = getSlavePositions(value);
        if (!isMasterInMotorGroup()) {
            master.moveAsync(value);
        }
        setState(State.Busy);

        if (motorGroup != null) {
            double[] positions = new double[motorGroup.getMotors().length];
            int index = 0;
            if (isMasterInMotorGroup()) {
                positions[index++] = value;
            }
            for (int i = 0; i < slaves.length; i++) {
                if (isSlaveInMotorGroup(i)) {
                    positions[index++] = slavePositions[i];
                }
            }
            motorGroup.moveAsync(positions).handle((ret, ex) -> {
                processState();
                return ret;
            });
        }

        for (int i = 0; i < slaves.length; i++) {
            if (!isSlaveInMotorGroup(i)) {
                slaves[i].moveAsync(slavePositions[i]).handle((ret, ex) -> {
                    processState();
                    return ret;
                });
            }
        }
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        return master.read();
    }

    @Override
    protected void processState() {
        Device[] devs = getComponents();
        for (Device dev : devs) {
            if (!dev.getState().isNormal()) {
                setState(State.Invalid);
                return;
            }
        }
        for (Device dev : devs) {
            if (dev.getState().isProcessing()) {
                setState(State.Busy);
                return;
            }
        }
        setState(State.Ready);
    }

    public PositionerBase getMaster() {
        return master;
    }

    public Positioner[] getSlaves() {
        return slaves;
    }

    public MotorGroup getMotorGroup() {
        return motorGroup;
    }

    public ArrayList<double[]> getInterpolationTable(){
        ArrayList<double[]> ret = new ArrayList<>();
        MasterPositionerConfig cfg = getConfig();

        try {
            double[] masterPos = cfg.masterPositions;
            double[][] slavesPos = new double[][]{cfg.slave1Positions, cfg.slave2Positions, cfg.slave3Positions, cfg.slave4Positions, cfg.slave5Positions, cfg.slave6Positions};

            if (masterPos != null) {
                for (int i = 0; i < cfg.masterPositions.length; i++) {
                    double[] entry = new double[slaves.length + 1];
                    entry[0] = masterPos[i];
                    for (int j = 0; j < slaves.length; j++) {
                        entry[j + 1] = slavesPos[j][i];
                    }
                    ret.add(entry);
                }
            }
        } catch (Exception ex) {
            getLogger().warning("Invalid interpolation table: resetting configuration entries.");
            try {
                setInterpolationTable(null);
            } catch (IOException ex1) {
                Logger.getLogger(MasterPositioner.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        return ret;
    }

    public void clearInterpolationTable() throws IOException {
        setInterpolationTable(null);
    }

    public void gear() throws IOException, InterruptedException {
        move(master.read());
    }

    
    public void setInterpolationTable(ArrayList<double[]> table) throws IOException {
        setInterpolationTable(table, true);
    }
    
    public void setInterpolationTable(ArrayList<double[]> table, boolean save) throws IOException {
        ArrayList<double[]> ret = new ArrayList<>();
        MasterPositionerConfig cfg = getConfig();

        double[] masterPos = new double[0];
        double[][] slavesPos = new double[6][0];

        if ((table != null) && (table.size() > 0)) {
            int points = table.size();
            masterPos = new double[points];
            for (int i = 0; i < slaves.length; i++) {
                slavesPos[i] = new double[points];
            }

            for (int i = 0; i < points; i++) {
                masterPos[i] = table.get(i)[0];
                for (int j = 0; j < slaves.length; j++) {
                    slavesPos[j][i] = table.get(i)[j + 1];
                }
            }

        }
        cfg.masterPositions = masterPos;
        cfg.slave1Positions = slavesPos[0];
        cfg.slave2Positions = slavesPos[1];
        cfg.slave3Positions = slavesPos[2];
        cfg.slave4Positions = slavesPos[3];
        cfg.slave5Positions = slavesPos[4];
        cfg.slave6Positions = slavesPos[5];
        
        if (save){
            cfg.save();
        }
    }

    public void addInterpolationTable(double[] values) throws IOException {
        addInterpolationTable(values, true);
    }
    
    public void addInterpolationTable(double[] values, boolean save) throws IOException {
        if (values.length != slaves.length + 1) {
            throw new RuntimeException("Invalid number of elements in interpolation table");
        }
        ArrayList<double[]> table = getInterpolationTable();
        double masterPos = values[0];
        int entryPos = 0;
        for (int i = 0; i < table.size(); i++) {
            double pos = table.get(i)[0];
            if (masterPos == pos) {
                for (int j = 0; j < slaves.length; j++) {
                    table.get(i)[j + 1] = values[j + 1];
                }
                setInterpolationTable(table, save);
                return;
            }
            if (masterPos > pos) {
                entryPos = i + 1;
            }
        }
        table.add(entryPos, values);
        setInterpolationTable(table, save);
    }

    public double[] getSetpoints() throws IOException, InterruptedException {
        double[] ret = new double[slaves.length + 1];
        ret[0] = master.getValue();
        for (int i = 0; i < slaves.length; i++) {
            ret[i + 1] = slaves[i].getValue();
        }
        return ret;
    }

    public double[] getReadbacks() throws IOException, InterruptedException {
        double[] ret = new double[slaves.length + 1];
        ret[0] = master.getPosition();
        for (int i = 0; i < slaves.length; i++) {
            ret[i + 1] = slaves[i].getPosition();
        }
        return ret;
    }

    public void addInterpolationTable(boolean setpoints) throws IOException, InterruptedException {
        addInterpolationTable(setpoints, true);
    }

    public void addInterpolationTable(boolean setpoints, boolean save) throws IOException, InterruptedException {
        addInterpolationTable(setpoints ? getSetpoints() : getReadbacks(), save);
    }

    public void delInterpolationTable(int index) throws IOException, InterruptedException {
        delInterpolationTable(index, true);
    }
    
    public void delInterpolationTable(int index, boolean save) throws IOException, InterruptedException {
        ArrayList<double[]> table = getInterpolationTable();
        table.remove(index);
        setInterpolationTable(table, save);        
    }

    public void removeInterpolationTable(int index) throws IOException {
        ArrayList<double[]> table = getInterpolationTable();
        table.remove(index);
        setInterpolationTable(table);
    }

    public int getSlaveIndex(Positioner slave) {
        for (int i = 0; i < slaves.length; i++) {
            if (slaves[i] == slave) {
                return i;
            }
        }
        return -1;
    }

    public int getInterpolationSegment(double masterPosition) throws IOException {
        ArrayList<double[]> table = getInterpolationTable();
        int segments = table.size() - 1;
        for (int i = 0; i < segments; i++) {
            double m0 = table.get(i)[0];
            double m1 = table.get(i + 1)[0];
            if ((m0 <= masterPosition) && (m1 >= masterPosition)) {
                return i;
            }
        }
        throw new IOException("Position not in interpolation table: " + masterPosition);
    }
    
    public int getInterpolationEntry(double masterPosition) throws IOException {
        ArrayList<double[]> table = getInterpolationTable();
        for (int i = 0; i < table.size(); i++) {
            if (Math.abs(table.get(i)[0] - masterPosition) < 1e-8){
                return i;
            }
        }        
        return -1;
    }        
    
    public Range getRange()  {
        ArrayList<double[]> table = getInterpolationTable();
        if (table.size() <2){
            return null;
        }
        return new Range(table.get(0)[0], table.get(table.size()-1)[0]);
    }
    
    @Override
    public double getMinValue() {
        double min = getConfig().minValue;
        Range range = getRange();
        if (range!=null){
            min = Math.max(min, range.min);
        }
        return adjustPrecision(min);
    }

    @Override
    public double getMaxValue() {
        double max = getConfig().maxValue;
        Range range = getRange();
        if (range!=null){
            max = Math.min(max, range.max);
        }
        return adjustPrecision(max);
    }
    

    public ArrayList<double[]> getInterpolationPlot(Positioner slave, double resolution) throws IOException {
        ArrayList<double[]> table = getInterpolationTable();
        int index = getSlaveIndex(slave);
        if (index < 0) {
            throw new IOException("Invalid slave: " + ((slave == null) ? "null" : slave.getName()));
        }
        ArrayList<Double> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();
        int segments = table.size() - 1;

        for (int i = 0; i < segments; i++) {
            double m0 = table.get(i)[0];
            double s0 = table.get(i)[index + 1];
            double m1 = table.get(i + 1)[0];
            double s1 = table.get(i + 1)[index + 1];
            if (m0 > m1) {
                throw new IOException("Invalid interpolation table");
            }
            double m;
            for (m = m0; m < m1; m += resolution) {
                x.add(m);
                y.add(((s1 - s0) * (m - m0)) / (m1 - m0) + s0);
            }
            if (m < m1) {
                x.add(m1);
                y.add(s1);
            }
        }

        ArrayList<double[]> ret = new ArrayList<>();
        ret.add((double[]) Convert.toPrimitiveArray(x, Double.class));
        ret.add((double[]) Convert.toPrimitiveArray(y, Double.class));
        return ret;
    }
}
