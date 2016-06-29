package thebombzen.tumblgififier.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import thebombzen.tumblgififier.ConcurrenceManager;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.resources.ExtrasManager;

public final class TextHelper {
	
	private static final TextHelper instance = new TextHelper();
	
	public static TextHelper getTextHelper(){
		return instance;
	}
	
	/**
	 * We dump the overlay text to a file so we don't have to escape it.
	 */
	private File tempOverlayFile;
	/**
	 * This is the escaped filename of the tempOverlayFile.
	 */
	private String tempOverlayFilename;
	/**
	 * This is the escaped filename of the Open Sans font file location.
	 */
	private String fontFile = escapeForVideoFilter(ExtrasManager.getExtrasManager().getOpenSansFontFileLocation());
	
	
	private TextHelper(){
		try {
			tempOverlayFile = IOHelper.createTempFile().getAbsoluteFile();
			ConcurrenceManager.getConcurrenceManager().addShutdownTask(new Runnable(){
				public void run(){
					IOHelper.deleteTempFile(tempOverlayFile);
				}
			});
			tempOverlayFilename = escapeForVideoFilter(tempOverlayFile.getAbsolutePath());
		} catch (IOException ioe) {
			throw new Error("Cannot create temporary files.");
		}
		
	}

	/**
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param list
	 *            The array of strings to conjoin.
	 * @return The conjoined string.
	 */
	public String join(String conjunction, String[] list) {
		return join(conjunction, Arrays.asList(list));
	}

	/**
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param iterator
	 *            An iterator of the strings to conjoin.
	 * @return The conjoined string.
	 */
	public String join(String conjunction, Iterator<String> iterator) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (iterator.hasNext()) {
			String item = iterator.next();
			if (first) {
				first = false;
			} else {
				sb.append(conjunction);
			}
			sb.append(item);
		}
		return sb.toString();
	}

	/**
	 * Utility method to join an array of Strings based on a delimiter.
	 * Seriously, why did it take until Java 8 to add this thing to the standard
	 * library? >_>
	 * 
	 * @param conjunction
	 *            The delimiter with which to conjoin the strings.
	 * @param list
	 *            The collection of strings to conjoin.
	 * @return The conjoined string.
	 */
	public String join(String conjunction, Iterable<String> list) {
		return join(conjunction, list.iterator());
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
		return input.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace(":", "\\:")
				.replace("'", "\\'").replace("[", "\\[").replace("]", "\\]").replace("=", "\\=");
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
		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(tempOverlayFile), Charset.forName("UTF-8")))) {
			writer.write(message);
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
		String drawText = "drawtext=x=(w-tw)*0.5:y=0.935*(h-0.5*" + size
				+ "):bordercolor=black:fontcolor=white:borderw=" + borderw + ":fontfile=" + fontFile + ":fontsize="
				+ size + ":textfile=" + tempOverlayFilename;
		return drawText;
	}
}
