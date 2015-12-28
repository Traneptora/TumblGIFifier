package thebombzen.tumblgififier.gui;

import java.awt.EventQueue;

import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import thebombzen.tumblgififier.processor.StatusProcessor;

public class StatusProcessorArea extends JTextArea  implements StatusProcessor {

	private static final long serialVersionUID = 1L;

	public StatusProcessorArea(){
		setEditable(false);
		DefaultCaret caret = (DefaultCaret)getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}
	
	@Override
	public void setEnabled(boolean enabled){
		if (!this.isEditable() && !enabled){
			return;
		} else {
			super.setEnabled(enabled);
		}
	}
	
	public void clearStatus(){
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				setText("");
			}
		});
	}
	
	
	public void appendStatus(final String status){
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				String text = getText();
				if (text.length() != 0){
					text += String.format("%n%s", status);
				} else {
					text += status;
				}
				setText(text);
			}
		});
	}
	
	public void replaceStatus(final String status){
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				String text = getText();
				String[] lines = text.split(String.format("%n"));
				lines[lines.length - 1] = status;
				text = String.join(String.format("%n"), lines);
				setText(text);
			}
		});
	}
	
}
