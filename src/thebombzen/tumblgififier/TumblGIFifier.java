package thebombzen.tumblgififier;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.Box;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.io.NullInputStream;
import thebombzen.tumblgififier.io.NullOutputStream;
import thebombzen.tumblgififier.io.SynchronizedOutputStream;
import thebombzen.tumblgififier.io.TeeOutputStream;
import thebombzen.tumblgififier.util.ExtrasManager;
import thebombzen.tumblgififier.util.ProcessTerminatedException;


public final class TumblGIFifier {

	/** The version of TumblGIFifier */
	public static final String VERSION = "0.5.1b";

	/**
	 * Run our program.
	 */
	public static void main(String[] args) throws IOException {
		
		File outputLogFile = new File(ExtrasManager.getExtrasManager().getLocalAppDataLocation(), "output.log");
		File errorLogFile = new File(ExtrasManager.getExtrasManager().getLocalAppDataLocation(), "error.log");
		File bothLogFile = new File(ExtrasManager.getExtrasManager().getLocalAppDataLocation(), "full_log.log");
		
		outputLogFileOutputStream = new SynchronizedOutputStream(new FileOutputStream(outputLogFile));
		errorLogFileOutputStream = new SynchronizedOutputStream(new FileOutputStream(errorLogFile));
		bothLogFileOutputStream = new SynchronizedOutputStream(new FileOutputStream(bothLogFile));
		
		System.setErr(new PrintStream(new TeeOutputStream(System.err, errorLogFileOutputStream, bothLogFileOutputStream)));
		System.setOut(new PrintStream(new TeeOutputStream(System.out, outputLogFileOutputStream, bothLogFileOutputStream)));
		
		EventQueue.invokeLater(new Runnable(){
			@Override
			public void run() {
				new MainFrame().setVisible(true);
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
			public void run(){
				TumblGIFifier.cleanUp();
			}
		}));
	}
	
	private static OutputStream outputLogFileOutputStream;
	private static OutputStream errorLogFileOutputStream;
	private static OutputStream bothLogFileOutputStream;

	/**
	 * A flag used to determine if we're cleaning up all the subprocesses we've
	 * started. Normally, ending a process will just cause the next stage in the
	 * GIF creation to continue. If this flag is set, we won't create any more
	 * processes.
	 */
	private static volatile boolean cleaningUp = false;
	
	/**
	 * This is a list of all processes started by our program. It's used so we
	 * can end them all upon exit.
	 */
	private static volatile List<Process> processes = new ArrayList<>();

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
	public static InputStream exec(boolean join, String... args) throws ProcessTerminatedException {
		try {
			if (join) {
				return exec(new NullOutputStream(), args);
			} else {
				return new BufferedInputStream(exec(null, args));
			}
		} catch (IOException ioe) {
			// NullOutputStream doesn't throw IOException, so if we get one here
			// it's really weird.
			if (ioe.getMessage().equals("Stream closed")){
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
	public static void stopAll(){
		cleaningUp = true;
		for (Process p : processes) {
			if (p.isAlive()){
				p.destroy();
			}
		}
		processes.clear();
		cleaningUp = false;
	}

	/**
	 * This stops all subprocesses and shuts down the thread pool.
	 */
	public static void cleanUp(){
		stopAll();
		TumblGIFifier.getThreadPool().shutdown();
		System.out.println();
		closeQuietly(outputLogFileOutputStream);
		closeQuietly(errorLogFileOutputStream);
		closeQuietly(bothLogFileOutputStream);
	}

	/**
	 * Quit the program. Cleans up the subprocesses, shuts down the thread pool, then exists.
	 */
	public static void quit() {
		cleanUp();
		System.exit(0);
	}
	
	/**
	 * Recursively enable or disable a component and all of its children.
	 */
	public static void setEnabled(Component component, boolean enabled) {
		component.setEnabled(enabled);
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				setEnabled(child, enabled);
			}
		}
	}
	
	/**
	 * True if the system is detected as a windows system, false otherwise.
	 */
	public static final boolean IS_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	
	/**
	 * File extension for executable files, with the period included. On
	 * windows, it's ".exe" and on other platforms it's the empty string.
	 */
	public static final String EXE_EXTENSION = IS_ON_WINDOWS ? ".exe" : "";
	
	/**
	 * Close a stream quietly because we honestly don't care if a stream.close()
	 * throws IOException
	 */
	public static void closeQuietly(Closeable cl) {
		try {
			cl.close();
		} catch (IOException ioe) {
			// do nothing
		}
	}
	
	/**
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param list
	 *            The array of strings to conjoin.
	 * @return The conjoined string.
	 */
	public static String join(String conjunction, String[] list) {
		return join(conjunction, Arrays.asList(list));
	}
	
	/**
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param list
	 *            The collection of strings to conjoin.
	 * @return The conjoined string.
	 */
	public static String join(String conjunction, Iterable<String> list) {
		return join(conjunction, list.iterator());
	}
	
	/**
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param iterator
	 *            An iterator of the strings to conjoin.
	 * @return The conjoined string.
	 */
	public static String join(String conjunction, Iterator<String> iterator) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (iterator.hasNext()) {
			String item = iterator.next();
			if (first) {
				first = false;
			} else {
				sb.append(conjunction);
			}
			sb.append(item);
		}
		return sb.toString();
	}
	
	/**
	 * This is the thread pool on which we should run thread-pool tasks.
	 */
	private static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	
	/**
	 * Escapes a string to be used in an FFmpeg video filter. Replaces backslash, comma, semicolon, colon, single quote, brackets, and equal signs with escaped versions.
	 * @param input This is the input string to escape
	 * @return An escaped string.
	 */
	public static String escapeForVideoFilter(String input){
		return input.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace(":", "\\:").replace("'", "\\'").replace("[", "\\[").replace("]", "\\]").replace("=", "\\=");
	}
	
	/**
	 * We dump the overlay text to a file so we don't have to escape it.
	 */
	private static File tempOverlayFile;
	
	/**
	 * This is the escaped filename of the tempOverlayFile.
	 */
	private static String tempOverlayFilename;
	
	/**
	 * This is the escaped filename of the Open Sans font file location.
	 */
	private static String fontFile = escapeForVideoFilter(ExtrasManager.getExtrasManager().getOpenSansFontFileLocation());
	
	static {
		try {
			tempOverlayFile = File.createTempFile("tumblgififier", ".tmp").getAbsoluteFile();
			tempOverlayFilename = escapeForVideoFilter(tempOverlayFile.getAbsolutePath());
		} catch (IOException ioe){
			throw new Error("Cannot create temporary files.");
		}
	}
	
	/**
	 * This creates the drawtext filter to be used with the text overlay feature.
	 * Just drop right after -vf.
	 * Note that you shouldn't use this more than once at a time,
	 * because it dumps the message to a file and points the filter to that file.
	 * @param width The video width
	 * @param height The video height
	 * @param fontSize The font point size of the message we want to render
	 * @param message The text of the message we want to render
	 * @return A drop-in drawtext filter, to be placed right after the -vf argument.
	 */
	public static String createDrawTextString(int width, int height, int fontSize, String message) {
		int size = (int)Math.ceil(fontSize * height / 1080D);
		int borderw = (int)Math.ceil(size * 7D / fontSize);
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempOverlayFile), Charset.forName("UTF-8")))){
			writer.write(message);
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
		String drawText = "drawtext=x=(w-tw)*0.5:y=0.935*(h-0.5*" + size + "):bordercolor=black:fontcolor=white:borderw=" + borderw + ":fontfile=" + fontFile + ":fontsize=" + size + ":textfile=" + tempOverlayFilename;
		return drawText;
	}
	
	
	public static Component wrapLeftAligned(Component comp) {
		Box box = Box.createHorizontalBox();
		box.add(comp);
		box.add(Box.createHorizontalGlue());
		return box;
	}
	
	public static Component wrapCenterAligned(Component comp) {
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(comp);
		box.add(Box.createHorizontalGlue());
		return box;
	}
	
	public static Component wrapLeftRightAligned(Component left, Component right) {
		Box box = Box.createHorizontalBox();
		box.add(left);
		box.add(Box.createHorizontalGlue());
		box.add(right);
		return box;
	}


	/**
	 * This returns the thread pool for thread-pool-like tasks. 
	 */
	public static ScheduledExecutorService getThreadPool(){
		return threadPool;
	}
	
	
}
