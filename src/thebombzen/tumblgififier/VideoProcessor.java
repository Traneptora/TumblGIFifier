package thebombzen.tumblgififier;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;
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
	
	public static VideoProcessor scanFile(StatusProcessor processor, String filename) {
		
		processor.appendStatus("Scanning File... ");
		
		ResourceLocation ffprobe = ResourcesManager.getResourcesManager().getFFprobeLocation();
		
		String line = null;
		int width = -1;
		int height = -1;
		double duration = -1;
		double framerate = -1;
		double durationTime = -1;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(ConcurrenceManager.getConcurrenceManager().exec(false, ffprobe.toString(),
				"-select_streams", "v", "-of", "flat", "-show_streams", "-show_format", filename)))) {
			while (null != (line = br.readLine())) {
				if (Pattern.compile("streams\\.stream\\.\\d+\\.width=").matcher(line).find()) {
					try {
						width = Integer.parseInt(line.replaceAll("streams\\.stream\\.\\d+\\.width=", ""));
					} catch (NumberFormatException nfe) {
						processor.appendStatus("Error reading width: " + line);
						continue;
					}
				} else if (Pattern.compile("streams\\.stream\\.\\d+\\.height=").matcher(line).find()) {
					try {
						height = Integer.parseInt(line.replaceAll("streams\\.stream\\.\\d+\\.height=", ""));
					} catch (NumberFormatException nfe) {
						processor.appendStatus("Error reading height: " + line);
						continue;
					}
				} else if (Pattern.compile("streams\\.stream\\.\\d+\\.duration=").matcher(line).find()) {
					try {
						duration = Double.parseDouble(line.replaceAll("streams\\.stream\\.\\d+\\.duration=", "").replace("\"", ""));
					} catch (NumberFormatException nfe) {
						continue;
					}
				} else if (Pattern.compile("streams\\.stream\\.\\d+\\.r_frame_rate=").matcher(line).find()) {
					String rate = line.replaceAll("streams\\.stream\\.\\d+\\.r_frame_rate=", "").replace("\"", "");
					try {
						framerate = Double.parseDouble(rate);
					} catch (NumberFormatException nfe) {
						String[] rat = rate.split("/");
						if (rat.length != 2) {
							processor.appendStatus("Error reading framerate: " + line);
							continue;
						} else {
							try {
								double first = Double.parseDouble(rat[0]);
								double second = Double.parseDouble(rat[1]);
								framerate = first / second;
							} catch (NumberFormatException e) {
								processor.appendStatus("Error reading framerate: " + line);
								continue;
							}
						}
					}
				} else if (line.contains("format.duration=") && duration < 0) {
					try {
						duration = Double.parseDouble(line.replace("format.duration=", "").replace("\"", ""));
					} catch (NumberFormatException nfe) {
						continue;
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		if (duration < 0){
			Queue<String> lineQueue = new ArrayDeque<>(2);
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(ConcurrenceManager.getConcurrenceManager().exec(false, ffprobe.toString(),
					"-select_streams", "v", "-of", "flat", "-show_entries", "packet=pts_time,duration_time", filename)))){
					while (null != (line = br.readLine())) {
						if (lineQueue.size() >= 2){
							lineQueue.poll();
						}
						lineQueue.offer(line);
					}
				} catch (IOException ioe){
					ioe.printStackTrace();
					processor.appendStatus("Error finding duration.");
				}
				String line1 = lineQueue.poll();
				String line2 = lineQueue.poll();
				if (line1 != null && line2 != null){
					if (line1.startsWith("packets.packet.0.") || line2.startsWith("packets.packet.0.")){
						processor.appendStatus("You just opened a still image. Don't do that.");
					} else {
						String pts_time = line1.replaceAll(".*?pts_time=", "").replace("\"", "");
						String duration_time = line2.replaceAll(".*?duration_time=", "").replace("\"", "");
						try {
							double ptsTime = Double.parseDouble(pts_time);
							durationTime = Double.parseDouble(duration_time);
							duration = durationTime + ptsTime;
						} catch (NumberFormatException nfe){
							nfe.printStackTrace();
							processor.appendStatus("Error finding duration.");
						}
					}
					
				}
		} else {
			durationTime = 1D / framerate;
		}
		
		if (duration < 0 || height < 0 || width < 0 || framerate < 0 || durationTime < 0) {
			processor.appendStatus("File Format Error.");
			return null;
		}
		
		return new VideoProcessor(width, height, duration, filename, framerate, durationTime);
	}
	
	private final double duration;
	private double endTime;
	private final double framerate;
	private final double durationTime;
	private File gifFile;
	private boolean halveFramerate;
	private final int height;
	private double highscale = 2D;
	private final String location;
	private double lowscale = 0D;
	private long maxSize;
	private long minSize;
	private File mkvFile;
	private File paletteFile;
	private double scale = 1D;
	private double startTime;
	private StatusProcessor statusProcessor;
	private final int width;
	private int prevWidth = -1;
	private int prevPrevWidth = -2;
	private int prevHeight = -1;
	private int prevPrevHeight = -2;
	
	public VideoProcessor(int width, int height, double duration, String location, double framerate, double durationTime) {
		this.width = width;
		this.height = height;
		this.duration = duration;
		this.location = location;
		this.framerate = framerate;
		this.durationTime = durationTime;
	}
	
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
		this.startTime = startTime;
		this.endTime = endTime;
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.halveFramerate = halveFramerate;
		
		lowscale = 0D;
		scale = minSize <= 0 ? 1D
				: 1D / Math.sqrt(width * height * framerate * (halveFramerate ? 0.5D : 1D) * (endTime - startTime)
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
			boolean finished = createGif(overlay, overlaySize);
			if (!finished) {
				return false;
			}
			adjustScale();
			int newWidth = (int) (width * scale);
			int newHeight = (int) (height * scale);
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
		int newWidth = (int) (width * scale);
		int newHeight = (int) (height * scale);
		
		PrintWriter writer = new PrintWriter(new StatusProcessorWriter(statusProcessor));
		
		writer.format("Testing Size: %dx%d%n%n", newWidth, newHeight);
		
		writer.print("Scaling Video... \r");
		
		writer.flush();
		
		ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		
		String scaleText = "scale=" + newWidth + ":" + newHeight;
		
		try {
			scanPercentDone("Scaling Video... ", endTime - startTime, writer, ConcurrenceManager.getConcurrenceManager().exec(false,
					ffmpeg.toString(), "-y", "-ss", Double.toString(this.startTime), "-i", location, "-map", "0:v",
					"-filter:v",
					overlay.length() == 0 ? scaleText
							: scaleText + ", "
									+ TextHelper.getTextHelper().createDrawTextString(newWidth, newHeight, overlaySize, overlay),
					"-t", Double.toString(this.endTime - this.startTime), "-pix_fmt", "yuv420p",
					halveFramerate ? "-r" : "-y", halveFramerate ? String.format("%f", framerate * 0.5D) : "-y", "-c",
					"ffv1", "-f", "matroska", this.mkvFile.getAbsolutePath()));
			
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
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-i", this.mkvFile.getAbsolutePath(), "-vf", "palettegen",
					"-c", "png", "-f", "image2", this.paletteFile.getAbsolutePath());
		} catch (ProcessTerminatedException ex) {
			ex.printStackTrace();
			writer.println("Generating Palette... Error.");
			ConcurrenceManager.getConcurrenceManager().stopAll();
			return false;
		}
		
		writer.println("Generating Palette... Done.");
		
		writer.print("Generating GIF... \r");
		
		try {
			scanPercentDone("Generating GIF... ", endTime - startTime, writer,
					ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.toString(), "-y", "-i", this.mkvFile.getAbsolutePath(), "-i",
							this.paletteFile.getAbsolutePath(), "-lavfi", "paletteuse", "-c", "gif", "-f", "gif",
							this.gifFile.getAbsolutePath()));
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
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VideoProcessor other = (VideoProcessor) obj;
		if (Double.doubleToLongBits(duration) != Double.doubleToLongBits(other.duration))
			return false;
		if (Double.doubleToLongBits(framerate) != Double.doubleToLongBits(other.framerate))
			return false;
		if (height != other.height)
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (width != other.width)
			return false;
		return true;
	}
	
	
	public double getDurationTime() {
		return durationTime;
	}
	
	public double getDuration() {
		return duration;
	}
	
	public double getFramerate() {
		return framerate;
	}
	
	public int getHeight() {
		return height;
	}
	
	public String getLocation() {
		return location;
	}
	
	public int getWidth() {
		return width;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(duration);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(framerate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + height;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + width;
		return result;
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
		return screenShot(overlay, time, width, height, overlaySize);
	}
	
	public BufferedImage screenShot(String overlay, double time, double scale, int overlaySize) {
		return screenShot(overlay, time, (int) (width * scale), (int) (height * scale), overlaySize);
	}
	
	public BufferedImage screenShot(String overlay, double time, int shotWidth, int shotHeight, int overlaySize) {
		if (time < 0 || time > duration) {
			throw new IllegalArgumentException("Time out of bounds!");
		}
		File shotFile = null;
		try {
			ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
			shotFile = IOHelper.createTempFile();
			String scale = "scale=" + shotWidth + ":" + shotHeight;
			
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-ss", Double.toString(time), "-i", location, "-map",
					"0:v", "-vf",
					"format=rgb24, " + (overlay.length() == 0 ? scale
							: scale + ", "
									+ TextHelper.getTextHelper().createDrawTextString(shotWidth, shotHeight, overlaySize, overlay)),
					"-t", "0.5", "-r", "1", "-c", "png", "-f", "image2", shotFile.getAbsolutePath());
			return ImageIO.read(shotFile);
		} catch (IOException ioe) {
			return null;
		} finally {
			IOHelper.deleteTempFile(shotFile);
		}
	}
	
	@Override
	public String toString() {
		return "VideoScan [width=" + width + ", height=" + height + ", duration=" + duration + ", location=" + location
				+ ", framerate=" + framerate + "]";
	}
	
}
