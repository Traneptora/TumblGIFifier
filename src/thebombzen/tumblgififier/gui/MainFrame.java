package thebombzen.tumblgififier.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import thebombzen.tumblgififier.util.Task;
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
		quit.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				TumblGIFifier.quit();
			}
		});
		about.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				new AboutDialog(MainFrame.this).setVisible(true);
			}
		});
		ActionListener l = new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				openDialog();
			}
		};
		defaultPanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(statusArea);
		defaultPanel.add(scrollPane, BorderLayout.CENTER);
		open.addActionListener(l);
		open.setEnabled(false);
		this.setSize(1280, 720);
		setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				TumblGIFifier.quit();
			}
		});
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher(){
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getID() == KeyEvent.KEY_PRESSED){
					switch (e.getKeyCode()){
					case KeyEvent.VK_Q:
						if (e.isControlDown() && !e.isShiftDown()){
							ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
								@Override
								public void run(){
									TumblGIFifier.quit();
								}
							});
							return true;
						}
						break;
					case KeyEvent.VK_O:
						if (e.isControlDown() && !e.isShiftDown()){
							openDialog();
							return true;
						}
						break;
					}
				}
				return false;
			}
			
		});
		setBusy(true);
		getStatusProcessor().appendStatus("Initializing Engine. This may take a while on the first execution.");
		File recentOpenFile = ResourcesManager.getResourcesManager().getLocalFile("recent_open.txt");
		if (recentOpenFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(recentOpenFile))) {
				mostRecentOpenDirectory = br.readLine();
			} catch (IOException ioe) {
				mostRecentOpenDirectory = null;
			}
		}
		ConcurrenceManager.getConcurrenceManager().addPostInitTask(new Task(-50){
			@Override
			public void run() {
				TumblGIFifier.executeOldVersionCleanup();
				try {
					ResourcesManager.loadedPkgs.addAll(ResourcesManager.getResourcesManager().initializeResources(getStatusProcessor()));
					List<String> rpkgs = new ArrayList<>(ResourcesManager.requiredPkgs);
					rpkgs.removeAll(ResourcesManager.loadedPkgs);
					if (!rpkgs.isEmpty()){
						getStatusProcessor().appendStatus("Unable to load all required resources.");
						for (String pkg : rpkgs){
							getStatusProcessor().appendStatus("Missing: " + pkg);
						}
					} else {
						List<String> opkgs = new ArrayList<>(ResourcesManager.optionalPkgs);
						opkgs.removeAll(ResourcesManager.loadedPkgs);
						for (String pkg : opkgs){
							switch (pkg){
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
						EventQueue.invokeLater(new Runnable(){
							@Override
							public void run() {
								open.setEnabled(true);
							}
						});
					}
				} catch (Throwable re){
					re.printStackTrace();
					getStatusProcessor().appendStatus("Error initializing.");
				}
			}
		});
	}
	
	public void open(String path){
		if (isBusy()) {
			JOptionPane.showMessageDialog(MainFrame.this, "Busy right now!", "Busy", JOptionPane.ERROR_MESSAGE);
		} else {
			setBusy(true);
			final VideoScan scan = VideoScan.scanFile(getStatusProcessor(), path);
			if (scan != null) {
				EventQueue.invokeLater(new Runnable(){
					@Override
					public void run() {
						if (mainPanel != null) {
							MainFrame.this.remove(mainPanel);
						} else {
							MainFrame.this.remove(defaultPanel);
						}
						mainPanel = new MainPanel(scan);
						MainFrame.this.add(mainPanel);
						MainFrame.this.pack();
						setLocationRelativeTo(null);
					}
				});
			} else {
				getStatusProcessor().appendStatus("Error scanning video file.");
			}
			setBusy(false);
		}
	}
	
	public void openDialog(){
		if (isBusy()) {
			JOptionPane.showMessageDialog(MainFrame.this, "Busy right now!", "Busy", JOptionPane.ERROR_MESSAGE);
		} else {
			final FileDialog fileDialog = new FileDialog(MainFrame.this, "Select a Video File",
					FileDialog.LOAD);
			fileDialog.setMultipleMode(false);
			if (mostRecentOpenDirectory != null) {
				fileDialog.setDirectory(mostRecentOpenDirectory);
			}
			fileDialog.setVisible(true);
			final String filename = fileDialog.getFile();
			if (filename != null) {
				mostRecentOpenDirectory = fileDialog.getDirectory();
				final File file = new File(mostRecentOpenDirectory, filename);
				ConcurrenceManager.getConcurrenceManager().executeLater(new Runnable(){
					@Override
					public void run() {
						File recentOpenFile = ResourcesManager.getResourcesManager().getLocalFile("recent_open.txt");
						try (FileWriter recentOpenWriter = new FileWriter(recentOpenFile)) {
							recentOpenWriter.write(mostRecentOpenDirectory);
							recentOpenWriter.close();
						} catch (IOException ioe) {
							// we don't really care if this fails, but
							// we'd like to know on standard error
							ioe.printStackTrace();
						}
						open(file.getAbsolutePath());
					}
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
