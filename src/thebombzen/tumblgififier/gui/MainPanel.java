package thebombzen.tumblgififier.gui;

import static thebombzen.tumblgififier.TumblGIFifier.wrapLeftAligned;
import static thebombzen.tumblgififier.TumblGIFifier.wrapLeftRightAligned;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.processor.VideoProcessor;
import thebombzen.tumblgififier.util.ExtrasManager;
import thebombzen.tumblgififier.util.ProcessTerminatedException;
import thebombzen.tumblgififier.util.StatusProcessor;

public class MainPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private JCheckBox cutFramerateInHalfCheckBox;
	private JSlider endSlider;
	private JPanel leftPanel;
	private int maxSize = 2000;
	private JCheckBox maxSizeCheckBox;
	private JTextField maxSizeTextField;
	private String mostRecentGIFDirectory = null;
	private JButton playButtonFast;

	private JButton playButtonSlow;

	private ImagePanel previewImageEndPanel;
	private ImagePanel previewImageStartPanel;
	private VideoProcessor scan;
	private JSlider startSlider;
	private StatusProcessorArea statusArea;
	private JButton fireButton = new JButton("Create GIF");
	
	private List<Component> onDisable = new ArrayList<>();
	private JTextField overlayTextField;
	private JTextField overlayTextSizeField;
	private int textSize = 96;
	
	private String currentText = "";
	
	public List<Component> getOnDisable() {
		return onDisable;
	}

	public MainPanel(VideoProcessor scan) {
		this.scan = scan;
		if (scan == null) {
			throw new NullPointerException();
		}
		setupLayout();
		TumblGIFifier.getThreadPool().scheduleWithFixedDelay(new Runnable(){
			public void run(){
				EventQueue.invokeLater(new Runnable(){
					public void run(){
						boolean update = !currentText.equals(overlayTextField.getText());
						currentText = overlayTextField.getText();
						if (update){
							updateStartScreenshot();
							updateEndScreenshot();
						}
					}
				});
			}
		}, 0, 1000, TimeUnit.MILLISECONDS); // 1,000,000 nanoseconds is 1 milliseconds
	}
	
	private void createGIF(final String path) {
		final int maxSizeBytes;
		final int minSizeBytes;
		if (maxSizeCheckBox.isSelected()) {
			maxSizeBytes = 1000 * maxSize;
			minSizeBytes = 1000 * (maxSize * 19 / 20);
		} else {
			maxSizeBytes = Integer.MAX_VALUE;
			minSizeBytes = 0;
		}
		final boolean halveFramerate = cutFramerateInHalfCheckBox.isSelected();
		final double clipStart = startSlider.getValue() * 0.25D;
		final double clipEnd = endSlider.getValue() * 0.25D;
		TumblGIFifier.getThreadPool().submit(new Runnable(){
			
			@Override
			public void run() {
				boolean success = scan.convert(overlayTextField.getText(), statusArea, path, clipStart, clipEnd, minSizeBytes, maxSizeBytes,
						halveFramerate, textSize);
				MainFrame.getMainFrame().setBusy(false);
				if (success) {
					statusArea.appendStatus("Done!");
					//JOptionPane.showMessageDialog(MainPanel.this, "Done!", "Success!", JOptionPane.INFORMATION_MESSAGE);
				} else {
					statusArea.appendStatus("Some error occured :(");
					//JOptionPane.showMessageDialog(MainPanel.this, "Some error occured :(", "Error",
						//	JOptionPane.ERROR_MESSAGE);
				}
			}
		});
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
		final String ffplay = ExtrasManager.getExtrasManager().getFFplayLocation();
		final String overlay = overlayTextField.getText();
		TumblGIFifier.getThreadPool().submit(new Runnable(){
			@Override
			public void run() {
				try {
					String scale;
					if (scan.getHeight() > 270){
						scale = "scale=-1:270";
					} else {
						scale = "scale";
					}
					
					TumblGIFifier.exec(true, ffplay, "-loop", "0", "-an", "-sn", "-vst", "0:v", scan.getLocation(), "-ss",
							Double.toString(clipStart), "-t", Double.toString(clipEnd - clipStart), "-vf", overlay.length() == 0 ? scale : scale + ", " + TumblGIFifier.createDrawTextString(scan.getHeight() > 270 ? scan.getWidth() * 270 / scan.getHeight() : scan.getWidth(), scan.getHeight() > 270 ? 270 : scan.getHeight(), textSize, overlay));
				} catch (ProcessTerminatedException ex){
					return;
				} finally {
					EventQueue.invokeLater(new Runnable(){
						@Override
						public void run() {
							MainFrame.getMainFrame().setBusy(false);
						}
					});
				}
			}
		});
	}
	
	private void playClipSlow() {
		
		MainFrame.getMainFrame().setBusy(true);
		
		final double clipStart = startSlider.getValue() * 0.25D;
		final double clipEnd = endSlider.getValue() * 0.25D;
		
		final String ffmpeg = ExtrasManager.getExtrasManager().getFFmpegLocation();
		final String ffplay = ExtrasManager.getExtrasManager().getFFplayLocation();
		final String overlay = overlayTextField.getText();
		
		TumblGIFifier.getThreadPool().submit(new Runnable(){
			@Override
			public void run() {
				File tempFile = null;
				try {
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
					if (scan.getHeight() > 270){
						scale = "scale=-1:270";
					} else {
						scale = "scale";
					}
					TumblGIFifier.exec(true, ffmpeg, "-y", "-ss", Double.toString(clipStart), "-i",
							scan.getLocation(), "-map", "0:v", "-t", Double.toString(clipEnd - clipStart), "-pix_fmt",
							"yuv420p", "-vf", overlay.length() == 0 ? scale : scale + ", " + TumblGIFifier.createDrawTextString(scan.getHeight() > 270 ? scan.getWidth() * 270 / scan.getHeight() : scan.getWidth(), scan.getHeight() > 270 ? 270 : scan.getHeight(), textSize, overlay), "-c",
							"ffv1", "-f", "matroska", tempFile.getAbsolutePath());
					TumblGIFifier.exec(true, ffplay, "-loop", "0", tempFile.getAbsolutePath());
				} catch (ProcessTerminatedException ex) {
					statusArea.appendStatus("Error rendering clip :(");
					TumblGIFifier.stopAll();
					return;
				} finally {
					if (tempFile != null){
						tempFile.delete();
					}
					EventQueue.invokeLater(new Runnable(){
						@Override
						public void run() {
							MainFrame.getMainFrame().setBusy(false);
						}
					});
				}
			}
		});
		
	}
	
	/**
	 * Execute this on the Event Dispatch thread
	 */
	private void fire(){
		
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
			File recentGIFFile = new File(ExtrasManager.getExtrasManager().getLocalAppDataLocation(),
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
	
	private void setupLayout() {
		BufferedImage previewImageStart = scan.screenShot("", 1D / 3D * scan.getDuration(), textSize);
		BufferedImage previewImageEnd = scan.screenShot("", 2D / 3D * scan.getDuration(), textSize);
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
		
		onDisable.add(startSlider);
		
		playButtonFast = new JButton("Play Clip (Fast, Inaccurate)");
		playButtonFast.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				playClipFast();
			}
		});
		
		onDisable.add(playButtonFast);
		
		playButtonSlow = new JButton("Play Clip (Slow, Accurate)");
		playButtonSlow.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				playClipSlow();
			}
		});
		
		onDisable.add(playButtonSlow);
		
		JPanel playButtonPanel = new JPanel(new BorderLayout());
		playButtonPanel.add(playButtonFast, BorderLayout.WEST);
		playButtonPanel.add(playButtonSlow, BorderLayout.EAST);
		
		rightBox.add(Box.createVerticalStrut(10));
		rightBox.add(playButtonPanel);
		rightBox.add(Box.createVerticalStrut(10));
		
		endSlider = new JSlider();
		endSlider.setMinimum(0);
		endSlider.setMaximum((int) (scan.getDuration() * 4D) - 1);
		endSlider.setValue(endSlider.getMaximum() * 2 / 3);
		rightBox.add(endSlider);
			
		onDisable.add(endSlider);
		
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
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(20));
		maxSizeTextField = new JTextField("2000");
		maxSizeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		maxSizeTextField.setMaximumSize(new Dimension(200, 25));
		maxSizeTextField.setPreferredSize(new Dimension(200, 25));
		maxSizeTextField.addFocusListener(new FocusAdapter(){
			
			@Override
			public void focusLost(FocusEvent e) {
				try {
					int size = Integer.parseInt(maxSizeTextField.getText());
					if (size >= 1) {
						maxSize = size;
					} else {
						maxSizeTextField.setText(Integer.toString(maxSize));
					}
				} catch (NumberFormatException nfe) {
					maxSizeTextField.setText(Integer.toString(maxSize));
				}
			}
		});
		
		onDisable.add(maxSizeTextField);
		
		maxSizeCheckBox = new JCheckBox("Maximum GIF Size in Kilobytes: ");
		maxSizeCheckBox.setSelected(true);
		
		onDisable.add(maxSizeCheckBox);
		
		leftPanel.add(wrapLeftRightAligned(maxSizeCheckBox, maxSizeTextField));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftAligned(new JLabel("The maximum size on Tumblr is 2000 Kilobytes.")));		
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(20));
		cutFramerateInHalfCheckBox = new JCheckBox("Cut Output Framerate in Half, to "
				+ String.format("%.2f", scan.getFramerate() * 0.5D));
		leftPanel.add(wrapLeftAligned(cutFramerateInHalfCheckBox));
		leftPanel.add(wrapLeftAligned(new JLabel("Halving the framerate will increase the physical size of the GIF.")));
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(20));
		overlayTextField = new JTextField();
		overlayTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		overlayTextField.setPreferredSize(new Dimension(200, 25));
		overlayTextField.setMaximumSize(new Dimension(200, 25));
		onDisable.add(overlayTextField);
		overlayTextSizeField = new JTextField();
		overlayTextSizeField.setHorizontalAlignment(SwingConstants.RIGHT);
		overlayTextSizeField.setPreferredSize(new Dimension(200, 25));
		overlayTextSizeField.setMaximumSize(new Dimension(200, 25));
		overlayTextSizeField.setText("96");
		onDisable.add(overlayTextSizeField);
		
		overlayTextSizeField.addFocusListener(new FocusAdapter(){
			
			@Override
			public void focusLost(FocusEvent e) {
				try {
					int size = Integer.parseInt(overlayTextSizeField.getText());
					if (size >= 1) {
						textSize = size;
						updateStartScreenshot();
						updateEndScreenshot();
					} else {
						overlayTextSizeField.setText(Integer.toString(textSize));
					}
				} catch (NumberFormatException nfe) {
					overlayTextSizeField.setText(Integer.toString(textSize));
				}
			}
		});
		
		leftPanel.add(wrapLeftRightAligned(new JLabel("Overlay text:"), overlayTextField));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(wrapLeftRightAligned(new JLabel("Overlay text size:"), overlayTextSizeField));
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(20));
		onDisable.add(cutFramerateInHalfCheckBox);
		
		JPanel createGIFPanel = new JPanel(new BorderLayout());
		createGIFPanel.add(fireButton, BorderLayout.CENTER);
		fireButton.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (fireButton.getText().equals("STOP")){
					TumblGIFifier.stopAll();
					MainFrame.getMainFrame().setBusy(false);
					return;
				}
				
				fire();
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
		File recentGIFFile = new File(ExtrasManager.getExtrasManager().getLocalAppDataLocation(), "recent_gif.txt");
		if (recentGIFFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(recentGIFFile))) {
				mostRecentGIFDirectory = br.readLine();
			} catch (IOException ioe) {
				mostRecentGIFDirectory = null;
			}
		}
	}
	
	private void updateEndScreenshot() {
		TumblGIFifier.getThreadPool().submit(new Runnable(){
			@Override
			public void run() {
				previewImageEndPanel.setImage(scan.screenShot(currentText, endSlider.getValue() * 0.25D, textSize));
			}
		});
	}
	
	private void updateStartScreenshot() {
		TumblGIFifier.getThreadPool().submit(new Runnable(){
			@Override
			public void run() {
				previewImageStartPanel.setImage(scan.screenShot(currentText, startSlider.getValue() * 0.25D, textSize));
			}
		});
	}

	public JButton getFireButton() {
		return fireButton;
	}
}
