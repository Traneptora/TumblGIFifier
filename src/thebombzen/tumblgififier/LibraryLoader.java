package thebombzen.tumblgififier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import thebombzen.tumblgififier.util.io.resources.ResourceNotFoundException;

@PreLoadable
public final class LibraryLoader {

	private LibraryLoader() {

	}

	public static void main(String[] args) throws Exception {
		List<Path> libraries = extractExternalLibraries();
		Path javaLocation = Paths.get(System.getProperty("java.home"), "bin",
				"java" + OperatingSystem.getLocalOS().getExeExtension());
		Path thisJar = Paths.get(LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (thisJar.getFileName().toString().endsWith(".jar")) {
			libraries.add(thisJar);
			String cp = String.join(File.pathSeparator,
					libraries.stream().map(Path::toString).collect(Collectors.toList()));
			List<String> newArgs = new ArrayList<>();
			newArgs.add(javaLocation.toString());
			newArgs.add("-cp");
			newArgs.add(cp);
			newArgs.add("thebombzen.tumblgififier.TumblGIFifier");
			newArgs.addAll(Arrays.asList(args));
			ProcessBuilder builder = new ProcessBuilder(newArgs);
			builder.inheritIO().start();
			System.exit(0);
		} else {
			Class<?> clazz = Class.forName("thebombzen.tumblgififier.TumblGIFifier", false,
					ClassLoader.getSystemClassLoader());
			Method method = clazz.getMethod("main", String[].class);
			method.invoke(null, (Object) args);
		}
	}

	private static Path localResourceLocation = null;

	private static Path getLocalResourceLocation() throws IOException {
		if (localResourceLocation != null) {
			return localResourceLocation;
		}
		localResourceLocation = OperatingSystem.getLocalOS().getLocalResourceLocation().toAbsolutePath();
		if (Files.exists(localResourceLocation) && !Files.isDirectory(localResourceLocation)) {
			Files.delete(localResourceLocation);
			System.err.println("Deleting existing tumblgififier non-directory.");
		}
		Files.createDirectories(localResourceLocation);
		return localResourceLocation;
	}

	private static List<Path> extractExternalLibraries() throws IOException, ResourceNotFoundException {
		List<Path> ret = new ArrayList<>();
		Path thisJarFile = null;
		try {
			thisJarFile = Paths.get(LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException ex) {
			throw new ResourceNotFoundException("Error in URI syntax?", ex);
		}
		if (!Files.isRegularFile(thisJarFile)) {
			return ret;
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
						if (Files.exists(lib) && Files.getLastModifiedTime(lib).equals(entry.getLastModifiedTime())
								&& Files.size(lib) == entry.getSize()) {
							System.out.println("Found " + fname + ", not extracting.");
							ret.add(lib);
							continue;
						}
						System.out.println("Extracting " + fname + ".");
						in = zipFile.getInputStream(entry);
						Files.copy(in, lib, StandardCopyOption.REPLACE_EXISTING);
						Files.setLastModifiedTime(lib, entry.getLastModifiedTime());
						ret.add(lib);
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
		} finally {
			if (zipFile != null) {
				zipFile.close();
			}
		}
		return ret;
	}
}
