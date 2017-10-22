package thebombzen.tumblgififier;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.Task;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.RuntimeIOException;
import thebombzen.tumblgififier.util.io.SynchronizedOutputStream;
import thebombzen.tumblgififier.util.io.TeeOutputStream;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;

public final class TumblGIFifier {
	
	/** The version of TumblGIFifier */
	public static final String VERSION = "0.7.4";
	
	/**
	 * This is so we can cleanup a mess left by older versions.
	 * The version identifier is written to a file on start.
	 * TumblGIFifier checks the version identifier written in the file to determine which major changes have been made that need to be cleaned up.
	 * For example, version identifiers earlier than 1 littered the temp directory with temporary files. Major changes:
	 * 
	 * 0 - Initial
	 * 1 - No longer litter the temp directory with persistent temp files
	 * 2 - No longer split the output/error stream.
	 * 3 - Moved the local directory from ~/.tumblgififier to ~/.config/tumblgififier
	 * 
	 */
	public static final int VERSION_IDENTIFIER = 3;
	
	/**
	 * This output stream prints to the log file.
	 */
	private static PrintStream logFileOutputStream;
	
	/**
	 * Set this to true once old version cleanup has happened.
	 */
	private static volatile boolean initializedCleanup = false;
	
	/**
	 * Run our program.
	 */
	public static void main(final String[] args) throws IOException {
		
		Path bothLogFile = ResourcesManager.getResourcesManager().getLocalFile("full_log.log");
		
		// We use UTF-8 even if it's not the platform's default
		logFileOutputStream = new PrintStream(new SynchronizedOutputStream(Files.newOutputStream(bothLogFile)), true, "UTF-8");
		
		System.setErr(
				new PrintStream(new TeeOutputStream(System.err, logFileOutputStream), true, "UTF-8"));
		System.setOut(
				new PrintStream(new TeeOutputStream(System.out, logFileOutputStream), true, "UTF-8"));
		
		if (args.length != 0){
			if ("--help".equals(args[0])){
				printHelpAndExit(true);
			} else if (args.length != 1){
				printHelpAndExit(false);
			} else {
				ConcurrenceManager.getConcurrenceManager().addPostInitTask(new Task(80){
					@Override
					public void run(){
						MainFrame.getMainFrame().open(Paths.get(args[0]));
					}
				});
			}
		}
		
		log(String.format("System.getenv(\"appdata\"): %s", System.getenv("appdata")));
		log(String.format("System.getenv(\"XDG_CONFIG_HOME\"): %s", System.getenv("XDG_CONFIG_HOME")));
		log(String.format("System.getProperty(\"os.name\"): %s", System.getProperty("os.name")));
		log(String.format("System.getProperty(\"os.arch\"): %s", System.getProperty("os.arch")));
		log(String.format("System.getProperty(\"user.home\"): %s", System.getProperty("user.home")));
		
		EventQueue.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				new MainFrame().setVisible(true);
				ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
					@Override
					public void run(){
						ConcurrenceManager.getConcurrenceManager().postInit();
					}
				});
			}
		});

	}
	
	/**
	 * Returns the Log File OutputStream, a PrintStream that prints to the log file (rather than to System.out)
	 */
	public static PrintStream getLogFileOutputStream(){
		return logFileOutputStream;
	}

	public static void log(String line) {
		logFileOutputStream.println(line);
	}

	public static void log(Throwable t) {
		t.printStackTrace(logFileOutputStream);
	}

	/**
	 * Print help and exit, useful if tumblgififier was invoked from the command line.
	 * @param good Exit with success if set to true, otherwise exit with status 1.
	 */
	private static void printHelpAndExit(boolean good){
		System.out.println("tumblgififier\t--help");
		System.out.println("tumblgififier\t[filename]");
		System.exit(good ? 0 : 1);
	}
	
	/**
	 * Clean up the mess left behind by old versions of TumblGIFifier.
	 */
	public static synchronized void executeOldVersionCleanup(){
		if (initializedCleanup){
			return;
		}
		StatusProcessor processor = MainFrame.getMainFrame().getStatusProcessor();
		int version;
		try {
			String versionString = IOHelper.getFirstLineOfFile(ResourcesManager.getResourcesManager().getLocalFile("version_identifier.txt"));
			version = Integer.parseInt(versionString);
			if (version <= 0){
				throw new NumberFormatException();
			}
		} catch (RuntimeIOException | NumberFormatException e){
			version = 0;
		}

		try {
			Path tempFileDirectory = ResourcesManager.getResourcesManager().getTemporaryDirectory();
			Files.list(tempFileDirectory).forEach((path) -> {
				processor.appendStatus("Cleaning old temporary files... " + path.getFileName());
				IOHelper.deleteTempFile(path);
			});
		} catch (RuntimeIOException | IOException ioe){
			processor.appendStatus("Error cleaning old temporary files.");
			processor.processException(ioe);
		}
		Path profileMedium = ResourcesManager.getResourcesManager().getLocalFile("Profile-Medium.otf");
		Path profileMediumXZ = ResourcesManager.getResourcesManager().getLocalFile("Profile-Medium.otf.xz");
		if (Files.exists(profileMedium) || Files.exists(profileMediumXZ)){
			processor.appendStatus("Cleaning old font files... ");
			IOHelper.deleteTempFile(profileMedium);
			IOHelper.deleteTempFile(profileMediumXZ);
			processor.replaceStatus("Cleaning old font files... Done.");
		}

		if (version < 2){
			//processor.appendStatus("Executing Cleanup Routine: 2.");
			Path error = ResourcesManager.getResourcesManager().getLocalFile("error.log");
			Path output = ResourcesManager.getResourcesManager().getLocalFile("output.log");
			
			if (Files.exists(error) || Files.exists(output)){
				processor.appendStatus("Cleaning output/error split...");
				IOHelper.deleteTempFile(error);
				IOHelper.deleteTempFile(output);
				processor.replaceStatus("Cleaning output/error split... done.");
			}
		}
		if (version < 3) {
			Path legacyLocation = ResourcesManager.getLegacyLocalResourceLocation();
			if (Files.exists(legacyLocation)) {
				try {
					processor.appendStatus("Cleaning legacy appdata location...");
					Files.walk(legacyLocation).sorted(Comparator.<Path>reverseOrder()).forEach(IOHelper::deleteQuietly);
					processor.replaceStatus("Cleaning legacy appdata location... done.");
				} catch (IOException ioe) {
					processor.appendStatus("Error cleaning old temporary files.");
					processor.processException(ioe);
				}
			}
		}
		try (Writer w = Files.newBufferedWriter(ResourcesManager.getResourcesManager().getLocalFile("version_identifier.txt"))){
			w.write(String.format("%d\n", TumblGIFifier.VERSION_IDENTIFIER));
		} catch (IOException ex) {
			log(ex);
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
