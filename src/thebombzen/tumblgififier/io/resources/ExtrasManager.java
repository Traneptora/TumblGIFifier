package thebombzen.tumblgififier.io.resources;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.tukaani.xz.XZInputStream;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.text.StatusProcessor;
import thebombzen.tumblgififier.text.StatusProcessorWriter;

public class ExtrasManager {
	
	private static ExtrasManager manager = new ExtrasManager();
	
	private static String getApplicationDataLocation() {
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
	
	public static ExtrasManager getExtrasManager() {
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
	
	public String getOpenSansFontFileLocation() {
		return new File(getLocalAppDataLocation(), "OpenSans-Semibold.ttf").getAbsolutePath();
	}
	
	private static String getFFprogName() {
		String name = System.getProperty("os.name");
		if (name.toLowerCase().contains("windows")) {
			return "ffprog.windows.zip.xz";
		} else if (name.toLowerCase().contains("mac") || name.toLowerCase().contains("osx")
				|| name.toLowerCase().contains("os x")) {
			return "ffprog.osx.zip.xz";
		} else {
			return "ffprog.linux.zip.xz";
		}
	}
	
	private String localAppDataLocation = null;
	
	private ExtrasManager() {
		
	}
	
	public File getLocalResource(String name){
		return new File(this.getLocalAppDataLocation(), name);
	}
	
	public ResourceLocation getFFmpegLocation() {
		return getXLocation("ffmpeg");
	}
	
	public ResourceLocation getFFplayLocation() {
		return getXLocation("ffplay");
	}
	
	public ResourceLocation getFFprobeLocation() {
		return getXLocation("ffprobe");
	}
	
	public String getLatestVersion() throws IOException {
		URL latestURL;
		try {
			latestURL = new URL(getLatestDownloadLocation());
		} catch (MalformedURLException ex) {
			throw new Error(ex);
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(latestURL.openStream(), Charset.forName("UTF-8")))) {
			return reader.readLine();
		}
	}
	
	public String getLocalAppDataLocation() {
		if (localAppDataLocation != null) {
			return localAppDataLocation;
		}
		try {
			String appData = getApplicationDataLocation();
			File localAppDataFile = new File(appData, ".tumblgififier").getCanonicalFile();
			if (localAppDataFile.exists() && !localAppDataFile.isDirectory()) {
				localAppDataFile.delete();
			}
			localAppDataFile.mkdirs();
			localAppDataLocation = localAppDataFile.getCanonicalPath();
			return localAppDataLocation;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	private ResourceLocation getXLocation(String x) {
		String[] pathElements = System.getenv("PATH").split(File.pathSeparator);
		String name = x + TumblGIFifier.EXE_EXTENSION;
		for (String el : pathElements) {
			if (new File(el, name).exists()) {
				return new ResourceLocation(new File(el, name).getPath(), true);
			}
		}
		return new ResourceLocation(new File(getLocalAppDataLocation(), name).getPath(), false);
	}
	
	public boolean intitilizeExtras(StatusProcessor processor) {
		boolean needDL = false;
		boolean noInternet = false;
		String[] names = {"ffmpeg", "ffprobe", "ffplay"};
		
		URL versions = IOHelper.wrapSafeURL(getFFprogVersionsLocation());
		
		File localVersionsFile = new File(getLocalAppDataLocation(), "ffprog-versions.txt");
		String localVersion = "";
		try {
			localVersion = IOHelper.getFirstLineOfFileQuietly(localVersionsFile);
		} catch (FileNotFoundException fnfe) {
			try {
				IOHelper.downloadFromInternet(versions, localVersionsFile);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				noInternet = true;
			}
		}
		
		String version = "";
		
		for (String name : names) {
			ResourceLocation loc = getXLocation(name);
			File f = new File(loc.toString()).getAbsoluteFile();
			processor.appendStatus("Checking for " + name + "...");
			if (loc.isFromPath() && f.exists() && f.canExecute()) {
				processor.replaceStatus("Checking for " + name + "... found in PATH.");
				continue;
			} else if (!noInternet && version.equals("")) {
				try {
					version = IOHelper.downloadFirstLineFromInternet(versions);
				} catch (IOException ioe) {
					ioe.printStackTrace();
					noInternet = true;
				}
			}
			if (!noInternet && (localVersion.equals("") || !version.equals(localVersion))) {
				processor.appendStatus("New version of FFmpeg found. Will re-download from the internet.");
				needDL = true;
				break;
			}
			if (f.exists() && (!f.isFile() || !f.canExecute())) {
				boolean did = f.delete();
				if (!did) {
					processor.appendStatus("Error: Bad " + name + ": " + f.getPath());
					return false;
				} else {
					processor.appendStatus("Found Bad " + name + ". Deleted.");
					processor.appendStatus(" ");
				}
			}
			if (!f.exists()) {
				processor.replaceStatus("Checking for " + name + "... not found.");
				needDL = true;
				break;
			} else {
				processor.replaceStatus("Checking for " + name + "... found.");
				f.setExecutable(true);
			}
		}
		boolean needOpenSansDL = false;
		File f = new File(getOpenSansFontFileLocation());
		processor.appendStatus("Checking for Open Sans Semibold ...");
		if (f.exists() && !f.isFile()) {
			boolean did = f.delete();
			if (!did) {
				processor.appendStatus("Error: Bad Open Sans Semibold in Path: " + f.getPath());
				return false;
			} else {
				processor.appendStatus("Found Bad Open Sans Semibold in Path. Deleted.");
			}
		}
		if (!f.exists()) {
			processor.replaceStatus("Checking for Open Sans Semibold... not found.");
			needOpenSansDL = true;
		} else {
			processor.replaceStatus("Checking for Open Sans Semibold... found.");
		}
		if ((needDL || needOpenSansDL) && noInternet) {
			processor.appendStatus(
					"Need to download dependencies from the internet, but it appears you have no internet access.");
			return false;
		}
		if (needDL) {
			processor.appendStatus("Downloading FFmpeg from the internet...");
			File tempFile = new File(getLocalAppDataLocation(), getFFprogName());
			URL website = IOHelper.wrapSafeURL(getFFprogDownloadLocation());
			try {
				IOHelper.downloadFromInternet(website, tempFile);
			} catch (IOException ioe) {
				processor.appendStatus("Error downloading: ");
				PrintWriter writer = new PrintWriter(new StatusProcessorWriter(processor));
				ioe.printStackTrace(writer);
				return false;
			}
			IOHelper.downloadFromInternetQuietly(versions, localVersionsFile);
			ZipInputStream zin = null;
			try {
				zin = new ZipInputStream(new XZInputStream(new BufferedInputStream(new FileInputStream(tempFile))));
				ZipEntry entry;
				while (null != (entry = zin.getNextEntry())) {
					String name = entry.getName();
					processor.appendStatus("Extracting " + name + "...");
					File path = new File(getLocalAppDataLocation(), name);
					if (path.exists()) {
						path.delete();
					}
					Files.copy(zin, path.toPath());
					path.setExecutable(true);
					processor.replaceStatus("Extracting " + name + "... extracted.");
				}
			} catch (IOException ioe) {
				processor.appendStatus("Error downloading: ");
				PrintWriter writer = new PrintWriter(new StatusProcessorWriter(processor));
				ioe.printStackTrace(writer);
				return false;
			} finally {
				IOHelper.closeQuietly(zin);
				IOHelper.deleteTempFile(tempFile);
			}
			processor.appendStatus("Done downloading.");
		} else {
			processor.appendStatus("FFmpeg found.");
		}
		if (needOpenSansDL) {
			processor.appendStatus("Downloading Open Sans Semibold from the internet...");
			File openSansFile = new File(getOpenSansFontFileLocation());
			URL website = IOHelper.wrapSafeURL(getOpenSansDownloadLocation());
			try {
				IOHelper.downloadFromInternet(website, openSansFile);
			} catch (IOException ioe) {
				processor.appendStatus("Error downloading: ");
				PrintWriter writer = new PrintWriter(new StatusProcessorWriter(processor));
				ioe.printStackTrace(writer);
				return false;
			}
			processor.appendStatus("Done downloading.");
		} else {
			processor.appendStatus("Open Sans Semibold found.");
		}
		processor.appendStatus("Initialization successful. Now, open a video file with File -> Open.");
		return true;
	}
}
