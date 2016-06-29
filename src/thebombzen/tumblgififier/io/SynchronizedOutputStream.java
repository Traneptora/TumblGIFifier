package thebombzen.tumblgififier.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SynchronizedOutputStream extends FilterOutputStream {
	
	public SynchronizedOutputStream(OutputStream out) {
		super(out);
	}
	
	@Override
	public synchronized void write(int b) throws IOException {
		super.write(b);
	}
	
	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		super.write(b, off, len);
	}
	
	@Override
	public synchronized void write(byte[] b) throws IOException {
		super.write(b);
	}
	
	@Override
	public synchronized void flush() throws IOException {
		super.flush();
	}
	
	@Override
	public synchronized void close() throws IOException {
		super.close();
	}
	
}
