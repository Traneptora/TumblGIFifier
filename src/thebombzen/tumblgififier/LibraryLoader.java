package thebombzen.tumblgififier;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import thebombzen.tumblgififier.util.io.RuntimeIOException;
import thebombzen.tumblgififier.util.io.resources.ResourceNotFoundException;

@PreLoadable
public final class LibraryLoader {
	
	private LibraryLoader() {
		
	}
	
	public static void main(String[] args) throws Exception {
		extractExternalLibraries();
		Class<?> clazz = Class.forName("thebombzen.tumblgififier.TumblGIFifier", false, ClassLoader.getSystemClassLoader());
		Method method = clazz.getMethod("main", String[].class);
		method.invoke(null, (Object)args);
	}
	
	private static Path localResourceLocation = null;
	
	/**
	 * Add a Jar file to the class path.
	 * 
	 * From StackOverflow: https://stackoverflow.com/a/60766/
	 * @param file
	 */
	private static void addToClasspath(Path path) {
		URL url = null;
		try {	
			url = path.toUri().toURL();
		} catch (MalformedURLException ex) {
			throw new ResourceNotFoundException("Malformed URL? " + path, ex);
		}
		ClassLoader loader = LibraryLoader.class.getClassLoader();
		try {
			Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", new Class<?>[]{URL.class});
			addUrl.setAccessible(true);
			addUrl.invoke(loader, url);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			throw new Error(ex);
		}
	}

	public static Path getLocalResourceLocation() {
		if (localResourceLocation != null) {
			return localResourceLocation;
		}
		try {
			localResourceLocation = OperatingSystem.getLocalOS().getLocalResourceLocation().toAbsolutePath();
			if (Files.exists(localResourceLocation) && !Files.isDirectory(localResourceLocation)) {
				Files.delete(localResourceLocation);
				System.err.println("Deleting existing tumblgififier non-directory.");
			}
			Files.createDirectories(localResourceLocation);
			return localResourceLocation;
		} catch (IOException ioe) {
			throw new RuntimeIOException(ioe);
		}
	}

	public static void extractExternalLibraries() {
		Path thisJarFile = null;
		try {
			thisJarFile = Paths.get(LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException ex) {
			throw new ResourceNotFoundException("Error in URI syntax?", ex);
		}
		if (!Files.isRegularFile(thisJarFile)) {
			return;
		}
		Path libs = getLocalResourceLocation().resolve("lib");
		ZipFile zipFile = null;
		try {
			Files.createDirectories(libs);
			zipFile = new ZipFile(thisJarFile.toString());
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().startsWith("lib/")) {
					String fname = entry.getName().substring(4);
					if (fname.equals("")) {
						continue;
					}
					InputStream in = null;
					try {
						Path lib = libs.resolve(fname);
						if (Files.exists(lib) && Files.getLastModifiedTime(lib).equals(entry.getLastModifiedTime()) && Files.size(lib) == entry.getSize()){
							System.out.println("Found " + fname + ", not extracting.");
							addToClasspath(lib);
							continue;
						}
						System.out.println("Extracting " + fname + ".");
						in = zipFile.getInputStream(entry);
						Files.copy(in, lib, StandardCopyOption.REPLACE_EXISTING);
						Files.setLastModifiedTime(lib, entry.getLastModifiedTime());
						addToClasspath(lib);
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException ioe) {
								// don't care
							}
						}
					}
				}
			}
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException ioe) {
					// don't care
				}
			}
		}
	}
	
}
