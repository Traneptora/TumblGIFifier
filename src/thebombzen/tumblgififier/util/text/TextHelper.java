package thebombzen.tumblgififier.util.text;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;

public final class TextHelper {
	
	private static TextHelper instance = null;
	
	public static TextHelper getTextHelper(){
		if (instance == null){
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
	 * This is the escaped filename of the Open Sans font file location.
	 * Using a getter allows it to be lazily populated.
	 */
	private String getFontFile(){
		if (fontFile == null){
			fontFile = escapeForVideoFilter(ResourcesManager.getResourcesManager().getOpenSansResource().getLocation().toString());
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
		//return input.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace(":", "\\:")
		//		.replace("'", "\\'").replace("[", "\\[").replace("]", "\\]").replace("=", "\\=");
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
			ex.printStackTrace();
			return "";
		}
		String drawText = "drawtext=x=(w-tw)*0.5:y=0.935*(h-0.5*" + size
				+ "):bordercolor=black:fontcolor=white:borderw=" + borderw + ":fontfile=" + getFontFile() + ":fontsize="
				+ size + ":textfile=" + tempOverlayEscapedFilename;
		return drawText;
	}
	
	public String createVideoFilter(String preprocess, String postprocess, int width, int height, boolean boxedScale, int decimator, int originalWidth, int originalHeight, int overlaySize, String overlayText){
		List<String> filters = new ArrayList<String>();
		if (preprocess != null && !preprocess.isEmpty()){
			filters.add(preprocess);
		}
		if (decimator > 0){
			filters.add("framestep=" + (1 + decimator));
		}
		if (!overlayText.isEmpty() && ResourcesManager.loadedPkgs.contains("OpenSans")){
			filters.add(createDrawTextString(originalWidth, originalHeight, overlaySize, overlayText));
		}
		filters.add("scale=w=" + width + ":h=" + height + (boxedScale ? ":force_original_aspect_ratio=decrease" : ""));
		if (postprocess != null && !postprocess.isEmpty()){
			filters.add(postprocess);
		}
		return String.join(",", filters);
	}
	
}
