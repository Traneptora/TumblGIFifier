package thebombzen.tumblgififier.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import thebombzen.tumblgififier.processor.FFmpegManager;
import thebombzen.tumblgififier.processor.StatusProcessor;
import thebombzen.tumblgififier.processor.VideoProcessor;

public class MainFrame extends JFrame {

	public static final boolean IS_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	public static final String EXE_EXTENSION = IS_ON_WINDOWS ? ".exe" : "";
	
	private static final long serialVersionUID = 1L;
	
	private JPanel defaultPanel = new JPanel();
	private MainPanel mainPanel;
	private StatusProcessorArea statusArea = new StatusProcessorArea();
	
	private static volatile List<Process> processes = new ArrayList<>();
	private static volatile boolean busy = false;
	private static volatile boolean cleaningUp = false;
	
	private static MainFrame mainFrame;
	
	public static MainFrame getMainFrame(){
		return mainFrame;
	}
	
	public StatusProcessor getStatusProcessor(){
		if (mainPanel != null){
			return mainPanel.getStatusProcessor();
		} else {
			return statusArea;
		}
	}
	
	public static void setEnabled(Component component, boolean enabled) {
	    component.setEnabled(enabled);
	    if (component instanceof Container) {
	        for (Component child : ((Container) component).getComponents()) {
	            setEnabled(child, enabled);
	        }
	    }
	}
	
	public static boolean isBusy() {
		return busy;
	}

	public static void setBusy(boolean busy) {
		MainFrame.busy = busy;
		setEnabled(mainFrame, !busy);
	}

	public static synchronized InputStream exec(boolean join, String... args) throws IOException {
		if (cleaningUp){
			return null;
		}
		ProcessBuilder pbuilder = new ProcessBuilder(args);
		pbuilder.redirectErrorStream(true);
		Process p = pbuilder.start();
		processes.add(p);
		if (join){
			join(p);
		}
		return p.getInputStream();
	}
	
	public MainFrame(){
		mainFrame = this;
		setTitle("TumblGIFifier");
		this.setLayout(new BorderLayout());
		this.getContentPane().add(defaultPanel);
		setResizable(false);
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem open = new JMenuItem("Open...");
		JMenuItem quit = new JMenuItem("Quit...");
		fileMenu.add(open);
		fileMenu.add(quit);
		menuBar.add(fileMenu);
		this.add(menuBar, BorderLayout.NORTH);
		quit.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});
		ActionListener l = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				if (isBusy()){
					JOptionPane.showMessageDialog(MainFrame.this, "Busy right now!", "Busy", JOptionPane.ERROR_MESSAGE);
				} else {
					JFileChooser jfc = new JFileChooser();
					jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					jfc.setMultiSelectionEnabled(false);
					
					int result = jfc.showOpenDialog(MainFrame.this);
					if (result == JFileChooser.APPROVE_OPTION){
						final File file = jfc.getSelectedFile();
						setEnabled(MainFrame.this, false);
						new Thread(new Runnable(){
							public void run(){
								try {
									final VideoProcessor scan = VideoProcessor.scanFile(statusArea, file.getAbsolutePath());
									if (scan != null){
										EventQueue.invokeLater(new Runnable(){
											public void run(){
												if (mainPanel != null){
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
										statusArea.appendStatus("Error scanning video file.");
									}
									
								} catch (IOException ioe){
									ioe.printStackTrace();
								}
								setEnabled(MainFrame.this, true);
							}
						}).start();
					}
				}
			}
		};
		defaultPanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(statusArea);
		defaultPanel.add(scrollPane, BorderLayout.CENTER);
		open.addActionListener(l);
		this.setSize(640, 360);
		setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				quit();
			}
		});
		setEnabled(this, false);
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				JOptionPane.showMessageDialog(MainFrame.this, "Initializing Engine. This may take a while on the first execution because we have to download FFmpeg from the internet.", "Initializing", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		boolean success = FFmpegManager.getFFmpegManager().intitilizeFFmpeg(statusArea);
		if (success){
			setEnabled(this, true);
		} else {
			JOptionPane.showMessageDialog(this, "Error Initializing.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	public void quit(){
		finalize();
		System.exit(0);
	}
	
	protected void finalize(){
		cleaningUp = true;
		for (Process p : processes){
			p.destroy();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new MainFrame().setVisible(true);
	}

	public static void join(Process p){
		boolean done = false;
		while(!done){
			try {
				p.waitFor();
				done = true;
			} catch (InterruptedException e){
				continue;
			}
		}
	}
}
