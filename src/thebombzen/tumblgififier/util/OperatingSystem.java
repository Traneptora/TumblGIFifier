package thebombzen.tumblgififier.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OperatingSystem {

	private static final OperatingSystem WINDOWS_64;
	private static final OperatingSystem WINDOWS_32;
	private static final OperatingSystem MACOS_64;
	private static final OperatingSystem POSIX;
	private static final OperatingSystem LOCAL_OS;
	
	static {
		WINDOWS_64 = new OperatingSystem(".exe", Paths.get(System.getenv("appdata"), "tumblgififier"));
		WINDOWS_32 = new OperatingSystem(".exe", Paths.get(System.getenv("appdata"), "tumblgififier"));
		MACOS_64 = new OperatingSystem("", Paths.get(System.getProperty("user.home"), "Library", "Application Support", "tumblgififier"));
		String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
		Path xdgConfigHomePath;
		if (xdgConfigHome == null) {
			xdgConfigHomePath = Paths.get(System.getProperty("user.home"), ".config", "tumblgififier"); 
		} else {
			xdgConfigHomePath = Paths.get(xdgConfigHome, "tumblgififier");
		}
		POSIX = new OperatingSystem("", xdgConfigHomePath);
		String osName = System.getProperty("os.name").toLowerCase().replaceAll("\\s", "");
		if (osName.contains("windows")) {
			String arch = System.getProperty("os.arch");
			if (arch.endsWith("86")) {
				LOCAL_OS = WINDOWS_32;
			} else {
				LOCAL_OS = WINDOWS_64;
			}
		} else if (osName.contains("macos")) {
			LOCAL_OS=MACOS_64;
		} else {
			LOCAL_OS=POSIX;
		}
	}
	
	public static OperatingSystem getLocalOS() {
		return LOCAL_OS;
	}
	
	private String exeExtension;
	private Path localResourceLocation;

	private OperatingSystem(String exeExtension, Path localResourceLocation) {
		this.exeExtension = exeExtension;
		this.localResourceLocation = localResourceLocation;
	}

	public String getExeExtension() {
		return exeExtension;
	}

	public Path getLocalResourceLocation() {
		return localResourceLocation;
	}

}
