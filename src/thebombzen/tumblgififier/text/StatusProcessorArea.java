package thebombzen.tumblgififier.text;

import java.awt.EventQueue;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class StatusProcessorArea extends JTextArea implements StatusProcessor {
	
	private static final long serialVersionUID = 1L;
	
	public StatusProcessorArea() {
		setEditable(false);
		DefaultCaret caret = (DefaultCaret) getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		this.setLineWrap(true);
	}
	
	@Override
	public void appendStatus(final String status) {
		EventQueue.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				String text = getText();
				if (text.length() != 0) {
					text += String.format("%n%s", status);
				} else {
					text += status;
				}
				setText(text);
			}
		});
		System.out.format("%n%s", status);
	}
	
	@Override
	public void clearStatus() {
		EventQueue.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				setText("");
			}
		});
	}
	
	@Override
	public void replaceStatus(final String status) {
		EventQueue.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				String text = getText();
				String[] lines = text.split(String.format("%n"));
				lines[lines.length - 1] = status;
				text = TextHelper.getTextHelper().join(String.format("%n"), lines);
				setText(text);
			}
		});
		System.out.print('\r' + status);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		if (!this.isEditable() && !enabled) {
			return;
		} else {
			super.setEnabled(enabled);
		}
	}
	
}
