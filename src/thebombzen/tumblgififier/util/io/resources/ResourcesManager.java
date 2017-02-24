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
	
	private static String getFFprogVersionsLocation() {
		return "https://thebombzen.github.io/TumblGIFifier/resources/ffprog-versions.txt";
	}
	
	private static String getFFprogDownloadLocation() {
		return "https://thebombzen.github.io/TumblGIFifier/resources/ffprog/" + getFFprogName();
	}
	
	private static String getOpenSansDownloadLocation() {
		return "https://thebombzen.github.io/TumblGIFifier/resources/ffprog/OpenSans-Semibold.ttf.xz";
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
	
	private static String getFFprogName() {
		String name = System.getProperty("os.name");
		if (name.toLowerCase().contains("windows")) {
			return "ffprog.windows.zip.xz";
		} else if (name.toLowerCase().contains("mac") || name.toLowerCase().contains("osx")
				|| name.toLowerCase().contains("os x")) {
			return "ffprog.osx.zip.xz";
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
	
	private Resource getXLocation(String pkg, String x) {
		String[] pathElements = System.getenv("PATH").split(File.pathSeparator);
		String name = x + TumblGIFifier.EXE_EXTENSION;
		for (String el : pathElements) {
			if (new File(el, name).exists()) {
				return new Resource(pkg, x, new File(el, name).getPath(), true);
			}
		}
		return new Resource(pkg, x, new File(this.getLocalResourceLocation(), name).getAbsolutePath(), false);
	}
	
	private boolean initializeFFmpeg(boolean mightHaveInternet, StatusProcessor processor) {
		String[] names = {"ffmpeg", "ffprobe", "ffplay"};
		boolean needDL = false;
		URL versions = IOHelper.wrapSafeURL(getFFprogVersionsLocation());
		File localVersionsFile = getLocalFile("ffprog-versions.txt");
		String localVersion = "";
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
		
		String remoteVersion = "";
		
		for (String name : names) {
			Resource res = getXLocation("FFmpeg", name);
			File resFile = new File(res.location).getAbsoluteFile();
			processor.appendStatus("Checking for " + name + "...");
			if (res.isInPath && resFile.exists() && !resFile.isDirectory() && resFile.canExecute()) {
				processor.replaceStatus("Checking for " + name + "... found in PATH.");
				continue;
			} else if (mightHaveInternet && remoteVersion.equals("")) {
				try {
					remoteVersion = IOHelper.downloadFirstLineFromInternet(versions);
				} catch (RuntimeIOException ioe) {
					ioe.printStackTrace();
					mightHaveInternet = false;
				}
			}
			if (mightHaveInternet && (localVersion.equals("") || !remoteVersion.equals(localVersion))) {
				processor.appendStatus("New version of FFmpeg found. Will re-download from the internet.");
				needDL = true;
				break;
			}
			if (resFile.exists() && (resFile.isDirectory() || !resFile.canExecute())) {
				boolean did = resFile.delete();
				if (!did) {
					processor.appendStatus("Error: Bad " + name + ": " + resFile.getPath());
					throw new ResourceNotFoundException("FFmpeg");
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
			processor.appendStatus("FFmpeg found.");
			return mightHaveInternet;
		}
		
		if (needDL && !mightHaveInternet) {
			throw new ResourceNotFoundException("FFmpeg", "Need to FFmpeg dependencies from the internet, but it appears you have no internet access.");
		}
		
		processor.appendStatus("Downloading FFmpeg from the internet...");
		String ffProgName = getFFprogName();
		if (ffProgName.isEmpty()){
			processor.appendStatus("No prebuilt FFmpeg binaries for your platform found.");
			processor.appendStatus("Please install ffmpeg, ffprobe, and ffplay into your PATH.");
			throw new ResourceNotFoundException("FFmpeg");
		}
		File tempFile = getLocalFile(ffProgName);
		URL website = IOHelper.wrapSafeURL(getFFprogDownloadLocation());
		try {
			IOHelper.downloadFromInternet(website, tempFile);
		} catch (RuntimeIOException ioe) {
			processor.appendStatus("Error downloading FFmpeg: ");
			processor.processException(ioe);
			throw new ResourceNotFoundException("FFmpeg", ioe);
		}
		try {
			IOHelper.downloadFromInternet(versions, localVersionsFile);
		} catch (RuntimeIOException ioe){
			ioe.printStackTrace();
			// we don't actually care, but logging it is nice
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
			throw new ResourceNotFoundException("FFmpeg", "Error downloading", e);
		} finally {
			IOHelper.closeQuietly(zin);
			IOHelper.deleteTempFile(tempFile);
		}
		processor.appendStatus("Done downloading.");
		return mightHaveInternet;
	}
	
	private boolean initializeOpenSans(boolean mightHaveInternet, StatusProcessor processor){
		boolean needDL = false;
		File fontFile = getLocalFile("OpenSans-Semibold.ttf");
		processor.appendStatus("Checking for Open Sans Semibold ...");
		if (fontFile.exists() && !fontFile.isFile()) {
			boolean did = fontFile.delete();
			if (!did) {
				throw new ResourceNotFoundException("OpenSans", "Error: Bad Open Sans Semibold in Path: " + fontFile.getPath());
			} else {
				processor.appendStatus("Found Bad Open Sans Semibold in Path. Deleted.");
			}
		}
		if (!fontFile.exists()) {
			processor.replaceStatus("Checking for Open Sans Semibold... not found.");
			needDL = true;
		} else {
			processor.replaceStatus("Checking for Open Sans Semibold... found.");
		}
		
		if (!needDL){
			processor.appendStatus("Open Sans Semibold found.");
			return mightHaveInternet;
		}
		
		if (needDL && !mightHaveInternet){
			throw new ResourceNotFoundException("OpenSans", "Need to OpenSans dependencies from the internet, but it appears you have no internet access.");
		}
		
		processor.appendStatus("Downloading Open Sans Semibold from the internet...");
		URL website = IOHelper.wrapSafeURL(getOpenSansDownloadLocation());
		try {
			IOHelper.downloadFromInternet(website, fontFile);
		} catch (RuntimeIOException ioe) {
			throw new ResourceNotFoundException("OpenSans", "Error downloading", ioe);
		}
		processor.appendStatus("Done downloading.");
		
		return mightHaveInternet;
	}
	
	/**
	 * Returns a set of found packages.
	 */
	public List<String> initializeResources(StatusProcessor processor) {
		
		List<String> pkgs = new ArrayList<>();
		
		boolean mightHaveInternet = true;
		try {
			mightHaveInternet = initializeFFmpeg(mightHaveInternet, processor);
			pkgs.add("FFmpeg");
		} catch (ResourceNotFoundException rnfe){
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null){
				rnfe.getCause().printStackTrace();
			}
		}
		
		try {
			mightHaveInternet = initializeOpenSans(mightHaveInternet, processor);
			pkgs.add("OpenSans");
			openSans = new Resource("OpenSans", "OpenSans-Semibold", getLocalFile("OpenSans-Semibold.ttf").getAbsolutePath(), false);
		} catch (ResourceNotFoundException rnfe){
			processor.appendStatus(rnfe.getMessage());
			if (rnfe.getCause() != null){
				rnfe.getCause().printStackTrace();
			}
		}
		
		return pkgs;
		
	}
}
