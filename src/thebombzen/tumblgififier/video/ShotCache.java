package thebombzen.tumblgififier.video;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import thebombzen.tumblgififier.gui.ImagePanel;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.resources.Resource;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;
import thebombzen.tumblgififier.util.text.TextHelper;

public class ShotCache {

	public ShotCache(VideoScan scan) {
		this.scan = scan;
	}

	private VideoScan scan;

	public VideoScan getScan() {
		return scan;
	}

	public void setScan(VideoScan scan) {
		this.scan = scan;
	}

	private Map<Integer, Path> shotFiles = new HashMap<>();
	private Map<Integer, Path> endShotFiles = new HashMap<>();

	public void screenShot(final Consumer<? super BufferedImage> callback, final ImagePanel parentPanel,
			final StatusProcessor processor, final String overlay, int frameNumber, final int shotWidth,
			final int shotHeight, final int overlaySize, final boolean end) {
		final Map<Integer, Path> shotFiles = end ? this.endShotFiles : this.shotFiles;
		double time = frameNumber * scan.getScreenshotDuration();
		if (time < 0 || time > scan.getDuration()) {
			throw new IllegalArgumentException("Time out of bounds!");
		}
		final int frameNumberF = time + scan.getScreenshotDuration() > scan.getDuration() ? frameNumber - 1
				: frameNumber;
		if (shotFiles.get(frameNumberF) == null) {
			try {
				screenShot0(overlay, frameNumberF - 8, shotWidth, shotHeight, overlaySize, 17, end);
			} catch (IOException ioe) {
				log(ioe);
				processor.appendStatus("Oh noes, it appears something went wrong.");
				return;
			}
		}
		try {
			callback.accept(ImageIO.read(Files.newInputStream(shotFiles.get(frameNumberF))));
		} catch (IOException ioe) {
			log(ioe);
			processor.appendStatus("Oh noes, it appears something went wrong.");
			return;
		}
	}

	private void screenShot0(String overlay, int frameNumber, int shotWidth, int shotHeight, int overlaySize,
			int frames, boolean end) throws IOException {
		log(String.format("Screenshotting: %s, %d, %d, %d, %d, %d, %b", overlay, frameNumber, shotWidth, shotHeight,
				overlaySize, frames, end));
		if (frameNumber < 0) {
			frameNumber = 0;
		}
		if (frameNumber + frames > scan.getDuration() * scan.getScreenshotsPerSecond()) {
			frames = (int) (scan.getDuration() * scan.getScreenshotsPerSecond() - frameNumber);
		}
		final Map<Integer, Path> shotFiles = end ? this.endShotFiles : this.shotFiles;
		Path shotPath = IOHelper.createTempFile();
		IOHelper.deleteTempFile(shotPath);
		Resource mpv = ResourcesManager.getMpvLocation();
		double startTimeCode = frameNumber * scan.getScreenshotDuration();
		String videoFilter = TextHelper.getTextHelper().createVideoFilter(null, "format=rgb24", shotWidth, shotHeight,
				true, 5, scan.getWidth(), scan.getHeight(), overlaySize, overlay);
		ConcurrenceManager.exec(false, true, mpv.getLocation().toString(), scan.getLocation().toString(), "--config=no",
				"--msg-level=all=v", "--msg-color=no",
				"--log-file=" + ResourcesManager.getLocalFile("mpv-screenshot.log"), "--input-terminal=no", "--aid=no",
				"--sid=no", "--correct-downscaling", "--scale=spline36", "--dscale=spline36", "--cscale=spline36",
				"--ofps=" + scan.getScreenshotsPerSecond(), "--of=image2", "--ovc=png", "--term-status-msg=",
				"--sws-scaler=spline", "--lavfi-complex=sws_flags=spline;[vid1]" + videoFilter + "[vo]",
				"--start=" + startTimeCode, "--frames=" + (frames - 1), "--o=" + shotPath.toString() + "_%06d.png");
		for (int i = 0; i < frames; i++) {
			String name = String.format("%s_%06d.png", shotPath.toString(), i + 1);
			Path tempShotPath = Paths.get(name);
			IOHelper.markTempFile(tempShotPath);
			if (shotFiles.get(frameNumber + i) != null) {
				IOHelper.deleteTempFile(tempShotPath);
			} else {
				shotFiles.put(frameNumber + i, tempShotPath);
			}
		}
	}
}
