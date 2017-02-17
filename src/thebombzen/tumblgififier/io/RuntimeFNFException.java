package thebombzen.tumblgififier.io;

import java.io.FileNotFoundException;
import thebombzen.tumblgififier.RuntimeIOException;


public class RuntimeFNFException extends RuntimeIOException {

	private static final long serialVersionUID = 1L;

	public RuntimeFNFException(FileNotFoundException source){
		super(source);
	}
}
