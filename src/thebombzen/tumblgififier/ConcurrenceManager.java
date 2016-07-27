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
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.NullInputStream;
import thebombzen.tumblgififier.io.NullOutputStream;
import thebombzen.tumblgififier.io.resources.ProcessTerminatedException;

public class ConcurrenceManager {
	
	private static final ConcurrenceManager instance = new ConcurrenceManager();
	
	public static ConcurrenceManager getConcurrenceManager(){
		return instance;
	}
	

	/**
	 * A flag used to determine if we're cleaning up all the subprocesses we've
	 * started. Normally, ending a process will just cause the next stage in the
	 * GIF creation to continue. If this flag is set, we won't create any more
	 * processes.
	 */
	private volatile boolean cleaningUp = false;
	
	/**
	 * This is a list of all processes started by our program. It's used so we
	 * can end them all upon exit.
	 */
	private volatile List<Process> processes = new ArrayList<>();
	
	private volatile List<Runnable> cleanUpJobs = Collections.synchronizedList(new ArrayList<Runnable>());
	
	private ConcurrenceManager(){
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
			@Override
			public void run() {
				cleanUp();
			}
		}));
		cleanUpJobs.add(new Runnable(){
			public void run(){
				System.out.println("Shutting Down...");
			}
		});
	}
	
	public void addShutdownTask(Runnable runnable){
		cleanUpJobs.add(runnable);
	}
	
	/**
	 * Create a subprocess and execute the arguments. This automatically
	 * redirects standard error to standard out. If the stream copyTo is not
	 * null, it will automatically copy the standard output of that process to
	 * the OutputStream copyTo. Copying the stream will cause this method to
	 * block. Declining to copy will cause this method to return immediately.
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
	 * Stop all subprocesses, but do not exit the program.
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
	 * This stops all subprocesses and shuts down the thread pool.
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
	
	/**
	 * This is the thread pool on which we should run thread-pool tasks.
	 */
	private ScheduledExecutorService threadPool = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	
	public Future<?> executeLater(Runnable r) {
		return threadPool.submit(r);
	}
	
	public Future<?> createImpreciseTickClock(Runnable r, long tickTime, TimeUnit timeUnit){
		return threadPool.scheduleWithFixedDelay(r, 0, tickTime, timeUnit);
	}
	
	
}
