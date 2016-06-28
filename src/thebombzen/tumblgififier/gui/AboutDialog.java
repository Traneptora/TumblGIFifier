package thebombzen.tumblgififier.gui;

import java.awt.BorderLayout;
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
		box.add(TumblGIFifier.wrapLeftAligned(new JLabel("See https://github.com/thebombzen/TumblGIFifier for details.")));
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
				new Thread(new Runnable(){
					public void run(){
						try {
							final String latest = ExtrasManager.getExtrasManager().getLatestVersion();
							if (latest.equals(TumblGIFifier.VERSION)){
								EventQueue.invokeLater(new Runnable(){
									public void run(){
										checkForUpdates.setText("Up to date.");
										close.setEnabled(true);
									}
								});
							} else {
								EventQueue.invokeLater(new Runnable(){
									public void run(){
										JOptionPane.showMessageDialog(AboutDialog.this, String.format("Update Available!%nLatest Version: %s", latest), "New Version", JOptionPane.INFORMATION_MESSAGE);
										close.setEnabled(true);
										checkForUpdates.setText("Updates Available");
									}
								});
							}
						} catch (IOException ioe){
							checkForUpdates.setText("Error checking.");
							close.setEnabled(true);
						}
					}
				}).start();
			}
		});
		this.add(outerBox);
		this.pack();
		this.setLocationRelativeTo(parent);
		this.setModal(true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
}
