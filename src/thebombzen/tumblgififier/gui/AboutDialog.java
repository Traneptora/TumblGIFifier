package thebombzen.tumblgififier.gui;

import static thebombzen.tumblgififier.TumblGIFifier.log;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Window;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import thebombzen.tumblgififier.TumblGIFifier;
import thebombzen.tumblgififier.util.ConcurrenceManager;
import thebombzen.tumblgififier.util.io.IOHelper;
import thebombzen.tumblgififier.util.io.resources.ResourcesManager;

public class AboutDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JButton checkForUpdates;
	private JButton close;

	public AboutDialog(Window parent) {
		super(parent, "About");
		this.setLayout(new BorderLayout());
		Box outerBox = Box.createHorizontalBox();
		outerBox.add(Box.createHorizontalStrut(10));
		Box box = Box.createVerticalBox();
		box.add(Box.createVerticalStrut(10));
		box.add(GUIHelper.wrapLeftAligned(new JLabel("TumblGIFifier version " + TumblGIFifier.VERSION)));
		box.add(GUIHelper.wrapLeftAligned(new JLabel("Copyright 2015-2017 Leo Izen (thebombzen)")));
		box.add(Box.createVerticalStrut(10));
		box.add(GUIHelper.wrapLeftAligned(new JLabel("Licensed under the MIT license.")));
		box.add(Box.createVerticalStrut(10));
		box.add(GUIHelper.wrapLeftAligned(new JLabel("See https://thebombzen.com/TumblGIFifier/ for details.")));
		box.add(Box.createVerticalStrut(10));
		checkForUpdates = new JButton("Check for updates");
		close = new JButton("Close");
		box.add(GUIHelper.wrapLeftRightAligned(checkForUpdates, close));
		box.add(Box.createVerticalStrut(10));
		outerBox.add(box);
		outerBox.add(Box.createHorizontalStrut(10));
		close.addActionListener(e -> AboutDialog.this.dispose());
		checkForUpdates.addActionListener(e -> {
			close.setEnabled(false);
			checkForUpdates.setEnabled(false);
			checkForUpdates.setText("Checking...");
			ConcurrenceManager.executeLater(AboutDialog.this::checkForUpdates);
		});
		this.add(outerBox);
		this.pack();
		this.setLocationRelativeTo(parent);
		this.setModal(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private void checkForUpdates() {
		final String latest;
		try {
			latest = ResourcesManager.getLatestVersion();
		} catch (IOException ioe) {
			checkForUpdates.setText("Error checking.");
			EventQueue.invokeLater(() -> {
				close.setEnabled(true);
			});
			return;
		}
		if (latest.equals(TumblGIFifier.VERSION)) {
			EventQueue.invokeLater(() -> {
				checkForUpdates.setText("Up to date");
				close.setEnabled(true);
			});
		} else {
			EventQueue.invokeLater(() -> {
				int answer = JOptionPane.showConfirmDialog(AboutDialog.this,
						String.format(
								"Update Available!%nLatest Version: %s%nDo you want to download the latest version?",
								latest),
						"New Version", JOptionPane.YES_NO_OPTION);
				close.setEnabled(true);
				checkForUpdates.setText("Updates Available");
				if (answer == JOptionPane.YES_OPTION) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(
									IOHelper.wrapSafeURI("https://github.com/thebombzen/TumblGIFifier/releases/"));
						} catch (IOException ioe) {
							log(ioe);
							JOptionPane.showMessageDialog(AboutDialog.this, "Error opening web browser.", "Error",
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						JOptionPane.showMessageDialog(AboutDialog.this, "Desktop utilities are not supported.", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			});
		}
	}

}
