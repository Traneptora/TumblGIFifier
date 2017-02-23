package thebombzen.tumblgififier.util.io.resources;

/**
 * Represents the lack of existence of a resource.
 * Sometimes this is fatal, and sometimes it's not, depending on the resource.
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	public final String pkg;
	
	
	public ResourceNotFoundException(String pkg){
		super();
		this.pkg = pkg;
	}

    public ResourceNotFoundException(String pkg, String message){
    	super(message);
    	this.pkg = pkg;
    }

    public ResourceNotFoundException(String pkg, Throwable cause){
    	super(cause);
    	this.pkg = pkg;
    }

    public ResourceNotFoundException(String pkg, String message, Throwable cause){
    	super(message, cause);
    	this.pkg = pkg;
    }
}
