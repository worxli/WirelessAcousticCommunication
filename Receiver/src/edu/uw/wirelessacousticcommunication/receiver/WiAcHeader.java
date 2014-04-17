package edu.uw.wirelessacousticcommunication.receiver;

public class WiAcHeader {
	
	private byte[] dest;
	private byte[] src;
	private long CRC;
	private int length;
	
	public WiAcHeader(byte[] dest, byte[] src, int length, long CRC){
		
		this.dest = dest;
		this.src = src;
		this.CRC = CRC;
		this.length = length;
		
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
	
	public long getCRC(){
		return this.CRC;
	}

}
