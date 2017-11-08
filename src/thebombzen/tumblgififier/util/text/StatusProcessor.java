package thebombzen.tumblgififier.util.text;

public interface StatusProcessor {

	public void appendStatus(String status);

	public void clearStatus();

	public void replaceStatus(String status);

	public void processException(Throwable t);

}
