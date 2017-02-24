package thebombzen.tumblgififier.util.io.resources;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.tukaani.xz.XZInputStream;

import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.RuntimeFNFException;
import thebombzen.tumblgififier.util.io.RuntimeIOException;
import thebombzen.tumblgififier.util.text.StatusProcessor;

/**
 * Framework for managing global external resources.
 */
public class ResourcesManager {
	
	private static ResourcesManager manager = new ResourcesManager();
	
	private static String getApplicationDataLocation() {
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
	
	private static String getLegacyApplicationDataLocation(){
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
	public static String getLegacyLocalResourceLocation() {
		return new File(getLegacyApplicationDataLocation(), ".tumblgififier").getAbsolutePath();
	}
	
	/**
	 * Returns the singleton instance of this framework.
	 */
	public static ResourcesManager getResourcesManager() {
		return manager;
	}
	
	private static String getLatestDownloadLocation() {
		return "https://thebombzen.github.io/TumblGIFifier/resources/latest.txt";
	}
	
	private static String getFFmpegVersionsLocation() {
		return "https://thebombzen.github.io/TumblGIFifier/resources/FFmpeg/FFmpeg-versions.txt";
	}
	
	private static String getExeDownloadLocation(String pkg) {
		return "https://thebombzen.github.io/TumblGIFifier/resources/" + pkg + "/" + getExeDLPkg(pkg);
	}
	
	private static String getOpenSansDownloadLocation() {
		return "https://thebombzen.github.io/TumblGIFifier/resources/OpenSans-Semibold.ttf.xz";
	}
	
	
	private Resource ffmpeg = null;
	private Resource ffplay = null;
	private Resource ffprobe = null;
	private Resource openSans = null;
	
	/**
	 * Returns the Open Sans font file resource.
	 * @return
	 */
	public Resource getOpenSansResource() {
		return openSans;
	}
	
	public File getLocalFile(String name){
		return new File(this.getLocalResourceLocation(), name);
	}
	
	private static String getExeDLPkg(String pkg) {
		String name = System.getProperty("os.name");
		if (name.toLowerCase().contains("windows")) {
			return pkg + ".windows.zip.xz";
		} else if (name.toLowerCase().contains("mac") || name.toLowerCase().contains("osx")
				|| name.toLowerCase().contains("os x")) {
			if (pkg.equals("gifsicle")){
				return "";
			}
			return pkg + ".osx.zip.xz";
		} else {
			return "";
		}
	}
	
	private String localAppDataLocation = null;
	
	/**
	 * This is a set of optional package names.
	 */
	public static final Set<String> optionalPkgs = new HashSet<>();
	
	/**
	 * This is a set of required package names.
	 */
	public static final Set<String> requiredPkgs = new HashSet<>();
	
	/**
	 * This is a set of all loaded package names, populated by the resource manager.
	 */
	public static final Set<String> loadedPkgs = new HashSet<>();
	
	static {
		ResourcesManager.requiredPkgs.add("FFmpeg");
		ResourcesManager.optionalPkgs.add("OpenSans");
		ResourcesManager.optionalPkgs.add("gifsicle");
	}
	
	private ResourcesManager() {
		
	}
	
	public Resource getFFmpegLocation() {
		if (ffmpeg == null){
			ffmpeg = getXLocation("FFmpeg", "ffmpeg"); 
		}
		return ffmpeg;
	}
	
	public Resource getFFplayLocation() {
		if (ffplay == null){
			ffplay = getXLocation("FFmpeg", "ffplay");
		}
		return ffplay;
	}
	
	public Resource getFFprobeLocation() {
		if (ffprobe == null){
			ffprobe = getXLocation("FFmpeg", "ffprobe");
		}
		return ffprobe;
	}
	
	public String getLatestVersion() {
		URL latestURL;
		try {
			latestURL = new URL(getLatestDownloadLocation());
		} catch (MalformedURLException ex) {
			throw new Error(ex);
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(latestURL.openStream(), Charset.forName("UTF-8")))) {
			return reader.readLine();
		} catch (IOException ioe){
			throw new RuntimeIOException(ioe);
		}
	}
	
	public String getLocalResourceLocation() {
		if (localAppDataLocation != null) {
			return localAppDataLocation;
		}
		try {
			String appData = getApplicationDataLocation();
			File localAppDataFile = new File(appData, "tumblgififier").getCanonicalFile();
			if (localAppDataFile.exists() && !localAppDataFile.isDirectory()) {
				localAppDataFile.delete();
			}
			localAppDataFile.mkdirs();
			localAppDataLocation = localAppDataFile.getCanonicalPath();
			return localAppDataLocation;
		} catch (IOException ioe) {
			throw new RuntimeIOException(ioe);
		}
	}
	
	public Resource getXLocation(String pkg, String x) {
		String[] pathElements = System.getenv("PATH").split(File.pathSeparator);
		String name = x + TumblGIFifier.EXE_EXTENSION;
		for (String el : pathElements) {
			if (new File(el, name).exists()) {
				return new Resource(pkg, x, new File(el, name).getPath(), true);
			}
		}
		return new Resource(pkg, x, new File(this.getLocalResourceLocation(), name).getAbsolutePath(), false);
	}
	
	
	private boolean initializeMultiExePackage(String pkg, String[] resources, String remoteVersionURL, String localVersionFilename, boolean mightHaveInternet, StatusProcessor processor){
		
		boolean needDL = false;
		
		String localVersion = "";
		URL versions = null;
		File localVersionsFile = null;
		if (!remoteVersionURL.isEmpty()){
			versions = IOHelper.wrapSafeURL(remoteVersionURL);
			localVersionsFile = getLocalFile(localVersionFilename);
			try {
				localVersion = IOHelper.getFirstLineOfFile(localVersionsFile);
			} catch (RuntimeFNFException fnfe) {
				try {
					if (mightHaveInternet){
						IOHelper.downloadFromInternet(versions, localVersionsFile);
					}
				} catch (RuntimeIOException ioe) {
					ioe.printStackTrace();
					mightHaveInternet = false;
				}
			}
		}
		
		String remoteVersion = "";
		
		for (String name : resources){
			Resource res = getXLocation(pkg, name);
			File resFile = new File(res.location).getAbsoluteFile();
			processor.appendStatus("Checking for " + name + "...");
			if (res.isInPath && resFile.exists() && !resFile.isDirectory() && resFile.canExecute()) {
				processor.replaceStatus("Checking for " + name + "... found in PATH.");
				continue;
			} else if (mightHaveInternet && !remoteVersionURL.isEmpty() && remoteVersion.equals("")) {
				try {
					remoteVersion = IOHelper.downloadFirstLineFromInternet(versions);
				} catch (RuntimeIOException ioe) {
					ioe.printStackTrace();
					mightHaveInternet = false;
				}
			}
			if (mightHaveInternet && !remoteVersionURL.isEmpty() && (localVersion.equals("") || !remoteVersion.equals(localVersion))) {
				processor.appendStatus("New version of "+pkg+" found. Will re-download from the internet.");
				needDL = true;
				break;
			}
			if (resFile.exists() && (resFile.isDirectory() || !resFile.canExecute())) {
				boolean did = resFile.delete();
				if (!did) {
					throw new ResourceNotFoundException(pkg, "Error: Bad " + name + ": " + resFile.getPath());
				} else {
					processor.appendStatus("Found Bad " + name + ". Deleted.");
					processor.appendStatus(" ");
				}
			}
			if (!resFile.exists()) {
				processor.replaceStatus("Checking for " + name + "... not found.");
				needDL = true;
				break;
			} else {
				processor.replaceStatus("Checking for " + name + "... found.");
				resFile.setExecutable(true);
			}
		}
		
		if (!needDL){
			processor.appendStatus(pkg+" found.");
			return mightHaveInternet;
		}
		
		if (needDL && !mightHaveInternet) {
			throw new ResourceNotFoundException(pkg, "Need "+pkg+" dependencies from the internet, but it appears you have no internet access.");
		}
		
		processor.appendStatus("Downloading "+pkg+" from the internet...");
		String execName = getExeDLPkg(pkg);
		if (execName.isEmpty()){
			throw new ResourceNotFoundException(pkg, "No prebuilt "+pkg+" binaries for your platform found.\nPlease install "+Arrays.toString(resources).replaceAll("[\\[\\]]", "")+" into your PATH.");
		}
		File tempFile = getLocalFile(execName);
		URL website = IOHelper.wrapSafeURL(getExeDownloadLocation(pkg));
		try {
			IOHelper.downloadFromInternet(website, tempFile);
		} catch (RuntimeIOException ioe) {
			ioe.printStackTrace();
			throw new ResourceNotFoundException(pkg, "Error downloading "+pkg+": ", ioe);
		}
		if (!remoteVersionURL.isEmpty()){
			try {
				IOHelper.downloadFromInternet(versions, localVersionsFile);
			} catch (RuntimeIOException ioe){
				ioe.printStackTrace();
				// we don't actually care, but logging it is nice
			}
		}
		ZipInputStream zin = null;
		try {
			zin = new ZipInputStream(new XZInputStream(new BufferedInputStream(new FileInputStream(tempFile))));
			ZipEntry entry;
			while (null != (entry = zin.getNextEntry())) {
				String name = entry.getName();
				processor.appendStatus("Extracting " + name + "...");
				File path = getLocalFile(name);
				if (path.exists()) {
					path.delete();
				}
				Files.copy(zin, path.toPath());
				path.setExecutable(true);
				processor.replaceStatus("Extracting " + name + "... extracted.");
			}
		} catch (IOException|RuntimeIOException e) {
			throw new ResourceNotFoundException(pkg, "Error downloading", e);
		} finally {
			IOHelper.closeQuietly(zin);
			IOHelper.deleteTempFile(tempFile);
		}
		processor.appendStatus("Done downloading.");
		return mightHaveInternet;
		
	}
	
	private boolean initializeSingletonPackage(String pkg, String fullname, String localfilename, String dlLocation, boolean mightHaveInternet, StatusProcessor processor){
		boolean needDL = false;
		File localfile = getLocalFile(localfilename);
		processor.appendStatus("Checking for " + fullname + " ...");
		if (localfile.exists() && !localfile.isFile()) {
			boolean did = localfile.delete();
			if (!did) {
				throw new ResourceNotFoundException(pkg, "Error: Bad "+fullname+" in Path: " + localfile.getPath());
			} else {
				processor.appendStatus("Found Bad "+fullname+" in Path. Deleted.");
			}
		}
		if (!localfile.exists()) {
			processor.replaceStatus("Checking for "+fullname+"... not found.");
			needDL = true;
		} else {
			processor.replaceStatus("Checking for "+fullname+"... found.");
		}
		
		if (!needDL){
			processor.appendStatus(fullname+" found.");
			return mightHaveInternet;
		}
		
		if (needDL && !mightHaveInternet){
			throw new ResourceNotFoundException(pkg, "Need " + pkg + " dependencies from the internet, but it appears you have no internet access.");
		}
		
		processor.appendStatus("Downloading "+fullname+" from the internet...");
		URL website = IOHelper.wrapSafeURL(dlLocation);
		try {
			IOHelper.downloadFromInternetXZ(website, localfile);
		} catch (RuntimeIOException ioe) {
			throw new ResourceNotFoundException(pkg, "Error downloading.", ioe);
		}
		processor.appendStatus("Done downloading " + fullname + ".");
		
		return mightHaveInternet;
	}
	
	/**
	 * Returns a set of found packages.
	 */
	public List<String> initializeResources(StatusProcessor processor) {
		
		List<String> pkgs = new ArrayList<>();
		
		boolean mightHaveInternet = true;
		try {
			mightHaveInternet = initializeMultiExePackage("FFmpeg", new String[]{"ffmpeg", "ffprobe", "ffplay"}, getFFmpegVersionsLocation(), "FFmpeg-versions.txt", mightHaveInternet, processor);
			pkgs.add("FFmpeg");
		} catch (ResourceNotFoundException rnfe){
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null){
				rnfe.getCause().printStackTrace();
			}
		}
		
		try {
			mightHaveInternet = initializeSingletonPackage("OpenSans", "Open Sans Semibold", "OpenSans-Semibold.ttf", getOpenSansDownloadLocation(), mightHaveInternet, processor);
			pkgs.add("OpenSans");
			openSans = new Resource("OpenSans", "OpenSans-Semibold", getLocalFile("OpenSans-Semibold.ttf").getAbsolutePath(), false);
		} catch (ResourceNotFoundException rnfe){
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null){
				rnfe.getCause().printStackTrace();
			}
		}
		
		try {
			mightHaveInternet = initializeMultiExePackage("gifsicle", new String[]{"gifsicle"}, "", "", mightHaveInternet, processor);
			pkgs.add("gifsicle");
		} catch (ResourceNotFoundException rnfe){
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null){
				rnfe.getCause().printStackTrace();
			}
		}
		
		return pkgs;
		
	}
}
