package thebombzen.tumblgififier;

import java.nio.file.Path;
import java.nio.file.Paths;

@PreLoadable
public enum OperatingSystem {

	WINDOWS_64(),
	WINDOWS_32(),
	MACOS_64(),
	POSIX();

	private static final OperatingSystem LOCAL_OS;

	static {
		WINDOWS_64.exeExtension = ".exe";
		WINDOWS_64.localResourceLocation = Paths.get(System.getenv("appdata"), "tumblgififier");
		WINDOWS_32.exeExtension = ".exe";
		WINDOWS_32.localResourceLocation = Paths.get(System.getenv("appdata"), "tumblgififier");
		MACOS_64.exeExtension = "";
		MACOS_64.localResourceLocation = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "tumblgififier");
		String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
		POSIX.exeExtension = "";
		if (xdgConfigHome == null) {
			POSIX.localResourceLocation = Paths.get(System.getProperty("user.home"), ".config", "tumblgififier"); 
		} else {
			POSIX.localResourceLocation = Paths.get(xdgConfigHome, "tumblgififier");
		}
		String osName = System.getProperty("os.name").toLowerCase().replaceAll("\\s", "");
		if (osName.contains("windows")) {
			String arch = System.getProperty("os.arch");
			if (arch.endsWith("86")) {
				LOCAL_OS = WINDOWS_32;
			} else {
				LOCAL_OS = WINDOWS_64;
			}
		} else if (osName.contains("macos")) {
			LOCAL_OS = MACOS_64;
		} else {
			LOCAL_OS = POSIX;
		}
	}
	
	public static OperatingSystem getLocalOS() {
		return LOCAL_OS;
	}
	
	private String exeExtension = null;
	private Path localResourceLocation = null;

	private OperatingSystem() {

	}

	public String getExeExtension() {
		return exeExtension;
	}

	public Path getLocalResourceLocation() {
		return localResourceLocation;
	}

}
