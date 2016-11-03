package thebombzen.tumblgififier.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tukaani.xz.XZInputStream;
import thebombzen.tumblgififier.ConcurrenceManager;
/**
 * Java's I/O libraries are nice but not perfect. This class contains some helper routines to make everything easier.
 */
public final class IOHelper {
	
	private IOHelper(){
		
	}
	/**
	 * We manage our own temp files. This is a list of the file names of temp files we've created. It's synchronized so we don't have to worry about anything like a ConcurrentModificationException.
	 */
	private static List<String> tempFiles = Collections.synchronizedList(new ArrayList<String>());
	
	/*
	 * In general, temp files should be deleted, but this is a backup just to be safe. 
	 */
	static {
		ConcurrenceManager.getConcurrenceManager().addShutdownTask(new Runnable(){
			public void run(){
				for (String f : tempFiles){
					new File(f).delete();
				}
			}
		});
	}
	
	/**
	 * This temp file creator automatically creates the name and file extension.
	 * Files created by this are also automatically deleted when the program exits.
	 * This will usually not throw an I/O exception, but could in some corner cases, like if the temp filesystem is mounted as read-only.
	 */
	public static File createTempFile() throws IOException {
		File f = File.createTempFile("tumblgififier", ".tmp");
		f.deleteOnExit();
		tempFiles.add(f.getCanonicalPath());
		return f;
	}
	
	/**
	 * This marks a file as a temporary file, so it will be deleted on exit.
	 */
	public static void markTempFile(String file) {
		tempFiles.add(file);
		new File(file).deleteOnExit();
	}

	/**
	 * Close a stream quietly because we honestly don't care if a stream.close()
	 * throws IOException
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
	public static boolean deleteTempFile(File f) {
		if (f == null) {
			return false;
		}
		f.deleteOnExit();
		f.delete();
		if (f.exists()){
			return false;
		} else {
			try {
				tempFiles.remove(f.getCanonicalPath());
			} catch (IOException ioe){
				tempFiles.remove(f.getAbsolutePath());
			}
			return true;
		}
	}

	/**
	 * Download a file from the given URL, and save it to the given File. This should only be used for small files, because it blocks while the file is downloading and doesn't provide a progress indicator.
	 * If an error occurs, an exception will be thrown.
	 */
	public static void downloadFromInternet(URL url, File downloadTo) throws IOException {
		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		try {
			rbc = Channels.newChannel(url.openStream());
			fos = new FileOutputStream(downloadTo);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} finally {
			closeQuietly(rbc);
			closeQuietly(fos);
		}
	}

	/**
	 * Download a file from the given URL, and save it to the given File. This should only be used for small files, because it blocks while the file is downloading and doesn't provide a progress indicator.
	 * If an error occurs, this method will return false, and upon success, return true. This is mostly useful for un-important downloads whom are non-critical errors if they fail.
	 */
	public static boolean downloadFromInternetQuietly(URL url, File downloadTo) {
		try {
			downloadFromInternet(url, downloadTo);
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
	}

	/**
	 * Download the first line of a text file from the given URL and return it as a string.
	 * This is mostly useful for checking versions and things of that sort.
	 * If an error occurs an exception will be thrown.
	 * The file is assumed to be in UTF-8.
	 */
	public static String downloadFirstLineFromInternet(URL url) throws IOException {
		return IOHelper.getFirstLineOfInputStream(url.openStream());
	}

	/**
	 * This method reads from the given InputStream and decodes it into text, assuming it's in UTF-8. Then it returns the first line of that text and closes the InputStream.
	 * If an I/O error occurs an exception will be thrown, but the stream will still be closed.
	 */
	public static String getFirstLineOfInputStream(InputStream in) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
			return reader.readLine();
		} finally {
			closeQuietly(reader);
		}
	}

	/**
	 * This method reads from the given File and decodes it into text, assuming its contents are encoded in UTF-8. Then it returns the first line of that text.
	 * If an I/O error occurs an exception will be thrown, but the stream will still be closed.
	 */
	public static String getFirstLineOfFile(File file) throws IOException {
		return getFirstLineOfInputStream(new FileInputStream(file));
	}

	/**
	 * This method reads from the given File and decodes it into text, assuming its contents are encoded in UTF-8. Then it returns the first line of that text.
	 * If an I/O error occurs then an empty string will be returned, unless it's a FileNotFoundException. That will be passed on to the caller.
	 */
	public static String getFirstLineOfFileQuietly(File file) throws FileNotFoundException {
		try {
			return getFirstLineOfInputStream(new FileInputStream(file));
		} catch (FileNotFoundException fnfe) {
			throw fnfe;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return "";
		}
	}

	/**
	 * Download the first line of a text file from the given URL and return it as a string.
	 * This is mostly useful for checking versions and things of that sort.
	 * If an error occurs an empty string will be returned.
	 * The file is assumed to be in UTF-8.
	 */
	public static String downloadFirstLineFromInternetQuietly(URL url) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")));
			return reader.readLine();
		} catch (IOException ioe) {
			return "";
		} finally {
			closeQuietly(reader);
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
	public static void downloadFromInternetXZ(URL url, File downloadTo) throws IOException {
		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		try {
			rbc = Channels.newChannel(new XZInputStream(url.openStream()));
			fos = new FileOutputStream(downloadTo);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} finally {
			closeQuietly(rbc);
			closeQuietly(fos);
		}
	}
}
