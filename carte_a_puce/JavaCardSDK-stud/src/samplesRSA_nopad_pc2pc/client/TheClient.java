package client;

import java.math.BigInteger;
import java.util.Date;
import java.util.Random;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import javax.crypto.Cipher;
import opencard.core.service.CardRequest;
import opencard.core.service.SmartCard;
import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.terminal.APDU;
import opencard.core.util.HexString;
import opencard.opt.util.PassThruCardService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Encoder;






public class TheClient {



	private final static byte CLA_TEST                    		= (byte)0x90;
	private final static byte INS_TESTDES_ECB_NOPAD_ENC       	= (byte)0x28;
	private final static byte INS_TESTDES_ECB_NOPAD_DEC       	= (byte)0x29;
	private final static byte INS_DES_ECB_NOPAD_ENC           	= (byte)0x20;
	private final static byte INS_DES_ECB_NOPAD_DEC           	= (byte)0x21;
	private final static byte INS_RSA_ENC		           	= (byte)0x00;
	private final static byte INS_RSA_DEC		           	= (byte)0x01;


	private PassThruCardService servClient = null;
	final static boolean DISPLAYAPDUS = true;


	public static void main( String[] args ) throws InterruptedException {
		new TheClient();
	}


	public TheClient() {
		try {
			SmartCard.start();
			System.out.print( "Smartcard inserted?... " ); 
			CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 
			SmartCard sm = SmartCard.waitForCard (cr);
			if (sm != null) {
				System.out.println ("got a SmartCard object!\n");
			} else
				System.out.println( "did not get a SmartCard object!\n" );
			initNewCard( sm ); 
			SmartCard.shutdown();
		} catch( Exception e ) {
			System.out.println( "TheClient error: " + e.getMessage() );
		}
		java.lang.System.exit(0) ;
	}


	private ResponseAPDU sendAPDU(CommandAPDU cmd) {
		return sendAPDU(cmd, DISPLAYAPDUS);
	}


	private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
		ResponseAPDU result = null;
		try {
			result = servClient.sendCommandAPDU( cmd );
			if(display)
				displayAPDU(cmd, result);
		} catch( Exception e ) {
			System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return result;
	}


	/************************************************
	 * *********** BEGINNING TOOLS ***************
	 * **********************************************/


	private String apdu2string( APDU apdu ) {
		return removeCR( HexString.hexify( apdu.getBytes() ) );
	}


	public void displayAPDU( APDU apdu ) {
		System.out.println( removeCR( HexString.hexify( apdu.getBytes() ) ) + "\n" );
	}


	public void displayAPDU( CommandAPDU termCmd, ResponseAPDU cardResp ) {
		System.out.println( "--> Term: " + removeCR( HexString.hexify( termCmd.getBytes() ) ) );
		System.out.println( "<-- Card: " + removeCR( HexString.hexify( cardResp.getBytes() ) ) );
	}


	private String removeCR( String string ) {
		return string.replace( '\n', ' ' );
	}


	/******************************************
	 * *********** ENDING TOOLS ***************
	 * ****************************************/


	private boolean selectApplet() {
		boolean cardOk = false;
		try {
			CommandAPDU cmd = new CommandAPDU( new byte[] {
				(byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
				    (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
				    (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
			} );
			ResponseAPDU resp = sendAPDU( cmd );
			if( apdu2string( resp ).equals( "90 00" ) )
				cardOk = true;
		} catch(Exception e) {
			System.out.println( "Exception caught in selectApplet: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return cardOk;
	}


	private void initNewCard( SmartCard card ) {
		if( card != null )
			System.out.println( "Smartcard inserted\n" );
		else {
			System.out.println( "Did not get a smartcard" );
			System.exit( -1 );
		}

		System.out.println( "ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");


		try {
			servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
		} catch( Exception e ) {
			System.out.println( e.getMessage() );
		}

		System.out.println("Applet selecting...");
		if( !selectApplet() ) {
			System.out.println( "Wrong card, no applet to select!\n" );
			System.exit( 1 );
			return;
		} else 
			System.out.println( "Applet selected\n" );

		try {
			mainContent();
		} catch( Exception e ) {
			System.out.println( "initNewCard: " + e );
		}
	}


	/************************************************/


	private void testProcessingDES( byte typeINS ) {
		byte[] headers = { 0, typeINS, 0, 0 };
		byte[] apdu = new byte[headers.length+0];
		System.arraycopy( headers, 0, apdu, 0, headers.length );
		sendAPDU(new CommandAPDU( apdu ));
	} 


	private void testDES( boolean displayAPDUs ) { 
		System.out.println( "**TESTING DES_CARD**");
		testProcessingDES(INS_TESTDES_ECB_NOPAD_ENC);
		testProcessingDES(INS_TESTDES_ECB_NOPAD_DEC);
		System.out.println( "**TESTING DES_CARD**");
	}


	private byte[] processingDES( byte typeINS, byte[] challenge ) {
		byte[] result = new byte[challenge.length];
		byte[] headers = { CLA_TEST, typeINS, 0, 0 };
		byte[] apdu = new byte[5+challenge.length+1];

		System.arraycopy( headers, 0, apdu, 0, headers.length );
		apdu[4] = (byte)challenge.length;
		System.arraycopy( challenge, 0, apdu, 5, challenge.length );
		apdu[apdu.length-1] = (byte)challenge.length;

		CommandAPDU cmd = new CommandAPDU( apdu );
		ResponseAPDU resp = sendAPDU( cmd, false );
		byte[] response = resp.getBytes();
		System.arraycopy( response, 0, result, 0, result.length );
		return result;
	}


	private byte[] crypt( byte[] challenge ) {
		return processingDES( INS_RSA_ENC, challenge );
	} 


	private byte[] uncrypt( byte[] challenge ) {
		return processingDES( INS_RSA_DEC, challenge );
	} 


	private void mainContent() throws Exception {
		// How to hardcode the RSA keys from byte[] to PublicKey and PrivateKey objects: 5 steps

		// Get keys binary (byte[]) content (step 1)
		byte[] modulus_b = new byte[] {
			(byte)0x90,(byte)0x08,(byte)0x15,(byte)0x32,(byte)0xb3,(byte)0x6a,(byte)0x20,(byte)0x2f,
			(byte)0x40,(byte)0xa7,(byte)0xe8,(byte)0x02,(byte)0xac,(byte)0x5d,(byte)0xec,(byte)0x11,
			(byte)0x1d,(byte)0xfa,(byte)0xf0,(byte)0x6b,(byte)0x1c,(byte)0xb7,(byte)0xa8,(byte)0x39,
			(byte)0x19,(byte)0x50,(byte)0x9c,(byte)0x44,(byte)0xed,(byte)0xa9,(byte)0x51,(byte)0x01,
			(byte)0x0f,(byte)0x11,(byte)0xd6,(byte)0xa3,(byte)0x60,(byte)0xa7,(byte)0x7e,(byte)0x95,
			(byte)0xa2,(byte)0xfa,(byte)0xe0,(byte)0x8d,(byte)0x62,(byte)0x5b,(byte)0xf2,(byte)0x62,
			(byte)0xa2,(byte)0x64,(byte)0xfb,(byte)0x39,(byte)0xb0,(byte)0xf0,(byte)0x6f,(byte)0xa2,
			(byte)0x23,(byte)0xae,(byte)0xbc,(byte)0x5d,(byte)0xd0,(byte)0x1a,(byte)0x68,(byte)0x11,
			(byte)0xa7,(byte)0xc7,(byte)0x1b,(byte)0xda,(byte)0x17,(byte)0xc7,(byte)0x14,(byte)0xab,
			(byte)0x25,(byte)0x92,(byte)0xbf,(byte)0xcc,(byte)0x81,(byte)0x65,(byte)0x7a,(byte)0x08,
			(byte)0x90,(byte)0x59,(byte)0x7f,(byte)0xc4,(byte)0xf9,(byte)0x43,(byte)0x9c,(byte)0xaa,
			(byte)0xbe,(byte)0xe4,(byte)0xf8,(byte)0xfb,(byte)0x03,(byte)0x74,(byte)0x3d,(byte)0xfb,
			(byte)0x59,(byte)0x7a,(byte)0x56,(byte)0xa3,(byte)0x19,(byte)0x66,(byte)0x43,(byte)0x77,
			(byte)0xcc,(byte)0x5a,(byte)0xae,(byte)0x21,(byte)0xf5,(byte)0x20,(byte)0xa1,(byte)0x22,
			(byte)0x8f,(byte)0x3c,(byte)0xdf,(byte)0xd2,(byte)0x03,(byte)0xe9,(byte)0xc2,(byte)0x38,
			(byte)0xe7,(byte)0xd9,(byte)0x38,(byte)0xef,(byte)0x35,(byte)0x82,(byte)0x48,(byte)0xb7
		};
		byte[] private_exponent_b = new byte[] {
			(byte)0x69,(byte)0xdf,(byte)0x67,(byte)0x25,(byte)0xa3,(byte)0xb8,(byte)0x88,(byte)0xfb,
			(byte)0xf2,(byte)0xfc,(byte)0xf9,(byte)0x90,(byte)0xad,(byte)0x7f,(byte)0x44,(byte)0xbd,
			(byte)0xb8,(byte)0x59,(byte)0xf3,(byte)0x4b,(byte)0xe9,(byte)0x0a,(byte)0x1f,(byte)0x80,
			(byte)0x09,(byte)0x59,(byte)0xb5,(byte)0xe4,(byte)0xfd,(byte)0x06,(byte)0x0e,(byte)0xe3,
			(byte)0x46,(byte)0x5e,(byte)0x88,(byte)0x76,(byte)0x03,(byte)0xe0,(byte)0x5b,(byte)0x2e,
			(byte)0x47,(byte)0x65,(byte)0x3e,(byte)0x96,(byte)0xef,(byte)0x0c,(byte)0x43,(byte)0x79,
			(byte)0xb9,(byte)0x81,(byte)0x9d,(byte)0x21,(byte)0xe5,(byte)0x2c,(byte)0x78,(byte)0x02,
			(byte)0xa9,(byte)0x54,(byte)0x12,(byte)0x66,(byte)0xab,(byte)0x48,(byte)0x1d,(byte)0xe2,
			(byte)0x6e,(byte)0x1d,(byte)0x7d,(byte)0xb2,(byte)0xce,(byte)0x7a,(byte)0x3f,(byte)0xbb,
			(byte)0x34,(byte)0xf2,(byte)0x46,(byte)0x5f,(byte)0x73,(byte)0x7c,(byte)0xba,(byte)0xf8,
			(byte)0xc1,(byte)0x29,(byte)0x97,(byte)0x85,(byte)0x67,(byte)0xdf,(byte)0x82,(byte)0x87,
			(byte)0x89,(byte)0x61,(byte)0x42,(byte)0xcc,(byte)0x1d,(byte)0xcc,(byte)0x03,(byte)0xce,
			(byte)0x41,(byte)0x7d,(byte)0x8f,(byte)0x25,(byte)0xc1,(byte)0x61,(byte)0xfe,(byte)0x06,
			(byte)0x4f,(byte)0x1a,(byte)0xf2,(byte)0x48,(byte)0x55,(byte)0xd8,(byte)0x6e,(byte)0xc6,
			(byte)0x3f,(byte)0x6d,(byte)0xe1,(byte)0xce,(byte)0xa9,(byte)0x28,(byte)0x9e,(byte)0x03,
			(byte)0x2d,(byte)0x74,(byte)0x59,(byte)0x1c,(byte)0xdb,(byte)0x18,(byte)0xb3,(byte)0x41
		};
		byte[] public_exponent_b = new byte[] { (byte)0x01,(byte)0x00,(byte)0x01 };

		// Transform byte[] into String (step 2)
		String mod_s =  HexString.hexify( modulus_b );
		mod_s = mod_s.replaceAll( " ", "" );
		mod_s = mod_s.replaceAll( "\n", "" );

		String pub_s =  HexString.hexify( public_exponent_b );
		pub_s = pub_s.replaceAll( " ", "" );
		pub_s = pub_s.replaceAll( "\n", "" );

		String priv_s =  HexString.hexify( private_exponent_b );
		priv_s = priv_s.replaceAll( " ", "" );
		priv_s = priv_s.replaceAll( "\n", "" );

		// Load the keys from String into BigIntegers (step 3)
		BigInteger modulus = new BigInteger(mod_s, 16);
		BigInteger pubExponent = new BigInteger(pub_s, 16);
		BigInteger privExponent = new BigInteger(priv_s, 16);

		// Create private and public key specs from BinIntegers (step 4)
		RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, pubExponent);
		RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(modulus, privExponent);

		// Create the RSA private and public keys (step 5)
		KeyFactory factory = KeyFactory.getInstance( "RSA" );
		PublicKey pub = factory.generatePublic(publicSpec);
		PrivateKey priv = factory.generatePrivate(privateSpec);


		// How to crypt and uncrypt using RSA_NOPAD: 4 Steps

		// Get Cipher able to apply RSA_NOPAD (step 1)
		// (must use "Bouncy Castle" crypto provider)
		Security.addProvider(new BouncyCastleProvider());
		Cipher cRSA_NO_PAD = Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );

		// Get challenge data (step 2)
		final int DATASIZE = 128;				//128 to use with RSA1024_NO_PAD
		Random r = new Random( (new Date()).getTime() );
		BASE64Encoder encoder = new BASE64Encoder();
		byte[] challengeBytes = new byte[DATASIZE];
		r.nextBytes( challengeBytes );
		System.out.println("challenge:\n" + encoder.encode( challengeBytes ) + "\n" );
		
		// Crypt with public key (step 3)
		cRSA_NO_PAD.init( Cipher.ENCRYPT_MODE, pub );
		byte[] ciphered = new byte[DATASIZE];
		System.out.println( "*" );
		cRSA_NO_PAD.doFinal(challengeBytes, 0, DATASIZE, ciphered, 0);
		//ciphered = cRSA_NO_PAD.doFinal( challengeBytes );
		System.out.println( "*" );
		System.out.println("ciphered by pc is:\n" + encoder.encode(ciphered) + "\n" );

		// Decrypt with private key (step 4)
		cRSA_NO_PAD.init( Cipher.DECRYPT_MODE, priv );
		byte[] unciphered = new byte[DATASIZE];
		cRSA_NO_PAD.doFinal( ciphered, 0, DATASIZE, unciphered, 0);
		System.out.println("unciphered by pc is:\n" + encoder.encode(unciphered) + "\n" );

	}


}
