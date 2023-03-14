package ch.psi.pshell.xscan;

import ch.psi.utils.EventBus;
import ch.psi.utils.EventBusListener;
import ch.psi.pshell.xscan.core.ActionLoop;
import ch.psi.pshell.xscan.core.Actor;
import ch.psi.pshell.xscan.core.ActorSensorLoop;
import ch.psi.pshell.xscan.core.ChannelAccessCondition;
import ch.psi.pshell.xscan.core.ChannelAccessFunctionActuator;
import ch.psi.pshell.xscan.core.ChannelAccessGuard;
import ch.psi.pshell.xscan.core.ChannelAccessGuardCondition;
import ch.psi.pshell.xscan.core.ChannelAccessLinearActuator;
import ch.psi.pshell.xscan.core.ChannelAccessPut;
import ch.psi.pshell.xscan.core.ChannelAccessSensor;
import ch.psi.pshell.xscan.core.ChannelAccessTableActuator;
import ch.psi.pshell.xscan.core.ComplexActuator;
import ch.psi.pshell.xscan.core.CrlogicLoopStream;
import ch.psi.pshell.xscan.core.CrlogicResource;
import ch.psi.pshell.xscan.core.Delay;
import ch.psi.pshell.xscan.core.DeviceLinearActuator;
import ch.psi.pshell.xscan.core.JythonFunction;
import ch.psi.pshell.xscan.core.JythonGlobalVariable;
import ch.psi.pshell.xscan.core.JythonManipulation;
import ch.psi.pshell.xscan.core.JythonParameterMapping;
import ch.psi.pshell.xscan.core.JythonParameterMappingChannel;
import ch.psi.pshell.xscan.core.JythonParameterMappingGlobalVariable;
import ch.psi.pshell.xscan.core.JythonParameterMappingID;
import ch.psi.pshell.xscan.core.Manipulation;
import ch.psi.pshell.xscan.core.ParallelCrlogic;
import ch.psi.pshell.xscan.core.PseudoActuatorSensor;
import ch.psi.pshell.xscan.core.ScrlogicLoop;
import ch.psi.pshell.xscan.core.Sensor;
import ch.psi.pshell.xscan.core.TimestampSensor;
import ch.psi.pshell.xscan.model.Action;
import ch.psi.pshell.xscan.model.ArrayDetector;
import ch.psi.pshell.xscan.model.ArrayPositioner;
import ch.psi.pshell.xscan.model.ChannelAction;
import ch.psi.pshell.xscan.model.ChannelParameterMapping;
import ch.psi.pshell.xscan.model.Configuration;
import ch.psi.pshell.xscan.model.ContinuousDimension;
import ch.psi.pshell.xscan.model.ContinuousPositioner;
import ch.psi.pshell.xscan.model.Detector;
import ch.psi.pshell.xscan.model.DetectorOfDetectors;
import ch.psi.pshell.xscan.model.DiscreteStepDimension;
import ch.psi.pshell.xscan.model.DiscreteStepPositioner;
import ch.psi.pshell.xscan.model.Function;
import ch.psi.pshell.xscan.model.FunctionPositioner;
import ch.psi.pshell.xscan.model.Guard;
import ch.psi.pshell.xscan.model.GuardCondition;
import ch.psi.pshell.xscan.model.IDParameterMapping;
import ch.psi.pshell.xscan.model.LinearPositioner;
import ch.psi.pshell.xscan.model.ParameterMapping;
import ch.psi.pshell.xscan.model.Positioner;
import ch.psi.pshell.xscan.model.PseudoPositioner;
import ch.psi.pshell.xscan.model.Recipient;
import ch.psi.pshell.xscan.model.Region;
import ch.psi.pshell.xscan.model.RegionPositioner;
import ch.psi.pshell.xscan.model.ScalarDetector;
import ch.psi.pshell.xscan.model.ScalerChannel;
import ch.psi.pshell.xscan.model.Scan;
import ch.psi.pshell.xscan.model.ScriptAction;
import ch.psi.pshell.xscan.model.ScriptManipulation;
import ch.psi.pshell.xscan.model.ShellAction;
import ch.psi.pshell.xscan.model.SimpleScalarDetector;
import ch.psi.pshell.xscan.model.Timestamp;
import ch.psi.pshell.xscan.model.Variable;
import ch.psi.pshell.xscan.model.VariableParameterMapping;
import ch.psi.pshell.xscan.model.Visualization;
import ch.psi.pshell.xscan.plot.LinePlot;
import ch.psi.pshell.xscan.plot.VDescriptor;
import ch.psi.pshell.xscan.plot.XYSeries;
import ch.psi.pshell.xscan.plot.XYZSeries;
import ch.psi.pshell.xscan.plot.YSeries;
import ch.psi.pshell.xscan.plot.YZSeries;
import ch.psi.pshell.xscan.ui.ModelUtil;
import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelDescriptor;
import ch.psi.jcae.ChannelService;
import ch.psi.jcae.impl.type.DoubleTimestamp;
import ch.psi.jcae.util.ComparatorAND;
import ch.psi.jcae.util.ComparatorOR;
import ch.psi.jcae.util.ComparatorREGEX;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ExecutionParameters;
import ch.psi.pshell.core.InlineDevice;
import ch.psi.pshell.core.LogManager;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.xscan.core.DeviceFunctionActuator;
import ch.psi.pshell.xscan.core.DeviceSensor;
import ch.psi.pshell.xscan.core.DeviceTableActuator;
import ch.psi.utils.Str;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Data acquisition engine for performing scans
 * Mapping is specific to scan model version 1.0
 */
public class Acquisition {
	
	private static Logger logger = Logger.getLogger(Acquisition.class.getName());

	private final AcquisitionConfiguration configuration;
	
	private ActionLoop actionLoop;
	private Manipulator manipulator;
	private EventBusListener serializer;
	
	private List<Manipulation> manipulations;
	private volatile boolean active = false;
	
	private NotificationAgent notificationAgent;
	
	private Handler logHandler = null;
	
	private File datafile;
	
	
	private ChannelService cservice;
	private List<Channel<?>> channels = new ArrayList<>();
	private List<Object> templates = new ArrayList<>();
	
	
	private Configuration configModel;
	
	private HashMap<String, JythonGlobalVariable> jVariableDictionary = new HashMap<String, JythonGlobalVariable>();
                      
        
        private final Map<String, Object> vars;
        
        private int functionId;
	
	public Acquisition(ChannelService cservice, AcquisitionConfiguration configuration, Map<String, Object> vars){
		this.cservice = cservice;
		this.configuration = configuration;
		this.actionLoop = null;
		this.manipulations = new ArrayList<Manipulation>();
                this.vars = vars;
	}
        
        private volatile Thread acquisitionThread;
        
        ExecutionParameters executionParameters = null;
	
	
	
	/**
	 * Get state of the acquisition engine
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

        public int getFunctionId(){
            return functionId++;
        }


	/**
	 * Acquire data
	 * 
	 * @param smodel		Model of the scan
	 * @param getQueue	Flag whether to return a queue or not. If false the return value of the function will be null.
	 * @throws InterruptedException
	 */
	public void initalize(EventBus bus, Configuration smodel){            

		// Create notification agent with globally configured recipients
		notificationAgent = new NotificationAgent();
		
		// Update recipients list of the Notifiaction Agent
		if(smodel.getNotification()!=null){
			for(Recipient r: smodel.getNotification().getRecipient()){
				notificationAgent.getRecipients().add(r);
			}
		}
		   
               
                String fprefix=configuration.getDataFilePrefix();
                
                if ((smodel.getData().getFileName()!=null) && (!smodel.getData().getFileName().trim().isEmpty())){
                    fprefix = fprefix.replace(Setup.TOKEN_EXEC_NAME, smodel.getData().getFileName()); 
                }
                fprefix = Context.getInstance().getSetup().expandPath(fprefix);
		// Construct filenames
		File xmlfile = new File(fprefix+".xml");
               
                
                if (Context.getInstance().getConfig().fdaSerialization){
                    //Priority to Type field
                    datafile = new File(fprefix+".txt");
                    executionParameters = Context.getInstance().getExecutionPars();
                    this.serializer = new SerializerTXT(datafile, configuration.getAppendSuffix(), executionParameters.getFlush());                    
                    executionParameters.setDataPath(datafile.getParentFile()); //Create base dir and trigger callbacks
                    
                } else {
                    this.serializer = new SerializerPShell(fprefix);
                    datafile = new File(Context.getInstance().getDataManager().getRootFileName());
                } 

		// Create required directories
                DataManager dm = Context.getInstance().getDataManager();
                		
		try{
			// Workaround - to avoid that multiple handlers get attached
			// this should be removed when rewriting the acquisition logic
			if(logHandler!=null){
                            Logger.getLogger("ch.psi.pshell.xscan").removeHandler(logHandler);
			}
			
			if (Context.getInstance().getConfig().fdaSerialization){
                            File logfile = new File(fprefix+".log");
                            logHandler = new FileHandler(logfile.getAbsolutePath());
                            logHandler.setFormatter(LogManager.formatter);
                        } else{
                            logHandler = new Handler() {
                                @Override
                                public void publish(LogRecord rec) {
                                    try {
                                        if (dm.isOpen()){
                                            String[] tokens = LogManager.parseLogRecord(rec);                            
                                            String log =  tokens[3] + " - " + tokens[4] + " [" + tokens[2] + "]";
                                            dm.appendLog(log);
                                        }
                                    } catch (Exception ex) {
                                          ex.printStackTrace();
                                    }
                                }

                                @Override
                                public void flush() {
                                }

                                @Override
                                public void close() throws SecurityException {
                                }
                            };
                        }
		    Logger.getLogger("ch.psi.pshell.xscan").addHandler(logHandler);
		    
		} catch (Exception ex) {
			ex.printStackTrace();
                }
		
		
		// Save a copy of the model to the data directory
		try {
                        ModelManager.marshall(smodel, xmlfile);
                        if (Context.getInstance()!=null){
                            Context.getInstance().addDetachedFileToSession(xmlfile);
                        }                    
                        //if (!serializationTXT){
                        //    dm.setDataset("/xml", Files.readAllBytes(xmlfile.toPath()));
                        //}
		} catch (Exception e) {
			throw new RuntimeException("Unable to serialize scan",e);
		}		

                if (vars!=null){
                    for (String var : vars.keySet()){
                        String id = var;
                        try{
                            //String setter = null;
                            Object value = vars.get(id);
                            if (id.contains(".")){                                
                                //int index = id.lastIndexOf(".");
                                //setter = "set" + Str.capitalizeFirst(id.substring(index + 1));
                                //id = id.substring(0, index);                                
                                String[] tokens = id.split("\\.");
                                if (tokens.length >3){
                                   throw new Exception();
                                }
                                id = tokens[0];
                                Object obj = ModelUtil.getInstance().getObject(id);
                                if (tokens.length == 3){
                                    int index = -1;
                                    String getter = "get" + Str.capitalizeFirst(tokens[1]); 
                                    if (getter.contains("[") && getter.endsWith("]")){
                                        int aux = getter.indexOf("[");
                                        index = Integer.valueOf(getter.substring(aux+1,getter.length()-1 ));
                                        getter = getter.substring(0, aux);                                        
                                    } 
                                    for (Method m: obj.getClass().getMethods()){
                                        if (m.getName().equals(getter)){
                                            obj = m.invoke(obj, new Object[]{});
                                            if (index>=0){
                                                if (obj instanceof List){
                                                    obj = ((List)obj).get(index);
                                                } else {
                                                    obj = Array.get(obj, index);
                                                }
                                            }
                                            break;
                                        }
                                    }                                                                        
                                }                                
                                String setter = "set" + Str.capitalizeFirst(tokens[tokens.length - 1]); 
                                for (Method m: obj.getClass().getMethods()){
                                    if (m.getName().equals(setter)){
                                        m.invoke(obj, new Object[]{value});
                                        break;
                                    }
                                }
                            } else {
                                Object obj = ModelUtil.getInstance().getObject(id);
                                if (obj instanceof Variable){
                                    ((Variable)obj).setValue(value);
                                    }
                                }
                        } catch (Exception ex){
                            throw new RuntimeException("Bad parameter value: " + var);
                        }
                    }
                }
                
                
		logger.fine("Map Model to internal logic");

                if(smodel.getScan().getManipulation()!= null && smodel.getScan().getManipulation().size()>0){      
			// Setup optimized with manipulations

                        EventBus b = new EventBus(AcquisitionConfiguration.eventBusModeAcq);                        
                        
			// Map scan to base model
			Collector collector = new Collector(b);
			mapScan(collector, smodel);
//			col = collector;
			logger.fine("ActionLoop and Collector initialized");
	
	
	
			// Add manipulator into processing chain
			this.manipulator = new Manipulator(bus, this.manipulations);
			b.register(this.manipulator);						
                }
		else{
			// Setup optimized without manipulations
			Collector collector = new Collector(bus);
			mapScan(collector, smodel);
//			col = collector;
			
		}
                bus.register(serializer);
	}
	
	/**
	 * Execute acquisition
	 * @throws InterruptedException 
	 */
	public void execute() throws InterruptedException {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname="unknown";
		}
		
		try{
                        acquisitionThread = Thread.currentThread();
			active = true;
	
			actionLoop.prepare();
			actionLoop.execute();
			actionLoop.cleanup();
	
			notificationAgent.sendNotification("Notification - XScan Execution Finished", "The execution of the XScan on '"+hostname+"' for file '"+datafile.getName()+"' finished successfully\n\nYou received this message because you are listed in the notification list for this data acquisition configuration.", false,true);
		}
		catch(InterruptedException e){
			logger.log(Level.INFO, "Execution interrupted: ", e);
			notificationAgent.sendNotification("Notification - XScan Execution was aborted", "The execution of the XScan on '"+hostname+"' for file '"+datafile.getName()+"' was aborted\n\nYou received this message because you are listed in the notification list for this data acquisition configuration.", false, true);
                        throw e;
		}
		catch(Exception e){
			logger.log(Level.WARNING, "Execution failed: ", e);
			notificationAgent.sendNotification("Notification - XScan Execution Failed", "The execution of the XScan failed on '"+hostname+"' for file '"+datafile.getName()+"'\n\nYou received this message because you are listed in the notification list for this data acquisition configuration.", true,false);
			throw e;
		}
		finally{
			active = false;
                        acquisitionThread = null;
		}
	}
	

	public void destroy(){
		if(actionLoop != null){
			logger.finest("Destroy managed resources");
			
			for(Channel<?> c: channels){
				try {
					c.destroy();
				} catch (Exception e) {
					logger.severe("Unable to destroy channel "+c.getName() + ": " + e.getMessage());
				}
			}
			for(Object o: templates){
				try {
					cservice.destroyAnnotatedChannels(o);
				} catch (Exception e) {
					logger.severe("Unable to destroy channels of template "+o.getClass().getName() + ": " + e.getMessage());
				}
			}
			
		}
		
		// Clear global variables Jython
		jVariableDictionary.clear();
                if (executionParameters!=null){
                    executionParameters.setDataPath(null);
                }
                
		// Remove log handler
		if(logHandler!=null){
			logger.fine("Close log handler");
			logHandler.close();
    			Logger.getLogger("").removeHandler(logHandler);
		}
	}

	public void abort(){
            if(actionLoop != null){
                try{
                    actionLoop.abort();                    
                }
                catch (Exception ex){
                    Logger.getLogger(ActorSensorLoop.class.getName()).log(Level.WARNING,null,ex);
                } 
                //Unpredictable consequences for crlogic?
                //Thread.sleep(100);
                //interrupt();
            }
            
	}   
        
        public void interrupt(){
            if ((acquisitionThread!=null) && (acquisitionThread!=Thread.currentThread())){
                try{
                    acquisitionThread.interrupt();
                } catch (Exception ex){
                }
            }
            
        }
	
	public void pause(){
            if(actionLoop != null){
                try{
                    actionLoop.pause();
                }
                catch (Exception ex){
                    Logger.getLogger(ActorSensorLoop.class.getName()).log(Level.WARNING,null,ex);
                }                         		
            }
	}

	public void resume(){
            if(actionLoop != null){
                try{
                    actionLoop.resume();
                }
                catch (Exception ex){
                    Logger.getLogger(ActorSensorLoop.class.getName()).log(Level.WARNING,null,ex);
                }                         		
            }
	}
        
	public boolean canPause(){
            if ((actionLoop != null) && (actionLoop instanceof ActorSensorLoop)){
                return !actionLoop.isPaused();
            }
            return false;
	}        

        public String getDatafileName(){
		return(datafile.getName());
	}
	
	/**
	 * Retrieve id string of the passed object
	 * @param object
	 * @return	Id string of object
	 */
	private static String resolveIdRef(Object object){
		String id;
		if(object instanceof Positioner){
			id = ((Positioner)object).getId();
		}
		else if (object instanceof Detector){
			id = ((Detector)object).getId();
		}
		else if (object instanceof ch.psi.pshell.xscan.model.Manipulation){
			id = ((ch.psi.pshell.xscan.model.Manipulation)object).getId();
		}
		else{
			throw new RuntimeException("Unable to identify id of object reference "+object);
		}
		return id;
	}
	
	
	
	/**
	 * Map scan to base model
	 * @param scan
	 */
	private void mapScan(Collector collector, Configuration configuration){
		this.configModel = configuration;
		Scan scan = configuration.getScan();
		
		for(Variable v: configuration.getVariable()){
			JythonGlobalVariable var = new JythonGlobalVariable();
			var.setName(v.getName());
			var.setValue(v.getValue());
			jVariableDictionary.put(v.getName(), var);                      
		}
                                
		// Map continuous dimension
		if(scan.getCdimension() != null){
			ActionLoop aLoop = mapContinuousDimension(scan.getCdimension());
			actionLoop = aLoop;
			collector.addEventBus(aLoop.getEventBus());
		}
		
		// Map discrete step dimensions
		for(DiscreteStepDimension d: scan.getDimension()){
			ActorSensorLoop l = mapDiscreteStepDimension(d);
			collector.addEventBus(l.getEventBus());
			if(actionLoop != null){
				l.getActionLoops().add(actionLoop);
			}
			actionLoop = l;
		}
		
		// No dimensions where specified for scan
		if(actionLoop == null){
			actionLoop = new ActorSensorLoop();
		}
		
		// Map pre actions to pre actions of the top level dimension
		actionLoop.getPreActions().addAll(mapActions(scan.getPreAction()));
		
		// Map post actions to post actions of the top level dimension
		actionLoop.getPostActions().addAll(mapActions(scan.getPostAction()));
		
		
		// TODO need to be removed! and done differently !!!!
		// Handle iterations by adding a pseudo dimension and setting the 
		// datagroup flag in the main loop
		if(configuration.getNumberOfExecution()>1){
			// Create Iterations pseudo loop
			ActorSensorLoop l = new ActorSensorLoop();
			PseudoActuatorSensor a = new PseudoActuatorSensor("iterations", configuration.getNumberOfExecution());
			l.getActors().add(a);
			l.getActionLoops().add(actionLoop);
			actionLoop.setDataGroup(true);  // Need to add setDataGroup to ActionLoop interface
			
			// Set toplevel action loop
			actionLoop = l;
			collector.addEventBus(l.getEventBus());
		}
		
		// handling manipulations
		for(ch.psi.pshell.xscan.model.Manipulation m : scan.getManipulation()){
			if(m instanceof ScriptManipulation){
				ScriptManipulation sm = (ScriptManipulation) m;
				
				List<JythonParameterMapping> mapping = new ArrayList<JythonParameterMapping>();
				for(ParameterMapping pm: sm.getMapping()){
					if(pm instanceof IDParameterMapping){
						String refid = resolveIdRef(((IDParameterMapping)pm).getRefid());
						mapping.add( new JythonParameterMappingID(pm.getVariable(), refid));
					}
					else if(pm instanceof ChannelParameterMapping){
						ChannelParameterMapping cpm = (ChannelParameterMapping) pm;
						if(cpm.getType().equals("String")){
							mapping.add( new JythonParameterMappingChannel<String>(cpm.getVariable(), createChannel(String.class, cpm.getChannel())));
						}
						else if(cpm.getType().equals("Integer")){
							mapping.add( new JythonParameterMappingChannel<Integer>(cpm.getVariable(), createChannel(Integer.class, cpm.getChannel())));
						}
						else if(cpm.getType().equals("Double")){
							mapping.add( new JythonParameterMappingChannel<Double>(cpm.getVariable(), createChannel(Double.class, cpm.getChannel())));
						}
						else{
							logger.warning("Channel type ["+cpm.getType()+"] is not supported for mapping");
						}
					}
					else if(pm instanceof VariableParameterMapping){
						VariableParameterMapping vp = (VariableParameterMapping) pm;
						Variable v = (Variable)vp.getName();
						JythonGlobalVariable var = jVariableDictionary.get(v.getName());
						var.setValue(v.getValue());
						mapping.add(new JythonParameterMappingGlobalVariable(vp.getVariable(), var));
					}
				}
				
				JythonManipulation manipulation = new JythonManipulation(sm.getId(), sm.getScript(), mapping, sm.isReturnArray());
				
				if(configuration.getData()!=null){ // Safety
					manipulation.setVariable("FILENAME", configuration.getData().getFileName());
					manipulation.setVariable("DATAFILE", datafile.getAbsoluteFile());
				}
				
				this.manipulations.add(manipulation);
			}
		}
	}
	
	/**
	 * Map a model action to base actions
	 * @param actions
	 * @return
	 */
	private List<ch.psi.pshell.xscan.core.Action> mapActions(List<Action> actions){
		List<ch.psi.pshell.xscan.core.Action> alist = new ArrayList<ch.psi.pshell.xscan.core.Action>();
		for(Action a: actions){
			if(a instanceof ChannelAction){
				ChannelAction ca = (ChannelAction) a;
				
				String operation = ca.getOperation(); // Default = put
				String type=ca.getType(); // Default = String
				
				if(operation.equals("put")){
					Long timeout = null;
					if(ca.getTimeout()!=null){
						timeout = Math.round(ca.getTimeout()*1000);
					}
					if(type.equals("String")){
						alist.add(new ChannelAccessPut<String>(createChannel(String.class, ca.getChannel()), ca.getValue(), false, timeout));
					}
					else if(type.equals("Integer")){
						alist.add(new ChannelAccessPut<Integer>(createChannel(Integer.class, ca.getChannel()), Integer.parseInt(ca.getValue()), false, timeout));
					}
					else if(type.equals("Double")){
						alist.add(new ChannelAccessPut<Double>(createChannel(Double.class,ca.getChannel()), Double.parseDouble(ca.getValue()), false, timeout));
					}
				}
				else if(operation.equals("putq")){
					if(type.equals("String")){
						alist.add(new ChannelAccessPut<String>(createChannel(String.class,ca.getChannel()), ca.getValue(), true, null));
					}
					else if(type.equals("Integer")){
						alist.add(new ChannelAccessPut<Integer>(createChannel(Integer.class,ca.getChannel()), Integer.parseInt(ca.getValue()), true, null));
					}
					else if(type.equals("Double")){
						alist.add(new ChannelAccessPut<Double>(createChannel(Double.class,ca.getChannel()), Double.parseDouble(ca.getValue()), true, null));
					}
				}
				else if(operation.equals("wait")){
					Long timeout = null ; // Default timeout = wait forever
					if(ca.getTimeout()!=null){
						timeout = Math.round(ca.getTimeout()*1000);
					}
					if(type.equals("String")){
						alist.add(new ChannelAccessCondition<String>(createChannel(String.class,ca.getChannel()), ca.getValue(), timeout));
					}
					else if(type.equals("Integer")){
						alist.add(new ChannelAccessCondition<Integer>(createChannel(Integer.class,ca.getChannel()), Integer.parseInt(ca.getValue()), timeout));
					}
					else if(type.equals("Double")){
						alist.add(new ChannelAccessCondition<Double>(createChannel(Double.class,ca.getChannel()), Double.parseDouble(ca.getValue()), timeout));
					}
				}
				else if(operation.equals("waitREGEX")){
					Long timeout = null ; // Default timeout = wait forever
					if(ca.getTimeout()!=null){
						timeout = Math.round(ca.getTimeout()*1000);
					}
					if(type.equals("String")){
						alist.add(new ChannelAccessCondition<>(createChannel(String.class, ca.getChannel()), ca.getValue(), new ComparatorREGEX(), timeout));
					}
					else{
						logger.warning("Operation "+operation+" wity type "+type+" for action is not supported");
					}
				}
				else if(operation.equals("waitOR")){
					Long timeout = null ; // Default timeout = wait forever
					if(ca.getTimeout()!=null){
						timeout = Math.round(ca.getTimeout()*1000);
					}
					
					if(type.equals("Integer")){
						alist.add(new ChannelAccessCondition<>(createChannel(Integer.class,ca.getChannel()), Integer.parseInt(ca.getValue()), new ComparatorOR(), timeout));
					}
					else{
						logger.warning("Operation "+operation+" wity type "+type+" for action is not supported");
					}
				}
				else if(operation.equals("waitAND")){
					Long timeout = null ; // Default timeout = wait forever
					if(ca.getTimeout()!=null){
						timeout = Math.round(ca.getTimeout()*1000);
					}
					if(type.equals("Integer")){
						alist.add(new ChannelAccessCondition<>(createChannel(Integer.class,ca.getChannel()),Integer.parseInt(ca.getValue()), new ComparatorAND(), timeout));
					}
					else {
						logger.warning("Operation "+operation+" wity type "+type+" for action is not supported");
					}
				}
				else{
					// Operation not supported
					logger.warning("Operation "+operation+" for action is not supported");
				}
				
				// Translate delay attribute to delay action
				if(ca.getDelay()!=null){
					Double x = ca.getDelay()*1000;
					alist.add(new Delay(x.longValue()));
				}
				
			}
			else if(a instanceof ShellAction){
				ShellAction sa = (ShellAction) a;
				String com = sa.getCommand().replaceAll("\\$\\{DATAFILE\\}", datafile.getAbsolutePath());
				com = com.replaceAll("\\$\\{FILENAME\\}", datafile.getName().replaceAll("\\.\\w*$", ""));
				ch.psi.pshell.xscan.core.ShellAction action = new ch.psi.pshell.xscan.core.ShellAction(com);
				action.setCheckExitValue(sa.isCheckExitValue());
				action.setExitValue(sa.getExitValue());
				alist.add(action);
			}
			else if(a instanceof ScriptAction){
				
				ScriptAction sa = (ScriptAction) a;
				
				// TODO set global variables DATAFILE and FILENAME
				
				// TODO create Jython Action
				Map<String, Channel<?>> mapping = new HashMap<>();
				for(ChannelParameterMapping ma: sa.getMapping()){
					if(ma.getType().equals("String")){
						mapping.put(ma.getVariable(), createChannel(String.class, ma.getChannel()));
					}
					else if(ma.getType().equals("Integer")){
						mapping.put(ma.getVariable(), createChannel(Integer.class, ma.getChannel()));
					}
					else if(ma.getType().equals("Double")){
						mapping.put(ma.getVariable(), createChannel(Double.class, ma.getChannel()));
					}
					else{
						logger.warning("Channel type ["+ma.getType()+"] is not supported for mapping");
					}
				}
				
				Map<String,Object> gobjects = new HashMap<>();
				gobjects.put("FILENAME", datafile.getName().replaceAll("\\.\\w*$", ""));
				gobjects.put("DATAFILE", datafile.getAbsoluteFile());
				ch.psi.pshell.xscan.core.JythonAction ja = new ch.psi.pshell.xscan.core.JythonAction(sa.getScript(), getFunctionId(), mapping, gobjects);
				
				alist.add(ja);
			}
		}
		return(alist);
	}
        
        
        Device getDevice(String name){
            Device dev = Context.getInstance().getDevicePool().getByName(name, Device.class);
            if (dev!=null){
                return dev;
            }
            try{
                return InlineDevice.create(name, null);
            } catch (Exception ex){
                
            }
            return null;
        }
	
	/**
	 * Map a discrete step dimension onto a actor sensor loop
	 * @param dimension
	 * @return
	 */
	private ActorSensorLoop mapDiscreteStepDimension(DiscreteStepDimension dimension){
		ActorSensorLoop aLoop = new ActorSensorLoop(dimension.isZigzag());
		// Set split flag of action loop (default is false)
		aLoop.setDataGroup(dimension.isDataGroup()); 
		
		// Mapping dimension pre-actions
		aLoop.getPreActions().addAll(mapActions(dimension.getPreAction()));
		
		Long moveTimeout = Long.valueOf(configuration.getActorMoveTimeout() * 1000); //millis
		
		// Mapping positioners
		Double stime = 0d;
		for(DiscreteStepPositioner p: dimension.getPositioner()){
			
			if(p.getSettlingTime()>stime){
				stime = p.getSettlingTime();
			}
			
			if(p instanceof LinearPositioner){
				LinearPositioner lp =(LinearPositioner) p;
                                Actor actuator;
                                Sensor sensor;
                                Device dev = getDevice(lp.getName());
                                ChannelAccessLinearActuator<?> a;
                                if (dev != null){
                                    a = new DeviceLinearActuator(dev, createDoneChannel(lp), getDoneValue(lp), lp.getDoneDelay(), lp.getStart(), lp.getEnd(), lp.getStepSize(), moveTimeout);
                                    sensor = new DeviceSensor(lp.getId(), dev, configModel.isFailOnSensorError());
                                } else {
                                   a = new ChannelAccessLinearActuator(createChannel(Double.class, lp.getName()), createDoneChannel(lp), getDoneValue(lp), lp.getDoneDelay(), lp.getStart(), lp.getEnd(), lp.getStepSize(), moveTimeout);
                                    // Add a sensor for the readback
                                    String name = lp.getReadback();
                                    if((name==null)||name.isBlank()){
                                            name = lp.getName();
                                    }
                                    sensor = new ChannelAccessSensor<Double>(lp.getId(), createChannel(Double.class, name), configModel.isFailOnSensorError());
                                }
                                a.setAsynchronous(lp.isAsynchronous());
                                actuator = a;
			
				aLoop.getActors().add(actuator);
				aLoop.getSensors().add(sensor);
			}
			else if(p instanceof FunctionPositioner){
				FunctionPositioner lp =(FunctionPositioner) p;
				
                                
				// Create function object
				JythonFunction function = mapFunction(lp.getFunction());
				
                                Device dev = getDevice(lp.getName());
                                ChannelAccessFunctionActuator<?> a;
                                Sensor sensor;
                                if (dev != null){                                                            
                                     a = new DeviceFunctionActuator(dev, createDoneChannel(lp), getDoneValue(lp), lp.getDoneDelay(), function, lp.getStart(), lp.getEnd(), lp.getStepSize(), moveTimeout);
                                     sensor = new DeviceSensor(lp.getId(), dev, configModel.isFailOnSensorError());
                                } else {
                                    // Create actuator
                                     a = new ChannelAccessFunctionActuator(createChannel(Double.class,lp.getName()), createDoneChannel(lp), getDoneValue(lp), lp.getDoneDelay(), function, lp.getStart(), lp.getEnd(), lp.getStepSize(), moveTimeout);       
                                    // Add a sensor for the readback
                                    String name = lp.getReadback();
                                    if((name==null)||name.isBlank()){
                                            name = lp.getName();
                                    }
                                    sensor = new ChannelAccessSensor<Double>(lp.getId(), createChannel(Double.class, name), configModel.isFailOnSensorError());

                                }
				
				a.setAsynchronous(lp.isAsynchronous());
				Actor actuator = a;
				
				aLoop.getActors().add(actuator);
                                aLoop.getSensors().add(sensor);
			}
			else if (p instanceof ArrayPositioner){
				ArrayPositioner ap = (ArrayPositioner) p;
				String[] positions = (ap.getPositions().trim()).split(" +");
				double[] table = new double[positions.length];
				for(int i=0;i<positions.length;i++){
					table[i] = Double.parseDouble(positions[i]);
				}
				
				ChannelAccessTableActuator<?> a;
                                Device dev = getDevice(p.getName());
                                Sensor sensor;
                                if (dev != null){                                                            
                                     a = new DeviceTableActuator(dev, createDoneChannel(p), getDoneValue(p), p.getDoneDelay(), table, moveTimeout);
                                     sensor = new DeviceSensor(p.getId(), dev, configModel.isFailOnSensorError());
                                } else {
                                    a = new ChannelAccessTableActuator(createChannel(Double.class, p.getName()),  createDoneChannel(p), getDoneValue(p), p.getDoneDelay(), table, moveTimeout);
                                    // Add a sensor for the readback
                                    String name = ap.getReadback();
                                    if((name==null)||name.isBlank()){
                                            name = ap.getName();
                                    }
                                    sensor = new ChannelAccessSensor<Double>(ap.getId(), createChannel(Double.class, name), configModel.isFailOnSensorError());
                                }
				a.setAsynchronous(p.isAsynchronous());
				Actor actuator = a;
				
				aLoop.getActors().add(actuator);
				aLoop.getSensors().add(sensor);
			}
			else if (p instanceof RegionPositioner){
				RegionPositioner rp = (RegionPositioner) p;
				
				ComplexActuator actuator = new ComplexActuator();
				/*
				 * Regions are translated into a complex actor consisting of a LinearActuator
				 * If consecutive regions are overlapping, i.e. end point of region a equals the
				 * start point of region b then the start point for the LinearActuator of region b
				 * is changes to its next step (start+/-stepSize depending on whether end position of the 
				 * region is > or < start of the region)
				 */
				Region lastRegion = null;
				for(Region r: rp.getRegion()){
					// Normal region
					if(r.getFunction()==null){

						// Check whether regions are consecutive
						double start = r.getStart();
						if(lastRegion!=null && start == lastRegion.getEnd()){ // TODO verify whether double comparison is ok 
							if(r.getStart()<r.getEnd()){
								start = start+r.getStepSize();
							}
							else{
								start=start-r.getStepSize();
							}
						}
						
						// Create actuator
                                                Actor a;
                                                ChannelAccessLinearActuator<?> act;
                                                Device dev = Context.getInstance().getDevicePool().getByName(rp.getName(), Device.class);
                                                if (dev != null){
                                                    act = new DeviceLinearActuator(dev, createDoneChannel(rp), getDoneValue(rp), rp.getDoneDelay(), r.getStart(), r.getEnd(), r.getStepSize(), moveTimeout);
                                                } else {                                                
                                                    act = new ChannelAccessLinearActuator(createChannel(Double.class, rp.getName()), createDoneChannel(rp), getDoneValue(rp), rp.getDoneDelay(), start, r.getEnd(), r.getStepSize(), moveTimeout);                 
                                                }
                                                act.setAsynchronous(rp.isAsynchronous());
                                                a=act;
						
						ComplexActuator ca = new ComplexActuator();
						ca.getActors().add(a);
						ca.getPreActions().addAll(mapActions(r.getPreAction()));
						actuator.getActors().add(ca);
						lastRegion = r;
					}
					else{
						// Function based region
						
						// Cannot check whether the regions are consecutive as the function 
						// used might change the start value to something else
						// [THIS LIMITATION NEEDS TO BE SOMEHOW RESOLVED IN THE NEXT VERSIONS]
						JythonFunction function = mapFunction(r.getFunction());
						ChannelAccessFunctionActuator<?> act;
                                                Device dev = Context.getInstance().getDevicePool().getByName(rp.getName(), Device.class);
                                                if (dev != null){
                                                    act = new DeviceFunctionActuator(dev, createDoneChannel(rp), getDoneValue(rp), rp.getDoneDelay(), function, r.getStart(), r.getEnd(), r.getStepSize(), moveTimeout);
                                                } else {  
                                                    act = new ChannelAccessFunctionActuator(createChannel(Double.class,rp.getName()), createDoneChannel(rp), getDoneValue(rp), rp.getDoneDelay(), function, r.getStart(), r.getEnd(), r.getStepSize(), moveTimeout);
                                                }
						act.setAsynchronous(rp.isAsynchronous());
						Actor a = act;
						
						ComplexActuator ca = new ComplexActuator();
						ca.getActors().add(a);
						ca.getPreActions().addAll(mapActions(r.getPreAction()));
						actuator.getActors().add(ca);
						lastRegion = r;
					}
				}
				aLoop.getActors().add(actuator);
                               
                                Sensor sensor;
                                Device dev = getDevice(rp.getName());
                                if (dev != null){
                                    sensor = new DeviceSensor(rp.getId(), dev, configModel.isFailOnSensorError());
                                } else {
                                    // Add a sensor for the readback
                                    String name = rp.getReadback();
                                    if((name==null)||name.isBlank()){
                                            name = rp.getName();
                                    }
                                    sensor = new ChannelAccessSensor<Double>(rp.getId(), createChannel(Double.class, name), configModel.isFailOnSensorError());
                                }
				aLoop.getSensors().add(sensor);
			}
			else if(p instanceof PseudoPositioner){
				PseudoPositioner pp =(PseudoPositioner) p;
				PseudoActuatorSensor actorSensor = new PseudoActuatorSensor(pp.getId(), pp.getCounts());
				
				// Register as actor
				aLoop.getActors().add(actorSensor);
				
				// Register as sensor
				aLoop.getSensors().add(actorSensor);
			}
			else{
				// Not supported
				logger.warning("Mapping for "+p.getClass().getName()+" not available");
			}
		}
		
		// Translate settling time to post actor action
		// Only add the post actor action if the settling time is > 0
		if(stime>0){
			Double delay = stime*1000;
			aLoop.getPostActorActions().add(new Delay(delay.longValue()));
		}
		
		// Map actions between positioner and detector
		aLoop.getPreSensorActions().addAll(mapActions(dimension.getAction()));
		
		
		// Map guard (if specified)
		Guard g = dimension.getGuard();
		if(g != null){
			// Map conditions
			List<ChannelAccessGuardCondition<?>> conditions = new ArrayList<>();
			for(GuardCondition con: g.getCondition()){
				if(con.getType().equals("Integer")){
					conditions.add(new ChannelAccessGuardCondition<Integer>(createChannel(Integer.class, con.getChannel()), Integer.parseInt(con.getValue())));
				}
				else if(con.getType().equals("Double")){
					conditions.add(new ChannelAccessGuardCondition<Double>(createChannel(Double.class, con.getChannel()), Double.parseDouble(con.getValue())));
				}
				else{
					conditions.add(new ChannelAccessGuardCondition<String>(createChannel(String.class, con.getChannel()), con.getValue()));
				}
			}
			// Create guard and add to loop
			ChannelAccessGuard guard = new ChannelAccessGuard(conditions);
			aLoop.setGuard(guard);
		}
		
		// Map detectors
		for(Detector detector : dimension.getDetector()){
			mapDetector(aLoop, detector);
		}
		
		
		// Mapping dimension post-actions
		aLoop.getPostActions().addAll(mapActions(dimension.getPostAction()));
		
		
		return aLoop;
	}
	
	/**
	 * Map function 
	 * @param f	Function object in the model
	 * @return	Internal function object
	 */
	private JythonFunction mapFunction(Function f){
		HashMap<String, Object> map = new HashMap<>();
		for(ParameterMapping m: f.getMapping()){
			if(m instanceof VariableParameterMapping){
				VariableParameterMapping vp = (VariableParameterMapping)m;
				Variable v = (Variable)vp.getName();
				JythonGlobalVariable var = jVariableDictionary.get(v.getName());
				var.setValue(v.getValue());
				map.put(vp.getVariable(), var);
                          
			} else if(m instanceof ChannelParameterMapping){
                              ChannelParameterMapping cpm = (ChannelParameterMapping) m;
                                JythonGlobalVariable var = new JythonGlobalVariable();
                                var.setName(cpm.getVariable());                    
                                if(cpm.getType().equals("String")){
                                    map.put(cpm.getVariable(),createChannel(String.class, cpm.getChannel()));
                                }
                                else if(cpm.getType().equals("Integer")){
                                    map.put(cpm.getVariable(),createChannel(Integer.class, cpm.getChannel()));                                    
                                }
                                else if(cpm.getType().equals("Double")){
                                    map.put(cpm.getVariable(),createChannel(Double.class, cpm.getChannel())); 
                                }
                                else{
                                        logger.warning("Channel type ["+cpm.getType()+"] is not supported for mapping");
                                }
                        }
		}
		JythonFunction function = new JythonFunction(f.getScript(), getFunctionId(), map);
		return function;
	}
	
	private void mapDetector(ActorSensorLoop aLoop, Detector detector){
		if(detector instanceof ScalarDetector){
			ScalarDetector sd = (ScalarDetector) detector;
			
			// Add pre actions
			aLoop.getPreSensorActions().addAll(mapActions(sd.getPreAction()));
			
			// Add sensor
			Sensor sensor;
                        Device dev = getDevice(sd.getName());
                        if (dev != null){
                            sensor = new DeviceSensor(sd.getId(), dev, configModel.isFailOnSensorError());
                        } else {                      
                            if(sd.getType().equals("String")){
                                    sensor = new ChannelAccessSensor<>(sd.getId(), createChannel(String.class,sd.getName()), configModel.isFailOnSensorError());
                            }
                            else{
                                    sensor = new ChannelAccessSensor<>(sd.getId(), createChannel(Double.class,sd.getName()), configModel.isFailOnSensorError());
                            }
                        }
			
			aLoop.getSensors().add(sensor);
		}
		else if (detector instanceof ArrayDetector){
			ArrayDetector ad = (ArrayDetector) detector;
			
			// Add pre actions
			aLoop.getPreSensorActions().addAll(mapActions(ad.getPreAction()));
			Sensor sensor;
			// Add sensor
                        Device dev = getDevice(ad.getName());
                        if (dev != null){
                            sensor = new DeviceSensor(ad.getId(), dev, configModel.isFailOnSensorError());
                        } else {     
                            sensor = new ChannelAccessSensor<>(ad.getId(), createChannel(double[].class, ad.getName(), ad.getArraySize()), configModel.isFailOnSensorError());
                        }
                        aLoop.getSensors().add(sensor);
		}
		else if (detector instanceof DetectorOfDetectors){
			DetectorOfDetectors dd = (DetectorOfDetectors) detector;
			
			// Add pre actions
			aLoop.getPreSensorActions().addAll(mapActions(dd.getPreAction()));
			
			for(Detector d: dd.getDetector()){
				// Recursively call mapping method
				mapDetector(aLoop, d);
			}
		}
		else if (detector instanceof Timestamp){
			Timestamp dd = (Timestamp) detector;
			
			// Ad sensor
			TimestampSensor sensor = new TimestampSensor(dd.getId());
			aLoop.getSensors().add(sensor);
		}
		else{
			// Not supported
			logger.warning("Detector type "+detector.getClass().getName()+" not supported");
		}
	}
	
	/**
	 * Map OTF dimension onto a OTF loop
	 * @param dimension
	 * @return
	 */
	private ActionLoop mapContinuousDimension(ContinuousDimension dimension) {

		ActionLoop aLoop = null;

		boolean hcrOnly = true;
		for (SimpleScalarDetector detector : dimension.getDetector()) {
			if (detector.isScr()) {
				hcrOnly = false;
				break;
			}
		}

		// Create loop
		boolean zigZag = dimension.isZigzag(); // default value is false

		CrlogicLoopStream actionLoop = new CrlogicLoopStream( cservice, zigZag, 
                        configuration.getCrlogicPrefix(), configuration.getCrlogicIoc(),  configuration.getCrlogicChannel(),  
                        configuration.getCrlogicAbortable(), configuration.getScrlogicSimulated());

		actionLoop.getPreActions().addAll(mapActions(dimension.getPreAction()));

		// Map positioner
		ContinuousPositioner p = dimension.getPositioner();
		double backlash = 0;
		if (p.getAdditionalBacklash() != null) {
			backlash = p.getAdditionalBacklash();
		}
		actionLoop.setActuator(p.getId(), p.getName(), p.getReadback(), p.getStart(), p.getEnd(), p.getStepSize(), p.getIntegrationTime(), backlash);

		// Map sensors
		// ATTENTION: the sequence of the mapping depends on the sequence in the
		// schema file !
		for (SimpleScalarDetector detector : dimension.getDetector()) {
			if (!detector.isScr()) {
				actionLoop.getSensors().add(new CrlogicResource(detector.getId(), detector.getName()));
			}
		}

		for (ScalerChannel detector : dimension.getScaler()) {
			actionLoop.getSensors().add(new CrlogicResource(detector.getId(), "SCALER" + detector.getChannel(), true));
		}

		Timestamp tdetector = dimension.getTimestamp();
		if (tdetector != null) {
			actionLoop.getSensors().add(new CrlogicResource(tdetector.getId(), "TIMESTAMP"));
		}

		actionLoop.getPostActions().addAll(mapActions(dimension.getPostAction()));

		if (hcrOnly) {
			// There are no additional channels to be read out while taking data
			// via hcrlogic
			// Therefore we just  register the hcr loop as action loop

			aLoop = actionLoop;
		} else {
			List<Channel<DoubleTimestamp>> sensors = new ArrayList<>();
			List<String> ids = new ArrayList<>();

			for (SimpleScalarDetector detector : dimension.getDetector()) {
				if (detector.isScr()) {
					ids.add(detector.getId());
					sensors.add(createChannel(DoubleTimestamp.class, detector.getName(), true));
				}
			}
			// Create soft(ware) based crlogic
			ScrlogicLoop scrlogic = new ScrlogicLoop(ids, sensors);

			// Create parallel logic
			ParallelCrlogic pcrlogic = new ParallelCrlogic(actionLoop, scrlogic);

			aLoop = pcrlogic;
		}
		return aLoop;
	}
	
        private Channel createDoneChannel(DiscreteStepPositioner p){
            
            if(p.getType().equals("String")){
                    return createChannel(String.class, p.getDone());
            }
            else if(p.getType().equals("Double")){
                    return  createChannel(Double.class,p.getDone());
            }
            // Default
            return createChannel(Integer.class, p.getDone());
        }
        
        private Object getDoneValue(DiscreteStepPositioner p){
            if(p.getType().equals("String")){
                    return p.getDoneValue();
            }
            else if(p.getType().equals("Double")){
                    return Double.valueOf(p.getDoneValue());
            }
            // Default            
            return Double.valueOf(p.getDoneValue()).intValue();                        
        }
        

        
	private <T> Channel<T> createChannel(Class<T> type, String name, boolean monitor){
            for (int i=0; i< configuration.getChannelCreationRetries(); i++){
		try {
			if(name== null){
				return null;
			}
			Channel<T> c = cservice.createChannel(new ChannelDescriptor<T>(type, name, monitor) );
			channels.add(c);
			return c;
		} catch (Exception e) {
                    if (i>= (configuration.getChannelCreationRetries()-1)){
			throw new RuntimeException("Unable to create channel: "+name,e);
                    } else {
                        logger.warning("Unable to create channel: " + name + " - Retrying");
                    }
		}
            }
            return null;
	}
	
	/**
	 * Create channel and remember to be able to destroy channels at the end
	 * @param name
	 * @param type
	 * @return	null if the name of the channel is null, otherwise the channel
	 */
	private <T> Channel<T> createChannel(Class<T> type, String name){
            for (int i=0; i< configuration.getChannelCreationRetries(); i++){
		try {
			if(name== null){
				return null;
			}
			Channel<T> c = cservice.createChannel(new ChannelDescriptor<T>(type, name) );
			channels.add(c);
			return c;
		} catch (Exception e) {
                    if (i>= (configuration.getChannelCreationRetries()-1)){
			throw new RuntimeException("Unable to create channel: "+name,e);
                    } else {
                        logger.warning("Unable to create channel: " + name + " - Retrying");
                    }
		}
            }
            return null;
	}
	
	private <T> Channel<T> createChannel(Class<T> type, String name, int size){
            for (int i=0; i< configuration.getChannelCreationRetries(); i++){
		try {
			if(name== null){
				return null;
			}
			Channel<T> c = cservice.createChannel(new ChannelDescriptor<T>(type, name, false, size) );
			channels.add(c);
			return c;
		} catch (Exception e) {
                    if (i>= (configuration.getChannelCreationRetries()-1)){
			throw new RuntimeException("Unable to create channel: "+name,e);
                    } else {
                        logger.warning("Unable to create channel: " + name + " - Retrying");
                    }
		}
            }
            return null;
	}	
        
	public static VDescriptor mapVisualizations(List<Visualization> vl){
		VDescriptor vd = new VDescriptor();
		
		
		for(Visualization v: vl){
			if(v instanceof ch.psi.pshell.xscan.model.LinePlot){
				ch.psi.pshell.xscan.model.LinePlot lp = (ch.psi.pshell.xscan.model.LinePlot) v;
				
				String x = getId(lp.getX());
				
				LinePlot lineplot = new LinePlot(lp.getTitle());
				List<Object> l = lp.getY();
				for(Object o: l){
					String y = getId(o);
					lineplot.getData().add(new XYSeries(x, y));
				}
				
				vd.getPlots().add(lineplot);
			}
			else if(v instanceof ch.psi.pshell.xscan.model.LinePlotArray){
				// Array visualization
				ch.psi.pshell.xscan.model.LinePlotArray lp = (ch.psi.pshell.xscan.model.LinePlotArray) v;
				
				LinePlot lineplot = new LinePlot(lp.getTitle());
				// Create data filter for visualization
				List<Object> l = lp.getY();
				for(Object o: l){
					String idY = getId(o);
					
					// TODO Need to actually check if minX of 
					lineplot.setMinX((double)lp.getOffset());
					lineplot.setMaxX((double)(lp.getOffset()+lp.getSize()));
                                        lineplot.setMaxSeries(lp.getMaxSeries());
					lineplot.getData().add(new YSeries(idY));
				}
                                vd.getPlots().add(lineplot);
			}
			else if(v instanceof ch.psi.pshell.xscan.model.MatrixPlot){
				
				// MatrixPlot does currently not support RegionPositioners because of the
				// plotting problems this would cause. If regions of the positioner have different
				// step sizes it is not easily possible (without (specialized) rasterization) to plot the data.
				
				ch.psi.pshell.xscan.model.MatrixPlot mp = (ch.psi.pshell.xscan.model.MatrixPlot) v;
				

				double minX, maxX;
				int nX;
				double minY, maxY;
				int nY;
				
				String idX, idY, idZ;
				
				// X Axis
				if(mp.getX() instanceof LinearPositioner){
					LinearPositioner linp = ((LinearPositioner)mp.getX());
					idX = linp.getId();
					
					minX = (Math.min(linp.getStart(), linp.getEnd()));
					maxX = (Math.max(linp.getStart(), linp.getEnd()));
					nX = ((int) Math.floor((Math.abs(maxX-minX))/linp.getStepSize()) + 1);
				}
				else if(mp.getX() instanceof PseudoPositioner){
					PseudoPositioner pp = ((PseudoPositioner)mp.getX());
					idX = pp.getId();
					minX = (1); // Count starts at 1
					maxX = (pp.getCounts());
					nX = (pp.getCounts());
				}
				else if(mp.getX() instanceof ContinuousPositioner){
					ContinuousPositioner conp = ((ContinuousPositioner)mp.getX());
					idX = conp.getId();
					
					minX = (Math.min(conp.getStart(), conp.getEnd()));
					maxX = (Math.max(conp.getStart(), conp.getEnd()));
					nX = ((int) Math.floor((Math.abs(maxX-minX))/conp.getStepSize()) + 1);
				}
				else{
					// Fail as we cannot determine the min, max and number of steps
					throw new RuntimeException(mp.getX().getClass().getName()+" is not supported as x-axis of a MatrixPlot");
				}
				
				// Y Axis
				if(mp.getY() instanceof LinearPositioner){
					LinearPositioner linp = ((LinearPositioner)mp.getY());
					idY = linp.getId();
					minY = (Math.min(linp.getStart(), linp.getEnd()));
					maxY = (Math.max(linp.getStart(), linp.getEnd()));
					nY = ((int) Math.floor((Math.abs(maxY-minY))/linp.getStepSize()) + 1);
				}
				else if(mp.getY() instanceof PseudoPositioner){
					PseudoPositioner pp = ((PseudoPositioner)mp.getY());
					idY = pp.getId();
					minY = (1); // Count starts at 1
					maxY = (pp.getCounts());
					nY = (pp.getCounts());
				}
				else{
					// Fail as we cannot determine the min, max and number of steps
					throw new RuntimeException(mp.getY().getClass().getName()+" is not supported as y-axis of a MatrixPlot");
				}
				
				// Z Dimension
				idZ = getId(mp.getZ());

				
				ch.psi.pshell.xscan.plot.MatrixPlot matrixplot = new ch.psi.pshell.xscan.plot.MatrixPlot(mp.getTitle());
				matrixplot.setMinX(minX);
				matrixplot.setMaxX(maxX);
				matrixplot.setnX(nX);
				matrixplot.setMinY(minY);
				matrixplot.setMaxY(maxY);
				matrixplot.setnY(nY);
                                matrixplot.setType(mp.getType());
				
				matrixplot.getData().add(new XYZSeries(idX, idY, idZ));
				vd.getPlots().add(matrixplot);
			}
			else if(v instanceof ch.psi.pshell.xscan.model.MatrixPlotArray){
				// Support for 2D waveform plots
				ch.psi.pshell.xscan.model.MatrixPlotArray mp = (ch.psi.pshell.xscan.model.MatrixPlotArray) v;
				
				// Get size of the array detector
				int arraySize = 0;
				Object o = mp.getZ();
				if(o instanceof ArrayDetector){
					ArrayDetector ad = (ArrayDetector) o;
					arraySize = ad.getArraySize();
				}
				else{
					// Workaround
					arraySize = mp.getSize(); // of array is from a manipulation the size is not known. Then the size will indicate the size of the array to display
				}
				
				int offset = mp.getOffset();
				// Determine size for array
				int size = mp.getSize();
				if(size>0 && offset+size<arraySize){
					size = mp.getSize();
				}
				else{
					size=arraySize-offset;
				}
				
				
				double minY, maxY;
				int nY;
				
				double minX = offset;
				double maxX = offset+size-1;
				int nX = size;
				
				String idY, idZ;
				
				// Y Axis
				if(mp.getY() instanceof LinearPositioner){
					LinearPositioner linp = ((LinearPositioner)mp.getY());
					idY = linp.getId();
					
					minY = (Math.min(linp.getStart(), linp.getEnd()));
					maxY = (Math.max(linp.getStart(), linp.getEnd()));
					nY = ((int) Math.floor((Math.abs(maxY-minY))/linp.getStepSize()) + 1);
				}
				else if(mp.getY() instanceof PseudoPositioner){
					PseudoPositioner pp = ((PseudoPositioner)mp.getY());
					idY = pp.getId();
					minY = (1); // Count starts at 1
					maxY = (pp.getCounts());
					nY = (pp.getCounts());
				}
				else if(mp.getY() instanceof ContinuousPositioner){
					ContinuousPositioner conp = ((ContinuousPositioner)mp.getY());
					idY = conp.getId();
					
					minY = (Math.min(conp.getStart(), conp.getEnd()));
					maxY = (Math.max(conp.getStart(), conp.getEnd()));
					nY = ((int) Math.floor((Math.abs(maxY-minY))/conp.getStepSize()) + 1);
				}
				else{
					// Fail as we cannot determine the min, max and number of steps
					throw new RuntimeException(mp.getY().getClass().getName()+" is not supported as x-axis of a MatrixPlot");
				}
				
				
				// Z Dimension
				idZ = getId(mp.getZ());

				ch.psi.pshell.xscan.plot.MatrixPlot matrixplot = new ch.psi.pshell.xscan.plot.MatrixPlot(mp.getTitle());
				matrixplot.setMinX(minX);
				matrixplot.setMaxX(maxX);
				matrixplot.setnX(nX);
				matrixplot.setMinY(minY);
				matrixplot.setMaxY(maxY);
				matrixplot.setnY(nY);
                                matrixplot.setType(mp.getType());
				
				matrixplot.getData().add(new YZSeries(idY, idZ));
				vd.getPlots().add(matrixplot);
				
			}
			else{
				logger.warning(v.getClass().getName()+" is not supported as visualization type");
			}
		}
		return vd;
	}
        
	private static String getId(Object object){
		String id;
		if(object instanceof Positioner){
			id = ((Positioner)object).getId();
		}
		else if (object instanceof Detector){
			id = ((Detector)object).getId();
		}
		else if (object instanceof ch.psi.pshell.xscan.model.Manipulation){
			id = ((ch.psi.pshell.xscan.model.Manipulation)object).getId();
		}
		// For testing purposes
		else if(object instanceof String){
			id = (String) object;
		}
		else{
			throw new RuntimeException("Unable to identify id of object reference "+object);
		}
		return id;
	}
        
}
