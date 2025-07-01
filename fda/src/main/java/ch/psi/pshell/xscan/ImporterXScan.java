/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.xscan;

import ch.psi.pshell.app.Importer;
import ch.psi.pshell.xscan.model.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 */
public class ImporterXScan implements Importer {

    StringBuilder sb;

    boolean importComparatorREGEX;
    boolean importComparatorOR;
    boolean importComparatorAND;

    int indentation = 0;
    int setpointIndex = 1;
    int dimensionCount = 1;
    int detectorIndex = 1;

    ArrayList<String> scanPositioners;
    ArrayList<String> scanDetectors;
    HashMap<String, ArrayList<String>> scanManipulations;
    HashMap<String, String> scanTags;
    ArrayList<String> scanStart;
    ArrayList<String> scanEnd;
    ArrayList<String> scanSteps;

    ArrayList<String> scanSetpoints;
    ArrayList<String> scanReadbacks;
    ArrayList<String> scanValues;

    @Override
    public String getDescription() {
        return "XScan files";
    }

    @Override
    public String[] getExtensions() {
        return new String[]{"xml"};
    }

    @Override
    public synchronized String importFile(File file) throws Exception {
        indentation = 0;
        setpointIndex = 1;
        dimensionCount = 1;
        detectorIndex = 1;

        Configuration cfg = ModelManager.unmarshall(file);
        sb = new StringBuilder();
        indentation = 0;
        int baseIndentation = 0;

        add("#Script imported from: " + file.getName());

        if (cfg.getVariable().size() > 0) {
            add("");
            add("#Variables");
            for (Variable v : cfg.getVariable()) {
                add(v.getName() + " = " + v.getValue());
            }
        }

        if (cfg.getNumberOfExecution() > 1) {
            add("");
            add("numberOfExecutions  = " + cfg.getNumberOfExecution());
            add("#TODO: Support to multiple iterations is partial: check if logic is ok");
            add("for iteration in range(numberOfExecutions):");
            baseIndentation = 4;
        }

        Scan scan = cfg.getScan();

        scanSetpoints = new ArrayList<>();
        scanReadbacks = new ArrayList<>();
        scanValues = new ArrayList<>();
        scanTags = new HashMap<>();

        if (scan.getPreAction().size() > 0) {
            add("");
            add("#Pre-actions");
            for (Action action : scan.getPreAction()) {
                addAction(action);
            }
        }

        if (scan.getCdimension() != null) {
            add("#Error: " + "Continuous dimension not supported\n");
        }

        List<DiscreteStepDimension> dimensions = scan.getDimension();
        if (dimensions.size() > 0) {
            Collections.reverse(dimensions);
            parsePositioners(scan);
            parseDetectors(scan);
            parseManipulations(scan);

            add("");
            add("#TODO: Set the diplay names of positioners and detectors");
            add("scan = ManualScan(['" + String.join("', '", scanPositioners) + "'], ['" + String.join("', '", scanDetectors) + "'] , [" + String.join(", ", scanStart) + "], [" + String.join(", ", scanEnd) + "], [" + String.join(", ", scanSteps) + "])");
            add("scan.start()");
            add("");

            openChannels(dimensions);
            add("");

            for (DiscreteStepDimension dimension : dimensions) {
                openDimension(dimension);
            }
            addManipulations();

            add("scan.append ([" + String.join(", ", scanSetpoints) + "], [" + String.join(", ", scanReadbacks) + "], [" + String.join(", ", scanValues) + "])");

            Collections.reverse(dimensions);
            for (DiscreteStepDimension dimension : dimensions) {
                closeDimension(dimension);
            }
            add("");
            closeChannels(dimensions);
            add("");
            add("scan.end()");
            Collections.reverse(dimensions);
        }

        indentation = baseIndentation;
        if (scan.getPostAction().size() > 0) {
            add("");
            add("#Post-actions");
            for (Action action : scan.getPostAction()) {
                addAction(action);
            }
        }

        StringBuilder imports = new StringBuilder();
        if (importComparatorREGEX) {
            imports.append("import ch.psi.jcae.util.ComparatorREGEX as ComparatorREGEX\n");
        }
        if (importComparatorOR) {
            imports.append("import ch.psi.jcae.util.ComparatorOR as ComparatorOR\n");
        }
        if (importComparatorAND) {
            imports.append("import ch.psi.jcae.util.ComparatorAND as ComparatorAND\n");
        }
        if (imports.length() > 0) {
            imports.append("\n");
            imports.append(sb);
            sb = imports;
        }
        return sb.toString();
    }

    void parsePositioners(Scan scan) {
        scanStart = new ArrayList<>();
        scanEnd = new ArrayList<>();
        scanSteps = new ArrayList<>();
        scanPositioners = new ArrayList<>();

        for (DiscreteStepDimension dimension : scan.getDimension()) {
            for (DiscreteStepPositioner positioner : dimension.getPositioner()) {
                if (positioner instanceof FunctionPositioner fp) {
                    String id = positioner.getId();
                    add("");
                    add("#Function of positioner " + id + " - TODO: Rename function names there is more than one more function positioner");
                    for (String line : parseScript(fp.getFunction().getMapping(), fp.getFunction().getScript())) {
                        add(line);
                    }
                }
            }
        }

        for (DiscreteStepDimension dimension : scan.getDimension()) {
            for (DiscreteStepPositioner positioner : dimension.getPositioner()) {
                String id = positioner.getId();
                if (!scanPositioners.contains(id)) {
                    scanPositioners.add(id);
                    if (positioner instanceof LinearPositioner lp) {
                        if (lp.getStepSize() == 0) {
                            scanSteps.add("0");
                        } else {
                            scanSteps.add(String.valueOf(getNumberOfSteps(lp.getStart(), lp.getEnd(), lp.getStepSize())));
                        }
                        scanStart.add(String.valueOf(lp.getStart()));
                        scanEnd.add(String.valueOf(lp.getEnd()));
                    } else if (positioner instanceof FunctionPositioner fp) {
                        if (fp.getStepSize() == 0) {
                            scanSteps.add("0");
                        } else {
                            scanSteps.add(String.valueOf(getNumberOfSteps(fp.getStart(), fp.getEnd(), fp.getStepSize())));
                        }
                        scanStart.add("calculate(" + fp.getStart() + ")");
                        scanEnd.add("calculate(" + fp.getEnd() + ")");
                    } else if (positioner instanceof RegionPositioner rp) {
                        double start = Double.MAX_VALUE;
                        double end = Double.MIN_VALUE;
                        int steps = 0;
                        Region lastRegion = null;
                        for (Region r : rp.getRegion()) {
                            start = Math.min(start, r.getStart());
                            start = Math.min(start, r.getEnd());
                            end = Math.max(end, r.getStart());
                            end = Math.max(end, r.getEnd());
                            int numSteps = getNumberOfSteps(r.getStart(), r.getEnd(), r.getStepSize());
                            if (lastRegion != null && r.getStart() != lastRegion.getEnd()) {
                                numSteps++;
                            }
                            steps += numSteps;
                            lastRegion = r;
                            if (r.getFunction() != null) {
                                add("#Error: function region not supported\n");
                                lastRegion = null;
                            }
                        }
                        if (steps > 0) {
                            scanStart.add(String.valueOf(start));
                            scanEnd.add(String.valueOf(end));
                            scanSteps.add(String.valueOf(steps));
                        }
                    } else if (positioner instanceof PseudoPositioner pp) {
                        scanStart.add("0.0");
                        scanEnd.add(String.valueOf((double) pp.getCounts()));
                        scanSteps.add(String.valueOf(pp.getCounts()));
                    } else if (positioner instanceof ArrayPositioner ap) {
                        String[] positions = (ap.getPositions().trim()).split(" +");
                        double[] table = new double[positions.length];
                        for (int i = 0; i < positions.length; i++) {
                            table[i] = Double.parseDouble(positions[i]);
                        }

                        List b = Arrays.asList(ArrayUtils.toObject(table));
                        double min = (Double) Collections.min(b);
                        double max = (Double) Collections.max(b);

                        scanStart.add(String.valueOf(min));
                        scanEnd.add(String.valueOf(max));
                        scanSteps.add(String.valueOf(table.length - 1));
                    }
                }
            }
        }
    }

    void parseDetectors(Scan scan) {
        scanDetectors = new ArrayList<>();
        for (DiscreteStepDimension dimension : scan.getDimension()) {
            for (Detector detector : dimension.getDetector()) {
                String id = detector.getId();
                if (detector instanceof ArrayDetector ad) {
                    scanDetectors.add(id + "[" + ad.getArraySize() + "]");
                } else if ((detector instanceof ScalarDetector)
                        || (detector instanceof Timestamp)) {
                    scanDetectors.add(id);
                }
            }
        }
    }

    void parseManipulations(Scan scan) {
        scanManipulations = new HashMap<>();
        for (Manipulation manipulation : scan.getManipulation()) {
            String id = manipulation.getId();
            if (manipulation instanceof ScriptManipulation sm) {
                String script = sm.getScript();
                scanManipulations.put(id, parseScript(sm.getMapping(), sm.getScript()));
                scanDetectors.add(id);
            }
        }
    }

    ArrayList<String> parseScript(List mappings, String script) {
        ArrayList<String> ret = new ArrayList<>();
        if (mappings.size() > 0) {
            ret.add("#Variable Mappings");
            for (Object obj : mappings) {
                if (obj instanceof ChannelParameterMapping map) {
                    String type = map.getType();
                    ret.add(map.getVariable() + " = Channel('" + map.getChannel() + "', type = '" + getType(type) + "')");
                } else if (obj instanceof IDParameterMapping map) {
                    ret.add(map.getVariable() + " =  " + resolveIdRef(map.getRefid()));
                } else if (obj instanceof VariableParameterMapping map) {
                    //TODO: Not right, must get value, not name but is not available in API: Must fix by hand
                    ret.add("#TODO: Check use of global variable: " + map.getVariable());
                }
            }
        }

        String[] lines = script.split("\n");
        boolean inProcess = false;
        int identation = 0;
        for (int i = 0; i < lines.length; i++) {
            if (identation > 0) {
                if (lines[i].length() < identation) {
                    inProcess = false;
                } else {
                    for (int j = 0; j < identation; j++) {
                        if ((lines[i].charAt(j) != ' ') && (lines[i].charAt(j) != '\t')) {
                            inProcess = false;
                        }
                    }
                }
                if (inProcess == false) {
                }
            } else if (lines[i].startsWith("def process")) {
                if (i >= (lines.length - 1)) {
                    break;
                }
                inProcess = true;
                String next = lines[i + 1];
                identation = 0;
                for (int j = 0; j < next.length(); j++) {
                    if ((next.charAt(j) != ' ') && (next.charAt(j) != '\t')) {
                        identation = j;
                        break;
                    }
                }
                continue;
            }

            if (inProcess) {
                ret.add(lines[i].substring(identation));
            } else {
                if (lines[i].startsWith("import")) {
                    ret.add("#TODO: Move, if needed, this import to the file header: " + lines[i]);
                } else {
                    ret.add(lines[i]);
                }
            }
        }
        return ret;
    }

    void addManipulations() {
        for (String manipulation : scanManipulations.keySet()) {
            add("#Manipulation " + manipulation);
            for (String line : scanManipulations.get(manipulation)) {
                for (String tag : scanTags.keySet()) {
                    line = line.replaceAll(tag, scanTags.get(tag));
                }
                line = line.replaceAll("return ", manipulation + " = ");
                add(line);
            }
            scanValues.add(manipulation);
        }
    }

    void addAction(Action action) {
        if (action instanceof ShellAction sa) {
            String com = sa.getCommand();
            //TODO
            //String com = com.replaceAll("\\$\\{DATAFILE\\}", datafile.getAbsolutePath());
            //com = com.replaceAll("\\$\\{FILENAME\\}", datafile.getName().replaceAll("\\.\\w*$", ""));
            int exitValue = sa.getExitValue();
            boolean checkExitValue = sa.isCheckExitValue();
            add("p = subproccess.Popen('" + com + "' +, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)");
            add("p.communicate()");
            add("ret = p.returncode");
            if (checkExitValue) {
                add("if ret <> " + checkExitValue + ":");
                add("   raise Exception('Script returned  with an exit value not equal to 0')");
            }
        } else if (action instanceof ChannelAction ca) {
            String channel = ca.getChannel();
            String type = ca.getType();
            String value = ca.getValue();
            String operation = ca.getOperation();
            Double delay = ca.getDelay();
            Long timeout = ca.getTimeout() == null ? null : (long) Math.round(ca.getTimeout().longValue());

            value = getValue(value, type);
            String strChannel = "'" + channel + "'";
            String strValue = ", " + value;
            String strTimeout = ((timeout != null) ? ", timeout = " + timeout : "");
            String strType = ", type = '" + getType(type) + "'";

            switch (operation) {
                case "put":
                    add("caput(" + strChannel + strValue + strTimeout + ")");
                    break;
                case "putq":
                    add("caputq(" + strChannel + strValue + ")");
                    break;
                case "wait":
                    add("cawait(" + strChannel + strValue + strTimeout + strType + ")");
                    break;
                case "waitREGEX":
                    add("cawait(" + strChannel + strValue + strTimeout + ", ComparatorREGEX()" + strType + ")");
                    importComparatorREGEX = true;
                    break;
                case "waitOR":
                    add("cawait(" + strChannel + strValue + strTimeout + ", ComparatorOR()" + strType + ")");
                    importComparatorOR = true;
                    break;
                case "waitAND":
                    add("cawait(" + strChannel + strValue + strTimeout + ", ComparatorAND()" + strType + ")");
                    importComparatorAND = true;
                    break;
                default:
                    add("#Error: " + "Unknown Operation " + operation + "\n");
            }
            if ((delay != null) && (delay > 0)) {
                //in secs
                add("sleep(" + delay + ")");  //TODO: Why is it after action?
            }

        } else if (action instanceof ScriptAction sa) {
            add("#Script action");
            for (String line : parseScript(sa.getMapping(), sa.getScript())) {
                add(line);
            }
        }
    }

    void openChannels(List<DiscreteStepDimension> dimensions) {
        int index = 1;
        setpointIndex = 1;
        for (DiscreteStepDimension dimension : dimensions) {
            add("#Creating channels: dimension " + index++);
            for (DiscreteStepPositioner positioner : dimension.getPositioner()) {
                createPositionerChannels(positioner);
            }
            createDetectorChannels(dimension.getDetector());
        }
        setpointIndex = 1; //For the positioner loops
    }

    void closeChannels(List<DiscreteStepDimension> dimensions) {
        add("#Closing channels");
        for (DiscreteStepDimension dimension : dimensions) {
            for (DiscreteStepPositioner positioner : dimension.getPositioner()) {
                closePositionerChannels(positioner);
            }
            closeDetectorsChannels(dimension.getDetector());
        }
    }

    void openDimension(DiscreteStepDimension dimension) {
        boolean zigzag = dimension.isZigzag();
        boolean datagroup = dimension.isDataGroup();
        add("#Dimension " + dimensionCount++);

        if (dimension.getPreAction().size() > 0) {
            add("#Dimension Pre-actions");
            for (Action action : dimension.getPreAction()) {
                addAction(action);
            }
        }

        if (dimension.getGuard() != null) {
            add("#Dimension Guard");
            for (GuardCondition gc : dimension.getGuard().getCondition()) {
                String channel = gc.getChannel();
                String type = gc.getType();
                String value = gc.getValue();
                String strType = ", type = '" + getType(type) + "'";
                add("cawait('" + channel + "', " + getValue(value, type) + strType + ")");
            }
        }
        //openDetectors(dimension.getDetector());
        for (DiscreteStepPositioner positioner : dimension.getPositioner()) {
            openPositioner(positioner);
        }

        if (dimension.getAction().size() > 0) {
            add("#Dimension Actions");
            for (Action action : dimension.getAction()) {
                addAction(action);
            }
        }
        addDetectors(dimension.getDetector());
    }

    void closeDimension(DiscreteStepDimension dimension) {
        Collections.reverse(dimension.getPositioner());
        for (DiscreteStepPositioner positioner : dimension.getPositioner()) {
            indentation -= 4;
            //closePositioner(positioner);            
        }
        //closeDetectors(dimension.getDetector());
        Collections.reverse(dimension.getPositioner());

        if (dimension.getPostAction().size() > 0) {
            add("#Dimension Post-actions");
            for (Action action : dimension.getPostAction()) {
                addAction(action);
            }
        }
    }

    String setpointVar;
    String readbackVar;

    void createPositionerChannels(DiscreteStepPositioner positioner) {
        String name = positioner.getName();
        String id = positioner.getId();
        add("#" + positioner.getClass().getSimpleName() + " " + id);
        String readback = positioner.getReadback();

        setpointVar = "setpoint" + (setpointIndex);// id ;     
        readbackVar = "readback" + (setpointIndex);// id ;     
        setpointIndex++;

        if (!(positioner instanceof PseudoPositioner)) {
            addPositionerChannel(id, name, readback);
        }
    }

    void openPositioner(DiscreteStepPositioner positioner) {
        String name = positioner.getName();
        String id = positioner.getId();
        add("#" + positioner.getClass().getSimpleName() + " " + id);
        boolean async = positioner.isAsynchronous();
        String done = positioner.getDone();
        String doneValue = positioner.getDoneValue();
        double doneDelay = positioner.getDoneDelay();
        String readback = positioner.getReadback();
        double settlingTime = positioner.getSettlingTime();
        String type = positioner.getType();

        setpointVar = "setpoint" + (setpointIndex);// id ;   
        readbackVar = "readback" + (setpointIndex);// id ;     
        setpointIndex++;

        if (positioner instanceof FunctionPositioner fp) {
            double end = fp.getEnd();
            double start = fp.getStart();
            double step = Math.abs(fp.getStepSize());
            if (end < start) {
                step *= -1;
            }

            //addPositionerChannel(id, name, readback);
            add("for x in frange(" + start + ", " + end + ", " + step + ", True):");
            indentation += 4;
            add("if x > " + Math.max(end, start) + " or x < " + Math.min(end, start) + ":");
            add(indent(4) + "break");
            add(setpointVar + " = calculate(x)");
            double accuracy = 0.1;
            addSetPositioner(id, readback, async, type, done, doneDelay, doneValue, settlingTime, accuracy);
        } else if (positioner instanceof PseudoPositioner pp) {
            int counts = pp.getCounts();
            add("for " + setpointVar + " in range(0, " + counts + "):");
            indentation += 4;
            if (done != null) {
                add("sleep( " + doneDelay + " ) # Done delay");
                if (getType(type).equals('s')) {
                    doneValue = "'" + doneValue + "'";
                }
                add("cawait('" + done + "', " + doneValue + ", '" + getType(type) + "')");
            }

            add(readbackVar + " = " + setpointVar);

            if (settlingTime > 0) {
                add("sleep( " + settlingTime + " ) # Settling time");
            }
            scanSetpoints.add(setpointVar);
            scanReadbacks.add(readbackVar);
            scanTags.put(id, setpointVar);

        } else if (positioner instanceof ArrayPositioner ap) {
            //addPositionerChannel(id, name, readback);
            String[] positions = (ap.getPositions().trim()).split(" +");
            add("for " + setpointVar + " in (" + String.join(", ", positions) + "):");
            indentation += 4;
            double accuracy = 0.1;

            addSetPositioner(id, readback, async, type, done, doneDelay, doneValue, settlingTime, accuracy);
        } else if (positioner instanceof RegionPositioner rp) {
            //addPositionerChannel(id, name, readback);
            Region lastRegion = null;
            double minStep = Double.MAX_VALUE;
            ArrayList<String> ranges = new ArrayList<>();
            ArrayList<Double> rangeStart = new ArrayList<>();
            for (Region r : rp.getRegion()) {
                double start = r.getStart();
                double end = r.getEnd();
                double step = r.getStepSize();
                minStep = Math.min(minStep, step);
                if (lastRegion != null && start == lastRegion.getEnd()) {
                    if (start < end) {
                        start = start + step;
                    } else {
                        start = start - step;
                    }
                }
                int numSteps = getNumberOfSteps(start, end, step);
                if (numSteps > 0) {
                    ranges.add("frange(" + start + ", " + end + ", " + step + ", True)");
                    rangeStart.add(start);
                }
                lastRegion = r;
            }
            if (ranges.size() > 0) {
                add("for " + setpointVar + " in " + String.join(" + ", ranges) + ":");
                indentation += 4;
                double accuracy = minStep / 2;

                for (int i = 0; i < rp.getRegion().size(); i++) {
                    Region r = rp.getRegion().get(i);
                    if (r.getPreAction().size() > 0) {
                        add("#Region " + (i + 1) + " pre-actions");
                        add("if " + setpointVar + " == " + rangeStart.get(i) + ":");
                        indentation += 4;
                        for (Action action : r.getPreAction()) {
                            addAction(action);
                        }
                        indentation -= 4;
                    }
                }

                addSetPositioner(id, readback, async, type, done, doneDelay, doneValue, settlingTime, accuracy);

            }

        } else if (positioner instanceof LinearPositioner lp) {
            double end = lp.getEnd();
            double start = lp.getStart();
            double step = Math.abs(lp.getStepSize());
            if (end < start) {
                step *= -1;
            }

            //addPositionerChannel(id, name, readback);
            add("for " + setpointVar + " in frange(" + start + ", " + end + ", " + step + ", True):");
            indentation += 4;
            add("if " + setpointVar + " > " + Math.max(end, start) + " or  " + setpointVar + " < " + Math.min(end, start) + ":");
            add(indent(4) + "break");

            double accuracy = step / 2;
            addSetPositioner(id, readback, async, type, done, doneDelay, doneValue, settlingTime, accuracy);
        }
    }

    void addPositionerChannel(String id, String setpointChannel, String readbackChannel) {
        String readbackId = id + "Readback";
        add(id + " = Channel('" + setpointChannel + "', type = 'd')");
        if ((readbackChannel != null) & (!readbackChannel.isBlank())){
            add(readbackId + " = Channel('" + readbackChannel + "', type = 'd')"); //
        }
    }

    void addSetPositioner(String id, String readback, boolean async, String type, String done, double doneDelay, String doneValue, double settlingTime, double accuracy) {
        String readbackId = id + "Readback";
        if (async) {
            add(id + ".putq(" + setpointVar + ")");
        } else {
            add(id + ".put(" + setpointVar + ", timeout=None) # TODO: Set appropriate timeout");
        }
        if (done != null) {
            add("sleep( " + doneDelay + " ) # Done delay");
            if (getType(type).equals('s')) {
                doneValue = "'" + doneValue + "'";
            }
            add("cawait('" + done + "', " + doneValue + ", '" + getType(type) + "')");
        }

        if ((readback != null) && (!readback.isBlank())){
            add(readbackVar + " = " + readbackId + ".get()");
        } else {
            add(readbackVar + " = " + id + ".get()");
        }

        add("if abs(" + readbackVar + " - " + setpointVar + ") > " + accuracy + " : # TODO: Check accuracy");
        add(indent(4) + "raise Exception('Actor " + id + " could not be set to the value ' + str(" + setpointVar + "))");

        if (settlingTime > 0) {
            add("sleep( " + settlingTime + " ) # Settling time");
        }
        scanSetpoints.add(setpointVar);
        scanReadbacks.add(readbackVar);
        scanTags.put(id, setpointVar);
    }

    int getNumberOfSteps(double start, double end, double step) {
        return (int) Math.ceil(Math.abs((start - end) / step));
    }

    void closePositionerChannels(DiscreteStepPositioner positioner) {
        String id = positioner.getId();
        String readback = positioner.getReadback();
        String readbackId = id + "Readback";

        //indentation -= 4;
        if (!(positioner instanceof PseudoPositioner)) {
            add(id + ".close()");
            if ((readback != null) && (!readback.isBlank())){
                add(readbackId + ".close()");
            }
        }
    }

    void addDetectors(List<Detector> detectors) {
        for (Detector detector : detectors) {
            String id = detector.getId();
            add("#Detector " + id);
            String var = "detector" + detectorIndex++;

            if (detector instanceof SimpleDetector sd) {
                if (detector instanceof ScalerChannel) {
                    ScalerChannel sc = (ScalerChannel) detector;
                } else if (detector instanceof Timestamp t) {
                    add(var + " = float(java.lang.System.currentTimeMillis())");
                    scanValues.add(var);
                    scanTags.put(id, var);
                } else if (detector instanceof SimpleScalarDetector ssd) {
                }

            } else if (detector instanceof ComplexDetector cd) {
                String name = cd.getName();
                List<Action> preActions = cd.getPreAction();
                if (preActions.size() > 0) {
                    add("#Detector " + name + " pre-actions");
                    for (Action action : preActions) {
                        addAction(action);
                    }
                }
                if (detector instanceof DetectorOfDetectors dd) {
                } else if (detector instanceof ArrayDetector ad) {
                    int size = ad.getArraySize();
                    add(var + " = " + id + ".get()");
                    scanValues.add(var);
                    scanTags.put(id, var);
                } else if (detector instanceof ScalarDetector sd) {
                    String type = sd.getType();
                    add(var + " = " + id + ".get()");
                    scanValues.add(var);
                    scanTags.put(id, var);
                }
            }
        }
    }

    void createDetectorChannels(List<Detector> detectors) {
        for (Detector detector : detectors) {
            String id = detector.getId();
            add("#" + detector.getClass().getSimpleName() + " " + id);

            if (detector instanceof SimpleDetector sd) {
                if (detector instanceof ScalerChannel sc) {
                    add("#Error: " + "ScalerChannel not supported\n");
                } else if (detector instanceof Timestamp) {
                } else if (detector instanceof SimpleScalarDetector ssd) {
                    add("#Error: " + "SimpleScalarDetector not supported\n");
                }

            } else if (detector instanceof ComplexDetector cd) {
                if (detector instanceof DetectorOfDetectors) {
                    DetectorOfDetectors dd = (DetectorOfDetectors) detector;
                    add("#Error: " + "DetectorOfDetectors not supported\n");
                } else if (detector instanceof ArrayDetector ad) {
                    int size = ad.getArraySize();
                    add(id + " = Channel('" + ad.getName() + "', type = '[d', size = " + size + ")");

                } else if (detector instanceof ScalarDetector sd) {
                    String type = sd.getType();
                    add(id + " = Channel('" + sd.getName() + "', type = '" + getType(type) + "')");
                }
            }
        }
    }

    void closeDetectorsChannels(List<Detector> detectors) {
        for (Detector detector : detectors) {
            String id = detector.getId();
            if (detector instanceof SimpleDetector sd) {
                if (detector instanceof ScalerChannel sc) {
                } else if (detector instanceof Timestamp t) {
                } else if (detector instanceof SimpleScalarDetector ssd) {
                }

            } else if (detector instanceof ComplexDetector cs) {
                if (detector instanceof DetectorOfDetectors dd) {
                } else if (detector instanceof ArrayDetector ad) {
                    add(id + ".close()");
                } else if (detector instanceof ScalarDetector sd) {
                    add(id + ".close()");
                }
            }
        }
    }

    String getType(String type) {
        if (type.equals("Integer")) {
            return "l";
        } else if (type.equals("Double")) {
            return "d";
        }
        return "s";

    }

    String getValue(Object value, String type) {
        String str = String.valueOf(value);
        if (type.equals("Integer")) {
            return String.valueOf(Math.round(Double.valueOf(str)));
        } else if (type.equals("Double")) {
            return Double.valueOf(str).toString();
        } else {
            return String.format("'%s'", value);
        }
    }

    void add(String code) {
        add(code, indentation);
    }

    String indent(int offset) {
        char[] array = new char[offset];
        Arrays.fill(array, " ".charAt(0));
        return new String(array);
    }

    void add(String code, int offset) {
        sb.append(indent(offset)).append(code).append("\n");
    }

    private static String resolveIdRef(Object object) {
        String id;
        if (object instanceof Positioner positioner) {
            id = positioner.getId();
        } else if (object instanceof Detector detector) {
            id = detector.getId();
        } else if (object instanceof Manipulation manipulation) {
            id = manipulation.getId();
        } else {
            throw new RuntimeException("Unable to identify id of object reference " + object);
        }
        return id;
    }

    public void main(String[] args) throws Exception {
        String fileName = args[0];
        String script = new ImporterXScan().importFile(new File(fileName));
        System.out.println(script);

    }
}
