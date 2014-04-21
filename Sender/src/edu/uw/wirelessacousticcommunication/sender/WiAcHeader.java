package edu.uw.wirelessacousticcommunication.sender;

import java.nio.ByteBuffer;

public class WiAcHeader {
	
	private byte[] dest;
	private byte[] src;
	private int length;
	
	public WiAcHeader(byte[] header){
	    
	    //define variables
	    byte[] src = new byte[4];
	    byte[] dest = new byte[4];
	    
	    //get src
	    for (int i = 0; i < 4; i++) {
	    	src[i] = header[i];
		}
	    
	    //get dest
	    for (int i = 0; i < 4; i++) {
	    	dest[i] = header[i+4];
		}
	    
	    //get dest
	    for (int i = 0; i < 4; i++) {
	    	dest[i] = header[i+4];
		}
	    
	    byte[] len = new byte[4];
	    System.arraycopy(header, 8, len, 0, 4);
	    ByteBuffer wrapped = ByteBuffer.wrap(len);
	    int length = wrapped.getInt();
		
	}
	
	public String getSrc(){
		
		String IP = Byte.toString(this.src[0])+"."+Byte.toString(this.src[1])+"."+Byte.toString(this.src[2])+"."+Byte.toString(this.src[3]);
		
		return IP;
	}
	
	public String getDest(){
		
		String IP = Byte.toString(this.dest[0])+"."+Byte.toString(this.dest[1])+"."+Byte.toString(this.dest[2])+"."+Byte.toString(this.dest[3]);
		
		return IP;
	}
	
	public int getLength(){
		return this.length;
	}

}
