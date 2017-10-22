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
	
	public ShotCache(VideoScan scan){
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
	
	public void screenShot(Consumer<BufferedImage> callback, ImagePanel parentPanel, StatusProcessor processor, String overlay, int frameNumber, int overlaySize, final boolean end) {
		screenShot(callback, parentPanel, processor, overlay, frameNumber, scan.getWidth(), scan.getHeight(), overlaySize, end);
	}
	
	public void screenShot(Consumer<BufferedImage> callback, ImagePanel parentPanel, StatusProcessor processor, String overlay, int frameNumber, double scale, int overlaySize, final boolean end) {
		screenShot(callback, parentPanel, processor, overlay, frameNumber, (int) (scan.getWidth() * scale), (int) (scan.getHeight() * scale), overlaySize, end);
	}
	
	public void screenShot(final Consumer<BufferedImage> callback, final ImagePanel parentPanel, final StatusProcessor processor, final String overlay, int frameNumber,
			final int shotWidth, final int shotHeight, final int overlaySize, final boolean end) {
		final Map<Integer, Path> shotFiles = end ? this.endShotFiles : this.shotFiles;
		double time = frameNumber * scan.getShotDuration();
		if (time < 0 || time > scan.getDuration()) {
			throw new IllegalArgumentException("Time out of bounds!");
		}
		final int frameNumberF = time + scan.getShotDuration() > scan.getDuration() ? frameNumber - 1 : frameNumber;
		if (shotFiles.get(frameNumberF) == null) {
			final boolean playing = parentPanel.isPlaying();
			if (playing){
				parentPanel.stop();
			}
			ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
				@Override
				public void run() {
					try {
						screenShot0(overlay, frameNumberF - 8, shotWidth, shotHeight, overlaySize, 17, end);
						callback.accept(ImageIO.read(Files.newInputStream(shotFiles.get(frameNumberF))));
						if (playing){
							if (parentPanel.isPlaying()){
								parentPanel.stop();
							} else {
								parentPanel.play();
							}
						}
					} catch (IOException ioe) {
						processor.appendStatus("Oh noes, it appears something went wrong.");
						log(ioe);
					}
				}
			});
		} else {
			try {
				callback.accept(ImageIO.read(Files.newInputStream(shotFiles.get(frameNumberF))));
			} catch (IOException ioe){
				processor.appendStatus("Oh noes, it appears something went wrong.");
				log(ioe);
			}
		}
	}

	private void screenShot0(String overlay, int frameNumber, int shotWidth, int shotHeight, int overlaySize, int frames, boolean end) throws IOException {
		if (frameNumber < 0) {
			frameNumber = 0;
		}
		if (frameNumber + frames > scan.getDuration() * scan.getCachePrecision()) {
			frames = (int)(scan.getDuration() * scan.getCachePrecision() - frameNumber); 
		}
		final Map<Integer, Path> shotFiles = end ? this.endShotFiles : this.shotFiles;
		Path shotPath = IOHelper.createTempFile();
		IOHelper.deleteTempFile(shotPath);
		Resource ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		
		double ffmpegStartTime = frameNumber * scan.getShotDuration() - ( end ? scan.getFrameDuration() : 0);
		String videoFilter = TextHelper.getTextHelper().createVideoFilter("fps=fps=" + scan.getCachePrecision(), "format=rgb24", shotWidth, shotHeight, true, 0, scan.getWidth(), scan.getHeight(), overlaySize, overlay);
		
		double totalTime = TextHelper.scanTotalTimeConverted(ConcurrenceManager.getConcurrenceManager().exec(false, ffmpeg.getLocation().toString(), "-y", "-ss", Double.toString(ffmpegStartTime),
				"-i", scan.getLocation().toString(), "-map", "0:v:0", "-vf",
				 videoFilter, "-sws_flags", "lanczos", "-vsync", "drop", "-frames:v", Integer.toString(frames), "-c", "png", "-f", "image2",
				shotPath.toString() + "_%06d.png"));
		if (totalTime <= 0) {
			log("Video file has no index, using slow seeking.");
			ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.getLocation().toString(), "-y", "-i", scan.getLocation().toString(), "-map", "0:v:0", "-ss", Double.toString(ffmpegStartTime), "-vf",
					 videoFilter, "-sws_flags", "lanczos", "-vsync", "drop", "-frames:v", Integer.toString(frames), "-c", "png", "-f", "image2",
					shotPath.toString() + "_%06d.png");
		}
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
