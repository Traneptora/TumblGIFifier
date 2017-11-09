package thebombzen.tumblgififier.util;

@FunctionalInterface
public interface ExceptionalRunnable {

	public void run() throws Exception;

	public static Runnable uncheck(ExceptionalRunnable er) {
		return er.uncheck();
	}

	public default Runnable uncheck() {
		return () -> {
			try {
				ExceptionalRunnable.this.run();
			} catch (Exception e) {
				ConcurrenceManager.sneakyThrow(e);
			}
		};
	}

}
