package thebombzen.tumblgififier.util;

public interface Constants {
	public static final boolean IS_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	public static final String EXE_EXTENSION = IS_ON_WINDOWS ? ".exe" : "";
}
