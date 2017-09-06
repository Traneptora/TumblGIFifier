package thebombzen.tumblgififier.util.io.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.RuntimeIOException;

public final class LibraryLoader {
	
	private LibraryLoader() {
		
	}
	
	private static LibraryLoader loader = new LibraryLoader();
	
	public static LibraryLoader getLibraryLoader() {
		return loader;
	}
	
	static String getApplicationDataLocation() {
		String name = System.getProperty("os.name");
		if (name.toLowerCase().contains("windows")) {
			return System.getenv("appdata");
		} else if (name.toLowerCase().contains("mac") || name.toLowerCase().contains("osx")
				|| name.toLowerCase().contains("os x")) {
			return System.getProperty("user.home") + "/Library/Application Support";
		} else {
			return System.getProperty("user.home") + "/.config";
		}
	}
	
	/**
	 * Add a Jar file to the class path.
	 * 
	 * From StackOverflow: https://stackoverflow.com/a/60766/
	 * @param file
	 */
	private void addToClasspath(File file) {
		URL url;
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException ex) {
			throw new ResourceNotFoundException("Malformed URL? " + file.toString(), ex);
		}
		URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Method method;
		try {
			method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		} catch (NoSuchMethodException ex) {
			throw new ResourceNotFoundException("Cannot find addURL", ex);
		}
		method.setAccessible(true);
		try {
			method.invoke(classLoader, url);
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new ResourceNotFoundException("Cannot call addURL", ex);
		}
	}

	public void extractExternalLibraries() {
		File thisJarFile = null;
		try {
			thisJarFile = new File(TumblGIFifier.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException ex) {
			throw new ResourceNotFoundException("Error in URI syntax?", ex);
		}
		if (!thisJarFile.isFile()) {
			return;
		}
		File libs = new File(getApplicationDataLocation() + File.separator + "tumblgififier" + File.separator + "lib");
		libs.mkdirs();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(thisJarFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().startsWith("lib/")) {
					String fname = entry.getName().substring(4);
					if (fname.equals("")) {
						continue;
					}
					FileOutputStream fout = null;
					InputStream in = null;
					ReadableByteChannel rbc = null;
					try {
						File lib = new File(libs, fname);
						if (lib.isFile() && lib.lastModified() == entry.getTime() && lib.length() == entry.getSize()) {
							System.out.println("Found " + fname + ", not extracting.");
							addToClasspath(lib);
							continue;
						}
						fout = new FileOutputStream(new File(libs, fname));
						FileChannel channel = fout.getChannel();
						in = zipFile.getInputStream(entry);
						rbc = Channels.newChannel(in);
						try {
							channel.transferFrom(rbc, 0L, Long.MAX_VALUE);
						} finally {
							
						}
						lib.setLastModified(entry.getTime());
						addToClasspath(lib);
					} finally {
						IOHelper.closeQuietly(fout);
						IOHelper.closeQuietly(in);
						IOHelper.closeQuietly(rbc);
					}
				}
			}
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		} finally {
			IOHelper.closeQuietly(zipFile);
		}
		
	}
	
}
