package thebombzen.tumblgififier.util.io.resources;

public class Resource {
	
	/**
	 * This is the global package containing the resource.
	 * Example: "FFmpeg"
	 */
	public final String resourcePackage;
	/**
	 * This is the local name of the resource.
	 * Example: "ffplay"
	 */
	public final String resourceName;
	
	/**
	 * This is the filesystem location of the resource.
	 * Example: "/usr/bin/ffprobe"
	 */
	public final String location;
	
	/**
	 * Did this resource originate from the PATH?
	 */
	public final boolean isInPath;
	
	public Resource(String resourcePackage, String resourceName, String location, boolean isFromPATH) {
		this.resourcePackage = resourcePackage;
		this.resourceName = resourceName;
		this.location = location;
		this.isInPath = isFromPATH;
	}
	
	/**
	 * Calling toString() on a resource just returns its location.
	 */
	@Override
	public String toString() {
		return location;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isInPath ? 1231 : 1237);
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
		result = prime * result + ((resourcePackage == null) ? 0 : resourcePackage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Resource other = (Resource) obj;
		if (isInPath != other.isInPath)
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (resourceName == null) {
			if (other.resourceName != null)
				return false;
		} else if (!resourceName.equals(other.resourceName))
			return false;
		if (resourcePackage == null) {
			if (other.resourcePackage != null)
				return false;
		} else if (!resourcePackage.equals(other.resourcePackage))
			return false;
		return true;
	}
}
