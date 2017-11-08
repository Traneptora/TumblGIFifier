package thebombzen.tumblgififier.video;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.resources.ProcessTerminatedException;
import thebombzen.tumblgififier.util.io.resources.Resource;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;

public class VideoScan {
	private final Path scanLocation;
	private final double scanDuration;
	private final double scanFramerate;
	private final int scanWidth;
	private final int scanHeight;

	public static VideoScan scanFile(StatusProcessor processor, Path pathname) {
		// String line = null;
		AtomicInteger widthA = new AtomicInteger(-1);
		AtomicInteger heightA = new AtomicInteger(-1);
		AtomicReference<Double> durationA = new AtomicReference<>(-1D);
		AtomicReference<Double> framerateA = new AtomicReference<>(-1D);
		processor.appendStatus("Scanning File... ");
		Resource mpv = ResourcesManager.getResourcesManager().getMpvLocation();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					ConcurrenceManager.getConcurrenceManager().exec(false, mpv.getLocation().toString(), "--config=no",
							"--msg-level=all=v", "--msg-color=no",
							"--log-file=" + ResourcesManager.getResourcesManager().getLocalFile("mpv-probe.log"),
							"--input-terminal=no", "-start=9999:99:99", "--vo=null", "--aid=no", "--sid=no",
							"--script=" + ResourcesManager.getResourcesManager().getLocalFile("lib")
									.resolve("playback-time.lua"),
							"--keep-open=always", pathname.toString()),
					StandardCharsets.UTF_8));
			br.lines().forEach(line -> {
				if (line.startsWith("[playback_time] ")) {
					log("Found playback_time: " + line);
					line = line.substring("[playback_time] ".length());
					String[] parts = line.split(",");
					try {
						durationA.set(Double.parseDouble(parts[0]));
						widthA.set(Double.valueOf(parts[1]).intValue());
						heightA.set(Double.valueOf(parts[2]).intValue());
						framerateA.set(Double.parseDouble(parts[3]));
					} catch (NumberFormatException nfe) {
						log(nfe);
					} catch (ArrayIndexOutOfBoundsException aioobe) {
						log(aioobe);
					}
				}
			});
		} catch (ProcessTerminatedException pte) {
			log(pte);
		} finally {
			IOHelper.closeQuietly(br);
		}

		double duration = durationA.get();
		double framerate = framerateA.get();
		int width = widthA.get();
		int height = heightA.get();

		/*
		 * if (duration < 0) {
		 * log("Did not find duration in metadata, checking packets...");
		 * Resource ffmpeg =
		 * ResourcesManager.getResourcesManager().getFFmpegLocation(); try {
		 * duration = TextHelper.scanTotalTimeConverted(ConcurrenceManager.
		 * getConcurrenceManager().exec(false, ffmpeg.getLocation().toString(),
		 * "-i", pathname.toString(), "-map", "0:v:0", "-f", "null", "-")); }
		 * catch (ProcessTerminatedException ex) { log(ex); } }
		 */

		if (duration < 0 || height < 0 || width < 0 || framerate < 0) {
			processor.appendStatus("File Format Error.");
			return null;
		}

		if (duration == 0D && framerate == 1D) {
			processor.appendStatus("Did you really just open a still image or a text file?");
			return null;
		}

		return new VideoScan(width, height, duration, pathname, framerate);
	}

	public VideoScan(int width, int height, double duration, Path location, double framerate) {
		this.scanWidth = width;
		this.scanHeight = height;
		this.scanDuration = duration;
		this.scanLocation = location;
		this.scanFramerate = framerate;
	}

	public double getFrameDuration() {
		return 1D / getFramerate();
	}

	public double getDuration() {
		return scanDuration;
	}

	public double getFramerate() {
		return scanFramerate;
	}

	public double getScreenshotsPerSecond() {
		return getFramerate() / 6D;
	}

	public double getScreenshotDuration() {
		return 1D / getScreenshotsPerSecond();
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
		if (scanWidth != other.scanWidth)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VideoScan [scanLocation=" + scanLocation + ", scanDuration=" + scanDuration + ", scanFramerate="
				+ scanFramerate + ", scanWidth=" + scanWidth + ", scanHeight=" + scanHeight + "]";
	}

}
