package thebombzen.tumblgififier.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This stream behaves like a FilterOutputStream for several output stream.
 * It forks the output into several output streams.
 */

public class TeeOutputStream extends OutputStream {
	
	protected List<? extends OutputStream> outs;
	
	public TeeOutputStream(OutputStream... outs) {
		this.outs = Arrays.asList(outs);
	}
	
	public TeeOutputStream(Collection<? extends OutputStream> outs) {
		this.outs = new ArrayList<>(outs);
	}
	
	@Override
	public void write(int b) throws IOException {
		for (OutputStream out : outs) {
			out.write(b);
		}
	}
	
	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		for (OutputStream out : outs) {
			out.write(buf, off, len);
		}
	}
	
	@Override
	public void write(byte[] buf) throws IOException {
		for (OutputStream out : outs) {
			out.write(buf);
		}
	}
	
	@Override
	public void flush() throws IOException {
		for (OutputStream out : outs) {
			out.flush();
		}
	}
	
	/**
	 * This implementation of this method does not throw IOException no matter what.
	 */
	@Override
	public void close() {
		for (OutputStream out : outs) {
			IOHelper.closeQuietly(out);
		}
	}
}
