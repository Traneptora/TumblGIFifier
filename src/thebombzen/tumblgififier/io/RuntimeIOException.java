package thebombzen.tumblgififier.io;

import java.io.IOException;

public class RuntimeIOException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public final IOException source;
	public RuntimeIOException(IOException source){
		super(source);
		this.source = source;
	}
}
