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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.tukaani.xz.XZInputStream;
import thebombzen.tumblgififier.ConcurrenceManager;
import thebombzen.tumblgififier.TumblGIFifier;

public final class IOHelper {
	
	private IOHelper(){
		
	}
	
	private static List<String> tempFiles = Collections.synchronizedList(new ArrayList<String>());
	
	static {
		ConcurrenceManager.getConcurrenceManager().addShutdownTask(new Runnable(){
			public void run(){
				for (String f : tempFiles){
					new File(f).delete();
				}
			}
		});
	}
	
	public static File createTempFile() throws IOException {
		File f = File.createTempFile("tumblgififier", ".tmp");
		f.deleteOnExit();
		tempFiles.add(f.getCanonicalPath());
		return f;
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

	public static boolean downloadFromInternetQuietly(URL url, File downloadTo) {
		try {
			downloadFromInternet(url, downloadTo);
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
	}

	public static String downloadFirstLineFromInternet(URL url) throws IOException {
		return IOHelper.getFirstLineOfInputStream(url.openStream());
	}

	public static String getFirstLineOfInputStream(InputStream in) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
			return reader.readLine();
		} finally {
			closeQuietly(reader);
		}
	}

	public static String getFirstLineOfFile(File file) throws IOException {
		return getFirstLineOfInputStream(new FileInputStream(file));
	}

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

	public static URL wrapSafeURL(String urlLocation) {
		try {
			return new URL(urlLocation);
		} catch (MalformedURLException ex) {
			throw new Error("You said it was safe!", ex);
		}
	}

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
