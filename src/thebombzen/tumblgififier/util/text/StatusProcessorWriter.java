package thebombzen.tumblgififier.util.text;

import thebombzen.tumblgififier.util.io.SimpleWriter;

public class StatusProcessorWriter extends SimpleWriter {

	private boolean haveReturn = false;
	private StringBuffer lineBuffer = new StringBuffer();
	private StatusProcessor processor;
	private boolean shouldReplace = false;

	public StatusProcessorWriter(StatusProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void flush() {
		flushLine();
	}

	private void flushLine() {
		if (shouldReplace) {
			processor.replaceStatus(lineBuffer.toString());
		} else {
			processor.appendStatus(lineBuffer.toString());
		}
		lineBuffer = new StringBuffer();
	}

	@Override
	public void write(char c) {
		switch (c) {
			case '\r':
				haveReturn = true;
				break;
			case '\n':
				flushLine();
				shouldReplace = false;
				haveReturn = false;
				break;
			default:
				if (haveReturn) {
					flushLine();
					shouldReplace = true;
					haveReturn = false;
				}
				lineBuffer.append(c);
				break;
		}
	}

}
