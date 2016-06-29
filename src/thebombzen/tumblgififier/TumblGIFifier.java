package thebombzen.tumblgififier;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.io.SynchronizedOutputStream;
import thebombzen.tumblgififier.io.TeeOutputStream;
import thebombzen.tumblgififier.io.resources.ExtrasManager;

public final class TumblGIFifier {
	
	/** The version of TumblGIFifier */
	public static final String VERSION = "0.6.0a";
	
	/**
	 * True if the system is detected as a windows system, false otherwise.
	 */
	public static final boolean IS_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	
	/**
	 * File extension for executable files, with the period included. On
	 * windows, it's ".exe" and on other platforms it's the empty string.
	 */
	public static final String EXE_EXTENSION = IS_ON_WINDOWS ? ".exe" : "";
	
	public static OutputStream outputLogFileOutputStream;
	public static OutputStream errorLogFileOutputStream;
	public static OutputStream bothLogFileOutputStream;
	
	
	
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
		
		System.setErr(
				new PrintStream(new TeeOutputStream(System.err, errorLogFileOutputStream, bothLogFileOutputStream)));
		System.setOut(
				new PrintStream(new TeeOutputStream(System.out, outputLogFileOutputStream, bothLogFileOutputStream)));
		
		EventQueue.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				new MainFrame().setVisible(true);
			}
		});
	}
	
	/**
	 * Quit the program. Cleans up the subprocesses, shuts down the thread pool,
	 * then exists.
	 */
	public static void quit() {
		System.exit(0);
	}
	
}
