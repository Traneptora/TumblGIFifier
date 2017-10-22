package thebombzen.tumblgififier.video;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.RuntimeIOException;
import thebombzen.tumblgififier.util.io.resources.ProcessTerminatedException;
import thebombzen.tumblgififier.util.io.resources.Resource;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;
import thebombzen.tumblgififier.util.text.StatusProcessorWriter;
import thebombzen.tumblgififier.util.text.TextHelper;

public class VideoProcessor {
	
	public VideoProcessor(VideoScan scan){
		this.scan = scan;
	}
	
	private VideoScan scan;
	
	
	public VideoScan getScan() {
		return scan;
	}

	
	public void setScan(VideoScan scan) {
		this.scan = scan;
	}

	private Path gifFile;
	private Path nutFile;
	private Path paletteFile;
	
	private boolean halveFramerate;
	
	private double highscale = 1D;
	private double lowscale = 0D;
	private long maxSize;
	private long minSize;
	private double scale = 1D;	

	private double clipStartTime;
	private double clipEndTime;
	
	private StatusProcessor statusProcessor;
	
	private int prevWidth = -1;
	private int prevPrevWidth = -2;
	private int prevHeight = -1;
	private int prevPrevHeight = -2;
	
	private void adjustScale() {
		StringBuilder sb = new StringBuilder();
		sb.append("Checking Filesize... ");
		long currFileSize;
		try {
			currFileSize = Files.size(gifFile);
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		}
		if (currFileSize > maxSize) {
			sb.append("Too Big: ");
			highscale = scale;
			scale = (lowscale + highscale) * 0.5D;
		} else if (currFileSize < minSize && scale < 1D) {
			sb.append("Too Small: ");
			lowscale = scale;
			scale = (lowscale + highscale) * 0.5D;
		} else {
			sb.append("Just Right: ");
		}
		sb.append(String.format("%d%n", currFileSize));
		this.statusProcessor.appendStatus(sb.toString());
	}
	
	public boolean convert(String overlay, StatusProcessor outputProcessor, Path path, double startTime,
			double endTime, long minSize, long maxSize, boolean halveFramerate, int overlaySize) {
		MainFrame.getMainFrame().setBusy(true);
		boolean success = true;
		try {
			convert0(overlay, outputProcessor, path, startTime, endTime, minSize, maxSize, halveFramerate, overlaySize);
		} catch (RuntimeIOException ioe){
			ioe.printStackTrace();
			success = false;
		}
		IOHelper.deleteTempFile(gifFile);
		IOHelper.deleteTempFile(nutFile);
		IOHelper.deleteTempFile(paletteFile);
		MainFrame.getMainFrame().setBusy(false);
		return success;
	}
	
	private void convert0(String overlay, StatusProcessor outputProcessor, Path path, double startTime,
			double endTime, long minSize, long maxSize, boolean halveFramerate, int overlaySize) {
		this.statusProcessor = outputProcessor;
		this.clipStartTime = startTime;
		this.clipEndTime = endTime;
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.halveFramerate = halveFramerate;
		
		lowscale = 0D;
		scale = minSize <= 0 ? 1D
				: 1D / Math.sqrt(scan.getWidth() * scan.getHeight() * scan.getFramerate() * (halveFramerate ? 0.5D : 1D) * (endTime - startTime)
						/ (3D * maxSize));
		if (scale > 1D) {
			scale = 1D;
		}
		highscale = 1D;
		
		this.gifFile = IOHelper.createTempFile();
		this.nutFile = IOHelper.createTempFile();
		this.paletteFile = IOHelper.createTempFile();
		
		prevWidth = -1;
		prevHeight = -1;
		prevPrevWidth = -2;
		prevPrevHeight = -2;
		
		long gifLength;
		try {
			gifLength = Files.size(gifFile);
		} catch (IOException ex) {
			throw new RuntimeIOException(ex);
		}

		while (gifLength == 0 || (gifLength < minSize && scale < 1) || gifLength > maxSize) {
			createGif(overlay, overlaySize);
			adjustScale();
			int newWidth = (int) (scan.getWidth() * scale);
			int newHeight = (int) (scan.getHeight() * scale);
			if (newWidth == prevWidth && newHeight == prevHeight
					|| newWidth == prevPrevWidth && newHeight == prevPrevHeight) {
				statusProcessor.appendStatus("Exiting Loop.");
				break;
			}
			prevPrevWidth = prevWidth;
			prevPrevHeight = prevHeight;
			prevWidth = newWidth;
			prevHeight = newHeight;
		}
		
		try {
			Files.copy(gifFile, path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ioe) {
			throw new RuntimeIOException(ioe);
		}

	}
	
	private void createGif(String overlay, int overlaySize) {
		int newWidth = (int) (scan.getWidth() * scale);
		int newHeight = (int) (scan.getHeight() * scale);
		
		PrintWriter writer = new PrintWriter(new StatusProcessorWriter(statusProcessor));
		
		writer.format("Testing Size: %dx%d%n%n", newWidth, newHeight);
		
		writer.print("Scaling Video... \r");
		
		writer.flush();
		
		Resource ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		
		String videoFilter = TextHelper.getTextHelper().createVideoFilter(null, "format=bgr0", newWidth, newHeight, false, halveFramerate ? 1 : 0, scan.getWidth(), scan.getHeight(), overlaySize, overlay);
		
		try {
			scanPercentDone("Scaling Video... ", clipEndTime - clipStartTime, writer,
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.getLocation().toString(), "-y", "-ss",
							Double.toString(this.clipStartTime), "-i", scan.getLocation().toString(), "-map", "0:v:0", "-vf", videoFilter,
							"-sws_flags", "lanczos", "-t", Double.toString(this.clipEndTime - this.clipStartTime),
							"-c", "ffv1", "-f", "nut", nutFile.toString()));
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Scaling Video... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			throw new RuntimeIOException(ex);
		}
		
		writer.println("Scaling Video... Done.");
		
		writer.print("Generating Palette... \r");
		writer.flush();
		
		try {
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.getLocation().toString(), "-y", "-i",
					this.nutFile.toString(), "-vf", "palettegen=max_colors=144", "-c", "png", "-f", "image2",
					this.paletteFile.toString());
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Generating Palette... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			throw new RuntimeIOException(ex);
		}
		
		writer.println("Generating Palette... Done.");
		
		writer.print("Generating GIF... \r");
		
		try {
			scanPercentDone("Generating GIF... ", clipEndTime - clipStartTime, writer,
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.getLocation().toString(), "-y", "-i",
							this.nutFile.toString(), "-i", this.paletteFile.toString(), "-lavfi",
							"paletteuse=dither=bayer:bayer_scale=3", "-c", "gif", "-f", "gif", this.gifFile.toString()));
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Generating GIF... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			throw new RuntimeIOException(ex);
		}
		
		writer.println("Generating GIF... Done.");
		
		if (ResourcesManager.loadedPkgs.contains("gifsicle")){
			try {
				Resource gifsicle = ResourcesManager.getResourcesManager().getXLocation("gifsicle", "gifsicle");
				writer.print("Crushing GIF... \r");
				ConcurrenceManager.getConcurrenceManager().exec(true, gifsicle.getLocation().toString(), "--batch", "--unoptimize", "--optimize=3", this.gifFile.toString());
				writer.println("Crushing GIF... Done.");
			} catch (ProcessTerminatedException ex){
				ex.printStackTrace();
				writer.println("Crushing GIF... Error.");
				ConcurrenceManager.getConcurrenceManager().stopAll();
				throw new RuntimeIOException(ex);
			}
		}

		writer.flush();
	}

	private void scanPercentDone(String prefix, double length, PrintWriter writer, InputStream in)
			throws ProcessTerminatedException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		try {
			while (null != (line = br.readLine())) {
				if (line.startsWith("frame=")) {
					double realTime = TextHelper.getFFmpegStatusTimeInSeconds(line);
					double percent = realTime * 100D / length;
					writer.format("%s%.2f%%\r", prefix, percent);
				}
			}
		} catch (IOException ioe) {
			throw new ProcessTerminatedException(ioe);
		}
	}
	
}
