package thebombzen.tumblgififier.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private BufferedImage image;
	
	public ImagePanel(BufferedImage image) {
		this.image = image;
	}
	
	public BufferedImage getImage() {
		return image;
	}
	
	@Override
	public void paint(Graphics g) {
		if (image == null) {
			g.setColor(Color.RED);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
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
