package thebombzen.tumblgififier.gui;

public enum TargetSize {
	FILESIZE,
	SCALE_W,
	SCALE_H;

	public String toString() {
		switch(this) {
		case FILESIZE:
			return "Maximum Filesize in Kilobytes";
		case SCALE_W:
			return "Exact Width";
		case SCALE_H:
			return "Exact Height";
		default:
				throw new Error();
		}
	}
	
}
