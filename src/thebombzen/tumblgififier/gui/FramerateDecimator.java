package thebombzen.tumblgififier.gui;

public enum FramerateDecimator {

	FULL_RATE(0), HALF_RATE(1), THIRD_RATE(2);

	public final int decimator;

	private FramerateDecimator(final int decimator) {
		this.decimator = decimator;
	}

	@Override
	public String toString() {
		switch (this) {
			case FULL_RATE:
				return "Full Framerate";
			case HALF_RATE:
				return "Half Framerate";
			case THIRD_RATE: // third-rate hack
				return "Third Framerate";
			default:
				throw new Error();
		}
	}

}
