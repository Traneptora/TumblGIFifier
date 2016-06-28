package thebombzen.tumblgififier.util;


public class ResourceLocation {
	private final String location;
	private final boolean path;
	public ResourceLocation(String location, boolean isFromPATH){
		this.location = location;
		this.path = isFromPATH;
	}
	public String getLocation(){
		return location;
	}
	public boolean isFromPath(){
		return path;
	}
	@Override
	public String toString(){
		return getLocation();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + (path ? 1231 : 1237);
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
		ResourceLocation other = (ResourceLocation) obj;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (path != other.path)
			return false;
		return true;
	}
}
