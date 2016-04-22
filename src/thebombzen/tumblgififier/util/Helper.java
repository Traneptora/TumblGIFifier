package thebombzen.tumblgififier.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class Helper {

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
	 *            The collection of strings to conjoin.
	 * @return The conjoined string.
	 */
	public static String join(String conjunction, Iterable<String> list) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list) {
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
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param list
	 *            An iterator of the strings to conjoin.
	 * @return The conjoined string.
	 */
	public static String join(String conjunction, Iterator<String> list) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (list.hasNext()) {
			String item = list.next();
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
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list) {
			if (first) {
				first = false;
			} else {
				sb.append(conjunction);
			}
			sb.append(item);
		}
		return sb.toString();
	}
	
	private static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	public static ScheduledExecutorService getThreadPool(){
		return threadPool;
	}
	
	public static String escapeForVideoFilter(String input){
		return input.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace(":", "\\:").replace("'", "\\'");
	}
	
	private static File tempOverlayFile; 
	private static String tempOverlayFilename;
	private static String fontFile = escapeForVideoFilter(ExtrasManager.getExtrasManager().getOpenSansFontFileLocation());
	
	static {
		try {
			tempOverlayFile = File.createTempFile("tumblgififier", ".tmp").getAbsoluteFile();
			tempOverlayFilename = escapeForVideoFilter(tempOverlayFile.getAbsolutePath());
		} catch (IOException ioe){
			throw new Error("Cannot create temporary files.");
		}
	}
	
	public static String createDrawTextString(int width, int height, int fontSize, String message) {
		int size = (int)Math.ceil(fontSize * 270D / 1080D);
		int borderw = (int)Math.ceil(size * 7D / fontSize);
		String drawText = "rawtext=x=(w-tw)*0.5:y=h*0.9:bordercolor=black:fontcolor=white:borderw=" + borderw + ":fontfile=" + fontFile + ":fontsize=" + size + ":textfile=" + tempOverlayFilename;
		return drawText;
	}
	
}
