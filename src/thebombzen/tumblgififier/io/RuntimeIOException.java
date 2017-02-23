package thebombzen.tumblgififier.io;

import java.io.IOException;

/**
 * This represents a runtime version of IOException. It fixes many of the issues with checked exceptions,
 * such as the inability to be thrown by Runnable, Iterator, or similar interfaces.
 */
public class RuntimeIOException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	/**
	 * This is public and final because we really don't have any desire to do anything interesting with it.
	 * It would actually be dishonest to do so, so using a getter is pointless. 
	 */
	public final IOException source;
	public RuntimeIOException(IOException source){
		super(source);
		this.source = source;
	}
}
