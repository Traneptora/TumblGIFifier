package thebombzen.tumblgififier.util.io.resources;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class represents a resource provided by an external resource package.
 */
public class Resource {
	
	protected String pkg;
	protected String name;
	protected Path location;
	protected boolean inHouse;

	public Resource(String resourcePackage, String resourceName, Path location, boolean inHouse) {
		this.pkg = resourcePackage;
		this.name = resourceName;
		this.location = location.toAbsolutePath();
		this.inHouse = inHouse;
	}

	public Resource(String resourcePackage, String resourceName, String location, boolean inHouse) {
		this(resourcePackage, resourceName, Paths.get(location), inHouse);
	}

	/**
	 * This is the global package containing the resource.
	 * Example: "FFmpeg"
	 */
	public String getPackage() {
		return pkg;
	}

	/**
	 * This is the local name of the resource.
	 * Example: "ffplay"
	 */
	public String getName() {
		return name;
	}

	/**
	 * This is the filesystem location of the resource.
	 * Example: "/usr/bin/ffprobe"
	 */
	public Path getLocation() {
		return location;
	}

	/**
	 * Is this resource provided by us?
	 * @return true if the resource is provided with TumblGIFifier, false if found on the system.
	 */
	public boolean isInHouse() {
		return inHouse;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (inHouse ? 1231 : 1237);
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
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
		if (inHouse != other.inHouse)
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (pkg == null) {
			if (other.pkg != null)
				return false;
		} else if (!pkg.equals(other.pkg))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Resource [pkg=" + pkg + ", name=" + name + ", location=" + location + ", inHouse=" + inHouse + "]";
	}
}
