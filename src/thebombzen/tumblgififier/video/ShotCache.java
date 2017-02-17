package thebombzen.tumblgififier.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import thebombzen.tumblgififier.ConcurrenceManager;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.resources.ResourceLocation;
import thebombzen.tumblgififier.io.resources.ResourcesManager;
import thebombzen.tumblgififier.text.StatusProcessor;
import thebombzen.tumblgififier.text.TextHelper;

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
	
	private Map<Integer, File> shotFiles = new HashMap<>();
	private Map<Integer, File> endShotFiles = new HashMap<>();
	
	public Future<BufferedImage> screenShot(StatusProcessor processor, String overlay, int frameNumber, int overlaySize, final boolean end) {
		return screenShot(processor, overlay, frameNumber, scan.getWidth(), scan.getHeight(), overlaySize, end);
	}
	
	public Future<BufferedImage> screenShot(StatusProcessor processor, String overlay, int frameNumber, double scale, int overlaySize, final boolean end) {
		return screenShot(processor, overlay, frameNumber, (int) (scan.getWidth() * scale), (int) (scan.getHeight() * scale), overlaySize, end);
	}
	
	public Future<BufferedImage> screenShot(final StatusProcessor processor, final String overlay, int frameNumber,
			final int shotWidth, final int shotHeight, final int overlaySize, final boolean end) {
		final Map<Integer, File> shotFiles = end ? this.endShotFiles : this.shotFiles;
		double time = frameNumber * 0.25D;
		if (time < 0 || time > scan.getDuration()) {
			throw new IllegalArgumentException("Time out of bounds!");
		}
		if (time + 0.25D > scan.getDuration()){
			frameNumber--;
			time -= 0.25D;
		}
		final int frameNumberF = frameNumber;
		Future<BufferedImage> future = new Future<BufferedImage>(){
			
			BufferedImage image = null;
			
			@Override
			public boolean cancel(boolean cancel) {
				return false;
			}
			
			@Override
			public BufferedImage get() throws InterruptedException, ExecutionException {
				if (image == null) {
					while (!isDone()) {
						Thread.sleep(10);
					}
					try {
						image = ImageIO.read(shotFiles.get(frameNumberF));
					} catch (IOException ioe) {
						return null;
					}
				}
				return image;
			}
			
			@Override
			public BufferedImage get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				long milis = 0;
				if (image == null) {
					while (!isDone()) {
						Thread.sleep(10);
						milis += 10;
						if (milis > unit.toMillis(timeout)) {
							throw new TimeoutException();
						}
					}
					try {
						image = ImageIO.read(shotFiles.get(frameNumberF));
					} catch (IOException ioe) {
						return null;
					}
				}
				return image;
			}
			
			@Override
			public boolean isCancelled() {
				return false;
			}
			
			@Override
			public boolean isDone() {
				return shotFiles.get(frameNumberF) != null;
			}
			
		};
		if (shotFiles.get(frameNumber) == null) {
			ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
				
				@Override
				public void run() {
					try {
						screenShot0(overlay, frameNumberF - 8, shotWidth, shotHeight, overlaySize, 17, end);
					} catch (IOException ex) {
						processor.processException(ex);
					}
				}
			});
		}
		return future;
	}

	private void screenShot0(String overlay, int frameNumber, int shotWidth, int shotHeight, int overlaySize, int frames, boolean end) throws IOException {
		if (frameNumber < 0) {
			frameNumber = 0;
		}
		if (frameNumber + frames > scan.getDuration() * 4D) {
			frames = (int)(scan.getDuration() * 4D - frameNumber); 
		}
		final Map<Integer, File> shotFiles = end ? this.endShotFiles : this.shotFiles;
		File shotFile = IOHelper.createTempFile();
		String shotFilename = shotFile.getAbsolutePath();
		IOHelper.deleteTempFile(shotFile);
		ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		
		double ffmpegStartTime = frameNumber / 4D - ( end ? scan.getFrameDuration() : 0);
		String videoFilter = TextHelper.getTextHelper().createVideoFilter("fps=fps=4:round=up" + ":start_time=" + ffmpegStartTime, "format=rgb24", shotWidth, shotHeight, true, 0, scan.getWidth(), scan.getHeight(), overlaySize, overlay);
		
		ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-ss", Double.toString(ffmpegStartTime),
				"-copyts", "-i", scan.getLocation(), "-map", "0:v", "-vf",
				 videoFilter, "-vsync", "drop", "-frames:v", Integer.toString(frames), "-c", "png", "-f", "image2",
				shotFilename + "_%06d.png");
		for (int i = 0; i < frames; i++) {
			String name = String.format("%s_%06d.png", shotFilename, i + 1);
			File tempShotFile = new File(name);
			IOHelper.markTempFile(tempShotFile.getAbsolutePath());
			if (shotFiles.get(frameNumber + i) != null) {
				IOHelper.deleteTempFile(tempShotFile);
			} else {
				shotFiles.put(frameNumber + i, tempShotFile);
			}
		}
	}
}
