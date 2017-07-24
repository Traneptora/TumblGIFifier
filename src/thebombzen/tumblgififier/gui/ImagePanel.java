package thebombzen.tumblgififier.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.JPanel;
import thebombzen.tumblgififier.util.Callback;
import thebombzen.tumblgififier.util.ConcurrenceManager;

public class ImagePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private BufferedImage image;
	
	private volatile boolean playing = false;
	private volatile Future<?> playClock = null;
	private final Callback<?> playCallback;
	
	public ImagePanel(BufferedImage image, final Callback<?> playCallback) {
		this.image = image;
		this.playCallback = playCallback;
		if (playCallback != null){
			this.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2){
						if (!playing){
							play();
						} else {
							stop();
						}
					}
				}
			});
		}
	}
	
	public boolean isPlaying() {
		return playing;
	}
	
	public void play(){
		playing = true;
		if (playClock != null && !playClock.isCancelled()){
			playClock.cancel(false);
		}
		playClock = ConcurrenceManager.getConcurrenceManager().createImpreciseTickClock(new Runnable(){
			public void run(){
				playCallback.call(null);
			}
		}, 100, TimeUnit.MILLISECONDS);
	}
	
	public void stop(){
		playing = false;
		if (playClock != null){
			playClock.cancel(false);
			playClock = null;
		}
	}
	
	public BufferedImage getImage() {
		return image;
	}
	
	@Override
	public void paint(Graphics g) {
		if (image == null) {
			g.setColor(Color.RED);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			return;
		}
		double thisAspect = (double) this.getWidth() / (double) this.getHeight();
		double imageAspect = (double) image.getWidth() / (double) image.getHeight();
		if (thisAspect > imageAspect) {
			int w = (int) (this.getHeight() * imageAspect);
			g.drawImage(image, (this.getWidth() - w) / 2, 0, w, this.getHeight(), null);
		} else if (thisAspect < imageAspect) {
			int h = (int) (this.getWidth() / imageAspect);
			g.drawImage(image, 0, (this.getHeight() - h) / 2, this.getWidth(), h, null);
		} else {
			g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
		}
	}
	
	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}
	
}
