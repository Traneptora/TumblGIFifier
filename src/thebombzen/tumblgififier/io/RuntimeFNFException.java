package thebombzen.tumblgififier.io;

import java.io.FileNotFoundException;


public class RuntimeFNFException extends RuntimeIOException {

	private static final long serialVersionUID = 1L;

	public RuntimeFNFException(FileNotFoundException source){
		super(source);
	}
}
