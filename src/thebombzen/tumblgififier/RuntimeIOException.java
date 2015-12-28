package thebombzen.tumblgififier;

import java.io.Closeable;
import java.io.IOException;

/**
 * For when an IOException really shouldn't occur...
 */
public class RuntimeIOException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public static void close(Closeable cl){
		try {
			cl.close();
		} catch (IOException ioe){
			throw new RuntimeIOException(ioe);
		}
	}
	
	public RuntimeIOException(IOException ioe){
		super(ioe);
	}
	
}
