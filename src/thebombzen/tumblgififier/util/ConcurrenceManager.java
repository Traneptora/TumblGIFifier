package thebombzen.tumblgififier.util;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.NullInputStream;
import thebombzen.tumblgififier.util.io.NullOutputStream;
import thebombzen.tumblgififier.util.io.resources.ProcessTerminatedException;

/**
 * This class handles all the program-wide concurrence utilities.
 *
 */
public final class ConcurrenceManager {

	/**
	 * A flag used to determine if we're cleaning up all the subprocesses we've
	 * started. Normally, if a sub-process ends, it will be assumed to be
	 * "completed," and the next stage in the GIF creation to continue. Setting
	 * this flag prevents the creation of new processes in order to safely clean
	 * up.
	 */
	private static volatile boolean cleaningUp = false;

	/**
	 * This is a list of all processes started by our program. It's used so we
	 * can end them all upon exit.
	 */
	private static volatile List<Process> processes = new ArrayList<>();

	/**
	 * These are a list of jobs that must be executed when the program shuts
	 * down normally (instead of halting).
	 */
	private static volatile Queue<Task> cleanUpJobs = new PriorityBlockingQueue<>();

	/**
	 * These are a list of jobs that must be executed after the program is fully
	 * initialized.
	 */
	private static volatile Queue<Task> postInitJobs = new PriorityBlockingQueue<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(ConcurrenceManager::cleanUp));
		addShutdownTask(new DefaultTask(0, () -> {
			System.out.println("Shutting Down...");
		}));
		addPostInitTask(new DefaultTask(50, () -> {
			MainFrame.getMainFrame().getStatusProcessor()
					.appendStatus("Initialization successful. Now, open a video file with File -> Open.");
		}));
	}

	/**
	 * Add a task to be executed on Post-Init. Priority zero is the default.
	 * Lower numbers will be executed first, so negative refers to more
	 * immediate priority and positive numbers are less immediate. The task
	 * object will be discarded upon execution.
	 */
	public static void addPostInitTask(Task task) {
		postInitJobs.add(task);
	}

	/**
	 * Add a task to be executed on shutdown. Priority zero is the default.
	 * Lower numbers will be executed first, so negative refers to more
	 * immediate priority and positive numbers are less immediate. The task
	 * object will be discarded upon execution.
	 */
	public static void addShutdownTask(Task task) {
		cleanUpJobs.add(task);
	}

	/**
	 * Create a subprocess and execute the arguments. This automatically
	 * redirects standard error to standard out. If the stream copyTo is not
	 * null, it will automatically copy the standard output of the created
	 * process to the OutputStream copyTo. Copying the stream will cause this
	 * method to block until the process's output returns an end-of-file.
	 * Declining to copy will cause this method to return immediately.
	 * 
	 * @param copyTo
	 *            If this is not null, this method will block until the process
	 *            terminates, and all the output of that process will be copied
	 *            to the stream. If it's set to mull, it will return immediately
	 *            and no copying will occur.
	 * @param args
	 *            The program name and arguments to execute. This is NOT passed
	 *            to a shell so everything from spaces to empty strings are
	 *            passed on.
	 * @return This returns an InputStream that reads from the Standard
	 *         output/error stream of the process. If this method was set to
	 *         copy then this InputStream will have reached End-Of-File.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public static InputStream exec(OutputStream copyTo, String... args) throws IOException {
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
	 * redirects standard error to standard out. Log its output on the full log.
	 * 
	 * @param join
	 *            If this is set to true, this method will block until the
	 *            process terminates. If it's set to false, it will return
	 *            immediately.
	 * @param args
	 *            The program name and arguments to execute. This is NOT passed
	 *            to a shell so everything from spaces to empty strings are
	 *            passed on.
	 * @return This returns an InputStream that reads from the Standard
	 *         output/error stream of the process. If this method was set to
	 *         block then this InputStream will have reached End-Of-File.
	 * @throws ProcessTerminatedException
	 *             if "join" is set to true but the process ends before its
	 *             end-of-file is reached
	 */
	public static InputStream exec(boolean join, String... args) throws ProcessTerminatedException {
		return exec(true, join, args);
	}

	/**
	 * Create a subprocess and execute the arguments. This automatically
	 * redirects standard error to standard out.
	 * 
	 * @param doLog
	 *            If this is set to true, this method will log the output of the
	 *            process in the full log. Set it to true if the output is not
	 *            very problematic, or false if the process has its own logging
	 *            method.
	 * @param join
	 *            If this is set to true, this method will block until the
	 *            process terminates. (Or rather, until it closes its standard
	 *            error and output.) If it's set to false, it will return
	 *            immediately.
	 * @param args
	 *            The program name and arguments to execute. This is NOT passed
	 *            to a shell so everything from spaces to empty strings are
	 *            passed on.
	 * @return This returns an InputStream that reads from the Standard
	 *         output/error stream of the process. If this method was set to
	 *         block then this InputStream will have reached End-Of-File.
	 * @throws ProcessTerminatedException
	 *             if "join" is set to true but the process ends before its
	 *             end-of-file is reached
	 */
	public static InputStream exec(boolean doLog, boolean join, String... args) throws ProcessTerminatedException {
		try {
			if (join) {
				log(String.join(" ", args));
				if (doLog) {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					InputStream in = exec(bout, args);
					TumblGIFifier.getLogFileOutputStream().write(bout.toByteArray());
					TumblGIFifier.getLogFileOutputStream().flush();
					return in;
				} else {
					return exec(new NullOutputStream(), args);
				}
			} else {
				return new BufferedInputStream(exec(null, args));
			}
		} catch (IOException ioe) {
			// NullOutputStream and ByteArrayOutputStream don't throw
			// IOException, so if we get one here
			// it's really weird.
			if (ioe.getMessage().equals("Stream closed")) {
				throw new ProcessTerminatedException(ioe);
			} else {
				log(ioe);
				return new NullInputStream();
			}
		}
	}

	/**
	 * Stop all subprocesses, but do not exit the program. This is uses to
	 * interrupt GIF creation.
	 */
	public static void stopAll() {
		cleaningUp = true;
		processes.parallelStream().filter(Process::isAlive).forEach(Process::destroy);
		processes.clear();
		cleaningUp = false;
	}

	public static <T> T sneakyThrow(Throwable e) {
		return ConcurrenceManager.<RuntimeException, T> sneakyThrow0(e);
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable, T> T sneakyThrow0(Throwable ex) throws E {
		throw (E) ex;
	}

	/**
	 * Stops all subprocesses, shuts down the thread pool, and executes shutdown
	 * tasks.
	 */
	private static void cleanUp() {
		stopAll();
		threadPool.shutdown();
		System.out.println();
		cleanUpJobs.stream().forEachOrdered((task) -> {
			task.run();
		});
		IOHelper.closeQuietly(TumblGIFifier.getLogFileOutputStream());
	}

	public static void initialize() {
		EventQueue.invokeLater(() -> {
			new MainFrame().setVisible(true);
			ConcurrenceManager.executeLater(() -> {
				try {
					postInitJobs.stream().forEachOrdered(task -> task.run());
				} catch (Throwable ex) {
					log(ex);
					MainFrame.getMainFrame().getStatusProcessor().appendStatus("Error initializing.");
				}
				postInitJobs.clear();
			});
		});
	}

	/**
	 * This is the thread pool on which we should run thread-pool tasks. We add
	 * 1 because this allows us to use all processors while one thread is
	 * waiting on I/O.
	 */
	private static ScheduledExecutorService threadPool = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1);

	/**
	 * This queues a given Runnable to be executed "soon" but with unimportant
	 * timing.
	 */
	public static Future<?> executeLater(Runnable r) {
		return threadPool.submit(r);
	}

	/**
	 * This queues a runnable to be executed at regular intervals. It's called
	 * an "Imprecise" tick clock because there is no guarantee that the
	 * intervals will be exact, and if the tasks takes longer to complete, the
	 * longer it will be until the next iteration is executed.
	 */
	public static Future<?> createImpreciseTickClock(long tickTime, TimeUnit timeUnit, Runnable callback) {
		return threadPool.scheduleWithFixedDelay(callback, 0, tickTime, timeUnit);
	}

	private ConcurrenceManager() {

	}

}
