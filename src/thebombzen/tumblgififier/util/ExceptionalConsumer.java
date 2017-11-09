package thebombzen.tumblgififier.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface ExceptionalConsumer<T> {
	public void accept(T t) throws Exception;

	public static <T> Consumer<T> uncheck(ExceptionalConsumer<T> consumer) {
		return consumer.uncheck();
	}

	public default Consumer<T> uncheck() {
		return (t) -> {
			try {
				ExceptionalConsumer.this.accept(t);
			} catch (Exception ex) {
				ConcurrenceManager.sneakyThrow(ex);
			}
		};
	}
}
