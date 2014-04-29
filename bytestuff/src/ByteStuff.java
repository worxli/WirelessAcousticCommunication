import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class ByteStuff {
	
	//we don't know destination so just broadcast
	private final static String destIP = "255.255.255.255";
		
	private BitSet message;// = new BitSet();
	private final int duration = 1; // seconds
	private final int bitRate = 300;
    private final int sampleRate = 44100;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private static double freqOfTone = 12001; // hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    
    private static byte[] pay;
    
    
    /*
	 * convertData(String message) ** 
	 * convert to byte array ** 
	 * create header and calculate CRC
	 * put all in a big byte array and then in a bitset
	 * add preamble
	 */
	private static BitSet convertData(String msg) {
		
		//get IP, if not available -> 0.0.0.0
		String srcIP = "1.1.1.1";//Utils.getIPAddress(true);
		
		//get bytes of message
		byte[] data = msg.getBytes();
		
		//src string to byte array
		String[] srcStr = srcIP.split("\\.");
		byte[] srcBytes = new byte[5];
		for (int i = 0; i < srcStr.length; i++) {
			srcBytes[i] = (byte)Integer.parseInt(srcStr[i]);
		}		
		
		//dest string to byte array
		String[] destStr = destIP.split("\\.");
		byte[] destBytes = new byte[5];
		for (int i = 0; i < srcStr.length; i++) {
			destBytes[i] = (byte)(int)Integer.parseInt(destStr[i]);
		}
		
		//Length of payload to byte array
		int len = data.length;
		byte[] length = ByteBuffer.allocate(4).putInt(len).array(); 
		
		//create 12 byte header
		byte[] header = new byte[12];
		
		for (int i = 0; i < srcBytes.length; i++) {
			header[i] = srcBytes[i];
		}
		for (int j = 0; j < destBytes.length; j++) {
			header[j+4] = destBytes[j];
		}
		for (int k = 0; k < length.length; k++) {
			header[k+8] = length[k];
		}
		
		String heaString = byteToString(length);
		//System.out.println("len length: "+heaString.length()+" bytes:"+heaString.length()/8+" bits:"+heaString);
		

		//create payload: header + data + CRC
		//actually we just add the preamble in front of it
		//preamble 1111'1111 1111'1111 -> so in byte rep.: 255 255
		int prelen = 2;
		byte[] payload = new byte[prelen+header.length+data.length+8];
		
		payload[0] = (byte) 0xff;
		payload[1] = (byte) 0xff;
		
		String preaString = byteToString(payload);
		//System.out.println("preamble length: "+preaString.length()+" bytes:"+preaString.length()/8+" bits:"+preaString);
		

		for (int i = 0; i < header.length; i++) {
			payload[i+prelen] = header[i];
		}
		for (int i = 0; i < data.length; i++) {
			payload[i+header.length+prelen] = data[i];
		}
		
		String headString = byteToString(header);
		//System.out.println("header length: "+headString.length()+" bytes:"+headString.length()/8+" bits:"+headString);
		
		// checksum with the specified array of bytes
		Checksum checksum = new CRC32();
		checksum.update(payload, 0, payload.length-8);
		byte[] check = ByteBuffer.allocate(8).putLong(checksum.getValue()).array();
		
		for (int i = 0; i < check.length; i++) {
			payload[i+payload.length-8] = check[i];
		}
		
		String checkString = byteToString(check);
		//System.out.println("check length: "+checkString.length()+" bytes:"+checkString.length()/8+" bits:"+checkString);
		
		//this is our "bitarray" of the packet
		BitSet bitstring = BitSet.valueOf(payload);
		
		return bitstring;
	}


	public static void main(String [] args ) {
		
		/*
		
		//get message
		String msg = "message hallo";//params[0];
		
		String headString = byteToString(msg.getBytes());
		System.out.println("data length: "+headString.length()+" bytes:"+headString.length()/8+" bits:"+headString);
		
		
		//get frequency
		int freq = 1200;//Integer.parseInt(params[1]);
		
		//debug
		freqOfTone = freq;
		
		//get bits per symbol
		int bps = 16;//Integer.parseInt(params[2]);
			
		//convert message to data packet
		BitSet packet = convertData(msg);
		
		String packetString = byteToString(packet.toByteArray());
		//System.out.println("packet length: "+packetString.length()+" bytes:"+packetString.length()/8+" bits:"+packetString);
		
		
		
		//modulate
		//modulate(bits, carrier signal, bitspersymbol)
		//Log.v("WORKER","Working!!!!!!!!!!!!!!!");
		//genTone();
		//playSound();

		//packet.flip(1);
		ByteBuffer bb = ByteBuffer.wrap(packet.toByteArray());
		
		findPreamble(bb, packet);*/
		
		/*
		 * byte[] packet = new byte[bits.length/8];
		for (int i = 0; i < packet.length; i++) {
			byte t = 0;
			for (int j = 0; j < 8; j++) {
				t = (byte) (t | (byte)(bits[7*i+j])<<j);	
			}
			
			packet[i] = t;
		}
		 */
		
	}	
	
	///testing only
	
	public static String byteToString(byte[] b){
		
		String s = "";
		
		for(int j=0; j<b.length; j++){
			
			s = s+String.format("%8s", Integer.toBinaryString(b[j] & 0xFF)).replace(' ', '0');
		}
		
		return s;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// reconstruction ///////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//find the preamble in the data
	public static void findPreamble(ByteBuffer buf, BitSet pack){
		
		String search = "";
		
		boolean found = false;
		boolean setup = false;
		boolean copied = false;
		int pos = 0;
		
		byte[] preamble = new byte[2];
		byte[] temp = new byte[2];
		preamble[0] = (byte) 0xff;
		preamble[1] = (byte) 0xff;
		String preamb = byteToString(preamble);
		
		byte[] searchBytes = new byte[4];
		
		while(!setup){
			if(buf.remaining()>=4){
				buf.get(searchBytes, 0, 4);
				setup = true;
			}
		}
		
		//search for preamble
		while(!found){
			
			//string to search in
			search = byteToString(searchBytes);
			
			//get position of first occurence of preamble
			pos = search.indexOf(preamb);
			
			//if not found -> go ahead, otherwise break
			if(pos!=-1){
				found = true;
				break;
			}
			
			//copy second half to first half
			System.arraycopy(searchBytes, 2, searchBytes, 0, 2);
			
			//get bytes from buffer
			while(!copied){
				if(buf.remaining()>=2){
					buf.get(temp, 0, 2);
					copied = true;
				}
			}
			copied = false;
			
			//copy into searchbytes 
			System.arraycopy(temp,0,searchBytes,2,2);
			
		}
		
		//get header bytes
		byte[] buffer = new byte[12];
		
		int ind = 0;
		while(true){
			
			if(buf.remaining()>=1){
				buf.get(buffer, ind, 1);
				ind++;
			}
			
			if(ind==11){
				break;
			}
		}
		
		byte[] header = new byte[13];
		int k;
		if(pos<8){
			header[0] = searchBytes[2];
			header[1] = searchBytes[3];
			k=2;
		} else if(pos<16) {
			header[0] = searchBytes[3];
			k=1;
		} else {
			k=0;
		}
		
		for (int i = 0; i < buffer.length-k; i++) {
			header[i+k] = buffer[i];
		}
		
		
		BitSet headerBits = BitSet.valueOf(header);
		BitSet headerBitsShift = new BitSet(96);
		
		int shift = pos%8;
		for (int i = shift; i < 96+shift; i++) {
			if(headerBits.get(i)){
				headerBitsShift.set(i-shift);
			}
		}
		
		byte[] headerBytes = new byte[12];
		byte[] headerBytesShifted = headerBitsShift.toByteArray();
		
		for (int i = 0; i < headerBytesShifted.length; i++) {
			headerBytes[i] = headerBytesShifted[i];
		}
		
		//reconstruct header		
		WiAcHeader mHeader = new WiAcHeader(headerBytes);
		
		//create array for data bytes
		if(mHeader.getLength()>0){
			byte[] data = new byte[mHeader.getLength()+1];
			
			byte[] dataBuffer = new byte[mHeader.getLength()];
			
			ind = 0;
			while(true){
				
				if(buf.remaining()>=1){
					buf.get(dataBuffer, ind, 1);
					ind++;
				}
				
				if(ind==mHeader.getLength()-1){
					break;
				}
			}
			
			if (k==2) {
				data[0] = buffer[10];
				k=1;
			} else if (k==1){
				data[0] = buffer[11];
			}
			
			for (int i = 0; i < dataBuffer.length-k; i++) {
				data[i+k] = dataBuffer[i];
			}
			
			BitSet dataBits = BitSet.valueOf(data);
			BitSet dataBitsShift = new BitSet();
			
			for (int i = shift; i < dataBits.length(); i++) {
				if(dataBits.get(i)){
					dataBitsShift.set(i-shift);
				}
			}
			
			byte[] dataBytes = new byte[mHeader.getLength()];
			byte[] dataBytesShifted = dataBitsShift.toByteArray();
			
			for (int i = 0; i < dataBytesShifted.length; i++) {
				dataBytes[i] = dataBytesShifted[i];
			}
			
			
			String headString = byteToString(dataBytes);
			System.out.println("data length: "+headString.length()+" bytes:"+headString.length()/8+" bits:"+headString);
			System.out.println(new String(dataBytes));
		} 
		
		
		System.out.println(mHeader.getDest());
		System.out.println(mHeader.getSrc());
		System.out.println(mHeader.getLength());
		
		
		//String headString = byteToString(headerBytes);
		//System.out.println("header length: "+headString.length()+" bytes:"+headString.length()/8+" bits:"+headString);
		
		//System.out.println(new String(data));
		
	}

}
