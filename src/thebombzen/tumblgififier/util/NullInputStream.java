package thebombzen.tumblgififier.util;

import java.io.IOException;
import java.io.InputStream;

public class NullInputStream extends InputStream {
	
	@Override
	public int read() throws IOException {
		return -1;
	}
	
	@Override
	public int read(byte[] buf, int off, int len){
		return -1;
	}
	
	@Override
	public int read(byte[] buf){
		return -1;
	}
	
	@Override
	public int available(){
		return 0;
	}
	
	@Override
	public void close(){
		// Do nothing
	}
	
}
