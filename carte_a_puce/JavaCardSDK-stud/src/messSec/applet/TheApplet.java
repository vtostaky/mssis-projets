package applet;


import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;



public class TheApplet extends Applet {

    static final byte UNCIPHERFILEBYCARD                        = (byte)0x13;
    static final byte CIPHERFILEBYCARD                          = (byte)0x12;
    static final byte CIPHERANDUNCIPHERNAMEBYCARD               = (byte)0x11;
    static final short MAXSIZEAPDU                              = (short)0x00F0;
    private final static byte INS_DES_ECB_NOPAD_ENC           	= (byte)0x20;
    private final static byte INS_DES_ECB_NOPAD_DEC           	= (byte)0x21;
	
	static final byte[] theDESKey = 
		new byte[] { (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA };
	
    // cipher instances
    private Cipher 
	    cDES_ECB_NOPAD_enc, cDES_ECB_NOPAD_dec;

    // key objects
			
    private Key 
	    secretDESKey, secretDES2Key, secretDES3Key;
		
    boolean 
	    pseudoRandom, secureRandom,
	    SHA1, MD5, RIPEMD160,
	    keyDES, DES_ECB_NOPAD, DES_CBC_NOPAD;

    protected TheApplet() {
	    initKeyDES(); 
	    initDES_ECB_NOPAD(); 
        this.register();
    }


    private void initKeyDES() {
	    try {
		    secretDESKey = KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES, false);
		    ((DESKey)secretDESKey).setKey(theDESKey,(short)0);
		    keyDES = true;
	    } catch( Exception e ) {
		    keyDES = false;
	    }
    }


    private void initDES_ECB_NOPAD() {
	    if( keyDES ) try {
		    cDES_ECB_NOPAD_enc = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
		    cDES_ECB_NOPAD_dec = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
		    cDES_ECB_NOPAD_enc.init( secretDESKey, Cipher.MODE_ENCRYPT );
		    cDES_ECB_NOPAD_dec.init( secretDESKey, Cipher.MODE_DECRYPT );
		    DES_ECB_NOPAD = true;
	    } catch( Exception e ) {
		    DES_ECB_NOPAD = false;
	    }
    }


    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
        new TheApplet();
    } 


    public boolean select() {
        return true;
    } 


    public void deselect() {
    }

    public void process(APDU apdu) throws ISOException {
        if( selectingApplet() == true )
            return;

        byte[] buffer = apdu.getBuffer();

        switch( buffer[1] )     {
            case UNCIPHERFILEBYCARD: uncipherFileByCard( apdu ); break;
            case CIPHERFILEBYCARD: cipherFileByCard( apdu ); break;
            case CIPHERANDUNCIPHERNAMEBYCARD: cipherAndUncipherNameByCard( apdu ); break;
            default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    void uncipherFileByCard( APDU apdu ) {
		if( DES_ECB_NOPAD ) 
			cipherGeneric( apdu, cDES_ECB_NOPAD_dec, KeyBuilder.LENGTH_DES);
    }


    void cipherFileByCard( APDU apdu ) {
		if( DES_ECB_NOPAD )
			cipherGeneric( apdu, cDES_ECB_NOPAD_enc, KeyBuilder.LENGTH_DES);
    }


    void cipherAndUncipherNameByCard( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();

		try { switch( buffer[2] ) {
			case INS_DES_ECB_NOPAD_ENC: if( DES_ECB_NOPAD )
				cipherGeneric( apdu, cDES_ECB_NOPAD_enc, KeyBuilder.LENGTH_DES); return;
			case INS_DES_ECB_NOPAD_DEC: if( DES_ECB_NOPAD ) 
				cipherGeneric( apdu, cDES_ECB_NOPAD_dec, KeyBuilder.LENGTH_DES); return;
			}
		} catch( Exception e ) {
		}
    }

	private void cipherGeneric( APDU apdu, Cipher cipher, short keyLength) {
		// Write the method ciphering/unciphering data from the computer.
		// The result is sent back to the computer.
		
		byte[] buffer = apdu.getBuffer();

	    apdu.setIncomingAndReceive();
	    cipher.doFinal( buffer,(short)5, (short)buffer[4], buffer, (short)5);
		
		apdu.setOutgoingAndSend((short)5,(short)buffer[4]);
	}
}
