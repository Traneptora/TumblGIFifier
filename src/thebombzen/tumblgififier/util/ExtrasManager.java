package thebombzen.tumblgififier.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.tukaani.xz.XZInputStream;
import thebombzen.tumblgififier.processor.StatusProcessor;

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
	
	private static String getFFprogDownloadLocation() {
		return "https://dl.dropboxusercontent.com/u/51080973/ffprog/" + getFFprogName();
	}
	
	private static String getOpenSansDownloadLocation() {
		return "https://dl.dropboxusercontent.com/u/51080973/ffprog/OpenSans-Semibold.ttf.xz";
	}
	
	public String getOpenSansFontFileLocation(){
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
	
	public String getFFmpegLocation() {
		return getXLocation("ffmpeg");
	}
	
	public String getFFplayLocation() {
		return getXLocation("ffplay");
	}
	
	public String getFFprobeLocation() {
		return getXLocation("ffprobe");
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
	
	private String getXLocation(String x) {
		String[] pathElements = System.getenv("PATH").split(File.pathSeparator);
		String name = x + Helper.EXE_EXTENSION;
		for (String el : pathElements) {
			if (new File(el, name).exists()) {
				return new File(el, name).getPath();
			}
		}
		return new File(getLocalAppDataLocation(), name).getPath();
	}
	
	public boolean intitilizeExtras(StatusProcessor processor) {
		try {
			boolean needDL = false;
			String[] names = {"ffmpeg", "ffprobe", "ffplay"};
			for (String name : names) {
				File f = new File(getXLocation(name)).getCanonicalFile();
				processor.appendStatus("Checking for " + name + "...");
				if (f.exists() && !f.isFile()) {
					boolean did = f.delete();
					if (!did) {
						processor.appendStatus("Error: Bad " + name + " in Path: " + f.getPath());
						return false;
					} else {
						processor.appendStatus("Found Bad " + name + " in Path. Deleted.");
					}
				}
				if (!f.exists()) {
					processor.replaceStatus("Checking for " + name + "... not found.");
					needDL = true;
				} else {
					processor.replaceStatus("Checking for " + name + "... found.");
					f.setExecutable(true);
				}
			}
			boolean needProfileDL = false;
			File f = new File(getOpenSansFontFileLocation());
			processor.appendStatus("Checking for Profile Medium ...");
			if (f.exists() && !f.isFile()) {
				boolean did = f.delete();
				if (!did) {
					processor.appendStatus("Error: Bad Profile Medium  in Path: " + f.getPath());
					return false;
				} else {
					processor.appendStatus("Found Bad Profile Medium in Path. Deleted.");
				}
			}
			if (!f.exists()) {
				processor.replaceStatus("Checking for Profile Medium... not found.");
				needProfileDL = true;
			} else {
				processor.replaceStatus("Checking for Profile Medium... found.");
			}
			if (needDL) {
				processor.appendStatus("Downloading FFmpeg from the internet...");
				File tempFile = new File(getLocalAppDataLocation(), getFFprogName());
				FileOutputStream fos = new FileOutputStream(tempFile);
				URL website = new URL(getFFprogDownloadLocation());
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				rbc.close();
				ZipInputStream zin = new ZipInputStream(new XZInputStream(new BufferedInputStream(new FileInputStream(
						tempFile))));
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
				zin.close();
				tempFile.delete();
				processor.appendStatus("Done downloading.");
			} else {
				processor.appendStatus("FFmpeg found.");
			}
			if (needProfileDL) {
				processor.appendStatus("Downloading Profile Medium from the internet...");
				File profileFile = new File(getOpenSansFontFileLocation());
				FileOutputStream fos = new FileOutputStream(profileFile);
				URL website = new URL(getOpenSansDownloadLocation());
				ReadableByteChannel rbc = Channels.newChannel(new XZInputStream(website.openStream()));
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				rbc.close();
				processor.appendStatus("Done downloading.");
			} else {
				processor.appendStatus("Profile Medium found.");
			}
			return true;
		} catch (IOException ioe) {
			// this should not occur
			ioe.printStackTrace();
			return false;
		}
	}
	
}
