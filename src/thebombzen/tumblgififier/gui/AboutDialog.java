package thebombzen.tumblgififier.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.util.ExtrasManager;

public class AboutDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	public AboutDialog(Window parent){
		super(parent, "About");
		this.setLayout(new BorderLayout());
		Box outerBox = Box.createHorizontalBox();
		outerBox.add(Box.createHorizontalStrut(10));
		Box box = Box.createVerticalBox();
		box.add(Box.createVerticalStrut(10));
		box.add(TumblGIFifier.wrapLeftAligned(new JLabel("TumblGIFifier version " + TumblGIFifier.VERSION)));
		box.add(TumblGIFifier.wrapLeftAligned(new JLabel("Copyright 2015/2016 Leo Izen (thebombzen)")));
		box.add(Box.createVerticalStrut(10));
		box.add(TumblGIFifier.wrapLeftAligned(new JLabel("Licensed under the MIT license")));
		box.add(TumblGIFifier.wrapLeftAligned(new JLabel("with included public domain XZ Utils")));
		box.add(Box.createVerticalStrut(10));
		box.add(TumblGIFifier.wrapLeftAligned(new JLabel("See https://thebombzen.github.io/TumblGIFifier/ for details.")));
		box.add(Box.createVerticalStrut(10));
		final JButton checkForUpdates = new JButton("Check for updates");
		final JButton close = new JButton("Close");
		box.add(TumblGIFifier.wrapLeftRightAligned(checkForUpdates, close));
		box.add(Box.createVerticalStrut(10));
		outerBox.add(box);
		outerBox.add(Box.createHorizontalStrut(10));
		close.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				AboutDialog.this.dispose();
			}
		});
		checkForUpdates.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				close.setEnabled(false);
				checkForUpdates.setEnabled(false);
				checkForUpdates.setText("Checking...");
				TumblGIFifier.getThreadPool().submit(new Runnable(){
					@Override
					public void run(){
						try {
							final String latest = ExtrasManager.getExtrasManager().getLatestVersion();
							if (latest.equals(TumblGIFifier.VERSION)){
								EventQueue.invokeLater(new Runnable(){
									@Override
									public void run(){
										checkForUpdates.setText("Up to date.");
										close.setEnabled(true);
									}
								});
							} else {
								EventQueue.invokeLater(new Runnable(){
									@Override
									public void run(){
										int answer = JOptionPane.showConfirmDialog(AboutDialog.this, String.format("Update Available!%nLatest Version: %s%nDo you want to download the latest version?", latest), "New Version", JOptionPane.YES_NO_OPTION);
										close.setEnabled(true);
										checkForUpdates.setText("Updates Available");
										if (answer == JOptionPane.YES_OPTION){
											if (Desktop.isDesktopSupported()){
												try {
													Desktop.getDesktop().browse(TumblGIFifier.wrapSafeURL("https://github.com/thebombzen/TumblGIFifier/releases/").toURI());
												} catch (RuntimeException ex){
													throw ex;
												} catch (Exception e){
													e.printStackTrace();
													JOptionPane.showMessageDialog(AboutDialog.this, "Error opening web browser.", "Error", JOptionPane.ERROR_MESSAGE);
												}
											} else {
												JOptionPane.showMessageDialog(AboutDialog.this, "Error opening web browser.", "Error", JOptionPane.ERROR_MESSAGE);
											}
										}
									}
								});
							}
						} catch (IOException ioe){
							checkForUpdates.setText("Error checking.");
							close.setEnabled(true);
						}
					}
				});
			}
		});
		this.add(outerBox);
		this.pack();
		this.setLocationRelativeTo(parent);
		this.setModal(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
}
