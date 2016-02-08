package thebombzen.tumblgififier.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import thebombzen.tumblgififier.processor.FFmpegManager;
import thebombzen.tumblgififier.processor.StatusProcessor;
import thebombzen.tumblgififier.processor.VideoProcessor;

public class MainPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private JCheckBox cutFramerateInHalfCheckBox;
	private JSlider endSlider;
	private JPanel leftPanel;
	private int maxSize = 2000;
	private JCheckBox maxSizeCheckBox;
	private JTextField maxSizeTextField;
	private String mostRecentGIFDirectory = null;
	private JButton playButton;
	private ImagePanel previewImageEndPanel;
	private ImagePanel previewImageStartPanel;
	private VideoProcessor scan;
	private JLabel sizeThresholdLabel;
	private JSlider sizeThresholdSlider;
	private JSlider startSlider;
	private StatusProcessorArea statusArea;
	
	public MainPanel(VideoProcessor scan) {
		this.scan = scan;
		if (scan == null) {
			throw new NullPointerException();
		}
		setupLayout();
	}
	
	private void createGIF(final String path) {
		MainFrame.getMainFrame().setBusy(true);
		final int maxSizeBytes;
		final int minSizeBytes;
		if (maxSizeCheckBox.isSelected()) {
			maxSizeBytes = 1000 * maxSize;
			minSizeBytes = 1000 * (maxSize - maxSize * sizeThresholdSlider.getValue() / 100);
		} else {
			maxSizeBytes = Integer.MAX_VALUE;
			minSizeBytes = 1;
		}
		final boolean halveFramerate = cutFramerateInHalfCheckBox.isSelected();
		final double clipStart = startSlider.getValue() * 0.25D;
		final double clipEnd = endSlider.getValue() * 0.25D;
		new Thread(new Runnable(){
			
			@Override
			public void run() {
				boolean success = scan.convert(statusArea, path, clipStart, clipEnd, minSizeBytes, maxSizeBytes,
						halveFramerate);
				MainFrame.getMainFrame().setBusy(false);
				if (success) {
					statusArea.appendStatus("Done!");
					JOptionPane.showMessageDialog(MainPanel.this, "Done!", "Success!", JOptionPane.INFORMATION_MESSAGE);
				} else {
					statusArea.appendStatus("Some error occured :(");
					JOptionPane.showMessageDialog(MainPanel.this, "Some error occured :(", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}).start();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(990, 640);
	}
	
	public StatusProcessor getStatusProcessor() {
		return statusArea;
	}
	
	private void playClipFast() {
		
		MainFrame.getMainFrame().setBusy(true);
		
		final double clipStart = startSlider.getValue() * 0.25D;
		final double clipEnd = endSlider.getValue() * 0.25D;
		final int w = 480;
		final int h = 270;
		
		final String ffplay = FFmpegManager.getFFmpegManager().getFFplayLocation();
		playButton.setEnabled(false);
		new Thread(new Runnable(){
			
			@Override
			public void run() {
				String scale;
				if (w < scan.getWidth()) {
					scale = "scale=" + w + ":-1";
				} else if (h < scan.getHeight()) {
					scale = "scale=-1:" + h;
				} else {
					scale = "scale";
				}
				MainFrame.getMainFrame().exec(true, ffplay, "-loop", "0", "-an", "-sn", "-vst", "0:v", scan.getLocation(), "-ss",
							Double.toString(clipStart), "-t", Double.toString(clipEnd - clipStart), "-vf", scale);
				EventQueue.invokeLater(new Runnable(){
					
					@Override
					public void run() {
						playButton.setEnabled(true);
						MainFrame.getMainFrame().setBusy(false);
					}
				});
			}
		}).start();
	}
	
	private void playClipSlow() {
		MainFrame.getMainFrame().setBusy(true);
		
		final double clipStart = startSlider.getValue() * 0.25D;
		final double clipEnd = endSlider.getValue() * 0.25D;
		
		final String ffmpeg = FFmpegManager.getFFmpegManager().getFFmpegLocation();
		final String ffplay = FFmpegManager.getFFmpegManager().getFFplayLocation();
		
		new Thread(new Runnable(){
			
			@Override
			public void run() {
				File tempFile = null;
				statusArea.appendStatus("Rendering Clip... ");
				try {
					tempFile = File.createTempFile("tumblrgififier", ".tmp");
					tempFile.deleteOnExit();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					statusArea.appendStatus("Error rendering clip :(");
					return;
				}
				String scale;
				final int w = 480;
				final int h = 270;
				if (w < scan.getWidth()) {
					scale = "scale=" + w + ":-1";
				} else if (h < scan.getHeight()) {
					scale = "scale=-1:" + h;
				} else {
					scale = "scale";
				}
				MainFrame.getMainFrame().exec(true, ffmpeg, "-y", "-ss", Double.toString(clipStart), "-i",
						scan.getLocation(), "-map", "0:v", "-t", Double.toString(clipEnd - clipStart), "-pix_fmt",
						"yuv420p", "-vf", scale, "-c", "ffvhuff", "-f", "matroska", tempFile.getAbsolutePath());
				MainFrame.getMainFrame().exec(true, ffplay, "-loop", "0", tempFile.getAbsolutePath());
				tempFile.delete();
				EventQueue.invokeLater(new Runnable(){
					
					@Override
					public void run() {
						MainFrame.getMainFrame().setBusy(false);
						MainFrame.getMainFrame().toFront();
						MainFrame.getMainFrame().setAlwaysOnTop(true);
						MainFrame.getMainFrame().setAlwaysOnTop(false);
						MainFrame.getMainFrame().requestFocus();
					}
				});
			}
		}).start();
		
	}
	
	private void setupLayout() {
		BufferedImage previewImageStart = scan.screenShot(1D / 3D * scan.getDuration());
		BufferedImage previewImageEnd = scan.screenShot(2D / 3D * scan.getDuration());
		if (previewImageStart == null) {
			previewImageStart = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
		}
		if (previewImageEnd == null) {
			previewImageEnd = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
		}
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(Box.createVerticalStrut(10));
		Box horizontalBox = Box.createHorizontalBox();
		this.add(horizontalBox);
		this.add(Box.createVerticalStrut(10));
		leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(480, 680));
		horizontalBox.add(Box.createHorizontalStrut(10));
		horizontalBox.add(leftPanel);
		horizontalBox.add(Box.createHorizontalStrut(10));
		Box rightBox = Box.createVerticalBox();
		previewImageStartPanel = new ImagePanel(previewImageStart);
		previewImageStartPanel.setPreferredSize(new Dimension(480, 270));
		rightBox.add(previewImageStartPanel);
		rightBox.add(Box.createVerticalStrut(10));
		startSlider = new JSlider();
		startSlider.setMinimum(0);
		startSlider.setMaximum((int) (scan.getDuration() * 4D) - 1);
		startSlider.setValue(startSlider.getMaximum() / 3);
		rightBox.add(startSlider);
		
		playButton = new JButton("Play Clip (Fast, Inaccurate)");
		playButton.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				playClipFast();
			}
		});
		
		JButton playButton2 = new JButton("Play Clip (Slow, Accurate)");
		playButton2.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				playClipSlow();
			}
		});
		
		JPanel playButtonPanel = new JPanel(new BorderLayout());
		playButtonPanel.add(playButton, BorderLayout.WEST);
		playButtonPanel.add(playButton2, BorderLayout.EAST);
		
		rightBox.add(Box.createVerticalStrut(10));
		rightBox.add(playButtonPanel);
		rightBox.add(Box.createVerticalStrut(10));
		
		endSlider = new JSlider();
		endSlider.setMinimum(0);
		endSlider.setMaximum((int) (scan.getDuration() * 4D) - 1);
		endSlider.setValue(endSlider.getMaximum() * 2 / 3);
		rightBox.add(endSlider);
		
		startSlider.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (startSlider.getValue() > endSlider.getValue()) {
					startSlider.setValue(endSlider.getValue());
				}
				if (!startSlider.getValueIsAdjusting() && scan != null) {
					updateStartScreenshot();
				}
				
			}
		});
		
		endSlider.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (endSlider.getValue() < startSlider.getValue()) {
					endSlider.setValue(startSlider.getValue());
				}
				if (!endSlider.getValueIsAdjusting() && scan != null) {
					updateEndScreenshot();
				}
			}
		});
		
		rightBox.add(Box.createVerticalStrut(10));
		previewImageEndPanel = new ImagePanel(previewImageEnd);
		previewImageEndPanel.setPreferredSize(new Dimension(480, 270));
		rightBox.add(previewImageEndPanel);
		horizontalBox.add(rightBox);
		horizontalBox.add(Box.createHorizontalStrut(10));
		
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(wrapLeftAligned(new JLabel("Video Stats")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftRightAligned(new JLabel("Width:"), new JLabel(Integer.toString(scan.getWidth()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftRightAligned(new JLabel("Height:"), new JLabel(Integer.toString(scan.getHeight()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftRightAligned(new JLabel("Duration:"),
				new JLabel(String.format("%.2f", scan.getDuration()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftRightAligned(new JLabel("Framerate:"),
				new JLabel(String.format("%.2f", scan.getFramerate()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(5));
		maxSizeTextField = new JTextField("2000");
		maxSizeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		maxSizeTextField.setMaximumSize(new Dimension(200, 25));
		maxSizeTextField.setPreferredSize(new Dimension(200, 25));
		maxSizeTextField.addFocusListener(new FocusAdapter(){
			
			@Override
			public void focusLost(FocusEvent e) {
				try {
					int size = Integer.parseInt(maxSizeTextField.getText());
					if (size >= 0) {
						maxSize = size;
					} else {
						maxSizeTextField.setText(Integer.toString(maxSize));
					}
				} catch (NumberFormatException nfe) {
					maxSizeTextField.setText(Integer.toString(maxSize));
				}
			}
		});
		maxSizeCheckBox = new JCheckBox("Maximum GIF Size in Kilobytes: ");
		maxSizeCheckBox.setSelected(true);
		leftPanel.add(wrapLeftRightAligned(maxSizeCheckBox, maxSizeTextField));
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(wrapLeftAligned(new JLabel("The maximum size on Tumblr is 2000 Kilobytes.")));
		leftPanel.add(Box.createVerticalStrut(20));
		sizeThresholdLabel = new JLabel("10 Percent");
		leftPanel.add(wrapLeftRightAligned(new JLabel("Acceptable Size Percent:"), sizeThresholdLabel));
		leftPanel.add(Box.createVerticalStrut(5));
		sizeThresholdSlider = new JSlider();
		sizeThresholdSlider.setMaximum(100);
		sizeThresholdSlider.setMinimum(3);
		sizeThresholdSlider.setValue(10);
		sizeThresholdSlider.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				sizeThresholdLabel.setText(sizeThresholdSlider.getValue() + " Percent");
			}
		});
		leftPanel.add(wrapLeftAligned(sizeThresholdSlider));
		
		maxSizeCheckBox.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				maxSizeTextField.setEnabled(maxSizeCheckBox.isSelected());
				sizeThresholdSlider.setEnabled(maxSizeCheckBox.isSelected());
			}
			
		});
		
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftAligned(new JLabel("How close do you want the GIF file size to the maximum size?")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftAligned(new JLabel("The lower this is, the closer your GIF")));
		leftPanel.add(wrapLeftAligned(new JLabel("will be to the maximum size.")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftAligned(new JLabel("However, the lower this is, the more likely")));
		leftPanel.add(wrapLeftAligned(new JLabel("the size will be something weird like 450x253.")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftAligned(new JLabel("The recommended acceptable size percent is 10 percent.")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(5));
		cutFramerateInHalfCheckBox = new JCheckBox("Cut Output Framerate in Half, to "
				+ String.format("%.2f", scan.getFramerate() * 0.5D));
		leftPanel.add(wrapLeftAligned(cutFramerateInHalfCheckBox));
		leftPanel.add(wrapLeftAligned(new JLabel("Cutting the framerate in half will allow the size to be bigger.")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(5));
		JPanel createGIFPanel = new JPanel(new BorderLayout());
		JButton fireButton = new JButton("Create GIF");
		createGIFPanel.add(fireButton, BorderLayout.CENTER);
		fireButton.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				FileDialog fileDialog = new FileDialog(MainFrame.getMainFrame(), "Save GIF as...", FileDialog.SAVE);
				fileDialog.setMultipleMode(false);
				
				if (mostRecentGIFDirectory != null) {
					fileDialog.setDirectory(mostRecentGIFDirectory);
				}
				
				fileDialog.setFilenameFilter(new FilenameFilter(){
					
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".gif");
					}
					
				});
				
				fileDialog.setVisible(true);
				final String filename = fileDialog.getFile();
				
				if (filename != null) {
					File recentGIFFile = new File(FFmpegManager.getFFmpegManager().getLocalAppDataLocation(),
							"recent_gif.txt");
					mostRecentGIFDirectory = fileDialog.getDirectory();
					try (FileWriter recentGIFWriter = new FileWriter(recentGIFFile)) {
						recentGIFWriter.write(mostRecentGIFDirectory);
					} catch (IOException ioe) {
						// we don't care much if this fails
						// but knowing on standard error is nice
						ioe.printStackTrace();
					}
					createGIF(new File(mostRecentGIFDirectory, filename).getAbsolutePath());
				}
				
			}
		});
		createGIFPanel.setMaximumSize(new Dimension(480, 30));
		leftPanel.add(createGIFPanel);
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(wrapLeftAligned(new JLabel("Status:")));
		leftPanel.add(Box.createVerticalStrut(5));
		JScrollPane scrollPane = new JScrollPane();
		statusArea = new StatusProcessorArea();
		JPanel scrollPanePanel = new JPanel(new BorderLayout());
		scrollPane.setViewportView(statusArea);
		scrollPanePanel.add(scrollPane, BorderLayout.CENTER);
		leftPanel.add(scrollPanePanel);
		File recentGIFFile = new File(FFmpegManager.getFFmpegManager().getLocalAppDataLocation(), "recent_gif.txt");
		if (recentGIFFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(recentGIFFile))) {
				mostRecentGIFDirectory = br.readLine();
			} catch (IOException ioe) {
				mostRecentGIFDirectory = null;
			}
		}
	}
	
	private void updateEndScreenshot() {
		new Thread(new Runnable(){
			
			@Override
			public void run() {
				previewImageEndPanel.setImage(scan.screenShot(endSlider.getValue() * 0.25D));
			}
		}).start();
	}
	
	private void updateStartScreenshot() {
		new Thread(new Runnable(){
			
			@Override
			public void run() {
				previewImageStartPanel.setImage(scan.screenShot(startSlider.getValue() * 0.25D));
			}
		}).start();
	}
	
	private Component wrapLeftAligned(Component comp) {
		Box box = Box.createHorizontalBox();
		box.add(comp);
		box.add(Box.createHorizontalGlue());
		return box;
	}
	
	private Component wrapLeftRightAligned(Component left, Component right) {
		Box box = Box.createHorizontalBox();
		box.add(left);
		box.add(Box.createHorizontalGlue());
		box.add(right);
		return box;
	}
}
