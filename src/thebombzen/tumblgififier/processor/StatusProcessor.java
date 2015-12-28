package thebombzen.tumblgififier.processor;


public interface StatusProcessor {
	
	public void clearStatus();
	public void appendStatus(String status);
	public void replaceStatus(String status);
	
}
