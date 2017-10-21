package thebombzen.tumblgififier.video;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Pattern;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.io.resources.Resource;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;

public class VideoScan {
	private final double scanDuration;
	private final double scanFramerate;
	private final double scanPacketDuration;
	private final int scanWidth;
	private final Path scanLocation;
	private final int scanHeight;
	
	public static VideoScan scanFile(StatusProcessor processor, Path pathname) {
		
		processor.appendStatus("Scanning File... ");
		
		Resource ffprobe = ResourcesManager.getResourcesManager().getFFprobeLocation();
		
		String line = null;
		int width = -1;
		int height = -1;
		double duration = -1;
		double framerate = -1;
		double durationTime = -1;
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(ConcurrenceManager.getConcurrenceManager().exec(false, ffprobe.getLocation().toString(),
						"-select_streams", "v", "-of", "flat", "-show_streams", "-show_format", pathname.toString()), Charset.forName("UTF-8")))) {
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
						duration = Double.parseDouble(
								line.replaceAll("streams\\.stream\\.\\d+\\.duration=", "").replace("\"", ""));
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
		
		if (duration < 0) {
			processor.appendStatus("Did not find duration in metadata, checking packets...");
			Queue<String> lineQueue = new ArrayDeque<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(
					ConcurrenceManager.getConcurrenceManager().exec(false, ffprobe.getLocation().toString(), "-select_streams", "v",
							"-of", "flat", "-show_entries", "packet=pts_time,duration_time", pathname.toString())))) {
				while (null != (line = br.readLine())) {
					if (lineQueue.size() >= 2) {
						lineQueue.poll();
					}
					lineQueue.offer(line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				processor.appendStatus("Error finding duration.");
			}
			String line1 = lineQueue.poll();
			String line2 = lineQueue.poll();
			if (line1 != null && line2 != null) {
				if (line1.startsWith("packets.packet.0.") || line2.startsWith("packets.packet.0.")) {
					processor.appendStatus("You just opened a still image. Don't do that.");
				} else {
					String pts_time = line1.replaceAll(".*?pts_time=", "").replace("\"", "");
					String duration_time = line2.replaceAll(".*?duration_time=", "").replace("\"", "");
					try {
						double ptsTime = Double.parseDouble(pts_time);
						durationTime = Double.parseDouble(duration_time);
						duration = durationTime + ptsTime;
					} catch (NumberFormatException nfe) {
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
		
		return new VideoScan(width, height, duration, pathname, framerate, durationTime);
	}
	
	public VideoScan(int width, int height, double duration, Path location, double framerate,
			double durationTime) {
		this.scanWidth = width;
		this.scanHeight = height;
		this.scanDuration = duration;
		this.scanLocation = location;
		this.scanFramerate = framerate;
		this.scanPacketDuration = durationTime;
	}
	
	
	public double getSinglePacketDuration() {
		return scanPacketDuration;
	}
	
	public double getFrameDuration(){
		return 1D / getFramerate();
	}
	
	public double getDuration() {
		return scanDuration;
	}
	
	public double getFramerate() {
		return scanFramerate;
	}
	
	public int getCachePrecision(){
		int precision = (int)(Math.ceil(getFramerate() / 6D));
		return precision < 4 ? 4 : precision;
	}
	
	public double getShotDuration(){
		return 1D / getCachePrecision(); 
	}
	
	public int getHeight() {
		return scanHeight;
	}
	
	public Path getLocation() {
		return scanLocation;
	}
	
	public int getWidth() {
		return scanWidth;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(scanDuration);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(scanFramerate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + scanHeight;
		result = prime * result + ((scanLocation == null) ? 0 : scanLocation.hashCode());
		temp = Double.doubleToLongBits(scanPacketDuration);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + scanWidth;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VideoScan other = (VideoScan) obj;
		if (Double.doubleToLongBits(scanDuration) != Double.doubleToLongBits(other.scanDuration))
			return false;
		if (Double.doubleToLongBits(scanFramerate) != Double.doubleToLongBits(other.scanFramerate))
			return false;
		if (scanHeight != other.scanHeight)
			return false;
		if (scanLocation == null) {
			if (other.scanLocation != null)
				return false;
		} else if (!scanLocation.equals(other.scanLocation))
			return false;
		if (Double.doubleToLongBits(scanPacketDuration) != Double.doubleToLongBits(other.scanPacketDuration))
			return false;
		if (scanWidth != other.scanWidth)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VideoScan [scanDuration=" + scanDuration + ", scanFramerate=" + scanFramerate + ", scanPacketDuration="
				+ scanPacketDuration + ", scanWidth=" + scanWidth + ", scanLocation=" + scanLocation + ", scanHeight="
				+ scanHeight + "]";
	}

	
}
