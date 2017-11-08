package thebombzen.tumblgififier.util.text;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;

public final class TextHelper {

	private static TextHelper instance = null;

	public static TextHelper getTextHelper() {
		if (instance == null) {
			instance = new TextHelper();
		}
		return instance;
	}

	/**
	 * We dump the overlay text to a file so we don't have to escape it.
	 */
	private Path tempOverlayPath;

	/**
	 * This is the escaped filename of the tempOverlayFile.
	 */
	private String tempOverlayEscapedFilename;
	private String fontFile = null;

	/**
	 * This is the escaped filename of the Open Sans font file location. Using a
	 * getter allows it to be lazily populated.
	 */
	private String getFontFile() {
		if (fontFile == null) {
			fontFile = escapeForVideoFilter(
					ResourcesManager.getResourcesManager().getOpenSansResource().getLocation().toString());
		}
		return fontFile;
	}

	private TextHelper() {
		tempOverlayPath = IOHelper.createTempFile();
		tempOverlayEscapedFilename = escapeForVideoFilter(tempOverlayPath.toString());
	}

	/**
	 * Escapes a string to be used in an FFmpeg video filter. Replaces
	 * backslash, comma, semicolon, colon, single quote, brackets, and equal
	 * signs with escaped versions.
	 * 
	 * @param input
	 *            This is the input string to escape
	 * @return An escaped string.
	 */
	public String escapeForVideoFilter(String input) {
		// return input.replace("\\", "\\\\").replace(",", "\\,").replace(";",
		// "\\;").replace(":", "\\:")
		// .replace("'", "\\'").replace("[", "\\[").replace("]",
		// "\\]").replace("=", "\\=");
		return "'" + input.replace("'", "'\\''") + "'";
	}

	/**
	 * This creates the drawtext filter to be used with the text overlay
	 * feature. Just drop right after -vf. Note that you shouldn't use this more
	 * than once at a time, because it dumps the message to a file and points
	 * the filter to that file.
	 * 
	 * @param width
	 *            The video width
	 * @param height
	 *            The video height
	 * @param fontSize
	 *            The font point size of the message we want to render
	 * @param message
	 *            The text of the message we want to render
	 * @return A drop-in drawtext filter, to be placed right after the -vf
	 *         argument.
	 */
	public String createDrawTextString(int width, int height, int fontSize, String message) {
		int size = (int) Math.ceil(fontSize * height / 1080D);
		int borderw = (int) Math.ceil(size * 7D / fontSize);
		try (Writer writer = Files.newBufferedWriter(tempOverlayPath)) {
			writer.write(message);
		} catch (IOException ex) {
			log(ex);
			return "";
		}
		String drawText = "drawtext=x=(w-tw)*0.5:y=0.935*(h-0.5*" + size
				+ "):bordercolor=black:fontcolor=white:borderw=" + borderw + ":fontfile=" + getFontFile() + ":fontsize="
				+ size + ":textfile=" + tempOverlayEscapedFilename;
		return drawText;
	}

	public static double scanTotalTimeConverted(InputStream in) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			return br.lines().filter(s -> s.startsWith("frame=")).mapToDouble(TextHelper::getFFmpegStatusTimeInSeconds)
					.max().orElse(-1D);
		} catch (IOException ioe) {
			log(ioe);
			return -1D;
		}
	}

	public static double getFFmpegStatusTimeInSeconds(String line) {
		if (!line.startsWith("frame=")) {
			throw new IllegalArgumentException("Must be an FFmpeg status Line!");
		}
		String time = "0";
		try (Scanner sc2 = new Scanner(line)) {
			sc2.useDelimiter("\\s");
			while (sc2.hasNext()) {
				String part = sc2.next();
				if (part.startsWith("time=")) {
					time = part.replaceAll("time=", "");
					break;
				}
			}
			String[] times = time.split(":");
			double realTime = 0D;
			for (int i = 0; i < times.length; i++) {
				try {
					realTime += Math.pow(60, i) * Double.parseDouble(times[times.length - i - 1]);
				} catch (NumberFormatException nfe) {
					log(nfe);
					return -1D;
				}
			}
			return realTime;
		}
	}

	/**
	 * Tests for a string's "validity." A string is "valid" provided that is is
	 * not null and is not empty.
	 * 
	 * @param The
	 *            string to test for validity.
	 * @return true if the string is non-null and non-empty, false otherwise.
	 */
	public static boolean validateString(String string) {
		return !sanitizeString(string).isEmpty();
	}

	/**
	 * Returns real strings unchanged. Replaces null with empty the empty
	 * string.
	 */
	public static String sanitizeString(String string) {
		return string != null ? string : "";
	}

	/**
	 * Replaces negative values with default ones. The first default value is
	 * for -1, the second is for -2, and so on.
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static int sanitizeInt(int value, int... defaultValues) {
		if (value < 0) {
			if (-value > defaultValues.length) {
				if (defaultValues.length > 0) {
					return defaultValues[0];
				} else {
					return 0;
				}
			} else {
				return defaultValues[-value - 1];
			}
		} else {
			return value;
		}
	}

	public static int sanitizeInteger(Integer value, int... defaultValues) {
		if (value == null) {
			return sanitizeInt(-1, defaultValues);
		} else {
			return sanitizeInt(value.intValue(), defaultValues);
		}
	}

	public static double sanitizeDouble(double value, double defaultValue) {
		if (Double.isFinite(value) && value >= 0D) {
			return value;
		} else {
			return defaultValue;
		}
	}

	public static String getTimeDurationFromSeconds(double seconds) {
		int hours = (int) (seconds / 3600D);
		seconds -= hours * 3600D;
		int minutes = (int) (seconds / 60D);
		seconds -= minutes * 60D;
		if (hours > 0) {
			return String.format("%02d:%02d:%06.3f", hours, minutes, seconds);
		} else if (minutes > 0) {
			return String.format("%02d:%06.3f", minutes, seconds);
		} else {
			return String.format("%06.3f", seconds);
		}
	}

	public String createVideoFilter(String preprocess, String postprocess, int width, int height, boolean boxedScale,
			int decimator, int originalWidth, int originalHeight, int overlaySize, String overlayText) {
		log("Creating video filter.");
		List<String> filters = new ArrayList<String>();
		filters.add("copy");
		if (validateString(preprocess)) {
			log("Adding preprocess filter: " + preprocess);
			filters.add(preprocess);
		}
		if (decimator > 0) {
			String framestep = "select=not(mod(n\\," + (1 + decimator) + "))";
			log("Adding framestep: " + framestep);
			filters.add(framestep);
		}
		if (validateString(overlayText)) {
			if (ResourcesManager.loadedPkgs.contains("OpenSans")) {
				String drawTextString = createDrawTextString(originalWidth, originalHeight, overlaySize, overlayText);
				log("Adding Draw Text String: " + drawTextString);
				filters.add(drawTextString);
			} else {
				log("No Open Sans, cannot overlay text: " + overlayText);
			}
		}
		if (width > 0 && width != originalWidth || height > 0 && height != originalHeight) {
			log(String.format("Adding Scaler. w: %s, h: %s, origw: %s, origh: %s", width, height, originalWidth,
					originalHeight));
			String scaler = "scale=w=" + width + ":h=" + height
					+ (boxedScale ? ":force_original_aspect_ratio=decrease" : "");
			log("Scaler filter: " + scaler);
			filters.add(scaler);
		}
		if (validateString(postprocess)) {
			log("Adding postprocess filter: " + preprocess);
			filters.add(postprocess);
		}
		String ret = String.join(",", filters);
		log("Filter created: " + ret);
		return ret;
	}

}
