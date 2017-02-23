package thebombzen.tumblgififier.util.io;

import java.io.OutputStream;

/**
 * This OutputStream behaves like /dev/null.
 * In other words, it discards all output written to it.
 */
public class NullOutputStream extends OutputStream {
	
	@Override
	public void write(byte[] buf) {
		
	}
	
	@Override
	public void write(byte[] buf, int off, int len) {
		
	}
	
	@Override
	public void write(int b) {
		
	}
	
}
