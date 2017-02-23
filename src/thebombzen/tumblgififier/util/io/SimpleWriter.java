package thebombzen.tumblgififier.util.io;

import java.io.IOException;
import java.io.Writer;

/**
 * This is a simple implementation of Writer that allows subclasses to only determine what happens to data one character at a time. It is inefficient but easy to work with, so it's ideal for small data sets.
 * Users of subclasses of this should probably buffer the data in some way, perhaps with BufferedWriter.  
 */
public abstract class SimpleWriter extends Writer {
	
	@Override
	public void close() throws IOException {
		
	}
	
	@Override
	public void flush() throws IOException {
		
	}
	
	public abstract void write(char c) throws IOException;
	
	@Override
	public void write(char[] buf) throws IOException {
		write(buf, 0, buf.length);
	}
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			write(cbuf[i + off]);
		}
	}
	
	@Override
	public void write(int i) throws IOException {
		write((char) (i & 0xFF_FF));
	}
	
}
