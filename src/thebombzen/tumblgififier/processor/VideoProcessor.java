package thebombzen.tumblgififier.processor;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Scanner;
import javax.imageio.ImageIO;
import thebombzen.tumblgififier.gui.MainFrame;
import thebombzen.tumblgififier.gui.StatusProcessorArea;

public class VideoProcessor {
	
	public static VideoProcessor scanFile(StatusProcessor processor, String filename) {
		
		processor.appendStatus("Scanning File... ");
		
		String ffprobe = FFmpegManager.getFFmpegManager().getFFprobeLocation();
		
		String line = null;
		int width = -1;
		int height = -1;
		double duration = -1;
		double framerate = -1;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(MainFrame.getMainFrame().exec(false, ffprobe,
				"-select_streams", "v", "-of", "flat", "-show_streams", "-show_format", filename)))) {
			while (null != (line = br.readLine())) {
				// System.err.println(line);
				if (line.contains("streams.stream.0.width=")) {
					try {
						width = Integer.parseInt(line.replace("streams.stream.0.width=", ""));
					} catch (NumberFormatException nfe) {
						processor.appendStatus("Error reading width: " + line);
						continue;
					}
				} else if (line.contains("streams.stream.0.height=")) {
					try {
						height = Integer.parseInt(line.replace("streams.stream.0.height=", ""));
					} catch (NumberFormatException nfe) {
						processor.appendStatus("Error reading height: " + line);
						continue;
					}
				} else if (line.contains("streams.stream.0.duration=")) {
					try {
						duration = Double.parseDouble(line.replace("streams.stream.0.duration=", "").replace("\"", ""));
					} catch (NumberFormatException nfe) {
						continue;
					}
				} else if (line.contains("streams.stream.0.r_frame_rate=")) {
					String rate = line.replace("streams.stream.0.r_frame_rate=", "").replace("\"", "");
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
				} else if (line.contains("format.duration=") && duration == -1) {
					try {
						duration = Double.parseDouble(line.replace("format.duration=", "").replace("\"", ""));
					} catch (NumberFormatException nfe) {
						processor.appendStatus("Error reading duration: " + line);
						continue;
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		if (duration < 0 || height < 0 || width < 0 || framerate < 0) {
			processor.appendStatus("File Format Error.");
			return null;
		}
		
		return new VideoProcessor(width, height, duration, filename, framerate);
	}
	
	private final double duration;
	private double endTime;
	private boolean foundLowerBound = false;
	private final double framerate;
	private File gifFile;
	private boolean halveFramerate;
	private final int height;
	private double highscale = 1D;
	private final String location;
	private double lowscale = 1D;
	private long maxSize;
	private long minSize;
	private File mkvFile;
	private File paletteFile;
	private double scale = 1D;
	private double startTime;
	private StatusProcessor statusProcessor;
	private final int width;
	
	public VideoProcessor(int width, int height, double duration, String location, double framerate) {
		this.width = width;
		this.height = height;
		this.duration = duration;
		this.location = location;
		this.framerate = framerate;
	}
	
	private void adjustScale() {
		StringBuilder sb = new StringBuilder();
		sb.append("Checking Filesize... ");
		long currFileSize = gifFile.length();
		if (currFileSize > maxSize) {
			sb.append("Too Big: ");
			if (!foundLowerBound) {
				this.highscale = scale;
				this.lowscale = scale * 0.5D;
				this.scale = lowscale;
			} else {
				this.highscale = scale;
				this.scale = (this.lowscale + this.highscale) * 0.5D;
			}
		} else if (currFileSize < minSize && scale < 1D) {
			sb.append("Too Small: ");
			this.foundLowerBound = true;
			this.lowscale = scale;
			this.scale = (this.lowscale + this.highscale) * 0.5D;
		} else {
			sb.append("Just Right: ");
		}
		sb.append(String.format("%d%n", currFileSize));
		this.statusProcessor.appendStatus(sb.toString());
	}
	
	public boolean convert(StatusProcessorArea outputProcessor, String path, double startTime, double endTime,
			long minSize, long maxSize, boolean halveFramerate) {
		scale = 1D;
		lowscale = 1D;
		highscale = 1D;
		foundLowerBound = false;
		this.statusProcessor = outputProcessor;
		this.startTime = startTime;
		this.endTime = endTime;
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.halveFramerate = halveFramerate;
		
		try {
			this.gifFile = File.createTempFile("tumblgififier", ".tmp");
			this.mkvFile = File.createTempFile("tumblgififier", ".tmp");
			this.paletteFile = File.createTempFile("tumblgififier", ".tmp");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		gifFile.deleteOnExit();
		mkvFile.deleteOnExit();
		paletteFile.deleteOnExit();
		
		while (gifFile.length() == 0 || (gifFile.length() < minSize && scale < 1) || gifFile.length() > maxSize) {
			createGif();
			adjustScale();
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
	
	private void createGif() {
		int newWidth = (int) (width * scale);
		int newHeight = (int) (height * scale);
		
		PrintWriter writer = new PrintWriter(new StatusProcessorWriter(statusProcessor));
		
		writer.format("Testing Size: %dx%d%n%n", newWidth, newHeight);
		
		writer.print("Scaling Video... \r");
		
		String ffmpeg = FFmpegManager.getFFmpegManager().getFFmpegLocation();
		
		scanPercentDone(
				"Scaling Video... ",
				endTime - startTime,
				writer,
				MainFrame.getMainFrame().exec(false, ffmpeg, "-y", "-ss", Double.toString(this.startTime), "-i",
						location, "-map", "0:v", "-t", Double.toString(this.endTime - this.startTime), "-pix_fmt",
						"yuv420p", "-s", newWidth + "x" + newHeight, halveFramerate ? "-r" : "-y",
						halveFramerate ? String.format("%f", framerate * 0.5D) : "-y", "-c", "ffv1", "-f", "matroska",
						this.mkvFile.getAbsolutePath()));
		
		writer.println("Scaling Video... Done.");
		
		writer.print("Generating Palette... \r");
		writer.flush();
		
		MainFrame.getMainFrame().exec(true, ffmpeg, "-y", "-i", this.mkvFile.getAbsolutePath(), "-vf", "palettegen",
				"-c", "png", "-f", "image2", this.paletteFile.getAbsolutePath());
		
		writer.println("Generating Palette... Done.");
		
		writer.print("Generating GIF... \r");
		
		scanPercentDone(
				"Generating GIF... ",
				endTime - startTime,
				writer,
				MainFrame.getMainFrame().exec(false, ffmpeg, "-y", "-i", this.mkvFile.getAbsolutePath(), "-i",
						this.paletteFile.getAbsolutePath(), "-lavfi", "paletteuse", "-c", "gif", "-f", "gif",
						this.gifFile.getAbsolutePath()));
		
		writer.println("Generating GIF... Done.");
		writer.flush();
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
	
	private void scanPercentDone(String prefix, double length, PrintWriter writer, InputStream in) {
		Scanner scanner = new Scanner(in);
		scanner.useDelimiter("(\r\n|\r|\n)");
		while (scanner.hasNext()) {
			String line = scanner.next();
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
		scanner.close();
	}
	
	public BufferedImage screenShot(double time) {
		return screenShot(time, width, height);
	}
	
	public BufferedImage screenShot(double time, double scale) {
		return screenShot(time, (int) (width * scale), (int) (height * scale));
	}
	
	public BufferedImage screenShot(double time, int shotWidth, int shotHeight) {
		if (time < 0 || time > duration) {
			throw new IllegalArgumentException("Time out of bounds!");
		}
		File shotFile = null;
		try {
			String ffmpeg = FFmpegManager.getFFmpegManager().getFFmpegLocation();
			shotFile = File.createTempFile("tumblgififier", ".tmp");
			shotFile.deleteOnExit();
			MainFrame.getMainFrame().exec(true, ffmpeg, "-y", "-ss", Double.toString(time), "-i", location, "-map",
					"0:v", "-t", Double.toString(0.5D / framerate), "-s", shotWidth + "x" + shotHeight, "-qscale", "5",
					"-pix_fmt", "yuvj420p", "-c", "mjpeg", "-f", "image2", shotFile.getAbsolutePath());
			return ImageIO.read(shotFile);
		} catch (IOException ioe) {
			return null;
		} finally {
			shotFile.delete();
		}
	}
	
	@Override
	public String toString() {
		return "VideoScan [width=" + width + ", height=" + height + ", duration=" + duration + ", location=" + location
				+ ", framerate=" + framerate + "]";
	}
	
}
