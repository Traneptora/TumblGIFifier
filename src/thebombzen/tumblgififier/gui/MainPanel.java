package thebombzen.tumblgififier.gui;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.Tuple;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.RuntimeIOException;
import thebombzen.tumblgififier.util.io.resources.ProcessTerminatedException;
import thebombzen.tumblgififier.util.io.resources.Resource;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;
import thebombzen.tumblgififier.util.text.StatusProcessorArea;
import thebombzen.tumblgififier.util.text.TextHelper;
import thebombzen.tumblgififier.video.ShotCache;
import thebombzen.tumblgififier.video.VideoProcessor;
import thebombzen.tumblgififier.video.VideoScan;

public class MainPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private JComboBox<FramerateDecimator> framerateDecimatorComboBox;
	private JPanel leftPanel;
	private int targetSize = 2000;
	private int targetWidth = 540;
	private int targetHeight = 300;
	private JComboBox<TargetSize> targetSizeComboBox;
	private JTextField targetSizeTextField;
	private String mostRecentGIFDirectory = null;
	
	private JButton playButtonSlow;
	
	private ImagePanel previewImageEndPanel;
	private ImagePanel previewImageStartPanel;

	private VideoProcessor videoProcessor;
	private VideoScan scan;

	private JSlider startSlider;
	private JSlider endSlider;
	
	private JLabel startLabel;
	private JLabel endLabel;

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
		updateStartScreenshot();
		updateEndScreenshot();

		if (ResourcesManager.loadedPkgs.contains("OpenSans")){
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
	}
	
	private void createGIF(final Path path) {
		final int maxSizeBytes;
		final int minSizeBytes;
		final int targetWidth;
		final int targetHeight;

		if (TargetSize.FILESIZE.equals(targetSizeComboBox.getSelectedItem())) {
			// Intentionally not 1024
			maxSizeBytes = 1000 * targetSize;
			minSizeBytes = 1000 * (targetSize * 19 / 20);
			targetWidth = -1;
			targetHeight = -1;
		} else if (TargetSize.SCALE_W.equals(targetSizeComboBox.getSelectedItem())) {
			targetWidth = this.targetWidth;
			targetHeight = -1;
			maxSizeBytes = Integer.MAX_VALUE;
			minSizeBytes = 0;
		} else { // SCALE_H
			targetWidth = -1;
			targetHeight = this.targetHeight;
			maxSizeBytes = Integer.MAX_VALUE;
			minSizeBytes = 0;
		}

		final int decimator = ((FramerateDecimator)framerateDecimatorComboBox.getSelectedItem()).decimator;
		final double clipStart = startSlider.getValue() * scan.getScreenshotDuration();
		final double clipEnd = endSlider.getValue() * scan.getScreenshotDuration();
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			
			@Override
			public void run() {
				boolean success = videoProcessor.convert(overlayTextField.getText(), statusArea, path, clipStart, clipEnd,
						minSizeBytes, maxSizeBytes, targetWidth, targetHeight, decimator, textSize);
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
	
	private void playClipSlow() {
		
		MainFrame.getMainFrame().setBusy(true);
		
		final double clipStart = startSlider.getValue() * scan.getScreenshotDuration();
		final double clipEnd = endSlider.getValue() * scan.getScreenshotDuration();
		final int decimator = ((FramerateDecimator)framerateDecimatorComboBox.getSelectedItem()).decimator;
		
		final String overlay = overlayTextField.getText();
		final Resource mpv = ResourcesManager.getResourcesManager().getMpvLocation();
		
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			
			@Override
			public void run() {
				Path tempFile = null;
				try {
					statusArea.appendStatus("Rendering Clip... ");
					try {
						tempFile = IOHelper.createTempFile();
					} catch (RuntimeIOException ioe) {
						log(ioe);
						statusArea.appendStatus("Error rendering clip :(");
						return;
					}
					String videoFilter = TextHelper.getTextHelper().createVideoFilter(null, null, -1, -1, true, decimator, scan.getWidth(), scan.getHeight(), textSize, overlay);
					ConcurrenceManager.getConcurrenceManager().exec(true, mpv.getLocation().toString(),
							"--config=no", "--msg-level=all=v", "--msg-color=no",
							"--log-file=" + ResourcesManager.getResourcesManager().getLocalFile("mpv.log"),
							"--term-osd=force", "--video-osd=no", "--term-status-msg=", "--term-osd-bar=no",
							"--title=TumblGIFifier Preview", "--force-window=yes", "--taskbar-progress=no",
							"--ontop=yes", "--autofit-larger=480x270", "--cursor-autohide=no", "--input-terminal=no",
							"--input-cursor=no", "--dscale=bicubic_fast", "--cscale=bicubic_fast",
							"--hwdec=auto", "--hwdec-codecs=hevc,vp9", "--input-default-bindings=no",
							"--loop-playlist=inf", "--osc=no", "--aid=no", "--sid=no", "--hr-seek=yes",
							"--lavfi-complex=[vid1]" + videoFilter + "[vo]", scan.getLocation().toString(),
							"--start=" + clipStart, "--end=" + clipEnd
							);
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
		
		if (TargetSize.FILESIZE.equals(targetSizeComboBox.getSelectedItem())) {
			final int maxSizeBytes = 1000 * targetSize;
			final int decimator = ((FramerateDecimator)framerateDecimatorComboBox.getSelectedItem()).decimator;
			final double clipStart = startSlider.getValue() * scan.getScreenshotDuration();
			final double clipEnd = endSlider.getValue() * scan.getScreenshotDuration();
			double widthGuess = scan.getWidth() / Math.sqrt(scan.getWidth() * scan.getHeight() * scan.getFramerate()
					/ (1D + decimator) * (clipEnd - clipStart) / (2D * maxSizeBytes));
			if (widthGuess < 300D) {
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
			Path recentGIFFile = ResourcesManager.getResourcesManager().getLocalFile("recent_gif.txt");
			mostRecentGIFDirectory = fileDialog.getDirectory();
			try (Writer recentGIFWriter = Files.newBufferedWriter(recentGIFFile)) {
				recentGIFWriter.write(mostRecentGIFDirectory);
			} catch (IOException ioe) {
				// we don't care much if this fails
				// but knowing on standard error is nice
				log(ioe);
			}
			createGIF(Paths.get(mostRecentGIFDirectory, filename).toAbsolutePath());
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
		previewImageStartPanel = new ImagePanel(null, new Consumer<Void>(){
			@Override
			public void accept(Void t) {
				int value = startSlider.getValue();
				if (value < endSlider.getValue()) {
					startSlider.setValue(value + 1);
				} else {
					previewImageStartPanel.stop();
				}
			}
		});
		previewImageStartPanel.setPreferredSize(new Dimension(480, 270));
		rightBox.add(previewImageStartPanel);
		rightBox.add(Box.createVerticalStrut(10));
		startSlider = new JSlider();
		//BoundedRangeModel startSliderModel = new BoundedRangeModel();
		//startSliderModel.
		startSlider.setMinimum(0);
		startSlider.setMaximum((int) (scan.getDuration() * scan.getScreenshotsPerSecond()));
		startSlider.setValue(startSlider.getMaximum() / 3);
		rightBox.add(startSlider);
		
		onDisable.add(startSlider);
		
		playButtonSlow = new JButton("Preview Clip");
		
		playButtonSlow.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				playClipSlow();
			}
		});
		onDisable.add(playButtonSlow);

		startLabel = new JLabel("Start: " + TextHelper.getTimeDurationFromSeconds(startSlider.getValue() * scan.getScreenshotDuration()));
	
		endSlider = new JSlider();
		endSlider.setMinimum(0);
		endSlider.setMaximum((int) (scan.getDuration() * scan.getScreenshotsPerSecond()));
		endSlider.setValue(endSlider.getMaximum() * 2 / 3);
		
		endLabel = new JLabel("End: " + TextHelper.getTimeDurationFromSeconds(endSlider.getValue() * scan.getScreenshotDuration()));
		
		Box playButtonBox = Box.createHorizontalBox();
		playButtonBox.add(Box.createHorizontalStrut(10));
		playButtonBox.add(startLabel);
		playButtonBox.add(Box.createHorizontalGlue());
		playButtonBox.add(playButtonSlow);
		playButtonBox.add(Box.createHorizontalGlue());
		playButtonBox.add(endLabel);
		playButtonBox.add(Box.createHorizontalStrut(10));
		
		rightBox.add(Box.createVerticalStrut(10));
		rightBox.add(playButtonBox);
		rightBox.add(Box.createVerticalStrut(10));
		
		rightBox.add(endSlider);
		
		onDisable.add(endSlider);
		
		startSlider.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (startSlider.getValue() > endSlider.getValue()) {
					startSlider.setValue(endSlider.getValue());
				}
				startLabel.setText("Start: " + TextHelper.getTimeDurationFromSeconds(startSlider.getValue() * scan.getScreenshotDuration()));
				if (!startSlider.getValueIsAdjusting()) {
					if (videoProcessor != null){
						updateStartScreenshot();
					}
				} else {
					previewImageStartPanel.stop();
				}
				
			}
		});
		
		endSlider.addChangeListener(new ChangeListener(){
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (endSlider.getValue() < startSlider.getValue()) {
					endSlider.setValue(startSlider.getValue());
				}
				endLabel.setText("End: " + TextHelper.getTimeDurationFromSeconds(endSlider.getValue() * scan.getScreenshotDuration()));
				if (!endSlider.getValueIsAdjusting()) {
					if (videoProcessor != null){
						updateEndScreenshot();
					}
				} else {
					previewImageEndPanel.stop();
				}
			}
		});
		
		rightBox.add(Box.createVerticalStrut(10));
		previewImageEndPanel = new ImagePanel(null, new Consumer<Void>(){
			@Override
			public void accept(Void t) {
				int value = endSlider.getValue();
				if (value < endSlider.getMaximum()) {
					endSlider.setValue(value + 1);
				} else {
					previewImageEndPanel.stop();
				}
			}
		});
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
				GUIHelper.wrapLeftRightAligned(new JLabel("Duration:"), new JLabel(TextHelper.getTimeDurationFromSeconds(scan.getDuration()))));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(
				GUIHelper.wrapLeftRightAligned(new JLabel("Framerate:"), new JLabel(String.format("%.2f", scan.getFramerate()))));
		leftPanel.add(Box.createVerticalStrut(15));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(15));
		targetSizeTextField = new JTextField("2000");
		targetSizeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		targetSizeTextField.setMaximumSize(new Dimension(200, 25));
		targetSizeTextField.setPreferredSize(new Dimension(200, 25));
		targetSizeTextField.addFocusListener(new FocusAdapter(){
			@Override
			public void focusLost(FocusEvent e) {
				try {
					int size = Integer.parseInt(targetSizeTextField.getText());
					switch ((TargetSize)targetSizeComboBox.getSelectedItem()) {
					case FILESIZE:
						if (size >= 1) {
							targetSize = size;
						} else {
							targetSizeTextField.setText(Integer.toString(targetSize));
						}
						break;
					case SCALE_W:
						if (size >= 1) {
							targetWidth = size;
						} else {
							targetSizeTextField.setText(Integer.toString(targetWidth));
						}
						break;
					case SCALE_H:
						if (size >= 1) {
							targetHeight = size;
						} else {
							targetSizeTextField.setText(Integer.toString(targetHeight));
						}
						break;
					}
				} catch (NumberFormatException nfe) {
					switch ((TargetSize)targetSizeComboBox.getSelectedItem()) {
					case FILESIZE:
						targetSizeTextField.setText(Integer.toString(targetSize));
						break;
					case SCALE_W:
						targetSizeTextField.setText(Integer.toString(targetWidth));
						break;
					case SCALE_H:
						targetSizeTextField.setText(Integer.toString(targetHeight));
						break;
					}
				}
			}
		});

		onDisable.add(targetSizeTextField);

		targetSizeComboBox = new JComboBox<TargetSize>();
		DefaultComboBoxModel<TargetSize> targetSizeComboBoxModel = new DefaultComboBoxModel<>();
		EnumSet.allOf(TargetSize.class).stream().forEach(targetSizeComboBoxModel::addElement);
		targetSizeComboBox.setModel(targetSizeComboBoxModel);
		targetSizeComboBox.setSelectedItem(TargetSize.FILESIZE);

		targetSizeComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				switch ((TargetSize)targetSizeComboBox.getSelectedItem()) {
				case FILESIZE:
					targetSizeTextField.setText(Integer.toString(targetSize));
					break;
				case SCALE_W:
					targetSizeTextField.setText(Integer.toString(targetWidth));
					break;
				case SCALE_H:
					targetSizeTextField.setText(Integer.toString(targetHeight));
					break;
				}
			}
		});

		onDisable.add(targetSizeComboBox);
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("Choose size control method:")));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftRightAligned(targetSizeComboBox, targetSizeTextField));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("The maximum GIF filesize on Tumblr is 2000 Kilobytes.")));
		leftPanel.add(Box.createVerticalStrut(15));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(15));
		framerateDecimatorComboBox = new JComboBox<FramerateDecimator>();
		DefaultComboBoxModel<FramerateDecimator> framerateDecimatorComboBoxModel = new DefaultComboBoxModel<>();
		EnumSet.allOf(FramerateDecimator.class).stream().forEach(framerateDecimatorComboBoxModel::addElement);
		framerateDecimatorComboBox.setModel(framerateDecimatorComboBoxModel);
		framerateDecimatorComboBox.setSelectedItem(FramerateDecimator.HALF_RATE);
		leftPanel.add(GUIHelper.wrapLeftAligned(framerateDecimatorComboBox));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("Cutting the framerate will increase the width & height or")));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("decrease the filesize, depending on the mode selected above.")));
		leftPanel.add(Box.createVerticalStrut(15));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(15));
		overlayTextField = new JTextField();
		overlayTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		overlayTextField.setPreferredSize(new Dimension(200, 25));
		overlayTextField.setMaximumSize(new Dimension(200, 25));
		if (ResourcesManager.loadedPkgs.contains("OpenSans")){
			onDisable.add(overlayTextField);
		} else {
			overlayTextField.setText("No Open Sans. Disabled.");
			overlayTextField.setEnabled(false);
		}
		
		overlayTextSizeField = new JTextField();
		overlayTextSizeField.setHorizontalAlignment(SwingConstants.RIGHT);
		overlayTextSizeField.setPreferredSize(new Dimension(200, 25));
		overlayTextSizeField.setMaximumSize(new Dimension(200, 25));
		
		if (ResourcesManager.loadedPkgs.contains("OpenSans")){
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
		} else {
			overlayTextSizeField.setText("No Open Sans. Disabled.");
			overlayTextSizeField.setEnabled(false);
		}

		leftPanel.add(GUIHelper.wrapLeftRightAligned(new JLabel("Overlay text:"), overlayTextField));
		leftPanel.add(Box.createVerticalStrut(5));
		leftPanel.add(GUIHelper.wrapLeftRightAligned(new JLabel("Overlay text size:"), overlayTextSizeField));
		leftPanel.add(Box.createVerticalStrut(15));
		leftPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
		leftPanel.add(Box.createVerticalStrut(15));
		onDisable.add(framerateDecimatorComboBox);
		
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
		leftPanel.add(Box.createVerticalStrut(15));
		leftPanel.add(GUIHelper.wrapLeftAligned(new JLabel("Status:")));
		leftPanel.add(Box.createVerticalStrut(5));
		JScrollPane scrollPane = new JScrollPane();
		statusArea = new StatusProcessorArea();
		JPanel scrollPanePanel = new JPanel(new BorderLayout());
		scrollPane.setViewportView(statusArea);
		scrollPanePanel.add(scrollPane, BorderLayout.CENTER);
		leftPanel.add(scrollPanePanel);
		Path recentGIFFile = ResourcesManager.getResourcesManager().getLocalFile("recent_gif.txt");
		if (Files.exists(recentGIFFile)) {
			try {
				mostRecentGIFDirectory = IOHelper.getFirstLineOfFile(recentGIFFile);
			} catch (RuntimeIOException ioe) {
				mostRecentGIFDirectory = null;
			}
		}
	}
	
	/**
	 * This method may be executed from any thread asynchronously.
	 */
	private void updateEndScreenshot() {
		final Consumer<BufferedImage> callback = new Consumer<BufferedImage>(){
			public void accept(BufferedImage image){
				previewImageEndPanel.setImage(image);
				EventQueue.invokeLater(new Runnable(){
					@Override
					public void run(){
						endSlider.requestFocusInWindow();
						endSlider.setEnabled(true);
					}
				});
			}
		};
		EventQueue.invokeLater(new Runnable(){
			@Override
			public void run(){
				endSlider.requestFocusInWindow();
				endSlider.setEnabled(false);
			}
		});
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			public void run(){
				endCacheMap.get(new Tuple<>(currentText, textSize)).screenShot(callback, previewImageEndPanel, getStatusProcessor(), currentText, endSlider.getValue(), 480, 270, textSize, true);
			}
		});
	}
	
	/**
	 * This method may be executed from any thread asynchronously.
	 */
	private void updateStartScreenshot() {
		final Consumer<BufferedImage> callback = new Consumer<BufferedImage>(){
			public void accept(BufferedImage image){
				previewImageStartPanel.setImage(image);
				EventQueue.invokeLater(new Runnable(){
					@Override
					public void run(){
						startSlider.requestFocusInWindow();
						startSlider.setEnabled(true);
					}
				});
			}
		};
		EventQueue.invokeLater(new Runnable(){
			@Override
			public void run(){
				startSlider.requestFocusInWindow();
				startSlider.setEnabled(false);
			}
		});
		ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
			public void run(){
				startCacheMap.get(new Tuple<>(currentText, textSize)).screenShot(callback, previewImageStartPanel, getStatusProcessor(), currentText, startSlider.getValue(), 480, 270, textSize, false);
			}
		});
	}
	
	public JButton getFireButton() {
		return fireButton;
	}
}
