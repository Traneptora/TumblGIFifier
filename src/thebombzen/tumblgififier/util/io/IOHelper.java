package thebombzen.tumblgififier.util.io;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tukaani.xz.XZInputStream;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.Task;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;

/**
 * Java's I/O libraries are nice but not perfect. This class contains a (sigh) framework and some helper routines to make everything easier.
 */
public final class IOHelper {
	
	private IOHelper(){
		
	}
	
	/**
	 * We manage our own temp files. This is a list of the file names of temp files we've created. It's synchronized so we don't have to worry about anything like a ConcurrentModificationException.
	 */
	private static List<Path> tempFiles = Collections.synchronizedList(new ArrayList<Path>());
	
	/*
	 * In general, temp files should be deleted, but this is a backup just to be safe. 
	 */
	static {
		ConcurrenceManager.getConcurrenceManager().addShutdownTask(new Task(){
			@Override
			public void run(){
				for (Path path : tempFiles){
					path.toFile().delete();
				}
			}
		});
	}
	
	/**
	 * This temp file creator automatically creates the name and file extension.
	 * Files created by this are also automatically deleted when the program exits.
	 * This will usually not throw an I/O exception, but could in some corner cases, like if the temp filesystem is mounted as read-only.
	 */
	public static Path createTempFile() {
		return createTempFile(ResourcesManager.getResourcesManager().getTemporaryDirectory());
	}

	public static Path createTempFile(Path parentDirectory) {
		try {
			Path path = Files.createTempFile(parentDirectory, "tumblgififier", ".tmp").toAbsolutePath();
			path.toFile().deleteOnExit();
			tempFiles.add(path);
			return path;
		} catch (IOException ioe){
			throw new RuntimeIOException(ioe);
		}
	}
	
	/**
	 * This marks a file as a temporary file, so it will be deleted on exit.
	 */
	public static void markTempFile(Path filepath) {
		Path absolutePath = filepath.toAbsolutePath();
		absolutePath.toFile().deleteOnExit();
		tempFiles.add(absolutePath);
	}

	/**
	 * Close a stream quietly because we honestly don't care if a stream.close() throws IOException
	 */
	public static void closeQuietly(Closeable cl) {
		if (cl == null) {
			return;
		}
		try {
			cl.close();
		} catch (IOException ioe) {
			// do nothing
		}
	}

	/**
	 * Safely delete a temporary file. We don't care if it actually is "marked as temporary" but it will be deleted anyway.
	 * @return true if the file was deleted successfully, false if it still exists.
	 */
	public static boolean deleteTempFile(Path path) {
		if (path == null) {
			return false;
		}
		path.toFile().deleteOnExit();
		try {
			Files.deleteIfExists(path);
		} catch (IOException ioe) {
			// probably still open on windows
			// but logging is nice
			log(ioe);
		}
		if (Files.exists(path)){
			return false;
		} else {
			try {
				tempFiles.remove(path.toRealPath());
			} catch (IOException ioe){
				tempFiles.remove(path.toAbsolutePath());
			}
			return true;
		}
	}

	/**
	 * Download a file from the given URL, and save it to the given File. This should only be used for small files, because it blocks while the file is downloading and doesn't provide a progress indicator.
	 * If an error occurs, an exception will be thrown.
	 */
	public static void downloadFromInternet(URL url, Path target) throws RuntimeIOException {
		InputStream in = null;
		try {
			in = url.openStream();
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ioe){
			throw new RuntimeIOException(ioe);
		} finally {
			closeQuietly(in);
		}
	}

	/**
	 * Download the first line of a text file from the given URL and return it as a string.
	 * This is mostly useful for checking versions and things of that sort.
	 * If an error occurs an exception will be thrown.
	 * The file is assumed to be in UTF-8.
	 */
	public static String downloadFirstLineFromInternet(URL url) throws RuntimeIOException {
		try {
			return IOHelper.getFirstLineOfInputStream(url.openStream());
		} catch (IOException ioe){
			throw new RuntimeIOException(ioe);
		}
	}

	/**
	 * This method reads from the given InputStream and decodes it into text, assuming it's in UTF-8. Then it returns the first line of that text and closes the InputStream.
	 * If an I/O error occurs an exception will be thrown, but the stream will still be closed.
	 */
	public static String getFirstLineOfInputStream(InputStream in) throws RuntimeIOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			return reader.lines().findFirst().orElse("");
		} finally {
			closeQuietly(reader);
		}
	}

	/**
	 * This method reads from the given File and decodes it into text, assuming its contents are encoded in UTF-8. Then it returns the first line of that text.
	 * If an I/O error occurs an exception will be thrown, but the stream will still be closed.
	 */
	public static String getFirstLineOfFile(Path path) throws RuntimeFNFException, RuntimeIOException {
		try {
			return getFirstLineOfInputStream(Files.newInputStream(path));
		} catch (FileNotFoundException fnfe){
			throw new RuntimeFNFException(fnfe);
		} catch (NoSuchFileException nsfe) {
			throw new RuntimeFNFException(nsfe);
		} catch (IOException ioe) {
			throw new RuntimeIOException(ioe);
		}
	}

	/**
	 * Java checks if a String represents a valid URL before it allows you to construct a URL, via the checked exception MalformedURLException.
	 * If we are certain that a URL is valid (for example, if it's hard-coded), this will gives us an unchecked way to construct a URL.
	 * Please do not pass un-safe URLs, or an Error will be thrown (not an Exception, an Error). 
	 */
	public static URL wrapSafeURL(String urlLocation) {
		try {
			return new URL(urlLocation);
		} catch (MalformedURLException ex) {
			throw new Error("You said it was safe!", ex);
		}
	}

	/**
	 * Download a file from the given URL, and save it to the given File. This should only be used for small files, because it blocks while the file is downloading and doesn't provide a progress indicator.
	 * The URL is assumed to point to an XZ-compressed file. It will decompress the file in realtime as it saves it, so the version downloaded will be uncompressed.
	 * If an error occurs, an exception will be thrown.
	 */
	public static void downloadFromInternetXZ(URL url, Path target) throws RuntimeIOException {
		try (InputStream in = new XZInputStream(url.openStream())) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ioe){
			throw new RuntimeIOException(ioe);
		}
	}

	/**
	 * Deletes the file quietly, suppressing annoying errors about it already not existing or being null.
	 * This method essentially ensures that the file doesn't exist.
	 * @param The path to delete.
	 * @return True if the file does not exist, False if it exists or it can't be determined.
	 */
	public static boolean deleteQuietly(Path path) {
		if (path == null) {
			return true;
		}
		try {
			Files.deleteIfExists(path);
		} catch (DirectoryNotEmptyException dnee) {

		} catch (IOException ex) {

		}
		return Files.notExists(path);
	}

}
