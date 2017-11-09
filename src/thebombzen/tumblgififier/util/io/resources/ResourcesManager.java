package thebombzen.tumblgififier.util.io.resources;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import thebombzen.tumblgififier.OperatingSystem;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.text.StatusProcessor;

/**
 * Framework for managing global external resources.
 */
public class ResourcesManager {

	private static Path localResourceLocation = null;

	public static Path getLocalResourceLocation() {
		if (localResourceLocation != null) {
			return localResourceLocation;
		}
		try {
			Path location = OperatingSystem.getLocalOS().getLocalResourceLocation().toAbsolutePath();
			if (Files.exists(location) && !Files.isDirectory(location)) {
				Files.delete(location);
				System.err.println("Deleting existing tumblgififier non-directory.");
			}
			Files.createDirectories(location);
			localResourceLocation = location;
		} catch (IOException ioe) {
			log(ioe);
			throw new Error("Can't access local resource location.", ioe);
		}
		return localResourceLocation;
	}

	private static String getLegacyApplicationDataLocation() {
		String name = System.getProperty("os.name");
		if (name.toLowerCase().contains("windows")) {
			return System.getenv("appdata");
		} else if (name.toLowerCase().contains("mac") || name.toLowerCase().contains("osx")
				|| name.toLowerCase().contains("os x")) {
			return System.getProperty("user.home") + "/Library/Application Support";
		} else {
			return System.getProperty("user.home");
		}
	}

	/**
	 * This is for the purpose of cleaning up the old application data location.
	 * It should not be used outside of cleanup.
	 */
	public static Path getLegacyLocalResourceLocation() {
		return Paths.get(getLegacyApplicationDataLocation(), ".tumblgififier").toAbsolutePath();
	}

	private static String getLatestDownloadLocation() {
		return "https://thebombzen.com/TumblGIFifier/resources/latest.txt";
	}

	private static String getPkgVersionsLocation(String pkg) {
		OperatingSystem local = OperatingSystem.getLocalOS();
		switch (pkg) {
			case "mpv":
				switch (OperatingSystem.getLocalOS()) {
					case WINDOWS_64:
					case WINDOWS_32:
					case MACOS_64:
						return String.format("https://thebombzen.com/TumblGIFifier/resources/%s/%s-%s-version.txt", pkg,
								pkg, local.name());
					default:
						return "";
				}
			case "gifsicle":
				switch (OperatingSystem.getLocalOS()) {
					case WINDOWS_64:
					case WINDOWS_32:
						return String.format("https://thebombzen.com/TumblGIFifier/resources/%s/%s-%s-version.txt", pkg,
								pkg, local.name());
					default:
						return "";
				}
			default:
				return "";
		}
	}

	private static String getExeDownloadLocation(String pkg, String version) {
		String name = getExeDLPkg(pkg, version);
		if (name.isEmpty()) {
			return "";
		} else {
			return "https://thebombzen.com/TumblGIFifier/resources/" + pkg + "/" + name;
		}
	}

	private static String getOpenSansDownloadLocation() {
		return "https://thebombzen.com/TumblGIFifier/resources/OpenSans-Semibold.ttf.xz";
	}

	private static Resource openSans = null;
	private static Resource mpv = null;

	/**
	 * Returns the Open Sans font file resource.
	 * 
	 * @return
	 */
	public static Resource getOpenSansResource() {
		return openSans;
	}

	public static Path getLocalFile(String name) {
		return getLocalResourceLocation().resolve(name);
	}

	private static String getExeDLPkg(String pkg, String version) {
		OperatingSystem local = OperatingSystem.getLocalOS();
		switch (pkg) {
			case "gifsicle":
				switch (local) {
					case WINDOWS_64:
					case WINDOWS_32:
						return String.format("gifsicle-%s-%s.tar.xz", version, local.name());
					default:
						return "";
				}
			case "mpv":
				switch (local) {
					case WINDOWS_64:
					case WINDOWS_32:
					case MACOS_64:
						return String.format("mpv-%s-%s.tar.xz", version, local.name());
					default:
						return "";
				}
			default:
				return "";
		}
	}

	/**
	 * This is a set of optional package names.
	 */
	public static final Set<String> optionalPkgs = new HashSet<>();

	/**
	 * This is a set of required package names.
	 */
	public static final Set<String> requiredPkgs = new HashSet<>();

	/**
	 * This is a set of all loaded package names, populated by the resource
	 * manager.
	 */
	public static final Set<String> loadedPkgs = new HashSet<>();

	static {
		ResourcesManager.requiredPkgs.add("mpv");
		ResourcesManager.optionalPkgs.add("OpenSans");
		ResourcesManager.optionalPkgs.add("gifsicle");
	}

	public static Resource getMpvLocation() {
		if (mpv == null) {
			mpv = getXLocation("mpv", "mpv");
		}
		return mpv;
	}

	public static String getLatestVersion() throws IOException {
		URL latestURL;
		try {
			latestURL = new URL(getLatestDownloadLocation());
		} catch (MalformedURLException ex) {
			throw new IOException("Bad Download Location", ex);
		}
		return IOHelper.downloadFirstLineFromInternet(latestURL);
	}

	public static Path getTemporaryDirectory() throws IOException {
		Path dir = getLocalResourceLocation().resolve("temp");
		if (Files.exists(dir) && !Files.isDirectory(dir)) {
			Files.delete(dir);
		}
		Files.createDirectories(dir);
		return dir;
	}

	public static Resource getXLocation(String pkg, String x) {
		String[] pathElements = System.getenv("PATH").split(File.pathSeparator);
		String name = x + OperatingSystem.getLocalOS().getExeExtension();
		for (String el : pathElements) {
			Path path = Paths.get(el, name);
			if (path.toFile().exists()) {
				return new Resource(pkg, x, path, false);
			}
		}
		return new Resource(pkg, x, getLocalResourceLocation().resolve(name), true);
	}

	private static boolean initializeMultiExePackage(String pkg, String[] resources, String remoteVersionURL,
			String localVersionFilename, boolean mightHaveInternet, StatusProcessor processor)
			throws ResourceNotFoundException {

		boolean needDL = false;

		String localVersion = "";
		URL versions = null;
		Path localVersionsFile = null;
		if (!remoteVersionURL.isEmpty()) {
			versions = IOHelper.wrapSafeURL(remoteVersionURL);
			localVersionsFile = getLocalFile(localVersionFilename);
			try {
				localVersion = IOHelper.getFirstLineOfFile(localVersionsFile);
			} catch (IOException ex) {
				log(ex);
				try {
					if (mightHaveInternet) {
						IOHelper.downloadFromInternet(versions, localVersionsFile);
					}
				} catch (IOException ioe) {
					log(ioe);
					mightHaveInternet = false;
				}
			}
		}

		String remoteVersion = "";

		for (String resourceName : resources) {
			Resource res = getXLocation(pkg, resourceName);
			Path resPath = res.getLocation();
			processor.appendStatus("Checking for " + resourceName + "...");
			if (!res.isInHouse() && Files.exists(resPath) && !Files.isDirectory(resPath)
					&& Files.isExecutable(resPath)) {
				processor.replaceStatus("Checking for " + resourceName + "... found in PATH.");
				continue;
			}
			log("mightHaveInternet: " + mightHaveInternet);
			log("remoteVersionURL: " + remoteVersionURL);
			log("remoteVersion: " + remoteVersion);
			log("localVersion: " + localVersion);
			if (mightHaveInternet && !remoteVersionURL.isEmpty() && remoteVersion.equals("")) {
				try {
					log("Fetching remote version...");
					remoteVersion = IOHelper.downloadFirstLineFromInternet(versions);
					log("Remote version obtained: " + remoteVersion);
				} catch (IOException ioe) {
					log(ioe);
					mightHaveInternet = false;
				}
			}
			if (mightHaveInternet && !remoteVersionURL.isEmpty()
					&& (localVersion.equals("") || !remoteVersion.equals(localVersion))) {
				processor.appendStatus("New version of " + pkg + " found. Will re-download from the internet.");
				needDL = true;
				break;
			}
			if (Files.exists(resPath) && (Files.isDirectory(resPath) || !Files.isExecutable(resPath))) {
				IOHelper.deleteQuietly(resPath);
				processor.appendStatus("Found Bad " + resourceName + ". Deleted.");
				processor.appendStatus(" ");
			}
			if (!Files.exists(resPath)) {
				processor.replaceStatus("Checking for " + resourceName + "... not found.");
				needDL = true;
				break;
			} else {
				processor.replaceStatus("Checking for " + resourceName + "... found.");
				if (EnumSet.of(OperatingSystem.MACOS_64, OperatingSystem.POSIX)
						.contains(OperatingSystem.getLocalOS())) {
					try {
						Files.setPosixFilePermissions(resPath, PosixFilePermissions.fromString("rwxr--r--"));
					} catch (IOException ioe) {
						throw new ResourceNotFoundException(pkg, "Could not set executable on " + pkg);
					}
				}
			}
		}

		if (!needDL) {
			processor.appendStatus(pkg + " found.");
			return mightHaveInternet;
		}

		if (needDL && !mightHaveInternet) {
			throw new ResourceNotFoundException(pkg,
					"Need " + pkg + " dependencies from the internet, but it appears you have no internet access.");
		}

		processor.appendStatus("Downloading " + pkg + " from the internet...");
		String execName = getExeDLPkg(pkg, remoteVersion);
		if (execName.isEmpty()) {
			throw new ResourceNotFoundException(pkg,
					"No prebuilt " + pkg + " binaries for your platform found.\nPlease install "
							+ Arrays.toString(resources).replaceAll("[\\[\\]]", "") + " into your PATH.");
		}
		Path tempFile = getLocalFile(execName);
		URL website = IOHelper.wrapSafeURL(getExeDownloadLocation(pkg, remoteVersion));
		try {
			IOHelper.downloadFromInternet(website, tempFile);
		} catch (IOException ioe) {
			log(ioe);
			throw new ResourceNotFoundException(pkg, "Error downloading " + pkg + ": ", ioe);
		}
		if (!remoteVersionURL.isEmpty()) {
			try {
				IOHelper.downloadFromInternet(versions, localVersionsFile);
			} catch (IOException ioe) {
				log(ioe);
				// we don't actually care, but logging it is nice
			}
		}
		ArchiveInputStream ain = null;
		InputStream cin = null;
		try {
			InputStream bin = new BufferedInputStream(Files.newInputStream(tempFile));
			try {
				cin = new BufferedInputStream(new CompressorStreamFactory(true).createCompressorInputStream(bin));
			} catch (CompressorException ex) {
				cin = bin;
			}
			try {
				ain = new ArchiveStreamFactory("UTF-8").createArchiveInputStream(cin);
			} catch (ArchiveException ex) {
				IOHelper.closeQuietly(cin);
				throw new ResourceNotFoundException(pkg, "Unable to recognize archive format for " + pkg, ex);
			}
			ArchiveEntry entry;
			while (null != (entry = ain.getNextEntry())) {
				String name = entry.getName();
				processor.appendStatus("Extracting " + name + "...");
				Path path = getLocalFile(name);
				if (entry.isDirectory()) {
					Files.createDirectories(path);
				} else {
					Files.copy(ain, path, StandardCopyOption.REPLACE_EXISTING);
				}
				if (EnumSet.of(OperatingSystem.MACOS_64, OperatingSystem.POSIX)
						.contains(OperatingSystem.getLocalOS())) {
					try {
						Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr--r--"));
					} catch (IOException ioe) {
						throw new ResourceNotFoundException(pkg, "Could not set executable on " + pkg);
					}
				}
				processor.replaceStatus("Extracting " + name + "... extracted.");
			}
		} catch (IOException e) {
			throw new ResourceNotFoundException(pkg, "Error downloading " + pkg + ".", e);
		} finally {
			IOHelper.closeQuietly(ain);
			IOHelper.deleteTempFile(tempFile);
		}
		processor.appendStatus("Done downloading.");
		return mightHaveInternet;
	}

	private static boolean initializeSingletonPackage(String pkg, String fullname, String localfilename,
			String dlLocation, boolean mightHaveInternet, StatusProcessor processor) throws ResourceNotFoundException {
		boolean needDL = false;
		Path localfile = getLocalFile(localfilename);
		processor.appendStatus("Checking for " + fullname + " ...");
		if (Files.exists(localfile) && !Files.isRegularFile(localfile)) {
			boolean gone = IOHelper.deleteQuietly(localfile);
			if (!gone) {
				throw new ResourceNotFoundException(pkg,
						"Error: Bad " + fullname + " in Path: " + localfile.toString());
			} else {
				processor.appendStatus("Found Bad " + fullname + " in Path. Deleted.");
			}
		}
		if (!Files.exists(localfile)) {
			processor.replaceStatus("Checking for " + fullname + "... not found.");
			needDL = true;
		} else {
			processor.replaceStatus("Checking for " + fullname + "... found.");
		}

		if (!needDL) {
			processor.appendStatus(fullname + " found.");
			return mightHaveInternet;
		}

		if (needDL && !mightHaveInternet) {
			throw new ResourceNotFoundException(pkg,
					"Need " + pkg + " dependencies from the internet, but it appears you have no internet access.");
		}

		processor.appendStatus("Downloading " + fullname + " from the internet...");
		URL website = IOHelper.wrapSafeURL(dlLocation);
		try {
			IOHelper.downloadFromInternetXZ(website, localfile);
		} catch (IOException ioe) {
			throw new ResourceNotFoundException(pkg, "Error downloading.", ioe);
		}
		processor.appendStatus("Done downloading " + fullname + ".");

		return mightHaveInternet;
	}

	/**
	 * Returns a set of found packages.
	 */
	public static List<String> initializeResources(StatusProcessor processor) {

		List<String> pkgs = new ArrayList<>();

		boolean mightHaveInternet = true;

		try {
			mightHaveInternet = initializeMultiExePackage("mpv", new String[]{"mpv"}, getPkgVersionsLocation("mpv"),
					"mpv-versions.txt", mightHaveInternet, processor);
			pkgs.add("mpv");
		} catch (ResourceNotFoundException rnfe) {
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null) {
				log(rnfe.getCause());
			}
		}

		try {
			mightHaveInternet = initializeSingletonPackage("OpenSans", "Open Sans Semibold", "OpenSans-Semibold.ttf",
					getOpenSansDownloadLocation(), mightHaveInternet, processor);
			pkgs.add("OpenSans");
			openSans = new Resource("OpenSans", "OpenSans-Semibold", getLocalFile("OpenSans-Semibold.ttf"), false);
		} catch (ResourceNotFoundException rnfe) {
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null) {
				log(rnfe.getCause());
			}
		}

		try {
			mightHaveInternet = initializeMultiExePackage("gifsicle", new String[]{"gifsicle"},
					getPkgVersionsLocation("gifsicle"), "gifsicle-versions.txt", mightHaveInternet, processor);
			pkgs.add("gifsicle");
		} catch (ResourceNotFoundException rnfe) {
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null) {
				log(rnfe.getCause());
			}
		}

		return pkgs;
	}

	private ResourcesManager() {

	}

}
