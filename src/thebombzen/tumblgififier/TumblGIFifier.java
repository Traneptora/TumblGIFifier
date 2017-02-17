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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.SynchronizedOutputStream;
import thebombzen.tumblgififier.io.TeeOutputStream;
import thebombzen.tumblgififier.io.resources.ResourcesManager;
import thebombzen.tumblgififier.text.StatusProcessor;

public final class TumblGIFifier {
	
	/** The version of TumblGIFifier */
	public static final String VERSION = "0.7.1";
	
	/**
	 * This is so we can cleanup a mess left by older versions.
	 * The version identifier is written to a file on start.
	 * TumblGIFifier checks the version identifier written in the file to determine which major changes have been made that need to be cleaned up.
	 * For example, version identifiers earlier than 1 littered the temp directory with temporary files. Major changes:
	 * 
	 * 0 - Initial
	 * 1 - No longer littler the temp directory with persistent temp files
	 * 2 - No longer split the output/error stream.
	 * 3 - Moved the local directory from ~/.tumblgififier to ~/.config/tumblgififier
	 * 
	 */
	public static final int VERSION_IDENTIFIER = 3;
	
	/**
	 * True if the system is detected as a windows system, false otherwise.
	 */
	public static final boolean IS_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	
	/**
	 * File extension for executable files, with the period included. On
	 * windows, it's ".exe" and on other platforms it's the empty string.
	 */
	public static final String EXE_EXTENSION = IS_ON_WINDOWS ? ".exe" : "";
	
	static OutputStream logFileOutputStream;
	
	private static volatile boolean initializedCleanup = false;
	
	/**
	 * Run our program.
	 */
	public static void main(final String[] args) throws IOException {
		
		File bothLogFile = ResourcesManager.getResourcesManager().getLocalResource("full_log.log");
		
		logFileOutputStream = new SynchronizedOutputStream(new FileOutputStream(bothLogFile));
		
		System.setErr(
				new PrintStream(new TeeOutputStream(System.err, logFileOutputStream)));
		System.setOut(
				new PrintStream(new TeeOutputStream(System.out, logFileOutputStream)));
		
		if (args.length != 0){
			if ("--help".equals(args[0])){
				printHelpAndExit(true);
			} else if (args.length != 1){
				printHelpAndExit(false);
			} else {
				ConcurrenceManager.getConcurrenceManager().addPostInitTask(new Task(80){
					@Override
					public void run(){
						MainFrame.getMainFrame().open(args[0]);
					}
				});
			}
		}
		
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
	
	private static void printHelpAndExit(boolean good){
		System.out.println("tumblgififier\t--help");
		System.out.println("tumblgififier\t[filename]");
		System.exit(good ? 0 : 1);
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
		} catch (RuntimeIOException | NumberFormatException e){
			version = 0;
		}
		if (version < 1){
			boolean did = false;

			try {
				File tempFile = IOHelper.createTempFile();
				File tempFileDirectory = tempFile.getParentFile();
				IOHelper.deleteTempFile(tempFile);
				for (File f : tempFileDirectory.listFiles()){
					if (f.getName().matches("^tumblgififier(.*)\\.tmp$")){
						if (!did){
							//processor.appendStatus("Executing Cleanup Routine: 1.");
							processor.appendStatus("Cleaning old temporary files... ");
						}
						did = true;
						processor.replaceStatus("Cleaning old temporary files... " + f.getName());
						IOHelper.deleteTempFile(f);
					}
				}
			} catch (RuntimeIOException ioe){
				processor.appendStatus("Error cleaning old temporary files.");
				processor.processException(ioe);
			}
			if (did){
				processor.replaceStatus("Cleaning old temporary files... Done.");
			}
			File profileMedium = ResourcesManager.getResourcesManager().getLocalResource("Profile-Medium.otf");
			File profileMediumXZ = ResourcesManager.getResourcesManager().getLocalResource("Profile-Medium.otf.xz");
			if (profileMedium.exists() || profileMediumXZ.exists()){
				processor.appendStatus("Cleaning old font files... ");
				IOHelper.deleteTempFile(profileMedium);
				IOHelper.deleteTempFile(profileMediumXZ);
				processor.replaceStatus("Cleaning old font files... Done.");
			}
			
		}
		if (version < 2){
			//processor.appendStatus("Executing Cleanup Routine: 2.");
			File error = ResourcesManager.getResourcesManager().getLocalResource("error.log");
			File output = ResourcesManager.getResourcesManager().getLocalResource("output.log");
			
			if (error.exists() || output.exists()){
				processor.appendStatus("Cleaning output/error split...");
				IOHelper.deleteTempFile(error);
				IOHelper.deleteTempFile(output);
				processor.replaceStatus("Cleaning output/error split... done.");
			}
		}
		if (version < 3) {
			File legacyLocation = new File(ResourcesManager.getLegacyLocalResourceLocation());
			if (legacyLocation.exists()) {
				try {
					processor.appendStatus("Cleaning legacy appdata location...");
					Files.walkFileTree(legacyLocation.toPath(), new SimpleFileVisitor<Path>(){
						
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}
						
						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
							if (e == null) {
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							} else {
								// directory iteration failed
								throw e;
							}
						}
						
					});
					processor.replaceStatus("Cleaning legacy appdata location... done.");
				} catch (IOException ioe) {
					processor.appendStatus("Error cleaning old temporary files.");
					processor.processException(ioe);
				}
			}
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
