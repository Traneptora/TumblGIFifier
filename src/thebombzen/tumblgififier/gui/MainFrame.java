package thebombzen.tumblgififier.gui;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.DefaultTask;
import thebombzen.tumblgififier.util.DuplicateSingletonException;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;
import thebombzen.tumblgififier.util.text.StatusProcessor;
import thebombzen.tumblgififier.util.text.StatusProcessorArea;
import thebombzen.tumblgififier.video.VideoScan;

/**
 * This represents the main JFrame of the program, and also serves as the
 * central class with most of the utility methods.
 */
public class MainFrame extends JFrame {

	/**
	 * The singleton instance of MainFrame.
	 */
	private static MainFrame mainFrame;

	/**
	 * I don't like to suppress warnings so this is here
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Return the singleton instance of MainFrame.
	 */
	public static MainFrame getMainFrame() {
		return mainFrame;
	}

	/**
	 * We use this panel on startup. It contains nothing but a
	 * StatusProcessorArea.
	 */
	private JPanel defaultPanel = new JPanel();

	/**
	 * Our main GUI panel.
	 */
	private MainPanel mainPanel;

	/**
	 * This is the last directory used by the "Open..." command. We make sure we
	 * return to the same location as last time.
	 */
	private String mostRecentOpenDirectory = null;

	/**
	 * This is the StatusProcessorArea inside the default panel.
	 */
	private StatusProcessorArea statusArea = new StatusProcessorArea();

	private JMenuBar menuBar;

	/**
	 * True if the program is marked as "busy," i.e. the interface should be
	 * disabled. For example, rendering a clip or creating a GIF or scanning a
	 * file make us "busy."
	 */
	public static volatile boolean busy = false;

	/**
	 * Initialization and construction code.
	 */
	public MainFrame() {
		if (mainFrame != null) {
			throw new DuplicateSingletonException("MainFrame");
		}
		mainFrame = this;
		setTitle("TumblGIFifier - Version " + TumblGIFifier.VERSION);
		this.setLayout(new BorderLayout());
		this.getContentPane().add(defaultPanel);
		setResizable(false);
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");
		JMenuItem about = new JMenuItem("About...");
		final JMenuItem open = new JMenuItem("Open...");
		JMenuItem quit = new JMenuItem("Quit...");
		fileMenu.add(open);
		fileMenu.add(quit);
		helpMenu.add(about);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);
		this.add(menuBar, BorderLayout.NORTH);
		quit.addActionListener(ae -> TumblGIFifier.quit());
		about.addActionListener(ae -> new AboutDialog(MainFrame.this).setVisible(true));
		defaultPanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(statusArea);
		defaultPanel.add(scrollPane, BorderLayout.CENTER);
		defaultPanel.setPreferredSize(new Dimension(990, 640));
		open.addActionListener(ae -> openDialog());
		open.setEnabled(false);
		pack();
		setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				TumblGIFifier.quit();
			}
		});
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_Q:
						if (e.isControlDown() && !e.isShiftDown()) {
							ConcurrenceManager.executeLater(TumblGIFifier::quit);
							return true;
						}
						break;
					case KeyEvent.VK_O:
						if (e.isControlDown() && !e.isShiftDown()) {
							openDialog();
							return true;
						}
						break;
				}
			}
			return false;
		});
		setBusy(true);
		getStatusProcessor().appendStatus("Initializing Engine. This may take a while on the first execution.");
		Path recentOpenPath = ResourcesManager.getLocalFile("recent_open.txt");
		if (Files.exists(recentOpenPath)) {
			try {
				mostRecentOpenDirectory = IOHelper.getFirstLineOfFile(recentOpenPath);
			} catch (IOException ioe) {
				log(ioe);
				mostRecentOpenDirectory = null;
			}
		}
		ConcurrenceManager.addPostInitTask(new DefaultTask(-50, () -> {
			TumblGIFifier.executeOldVersionCleanup();
			ResourcesManager.loadedPkgs.addAll(ResourcesManager.initializeResources(getStatusProcessor()));
			List<String> rpkgs = new ArrayList<>(ResourcesManager.requiredPkgs);
			rpkgs.removeAll(ResourcesManager.loadedPkgs);
			if (!rpkgs.isEmpty()) {
				getStatusProcessor().appendStatus("Unable to load all required resources.");
				for (String pkg : rpkgs) {
					getStatusProcessor().appendStatus("Missing: " + pkg);
				}
			} else {
				List<String> opkgs = new ArrayList<>(ResourcesManager.optionalPkgs);
				opkgs.removeAll(ResourcesManager.loadedPkgs);
				for (String pkg : opkgs) {
					switch (pkg) {
						case "OpenSans":
							getStatusProcessor().appendStatus("Missing Open Sans. Text overlay is disabled.");
							break;
						case "gifsicle":
							getStatusProcessor().appendStatus("Missing gifsicle. GIFs will be less optimized.");
							break;
						default:
							getStatusProcessor().appendStatus("Unknown missing package: " + pkg);
							throw new Error("Unknown missing package.");
					}
				}
				setBusy(false);
				EventQueue.invokeLater(() -> {
					open.setEnabled(true);
				});
			}
		}));
	}

	public void open(Path path) {
		if (isBusy()) {
			JOptionPane.showMessageDialog(MainFrame.this, "Busy right now!", "Busy", JOptionPane.ERROR_MESSAGE);
		} else {
			setBusy(true);
			final VideoScan scan = VideoScan.scanFile(getStatusProcessor(), path);
			if (scan != null) {
				EventQueue.invokeLater(() -> {
					if (mainPanel != null) {
						MainFrame.this.remove(mainPanel);
					} else {
						MainFrame.this.remove(defaultPanel);
					}
					mainPanel = new MainPanel(scan);
					MainFrame.this.add(mainPanel);
					MainFrame.this.pack();
					setLocationRelativeTo(null);
				});
			} else {
				getStatusProcessor().appendStatus("Error scanning video file.");
			}
			setBusy(false);
		}
	}

	public void openDialog() {
		if (isBusy()) {
			JOptionPane.showMessageDialog(MainFrame.this, "Busy right now!", "Busy", JOptionPane.ERROR_MESSAGE);
		} else {
			final FileDialog fileDialog = new FileDialog(MainFrame.this, "Select a Video File", FileDialog.LOAD);
			fileDialog.setMultipleMode(false);
			if (mostRecentOpenDirectory != null) {
				fileDialog.setDirectory(mostRecentOpenDirectory);
			}
			fileDialog.setVisible(true);
			final String filename = fileDialog.getFile();
			if (filename != null) {
				mostRecentOpenDirectory = fileDialog.getDirectory();
				final Path path = Paths.get(mostRecentOpenDirectory, filename);
				ConcurrenceManager.executeLater(() -> {
					Path recentOpenPath = ResourcesManager.getLocalFile("recent_open.txt");
					try (Writer recentOpenWriter = Files.newBufferedWriter(recentOpenPath)) {
						recentOpenWriter.write(mostRecentOpenDirectory);
					} catch (IOException ioe) {
						// we don't really care if this fails, but
						// we'd like to know on standard error
						log(ioe);
					}
					open(path.toAbsolutePath());
				});
			}
		}
	}

	/**
	 * This returns the StatusProcessor that currently prints status lines.
	 * Sometimes it's the stats area of the default panel, sometimes it's the
	 * status area of the main panel.
	 */
	public StatusProcessor getStatusProcessor() {
		if (mainPanel != null) {
			return mainPanel.getStatusProcessor();
		} else {
			return statusArea;
		}
	}

	/**
	 * True if the program is marked as "busy," i.e. the interface should be
	 * disabled. For example, rendering a clip or creating a GIF or scanning a
	 * file make us "busy."
	 */
	public static boolean isBusy() {
		return MainFrame.busy;
	}

	/**
	 * Set to true if the program is marked as "busy," i.e. the interface should
	 * be disabled. For example, rendering a clip or creating a GIF or scanning
	 * a file make us "busy."
	 */
	public void setBusy(boolean busy) {
		MainFrame.busy = busy;
		MainFrame.getMainFrame().toFront();
		MainFrame.getMainFrame().setAlwaysOnTop(true);
		MainFrame.getMainFrame().setAlwaysOnTop(false);
		MainFrame.getMainFrame().requestFocus();
		if (mainPanel != null) {
			mainPanel.getFireButton().setText(busy ? "STOP" : "Create GIF");
			for (Component c : mainPanel.getOnDisable()) {
				c.setEnabled(!busy);
			}
			GUIHelper.setEnabled(menuBar, !busy);
			mainPanel.getFireButton().requestFocusInWindow();
		}
	}

}
