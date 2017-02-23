package thebombzen.tumblgififier.util;

public abstract class Task implements Runnable, Comparable<Task> {
	
	private int priority;
	
	public Task(int priority){
		this.priority = priority;
	}

	public Task(){
		this(0);
	}
	
	@Override
	public int compareTo(Task o) {
		return Integer.compare(priority, o.priority);
	}
	
}
