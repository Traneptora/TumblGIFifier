package thebombzen.tumblgififier.util;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream {
	
	@Override
	public void write(byte[] buf) {
		
	}
	
	@Override
	public void write(byte[] buf, int off, int len) {
		
	}
	
	@Override
	public void write(int b) throws IOException {
		
	}
	
}
