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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import thebombzen.tumblgififier.ConcurrenceManager;
import thebombzen.tumblgififier.Tuple;
import thebombzen.tumblgififier.io.IOHelper;
import thebombzen.tumblgififier.io.resources.ProcessTerminatedException;
import thebombzen.tumblgififier.io.resources.ResourceLocation;
import thebombzen.tumblgififier.io.resources.ResourcesManager;
import thebombzen.tumblgififier.text.StatusProcessor;
import thebombzen.tumblgififier.text.StatusProcessorArea;
import thebombzen.tumblgififier.text.TextHelper;
import thebombzen.tumblgififier.video.ShotCache;
import thebombzen.tumblgififier.video.VideoProcessor;
import thebombzen.tumblgififier.video.VideoScan;

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
	private VideoProcessor videoProcessor;
	private VideoScan scan;
	private JSlider startSlider;
	private StatusProcessorArea statusArea;
	private JButton fireButton = new JButton("Create GIF");
	
	private List<Component> onDisable = new ArrayList<>();
	private JTextField overlayTextField;
	private JTextField overlayTextSizeField;
	private int textSize = 96;
	
	private String currentText = "";
	private Map<Tuple<String, Integer>, ShotCache> startCacheMap = new HashMap<>();
	private Map<Tuple<String, Integer>, ShotCache> endCacheMap = new HashMap<>();
	
	public List<Component> getOnDisable() {
		return onDisable;
	}
	
	public MainPanel(final VideoScan videoScan) {
		if (videoScan == null) {
			throw new NullPointerException();
		}
		this.scan = videoScan;
		this.videoProcessor = new VideoProcessor(videoScan);
		this.startCacheMap.put(new Tuple<String, Integer>("", 96), new ShotCache(scan));
		this.endCacheMap.put(new Tuple<String, Integer>("", 96), new ShotCache(scan));
		setupLayout();
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			@Override
			public void run(){
				updateStartScreenshot();
				updateEndScreenshot();
			}
		});

		ConcurrenceManager.getConcurrenceManager().createImpreciseTickClock(new Runnable(){
			
			@Override
			public void run() {
				EventQueue.invokeLater(new Runnable(){
					
					@Override
					public void run() {
						int newTextSize = textSize;
						try {
							newTextSize = Integer.parseInt(overlayTextSizeField.getText());
						} catch (NumberFormatException e){
							// nothing
						}
						boolean update = !currentText.equals(overlayTextField.getText()) || newTextSize != textSize;
						currentText = overlayTextField.getText();
						textSize = newTextSize;
						if (update) {
							Tuple<String, Integer> tuple = new Tuple<>(currentText, textSize);
							if (startCacheMap.get(tuple) == null){
								startCacheMap.put(tuple, new ShotCache(scan));
							}
							if (endCacheMap.get(tuple) == null){
								endCacheMap.put(tuple, new ShotCache(scan));
							}
							updateStartScreenshot();
							updateEndScreenshot();
						}
					}
				});
			}
		}, 2500, TimeUnit.MILLISECONDS);
		
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
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			
			@Override
			public void run() {
				boolean success = videoProcessor.convert(overlayTextField.getText(), statusArea, path, clipStart, clipEnd,
						minSizeBytes, maxSizeBytes, halveFramerate, textSize);
				MainFrame.getMainFrame().setBusy(false);
				if (success) {
					statusArea.appendStatus("Done!");
					// JOptionPane.showMessageDialog(MainPanel.this, "Done!",
					// "Success!", JOptionPane.INFORMATION_MESSAGE);
				} else {
					statusArea.appendStatus("Some error occured :(");
					// JOptionPane.showMessageDialog(MainPanel.this, "Some error
					// occured :(", "Error",
					// JOptionPane.ERROR_MESSAGE);
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
		
		final boolean shouldHalfFramerate = this.cutFramerateInHalfCheckBox.isSelected();
		
		final double clipStart = startSlider.getValue() * 0.25D;
		final double clipEnd = endSlider.getValue() * 0.25D;
		final ResourceLocation ffplay = ResourcesManager.getResourcesManager().getFFplayLocation();
		final String overlay = overlayTextField.getText();
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			
			@Override
			public void run() {
				try {
					String videoFilter = TextHelper.getTextHelper().createVideoFilter("format=yuv420p", null, -1, scan.getHeight() > 270 ? 270 : -1, true, shouldHalfFramerate ? 1 : 0, scan.getWidth(), scan.getHeight(), textSize, overlay);
					ConcurrenceManager.getConcurrenceManager().exec(true, ffplay.toString(), "-loop", "0", "-an", "-sn", "-vst", "0:v",
							scan.getLocation(), "-ss", Double.toString(clipStart), "-t",
							Double.toString(clipEnd - clipStart), "-vf", videoFilter);
				} catch (ProcessTerminatedException ex) {
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
		final boolean shouldHalfFramerate = this.cutFramerateInHalfCheckBox.isSelected();
		
		final ResourceLocation ffmpeg = ResourcesManager.getResourcesManager().getFFmpegLocation();
		final ResourceLocation ffplay = ResourcesManager.getResourcesManager().getFFplayLocation();
		final String overlay = overlayTextField.getText();
		
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			
			@Override
			public void run() {
				File tempFile = null;
				try {
					statusArea.appendStatus("Rendering Clip... ");
					try {
						tempFile = IOHelper.createTempFile();
					} catch (IOException ioe) {
						ioe.printStackTrace();
						statusArea.appendStatus("Error rendering clip :(");
						return;
					}
					String videoFilter = TextHelper.getTextHelper().createVideoFilter("format=yuv420p", null, -1, scan.getHeight() > 270 ? 270 : -1, true, shouldHalfFramerate ? 1 : 0, scan.getWidth(), scan.getHeight(), textSize, overlay);
					ConcurrenceManager.getConcurrenceManager().exec(true, ffmpeg.toString(), "-y", "-ss", Double.toString(clipStart), "-i",
							scan.getLocation(), "-map", "0:v", "-t", Double.toString(clipEnd - clipStart), "-vf", videoFilter,
							"-c", "ffv1", "-f", "matroska", tempFile.getAbsolutePath());
					ConcurrenceManager.getConcurrenceManager().exec(true, ffplay.toString(), "-loop", "0", tempFile.getAbsolutePath());
				} catch (ProcessTerminatedException ex) {
					statusArea.appendStatus("Error rendering clip :(");
					ConcurrenceManager.getConcurrenceManager().stopAll();
					return;
				} finally {
					IOHelper.deleteTempFile(tempFile);
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
	private void fire() {
		
		if (maxSizeCheckBox.isSelected()) {
			final int maxSizeBytes = 1000 * maxSize;
			final boolean halveFramerate = cutFramerateInHalfCheckBox.isSelected();
			final double clipStart = startSlider.getValue() * 0.25D;
			final double clipEnd = endSlider.getValue() * 0.25D;
			double newWidth = scan.getWidth() / Math.sqrt(scan.getWidth() * scan.getHeight() * scan.getFramerate()
					* (halveFramerate ? 0.5D : 1D) * (clipEnd - clipStart) / (2D * maxSizeBytes));
			if (newWidth < 300D) {
				int dialogResult = JOptionPane.showConfirmDialog(this,
						String.format(
								"This GIF will probably be less than 300 pixels wide, which means Tumblr won't expand it to fit the window. Is this okay?%n(If not then you should drag the sliders on the right to decrease the duration.)"),
						"Warning", JOptionPane.OK_CANCEL_OPTION);
				if (dialogResult == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
		}
		
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
		String filename = fileDialog.getFile();
		
		if (filename != null) {
			if (!filename.toLowerCase().endsWith(".gif")) {
				filename += ".gif";
			}
			File recentGIFFile = ResourcesManager.getResourcesManager().getLocalResource("recent_gif.txt");
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
		previewImageStartPanel = new ImagePanel(null);
		previewImageStartPanel.setPreferredSize(new Dimension(480, 270));
		rightBox.add(previewImageStartPanel);
		rightBox.add(Box.createVerticalStrut(10));
		startSlider = new JSlider();
		startSlider.setMinimum(0);
		startSlider.setMaximum((int) (scan.getDuration() * 4D));
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
		endSlider.setMaximum((int) (scan.getDuration() * 4D));
		endSlider.setValue(endSlider.getMaximum() * 2 / 3);
		rightBox.add(endSlider);
		
		onDisable.add(endSlider);
		
		startSlider.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (startSlider.getValue() > endSlider.getValue()) {
					startSlider.setValue(endSlider.getValue());
				}
				if (!startSlider.getValueIsAdjusting() && videoProcessor != null) {
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
				if (!endSlider.getValueIsAdjusting() && videoProcessor != null) {
					updateEndScreenshot();
				}
			}
		});
		
		rightBox.add(Box.createVerticalStrut(10));
		previewImageEndPanel = new ImagePanel(null);
		previewImageEndPanel.setPreferredSize(new Dimension(480, 270));
		rightBox.add(previewImageEndPanel);
		horizontalBox.add(rightBox);
		horizontalBox.add(Box.createHorizontalStrut(10));
		
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("Video Stats")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftRightAligned(new JLabel("Width:"), new JLabel(Integer.toString(scan.getWidth()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftRightAligned(new JLabel("Height:"), new JLabel(Integer.toString(scan.getHeight()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(
				GUIHelper.wrapLeftRightAligned(new JLabel("Duration:"), new JLabel(String.format("%.2f", scan.getDuration()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(
				GUIHelper.wrapLeftRightAligned(new JLabel("Framerate:"), new JLabel(String.format("%.2f", scan.getFramerate()))));
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
		
		leftPanel.add(GUIHelper.wrapLeftRightAligned(maxSizeCheckBox, maxSizeTextField));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("The maximum size on Tumblr is 2000 Kilobytes.")));
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(20));
		cutFramerateInHalfCheckBox = new JCheckBox(
				"Cut Output Framerate in Half, to " + String.format("%.2f", scan.getFramerate() * 0.5D));
		leftPanel.add(GUIHelper.wrapLeftAligned(cutFramerateInHalfCheckBox));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("Halving the framerate will increase the physical size of the GIF.")));
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
		
		leftPanel.add(GUIHelper.wrapLeftRightAligned(new JLabel("Overlay text:"), overlayTextField));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftRightAligned(new JLabel("Overlay text size:"), overlayTextSizeField));
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(20));
		onDisable.add(cutFramerateInHalfCheckBox);
		
		JPanel createGIFPanel = new JPanel(new BorderLayout());
		createGIFPanel.add(fireButton, BorderLayout.CENTER);
		fireButton.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (fireButton.getText().equals("STOP")) {
					ConcurrenceManager.getConcurrenceManager().stopAll();
					MainFrame.getMainFrame().setBusy(false);
					return;
				}
				
				fire();
			}
		});
		createGIFPanel.setMaximumSize(new Dimension(480, 30));
		leftPanel.add(createGIFPanel);
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("Status:")));
		leftPanel.add(Box.createVerticalStrut(5));
		JScrollPane scrollPane = new JScrollPane();
		statusArea = new StatusProcessorArea();
		JPanel scrollPanePanel = new JPanel(new BorderLayout());
		scrollPane.setViewportView(statusArea);
		scrollPanePanel.add(scrollPane, BorderLayout.CENTER);
		leftPanel.add(scrollPanePanel);
		File recentGIFFile = ResourcesManager.getResourcesManager().getLocalResource("recent_gif.txt");
		if (recentGIFFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(recentGIFFile))) {
				mostRecentGIFDirectory = br.readLine();
			} catch (IOException ioe) {
				mostRecentGIFDirectory = null;
			}
		}
	}
	
	private void updateEndScreenshot() {
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			@Override
			public void run() {
				try {
					Future<BufferedImage> future = endCacheMap.get(new Tuple<>(currentText, textSize)).screenShot(getStatusProcessor(), currentText, endSlider.getValue(), 480, 270, textSize, true);
					while (!future.isDone()){
						EventQueue.invokeLater(new Runnable(){
							@Override
							public void run(){
								endSlider.setEnabled(false);
							}
						});
						previewImageEndPanel.setImage(null);
						Thread.sleep(10);
					}
					previewImageEndPanel.setImage(future.get());
					EventQueue.invokeLater(new Runnable(){
						@Override
						public void run(){
							endSlider.setEnabled(true);
							endSlider.requestFocusInWindow();
						}
					});
				} catch (InterruptedException | ExecutionException ex) {
					getStatusProcessor().processException(ex);
				}
			}
		});
	}
	
	private void updateStartScreenshot() {
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			@Override
			public void run() {
				try {
					Future<BufferedImage> future = startCacheMap.get(new Tuple<>(currentText, textSize)).screenShot(getStatusProcessor(), currentText, startSlider.getValue(), 480, 270, textSize, false);
					while (!future.isDone()){
						EventQueue.invokeLater(new Runnable(){
							@Override
							public void run(){
								startSlider.setEnabled(false);
							}
						});
						previewImageStartPanel.setImage(null);
						Thread.sleep(10);
					}
					previewImageStartPanel.setImage(future.get());
					EventQueue.invokeLater(new Runnable(){
						@Override
						public void run(){
							startSlider.setEnabled(true);
							startSlider.requestFocusInWindow();
						}
					});
				} catch (InterruptedException | ExecutionException ex) {
					getStatusProcessor().processException(ex);
				}
			}
		});
	}
	
	public JButton getFireButton() {
		return fireButton;
	}
}
