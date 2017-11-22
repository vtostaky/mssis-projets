package applet;




import javacard.framework.*;




public class TheApplet extends Applet {  


	final static byte  BINARY_WRITE = (byte) 0xD0;
	final static byte  BINARY_READ  = (byte) 0xB0;
	final static byte  SELECT       = (byte) 0xA4;

	final static short NVRSIZE      = (short)1024;
	static byte[] NVR               = new byte[NVRSIZE];


	protected TheApplet() {
		register();
	}


	public static void install( byte[] bArray, short bOffset, byte bLength ){
		new TheApplet();
	}


	public boolean select(){
		return true;
	}


	public void deselect(){
	}


	/*
	void verify( APDU apdu ) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		if( !pin.check( buffer, (byte)5, buffer[4] ) ) 
			ISOException.throwIt( SW_VERIFICATION_FAILED );
	}
	*/


	public void process(APDU apdu) throws ISOException {
		byte[] buffer = apdu.getBuffer();
		switch (buffer[1]) {
	  
			case SELECT: return;
			   
			case BINARY_READ:
				Util.arrayCopy(NVR,(byte)0,buffer,(short)0,buffer[4]);
				apdu.setOutgoingAndSend((short)0,buffer[4]);
				break;
		
			case BINARY_WRITE:	
				apdu.setIncomingAndReceive();  
				Util.arrayCopy(buffer,(short)5,NVR,(short)0,buffer[4]);
				break;
				
			default:  
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
  
	}

}
