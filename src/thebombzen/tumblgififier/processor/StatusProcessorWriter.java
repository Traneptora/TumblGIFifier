package thebombzen.tumblgififier.processor;

public class StatusProcessorWriter extends SimpleWriter {
	
	private StatusProcessor processor;
	private boolean haveReturn = false;
	private boolean shouldReplace = false;
	private StringBuffer lineBuffer = new StringBuffer();
	
	public StatusProcessorWriter(StatusProcessor processor) {
		this.processor = processor;
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
	
}
