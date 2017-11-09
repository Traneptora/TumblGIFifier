package thebombzen.tumblgififier.util;

import thebombzen.tumblgififier.PreLoadable;

@PreLoadable
public class DuplicateSingletonException extends RuntimeException {

	public DuplicateSingletonException() {
		super();
	}

	public DuplicateSingletonException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DuplicateSingletonException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateSingletonException(String message) {
		super(message);
	}

	public DuplicateSingletonException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

}
