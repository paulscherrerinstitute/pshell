package ch.psi.pshell.xscan.core;

import ch.psi.utils.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParallelCrlogic implements ActionLoop {
	
	private static final Logger logger = Logger.getLogger(ParallelCrlogic.class.getName());

	/**
	 * Flag to indicate whether the data of this loop will be grouped
	 * According to this flag the dataGroup flag in EndOfStream will be set.
	 */
	private boolean dataGroup = false;
	
	private CrlogicLoopStream crlogic;
	private ScrlogicLoop scrlogic;
	
	/**
	 * List of actions that are executed at the beginning of the loop.
	 */
	private List<Action> preActions;
	/**
	 * List of actions that are executed at the end of the loop.
	 */
	private List<Action> postActions;
	
	private MergeLogic mergeLogic;
	
	
	private final EventBus eventbus;
	
	public ParallelCrlogic(CrlogicLoopStream crlogic, ScrlogicLoop scrlogic){
		
		if(crlogic==null){
			throw new IllegalArgumentException("No Crloop specified");
		}
		if(scrlogic==null){
			throw new IllegalArgumentException("No Scrloop specified");
		}
		
		
		this.eventbus = new EventBus();
		
		this.crlogic = crlogic;
		// Add timestamp to sensor at the beginning of the sensor list as this is required for merging the data
		// Timestamp will be at the second position of a message in the queue!
		this.crlogic.getSensors().add(0, new CrlogicResource("tmp_timestamp","TIMESTAMP"));
		this.scrlogic = scrlogic;
		
		// Initialize lists used by the loop
		this.preActions = new ArrayList<Action>();
		this.postActions = new ArrayList<Action>();
		
		
		
		this.mergeLogic = new MergeLogic(crlogic.getEventBus(), scrlogic.getEventBus(), eventbus);
	}
	
	@Override
	public void prepare() {
	}
	
	@Override
	public void execute() throws InterruptedException {
		
		mergeLogic.enable();
		
		// Execute pre actions
		for(Action action: preActions){
			action.execute();
		}
		
		
		ExecutorService service = Executors.newFixedThreadPool(3); // 2 for the parallel data acquisition and 1 for the merger thread
		final CyclicBarrier b = new CyclicBarrier(2);
		
		List<Future<Boolean>> list = new ArrayList<Future<Boolean>>();
		
		// Start a thread for each logic
		logger.info("Submit logic");
		Future<Boolean> f = service.submit(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				
				// Prepare logic
				crlogic.prepare();
				
				// Ensure that really all parallel logics start in parallel
				b.await();
				
				// Execute the logic of this path
                                try{
                                    crlogic.execute();
                                } finally {

    //				crlogic.cleanup();

                                    // Need to stop the scrlogic logic (otherwise it would keep going to take data)
                                    scrlogic.abort();
                                }
				return true;
			}});
		list.add(f);
		
		//Start a thread for the scrlogic
		logger.info("Submit logic");
		f = service.submit(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				
				// Prepare logic
				scrlogic.prepare();
				
				// Ensure that really all parallel logics start in parallel
				b.await();
				
				// Execute the logic of this path
				scrlogic.execute(); // This actually just starts the collection ...
				
//				scrlogic.cleanup();
				return true;
			}});
		list.add(f);
		
		for(Future<Boolean> bf: list){
			// Wait for completion of the thread
			try {
				bf.get();
			} catch (ExecutionException e) {
				logger.log(Level.WARNING, "Something went wrong while waiting for crthreads to finish: " + e.getMessage());
				throw new RuntimeException(e);
			}
		}
		
		// Wait until all threads have finished
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);
		
		
		// Execute post actions
		for(Action action: postActions){
			action.execute();
		}
		
		mergeLogic.disable();
	}

	@Override
	public void cleanup() {
	}

	@Override
	public List<Action> getPreActions() {
		return preActions;
	}

	@Override
	public List<Action> getPostActions() {
		return postActions;
	}

	@Override
	public boolean isDataGroup() {
		return dataGroup;
	}

	@Override
	public void setDataGroup(boolean dataGroup) {
		this.dataGroup = dataGroup;
	}

	@Override
	public EventBus getEventBus(){
		return eventbus;
	}
}
