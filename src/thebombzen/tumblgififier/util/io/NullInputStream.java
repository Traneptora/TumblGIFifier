package thebombzen.tumblgififier.util.io;

import java.io.InputStream;

/**
 * This input stream behaves like /dev/null. In other words, it returns
 * end-of-file on any attempt to read from it.
 */
public class NullInputStream extends InputStream {

	@Override
	public int read() {
		return -1;
	}

	@Override
	public int read(byte[] buf, int off, int len) {
		return -1;
	}

	@Override
	public int read(byte[] buf) {
		return -1;
	}

	@Override
	public int available() {
		return 0;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		// do nothing
	}

	@Override
	public synchronized void reset() {
		// do nothing
	}

	@Override
	public void close() {
		// Do nothing
	}

}
