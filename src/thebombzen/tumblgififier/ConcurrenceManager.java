package thebombzen.tumblgififier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.NullInputStream;
import thebombzen.tumblgififier.io.NullOutputStream;
import thebombzen.tumblgififier.io.resources.ProcessTerminatedException;

/**
 * This class handles all the program-wide concurrence utilities. 
 *
 */
public final class ConcurrenceManager {
	
	private static final ConcurrenceManager instance = new ConcurrenceManager();
	
	/**
	 * There is one instance of a ConcurrentManager. This returns the singleton instance.
	 */
	public static ConcurrenceManager getConcurrenceManager(){
		return instance;
	}
	

	/**
	 * A flag used to determine if we're cleaning up all the subprocesses we've
	 * started. Normally, if a sub-process ends, it will be assumed to be "completed," and the next stage in the
	 * GIF creation to continue. Setting this flag prevents the creation of new processes in order to safely clean up.
	 */
	private volatile boolean cleaningUp = false;
	
	/**
	 * This is a list of all processes started by our program. It's used so we
	 * can end them all upon exit.
	 */
	private volatile List<Process> processes = new ArrayList<>();
	
	/**
	 * These are a list of jobs that must be executed when the program shuts down normally (instead of halting).
	 */
	private volatile List<Runnable> cleanUpJobs = Collections.synchronizedList(new ArrayList<Runnable>());
	
	/**
	 * These are a list of jobs that must be executed after the program is fully initialized.
	 */
	private volatile List<Runnable> postInitJobs = Collections.synchronizedList(new ArrayList<Runnable>());
	
	/**
	 * This constructor creates the singleton instance of this class. It's private so only there can only be one instance.
	 */
	private ConcurrenceManager(){
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
			@Override
			public void run() {
				cleanUp();
			}
		}));
		cleanUpJobs.add(new Runnable(){
			@Override
			public void run(){
				System.out.println("Shutting Down...");
			}
		});
		postInitJobs.add(new Runnable(){
			@Override
			public void run(){
				MainFrame.getMainFrame().getStatusProcessor().appendStatus("Initialization successful. Now, open a video file with File -> Open.");
			}
		});
	}
	
	public void addPostInitTask(Runnable runnable){
		postInitJobs.add(runnable);
	}
	
	/**
	 * We want users to be able to add shutdown tasks, but not to be able to remove them. This adds a Runnable as a shutdown task in a way that it cannot be removed.
	 * @param runnable This will be executed upon a clean exit of the program.
	 */
	public void addShutdownTask(Runnable runnable){
		cleanUpJobs.add(runnable);
	}
	
	/**
	 * Create a subprocess and execute the arguments. This automatically
	 * redirects standard error to standard out. If the stream copyTo is not
	 * null, it will automatically copy the standard output of the created process to
	 * the OutputStream copyTo. Copying the stream will cause this method to
	 * block until the process's output returns an end-of-file.
	 * Declining to copy will cause this method to return immediately.
	 * 
	 * @param copyTo
	 *            If this is not null, this method will block until the process
	 *            terminates, and all the output of that process will be copied
	 *            to the stream. If it's set to mull, it will return immediately
	 *            and no copying will occur.
	 * @param args
	 *            The program name and arguments to execute. This is NOT passed
	 *            to a shell so you have to be careful with spacing or with
	 *            empty strings.
	 * @return This returns an InputStream that reads from the Standard
	 *         output/error stream of the process. If this method was set to
	 *         copy then this InputStream will have reached End-Of-File.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public InputStream exec(OutputStream copyTo, String... args) throws IOException {
		if (cleaningUp) {
			return null;
		}
		ProcessBuilder pbuilder = new ProcessBuilder(args);
		pbuilder.redirectErrorStream(true);
		Process p = pbuilder.start();
		processes.add(p);
		if (copyTo != null) {
			p.getOutputStream().close();
			InputStream str = p.getInputStream();
			int i;
			while (-1 != (i = str.read())) {
				copyTo.write(i);
			}
		}
		return p.getInputStream();
	}
	
	/**
	 * Create a subprocess and execute the arguments. This automatically
	 * redirects standard error to standard out.
	 * 
	 * @param join
	 *            If this is set to true, this method will block until the
	 *            process terminates. If it's set to false, it will return
	 *            immediately.
	 * @param args
	 *            The program name and arguments to execute. This is NOT passed
	 *            to a shell so you have to be careful with spacing or with
	 *            empty strings.
	 * @return This returns an InputStream that reads from the Standard
	 *         output/error stream of the process. If this method was set to
	 *         block then this InputStream will have reached End-Of-File.
	 * @throws ProcessTerminatedException if "join" is set to true but the process ends before its end-of-file is reached
	 */
	public InputStream exec(boolean join, String... args) throws ProcessTerminatedException {
		//System.err.println(TextHelper.getTextHelper().join(" ", args));
		try {
			if (join) {
				return exec(new NullOutputStream(), args);
			} else {
				return new BufferedInputStream(exec(null, args));
			}
		} catch (IOException ioe) {
			// NullOutputStream doesn't throw IOException, so if we get one here
			// it's really weird.
			if (ioe.getMessage().equals("Stream closed")) {
				throw new ProcessTerminatedException(ioe);
			} else {
				ioe.printStackTrace();
				return new NullInputStream();
			}
		}
	}
	
	/**
	 * Stop all subprocesses, but do not exit the program. This is uses to interrupt GIF creation.
	 */
	public void stopAll() {
		cleaningUp = true;
		for (Process p : processes) {
			if (p.isAlive()) {
				p.destroy();
			}
		}
		processes.clear();
		cleaningUp = false;
	}
	
	/**
	 * Stops all subprocesses, shuts down the thread pool, and executes shutdown tasks.
	 */
	protected void cleanUp() {
		stopAll();
		threadPool.shutdown();
		System.out.println();
		for (Runnable r : cleanUpJobs){
			r.run();
		}
		IOHelper.closeQuietly(TumblGIFifier.logFileOutputStream);
	}
	
	protected void postInit(){
		for (Runnable r : postInitJobs){
			r.run();
		}
	}
	
	/**
	 * This is the thread pool on which we should run thread-pool tasks. We add 1 because this allows us to use all processors while one thread is waiting on I/O.
	 */
	private ScheduledExecutorService threadPool = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	
	/**
	 * This queues a given Runnable to be executed "soon" but with unimportant timing.
	 */
	public Future<?> executeLater(Runnable r) {
		return threadPool.submit(r);
	}
	
	/** This queues a runnable to be executed at regular intervals.
	 * It's called an "Imprecise" tick clock because there is no guarantee that the intervals will be exact, and if the tasks takes longer to complete, the longer it will be until the next iteration is executed.
	 */
	public Future<?> createImpreciseTickClock(Runnable r, long tickTime, TimeUnit timeUnit){
		return threadPool.scheduleWithFixedDelay(r, 0, tickTime, timeUnit);
	}
	
}
