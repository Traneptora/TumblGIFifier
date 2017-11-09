package thebombzen.tumblgififier.util;

public class DefaultTask implements Task {

	private int priority;
	private Runnable target;

	public DefaultTask(int priority, ExceptionalRunnable target) {
		this.priority = priority;
		this.target = target.uncheck();
	}

	@Override
	public void run() {
		target.run();
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

}
