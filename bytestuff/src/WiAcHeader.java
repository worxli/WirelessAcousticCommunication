

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WiAcHeader {
	
	private byte[] dest = new byte[4];
	private byte[] src = new byte[4];
	private int length;
	
	public WiAcHeader(byte[] header){
	    
	    //get src
	    for (int i = 0; i < 4; i++) {
	    	this.src[i] = header[i];
		}
	    
	    //get dest
	    for (int i = 0; i < 4; i++) {
	    	this.dest[i] = header[i+4];
		}	    
	    
	    byte[] len = new byte[4];
	    System.arraycopy(header, 8, len, 0, 4);
	    ByteBuffer wrapped = ByteBuffer.wrap(len);
	    this.length = wrapped.getInt();
		
	}
	
	public String getSrc(){
		
		String IP = (this.src[0]&0xff)+"."+(this.src[1]&0xff)+"."+(this.src[2]&0xff)+"."+(this.src[3]&0xff);
		
		return IP;
	}
	
	public String getDest(){
		
		String IP = (this.dest[0]&0xff)+"."+(this.dest[1]&0xff)+"."+(this.dest[2]&0xff)+"."+(this.dest[3]&0xff);
		
		return IP;
	}
	
	public int getLength(){
		return this.length;
	}

}
