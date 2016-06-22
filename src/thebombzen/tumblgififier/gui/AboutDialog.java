package thebombzen.tumblgififier.gui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import thebombzen.tumblgififier.TumblGIFifier;

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
		JButton close = new JButton("Close");
		box.add(TumblGIFifier.wrapCenterAligned(close));
		box.add(Box.createVerticalStrut(10));
		outerBox.add(box);
		outerBox.add(Box.createHorizontalStrut(10));
		close.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				AboutDialog.this.dispose();
			}
		});
		this.add(outerBox);
		this.pack();
		this.setLocationRelativeTo(parent);
		this.setModal(true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
}
