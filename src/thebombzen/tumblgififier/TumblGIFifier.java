package thebombzen.tumblgififier;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.SynchronizedOutputStream;
import thebombzen.tumblgififier.io.TeeOutputStream;
import thebombzen.tumblgififier.io.resources.ResourcesManager;
import thebombzen.tumblgififier.text.StatusProcessor;

public final class TumblGIFifier {
	
	/** The version of TumblGIFifier */
	public static final String VERSION = "0.6.0b";
	
	public static final int VERSION_IDENTIFIER = 2;
	
	/**
	 * True if the system is detected as a windows system, false otherwise.
	 */
	public static final boolean IS_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	
	/**
	 * File extension for executable files, with the period included. On
	 * windows, it's ".exe" and on other platforms it's the empty string.
	 */
	public static final String EXE_EXTENSION = IS_ON_WINDOWS ? ".exe" : "";
	
	public static OutputStream logFileOutputStream;
	
	private static volatile boolean initializedCleanup = false;
	
	/**
	 * Run our program.
	 */
	public static void main(String[] args) throws IOException {
		
		File bothLogFile = new File(ResourcesManager.getResourcesManager().getLocalAppDataLocation(), "full_log.log");
		
		logFileOutputStream = new SynchronizedOutputStream(new FileOutputStream(bothLogFile));
		
		System.setErr(
				new PrintStream(new TeeOutputStream(System.err, logFileOutputStream)));
		System.setOut(
				new PrintStream(new TeeOutputStream(System.out, logFileOutputStream)));
		
		EventQueue.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				new MainFrame().setVisible(true);
			}
		});

	}
	
	
	public static synchronized void executeOldVersionCleanup(){
		if (initializedCleanup){
			return;
		}
		StatusProcessor processor = MainFrame.getMainFrame().getStatusProcessor();
		int version;
		try {
			String versionString = IOHelper.getFirstLineOfFile(ResourcesManager.getResourcesManager().getLocalResource("version_identifier.txt"));
			version = Integer.parseInt(versionString);
			if (version <= 0){
				throw new NumberFormatException();
			}
		} catch (IOException | NumberFormatException e){
			version = 0;
		}
		if (version < 1){
			processor.appendStatus("Executing Cleanup Routine: 1.");
			processor.appendStatus("Cleaning old temporary files... ");
			try {
				File tempFile = IOHelper.createTempFile();
				File tempFileDirectory = tempFile.getParentFile();
				IOHelper.deleteTempFile(tempFile);
				for (File f : tempFileDirectory.listFiles()){
					if (f.getName().matches("^tumblgififier(.*)\\.tmp$")){
						processor.replaceStatus("Cleaning old temporary files... " + f.getName());
						IOHelper.deleteTempFile(f);
					}
				}
			} catch (IOException ioe){
				processor.appendStatus("Error cleaning old temporary files.");
				processor.processException(ioe);
			}
			processor.replaceStatus("Cleaning old temporary files... Done.");
			processor.appendStatus("Cleaning old font files... ");
			File profileMedium = ResourcesManager.getResourcesManager().getLocalResource("Profile-Medium.otf");
			IOHelper.deleteTempFile(profileMedium);
			File profileMediumXZ = ResourcesManager.getResourcesManager().getLocalResource("Profile-Medium.otf.xz");
			IOHelper.deleteTempFile(profileMediumXZ);
			processor.replaceStatus("Cleaning old font files... Done.");
		}
		if (version < 2){
			
			processor.appendStatus("Executing Cleanup Routine: 2.");
			
			processor.appendStatus("Cleaning output/error split...");
			
			File error = ResourcesManager.getResourcesManager().getLocalResource("error.log");
			IOHelper.deleteTempFile(error);
			File output = ResourcesManager.getResourcesManager().getLocalResource("output.log");
			IOHelper.deleteTempFile(output);
			
			processor.replaceStatus("Cleaning output/error split... done.");
			
		}
		try (Writer w = new OutputStreamWriter(new FileOutputStream(ResourcesManager.getResourcesManager().getLocalResource("version_identifier.txt")), Charset.forName("UTF-8"))){
			w.write(Integer.toString(TumblGIFifier.VERSION_IDENTIFIER));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		initializedCleanup = true;
	}
	
	/**
	 * Quit the program. Cleans up the subprocesses, shuts down the thread pool,
	 * then exists.
	 */
	public static void quit() {
		System.exit(0);
	}
	
}
