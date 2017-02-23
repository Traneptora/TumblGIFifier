package thebombzen.tumblgififier.video;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Scanner;

import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.RuntimeIOException;
import thebombzen.tumblgififier.util.io.resources.ProcessTerminatedException;
import thebombzen.tumblgififier.util.io.resources.ResourceLocation;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;
import thebombzen.tumblgififier.util.text.StatusProcessorArea;
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

	private File gifFile;
	private File mkvFile;
	private File paletteFile;
	
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
		long currFileSize = gifFile.length();
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
	
	public boolean convert(String overlay, StatusProcessorArea outputProcessor, String path, double startTime,
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
		IOHelper.deleteTempFile(mkvFile);
		IOHelper.deleteTempFile(paletteFile);
		MainFrame.getMainFrame().setBusy(false);
		return success;
	}
	
	private void convert0(String overlay, StatusProcessorArea outputProcessor, String path, double startTime,
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
		this.mkvFile = IOHelper.createTempFile();
		this.paletteFile = IOHelper.createTempFile();
		
		prevWidth = -1;
		prevHeight = -1;
		prevPrevWidth = -2;
		prevPrevHeight = -2;
		
		while (gifFile.length() == 0 || (gifFile.length() < minSize && scale < 1) || gifFile.length() > maxSize) {
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
		
		File newFile = new File(path);
		
		if (newFile.exists()) {
			newFile.delete();
		}
		
		try {
			Files.copy(gifFile.toPath(), newFile.toPath());
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
		
		ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		
		String videoFilter = TextHelper.getTextHelper().createVideoFilter(null, "format=bgr0", newWidth, newHeight, false, halveFramerate ? 1 : 0, scan.getWidth(), scan.getHeight(), overlaySize, overlay);
		
		try {
			scanPercentDone("Scaling Video... ", clipEndTime - clipStartTime, writer,
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.toString(), "-y", "-ss",
							Double.toString(this.clipStartTime), "-i", scan.getLocation(), "-map", "v", "-vf", videoFilter,
							"-t", Double.toString(this.clipEndTime - this.clipStartTime),
							"-c", "ffv1", "-f", "matroska", this.mkvFile.getAbsolutePath()));
			
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
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-i",
					this.mkvFile.getAbsolutePath(), "-vf", "palettegen=max_colors=128", "-c", "png", "-f", "image2",
					this.paletteFile.getAbsolutePath());
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
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.toString(), "-y", "-i",
							this.mkvFile.getAbsolutePath(), "-i", this.paletteFile.getAbsolutePath(), "-lavfi",
							"paletteuse=dither=bayer:bayer_scale=3", "-c", "gif", "-f", "gif", this.gifFile.getAbsolutePath()));
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Generating GIF... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			throw new RuntimeIOException(ex);
		}
		
		writer.println("Generating GIF... Done.");
		writer.flush();

	}

	private void scanPercentDone(String prefix, double length, PrintWriter writer, InputStream in)
			throws ProcessTerminatedException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		try {
			while (null != (line = br.readLine())) {
				if (line.startsWith("frame=")) {
					String time = "0";
					Scanner sc2 = new Scanner(line);
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
							nfe.printStackTrace();
						}
					}
					sc2.close();
					double percent = realTime * 100D / length;
					writer.format("%s%.2f%%\r", prefix, percent);
				}
			}
		} catch (IOException ioe) {
			throw new ProcessTerminatedException(ioe);
		}
	}
	
}
