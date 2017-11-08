package thebombzen.tumblgififier.gui;

import java.awt.Component;
import java.awt.Container;
import javax.swing.Box;

public final class GUIHelper {
	private GUIHelper() {

	}

	public static Component wrapLeftAligned(Component comp) {
		Box box = Box.createHorizontalBox();
		box.add(comp);
		box.add(Box.createHorizontalGlue());
		return box;
	}

	public static Component wrapCenterAligned(Component comp) {
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(comp);
		box.add(Box.createHorizontalGlue());
		return box;
	}

	public static Component wrapLeftRightAligned(Component left, Component right) {
		Box box = Box.createHorizontalBox();
		box.add(left);
		box.add(Box.createHorizontalGlue());
		box.add(right);
		return box;
	}

	/**
	 * Recursively enable or disable a component and all of its children.
	 */
	public static void setEnabled(Component component, boolean enabled) {
		component.setEnabled(enabled);
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				setEnabled(child, enabled);
			}
		}
	}
}
