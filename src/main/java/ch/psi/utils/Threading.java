package ch.psi.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities to simplify parallelization and synchronization.
 */
public class Threading {

    public static List<Future> fork(Callable[] callables) {
        return fork(callables, false);
    }

    public static List<Future> fork(Callable[] callables, boolean limitPoolSize) {
        return fork(callables, limitPoolSize, null);
    }

    public static List<Future> fork(Callable[] callables,String poolName) {
        return fork(callables, false, poolName);
    }

    public static List<Future> fork(Callable[] callables, boolean limitPoolSize, String poolName) {
        int threads = callables.length;
        if (limitPoolSize) {
            int processors = Runtime.getRuntime().availableProcessors();
            threads = (threads > processors) ? processors : threads;
        }
        List<Future> futures = new ArrayList<>();
        if (threads>0){
            ExecutorService executor = (poolName != null)
                    ? Executors.newFixedThreadPool(threads, new NamedThreadFactory(poolName))
                    : Executors.newFixedThreadPool(threads);


            for (Callable callable : callables) {
                futures.add(executor.submit(callable));
            }
            executor.shutdown();    //Will remove threads after execution
        }        
        return futures;

    }

    public static List join(List<Future> futures) throws InterruptedException, ExecutionException {
        List ret = new ArrayList();
        try {
            for (Future future : futures) {
                ret.add(future.get());
            }
        } catch (InterruptedException ex) {
            for (Future future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
            throw ex;
        }
        return ret;
    }

    public static List parallelize(Callable[] callables) throws InterruptedException, ExecutionException {
        return parallelize(callables, false);
    }

    public static List<Future> parallelize(Callable[] callables, boolean limitPoolSize) throws InterruptedException, ExecutionException {
        return parallelize(callables, limitPoolSize, null);
    }
    
    public static List<Future> parallelize(Callable[] callables,String poolName) throws InterruptedException, ExecutionException {
        return parallelize(callables, false, poolName);
    }    

    public static List<Future> parallelize(Callable[] callables, boolean limitPoolSize, String poolName) throws InterruptedException, ExecutionException {
        List<Future> futures = fork(callables, limitPoolSize, poolName);
        return join(futures);
    }

    public static boolean stop(Thread thread, boolean force, int waitMillis) throws InterruptedException {
        if (thread.isAlive()) {
            thread.interrupt();
            if (waitMillis > 0) {
                for (int i = 0; i < waitMillis; i++) {
                    Thread.sleep(1);
                    if (!thread.isAlive()) {
                        return true;
                    } else if (!thread.isInterrupted()) {
                        thread.interrupt();
                    }
                }
            }
            if (force) {
                Thread.sleep(0);
                if (thread.isAlive()) {
                    Logger.getLogger(Threading.class.getName()).severe("Force stopping thread: " + thread.getName());
                    thread.stop();
                }
                return !thread.isAlive();
            }
            return false;
        }
        return true;
    }

    public static ScheduledExecutorService scheduleAtFixedRateNotRetriggerable(final Runnable task,
            final long delay, final long interval, final TimeUnit timeUnit, final String threadName) {
        ScheduledExecutorService scheduler = threadName == null
                ? Executors.newSingleThreadScheduledExecutor()
                : Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(threadName));
        Runnable timer = new Runnable() {
            @Override
            public void run() {
                if (!scheduler.isShutdown()) {
                    Chrono chrono = new Chrono();
                    try {
                        task.run();
                    } catch (Exception ex) {
                        Logger.getLogger(Threading.class.getName()).fine("Exception in scheduler thead " + Thread.currentThread().getName() + ": " + ex.getMessage());
                    }
                    if (interval >= 0) {
                        if (!scheduler.isShutdown() && !Thread.currentThread().isInterrupted()) {
                            scheduler.schedule(this, Math.max(0, interval - chrono.getEllapsed()), timeUnit);
                        }
                    }
                }
            }
        };
        scheduler.schedule(timer, delay, timeUnit);
        return scheduler;
    }

    public interface SupplierWithException<T> {

        T get() throws Exception;
    }

    public static class VisibleCompletableFuture<T> extends CompletableFuture<T> {

        private Thread runningThread;
        final Object lock = new Object();

        void setRunningThread() {
            runningThread = Thread.currentThread();

            synchronized (lock) {
                lock.notifyAll();
            }
        }

        public Thread getRunningThread() {
            return runningThread;
        }

        public Thread waitRunningThread(int timeout) throws InterruptedException {
            synchronized (lock) {
                if (runningThread == null) {
                    try {
                        lock.wait(timeout);
                    } catch (Exception ex) {
                    }
                }
            }
            return runningThread;
        }
    }

    //CompletableFuture generation settting completeExceptionally 
    public static CompletableFuture<?> getFuture(final SupplierWithException<?> supplier) {
        return getFuture(supplier, ForkJoinPool.commonPool());
    }

    public static CompletableFuture<?> getFuture(final SupplierWithException<?> supplier, Executor executor) {
        VisibleCompletableFuture<Object> ret = new VisibleCompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            try {
                ret.setRunningThread();
                Object obj = supplier.get();
                ret.complete(obj);
                return obj;
            } catch (Throwable ex) {
                Logger.getLogger(Threading.class.getName()).log(Level.FINER, null, ex);
                ret.completeExceptionally(ex);
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        }, executor);

        return ret;
    }

    public interface RunnableWithException {

        void run() throws Exception;
    }

    public static CompletableFuture<?> getFuture(final RunnableWithException runnable) {
        return getFuture(runnable, ForkJoinPool.commonPool());
    }

    public static CompletableFuture<?> getFuture(RunnableWithException runnable, Executor executor) {
        VisibleCompletableFuture<Object> ret = new VisibleCompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                ret.setRunningThread();
                runnable.run();
                ret.complete(null);
            } catch (Throwable ex) {
                Logger.getLogger(Threading.class.getName()).log(Level.FINER, null, ex);
                ret.completeExceptionally(ex);
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }, executor);
        return ret;
    }
}
