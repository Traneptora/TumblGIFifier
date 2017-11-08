package thebombzen.tumblgififier.util.io;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;

/**
 * This represents a runtime version of FileNotFoundException. It's the
 * equivalent of RuntimeIOException but for FNFs.
 */
public class RuntimeFNFException extends RuntimeIOException {

	private static final long serialVersionUID = 1L;

	public RuntimeFNFException(FileNotFoundException source) {
		super(source);
	}

	public RuntimeFNFException(NoSuchFileException source) {
		super(source);
	}
}
