package thebombzen.tumblgififier;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.imageio.ImageIO;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.resources.ProcessTerminatedException;
import thebombzen.tumblgififier.io.resources.ResourceLocation;
import thebombzen.tumblgififier.io.resources.ResourcesManager;
import thebombzen.tumblgififier.text.StatusProcessor;
import thebombzen.tumblgififier.text.StatusProcessorArea;
import thebombzen.tumblgififier.text.StatusProcessorWriter;
import thebombzen.tumblgififier.text.TextHelper;

public class VideoProcessor {
	
	public VideoProcessor(VideoScan scan){
		this.scan = scan;
		ConcurrenceManager.getConcurrenceManager().addShutdownTask(new Runnable(){
			public void run(){
				for (File shotFile : shotFiles.values()){
					IOHelper.deleteTempFile(shotFile);
				}
			}
		});
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
	
	private Map<Double, File> shotFiles = new HashMap<>();
	
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
		boolean status = convert0(overlay, outputProcessor, path, startTime, endTime, minSize, maxSize, halveFramerate,
				overlaySize);
		IOHelper.deleteTempFile(gifFile);
		IOHelper.deleteTempFile(mkvFile);
		IOHelper.deleteTempFile(paletteFile);
		MainFrame.getMainFrame().setBusy(false);
		return status;
	}
	
	private boolean convert0(String overlay, StatusProcessorArea outputProcessor, String path, double startTime,
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
		
		try {
			this.gifFile = IOHelper.createTempFile();
			this.mkvFile = IOHelper.createTempFile();
			this.paletteFile = IOHelper.createTempFile();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		prevWidth = -1;
		prevHeight = -1;
		prevPrevWidth = -2;
		prevPrevHeight = -2;
		
		while (gifFile.length() == 0 || (gifFile.length() < minSize && scale < 1) || gifFile.length() > maxSize) {
			boolean success = createGif(overlay, overlaySize);
			if (!success) {
				return false;
			}
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
			ioe.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean createGif(String overlay, int overlaySize) {
		int newWidth = (int) (scan.getWidth() * scale);
		int newHeight = (int) (scan.getHeight() * scale);
		
		PrintWriter writer = new PrintWriter(new StatusProcessorWriter(statusProcessor));
		
		writer.format("Testing Size: %dx%d%n%n", newWidth, newHeight);
		
		writer.print("Scaling Video... \r");
		
		writer.flush();
		
		ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		
		String scaleText = "scale=" + newWidth + ":" + newHeight;
		
		try {
			scanPercentDone("Scaling Video... ", clipEndTime - clipStartTime, writer,
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.toString(), "-y", "-ss",
							Double.toString(this.clipStartTime), "-i", scan.getLocation(), "-map", "0:v", "-filter:v",
							overlay.length() == 0 ? scaleText
									: scaleText + ", "
											+ TextHelper.getTextHelper().createDrawTextString(newWidth, newHeight,
													overlaySize, overlay),
							"-t", Double.toString(this.clipEndTime - this.clipStartTime), "-pix_fmt", "rgb24",
							halveFramerate ? "-r" : "-y", halveFramerate ? String.format("%f", scan.getFramerate() * 0.5D) : "-y",
							"-c", "ffv1", "-f", "matroska", this.mkvFile.getAbsolutePath()));
			
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Scaling Video... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			return false;
		}
		
		writer.println("Scaling Video... Done.");
		
		writer.print("Generating Palette... \r");
		writer.flush();
		
		try {
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-i",
					this.mkvFile.getAbsolutePath(), "-vf", "palettegen", "-c", "png", "-f", "image2",
					this.paletteFile.getAbsolutePath());
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Generating Palette... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			return false;
		}
		
		writer.println("Generating Palette... Done.");
		
		writer.print("Generating GIF... \r");
		
		try {
			scanPercentDone("Generating GIF... ", clipEndTime - clipStartTime, writer,
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.toString(), "-y", "-i",
							this.mkvFile.getAbsolutePath(), "-i", this.paletteFile.getAbsolutePath(), "-lavfi",
							"paletteuse", "-c", "gif", "-f", "gif", this.gifFile.getAbsolutePath()));
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Generating GIF... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			return false;
		}
		
		writer.println("Generating GIF... Done.");
		writer.flush();
		
		return true;
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
	
	public BufferedImage screenShot(String overlay, double time, int overlaySize) {
		return screenShot(overlay, time, scan.getWidth(), scan.getHeight(), overlaySize);
	}
	
	public BufferedImage screenShot(String overlay, double time, double scale, int overlaySize) {
		return screenShot(overlay, time, (int) (scan.getWidth() * scale), (int) (scan.getHeight() * scale), overlaySize);
	}
	
	public BufferedImage screenShot(String overlay, double time, int shotWidth, int shotHeight, int overlaySize) {
		if (time < 0 || time > scan.getDuration()) {
			throw new IllegalArgumentException("Time out of bounds!");
		}
		File shotFile = shotFiles.get(time);
		try {
			if (shotFile != null){
				return ImageIO.read(shotFiles.get(time));
			}
			ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
			shotFile = IOHelper.createTempFile();
			String scale = "scale=" + shotWidth + ":" + shotHeight;
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-ss", Double.toString(time),
					"-i", scan.getLocation(), "-map", "0:v", "-vf",
					"format=rgb24, " + (overlay.length() == 0 ? scale
							: scale + ", "
									+ TextHelper.getTextHelper().createDrawTextString(shotWidth, shotHeight,
											overlaySize, overlay)),
					"-t", "0.5", "-r", "1", "-c", "png", "-f", "image2", shotFile.getAbsolutePath());
			return ImageIO.read(shotFile);
		} catch (IOException ioe) {
			shotFile = null;
			return null;
		} finally {
			shotFiles.put(time, shotFile);
		}
	}
	
}
