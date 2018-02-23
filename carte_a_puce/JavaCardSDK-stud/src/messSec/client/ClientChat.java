package client;

import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Base64;

public class ClientChat extends Thread {

    BufferedReader inputConsole, inputNetwork;
    PrintStream outputConsole, outputNetwork;
    boolean isAlive = false;
    HashMap<String, OutputStream> transferMap;
    
    private PassThruCardService servClient = null;
    boolean DISPLAY = true;
    boolean loop = true;
	boolean isAuthenticated = false;
	private String clientName;

    static final byte CLA_DES                                   = (byte)0x00;
    static final byte P1                                        = (byte)0x00;
    static final byte P2                                        = (byte)0x00;
    static final byte UNCIPHERFILEBYCARD                        = (byte)0x13;
    static final byte CIPHERFILEBYCARD                          = (byte)0x12;
    static final short MAXSIZEAPDU                              = (short)0x00F0;
    private final static byte INS_DES_ECB_NOPAD_ENC             = (byte)0x20;
    private final static byte INS_DES_ECB_NOPAD_DEC             = (byte)0x21;
	private final static byte CLA_RSA                           = (byte)0x90;
	private final static byte INS_RSA_ENCRYPT                   = (byte)0xA0;
	private final static byte INS_RSA_DECRYPT                   = (byte)0xA2;
	private final static byte INS_RSA_SERVER_ENCRYPT            = (byte)0xA4;
	private final static byte INS_GET_PUBLIC_RSA_KEY            = (byte)0xFE;
	private final static byte INS_PUT_PUBLIC_RSA_KEY            = (byte)0xF4;
	private final static byte INS_GENERATE_RSA_KEY              = (byte)0xF6;
	private final static byte INS_PUT_SERVER_PUBLIC_RSA_KEY     = (byte)0xF8;

    public ClientChat( String[] args ) {
        try {
            int port = 2222;
            String address = "localhost";
            if(args.length > 0)
                address = args[0];
            if(args.length > 1)
                port = Integer.parseInt(args[1]);
            Socket s = new Socket(address, port);
            transferMap = new HashMap<String, OutputStream>();
            initInputOutput(s);
            
            //Start listenNetwork thread
            start();
            
            //Start console listener
            listenConsole();

            s.close();
        } catch( IOException e ) {
            System.out.println( "connexion problem" );
        } catch( IllegalArgumentException e) {
            System.out.println( "Port should be an integer smaller than 65535" );
        }
    }
    
    private ResponseAPDU sendAPDU(CommandAPDU cmd) {
        return sendAPDU(cmd, true);
    }

    private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
        ResponseAPDU result = null;
        try {
            result = this.servClient.sendCommandAPDU( cmd );
            if(display)
                displayAPDU(cmd, result);
        } catch( Exception e ) {
            System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
            java.lang.System.exit( -1 );
        }
        return result;
    }


    /************************************************
     *   * *********** BEGINNING OF TOOLS ***************
     *       * **********************************************/


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
     *   * *********** END OF TOOLS ***************
     *       * ****************************************/


    private boolean selectApplet() {
        boolean cardOk = false;
        try {
            CommandAPDU cmd = new CommandAPDU( new byte[] {
                (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
                    (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
                    (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
            } );
            ResponseAPDU resp = this.sendAPDU( cmd );
            if( this.apdu2string( resp ).equals( "90 00" ) )
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
            this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
        } catch( Exception e ) {
            System.out.println( e.getMessage() );
        }

        System.out.println("Applet selecting...");
        if( !this.selectApplet() ) {
            System.out.println( "Wrong card, no applet to select!\n" );
            System.exit( 1 );
            return;
        } else 
            System.out.println( "Applet selected" );
    }
    
    void activateSmartcard()
    {
        try {
            //Start smartcard
            SmartCard.start();
            System.out.print( "Smartcard inserted?... " );
            
            CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 

            SmartCard sm = SmartCard.waitForCard (cr);

            if (sm != null) {
                System.out.println ("got a SmartCard object!\n");
            } else
                System.out.println( "did not get a SmartCard object!\n" );

            this.initNewCard( sm );
            
        } catch( Exception e ) {
            System.out.println( "TheClient error: " + e.getMessage() );
        }
    }

    void uncipherFileByCard(String filename)
    {
        byte[] challengeDES = new byte[16];
        byte[] response;
        int i = 0;
        
        activateSmartcard();

        try {
            DataInputStream br = new DataInputStream(new FileInputStream(filename+"_crypt"));
            DataOutputStream obr = new DataOutputStream(new FileOutputStream(filename));
            byte[] buffer = new byte[0x16];
            int res = 0;

            do
            {
                try {
                    res = br.read(buffer, 0, 16);
                } catch (Exception e) {
                    System.out.println( "Problem with buffer reader" );
                }
                if(res < 16)
                    break;
                for(i = 0; i < 16; i++)
                    challengeDES[i] = buffer[i];
                
                response = cipherGeneric(UNCIPHERFILEBYCARD, P1, challengeDES);
                
                //Find starting point of padding
                for(i = 15; i >= 0; i--)
                    if(response[i] == (byte)0x80)
                        break;

                try {
                    obr.write(response, 0, i);
                    obr.flush();
                } catch (Exception e) {
                    System.out.println( "Problem with buffer writer" );
                }
                
            }while(res == 16);
            br.close();
            obr.close();
        } catch( Exception e ) {}

        System.out.println( "" );
        
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "TheClient error: " + e.getMessage() );
        }
    }


    void cipherFileByCard(String filename)
    {
        byte[] challengeDES = new byte[16];
        byte[] response;
        
        activateSmartcard();

        try {
            DataInputStream br = new DataInputStream(new FileInputStream(filename));
            DataOutputStream obr = new DataOutputStream(new FileOutputStream(filename+"_crypt"));
            byte[] buffer = new byte[0x8];
            int res = 0;

            do
            {
                try {
                    res = br.read(buffer, 0, 8);
                } catch (Exception e) {
                    System.out.println( "Problem with buffer reader" );
                }
                for(int i = 0; i < 16; i++)
                    if( i < res)
                        challengeDES[i] = buffer[i];
                    else
                        challengeDES[i] = 0;
                
                if(res < 16)
                    challengeDES[res] = (byte)0x80;
                
                response = cipherGeneric(CIPHERFILEBYCARD, P1, challengeDES);

                try {
                    obr.write(response, 0, response.length);
                    obr.flush();
                } catch (Exception e) {
                    System.out.println( "Problem with buffer writer" );
                }
                
            }while(res == 8);
            br.close();
            obr.close();
        } catch( Exception e ) {}

        System.out.println( "" );
        
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "TheClient error: " + e.getMessage() );
        }
    }
	
    private String uncipherMessageByCard(String message) {
	    byte[] response;
	    byte[] unciphered; 
		byte[] challengeDES = new byte[16];
		String res = "";
		
		System.out.println(message);
		
		activateSmartcard();

        try{
			byte[] mess = Base64.getDecoder().decode(message);
			byte[] result = new byte[mess.length];

			for(int j = 0; j < mess.length/16; j+=16)
			{
				for(int i = 0; i < 16; i++)
				{
					challengeDES[i] = mess[j+i];
				}
				//tableau challenge des
				System.out.println("\nUncipher challenge:\n" + challengeDES + "\n");
				//cipher generique est pour chiffrer et dechiffrer
				response = cipherGeneric(UNCIPHERFILEBYCARD,INS_DES_ECB_NOPAD_DEC, challengeDES);
				System.arraycopy(response,0,result,j,response.length);
			}
			res = new String(result);
        } catch( Exception e ) {}
		
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "TheClient error: " + e.getMessage() );
        }
		return res;
    }
	
    private String cipherMessageByCard(String message) {
	    byte[] response;
	    byte[] unciphered; 
		byte[] challengeDES = new byte[16];
		byte[] mess = message.getBytes();
		byte[] result = new byte[mess.length/16*16+16];
		String res = "";
		int j = 0, i = 0;
		
		activateSmartcard();
		
        try{
			for(j = 0; j < mess.length/16; j+=16)
			{
				for(i = 0; i < 16; i++)
				{
					challengeDES[i] = mess[j+i];
				}
				//tableau challenge des
				System.out.println("\nCipher challenge "+i+":\n" + challengeDES + "\n");
				//cipher generique est pour chiffrer et dechiffrer
				response = cipherGeneric(CIPHERFILEBYCARD,INS_DES_ECB_NOPAD_ENC, challengeDES);
				System.arraycopy(response,0,result,j,response.length);
			}
			for(i = 0; i < 16; i++)
			{
				if(j+i < mess.length)
					challengeDES[i] = mess[j+i];
				else
					challengeDES[i] = 0;
			}
		
			//tableau challenge des
			System.out.println("\nCipher challenge final:\n" + Base64.getEncoder().withoutPadding().encodeToString(challengeDES) + "\n");
			//cipher generique est pour chiffrer et dechiffrer
			response = cipherGeneric(CIPHERFILEBYCARD,INS_DES_ECB_NOPAD_ENC, challengeDES);
			System.arraycopy(response,0,result,j,response.length);
			res = Base64.getEncoder().withoutPadding().encodeToString(result);

        } catch( Exception e ) {
			System.out.println("Exception "+e.getMessage());
		}
		
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "TheClient error: " + e.getMessage() );
        }
		return res;
    }

    private byte[] cipherGeneric(byte typeAPDU, byte crypto, byte[] challenge ) {
        byte[] result = new byte[challenge.length];
        byte cmd [] ={CLA_DES,typeAPDU,crypto,P2,(byte)challenge.length};
        byte  [] cmd_1=new byte[challenge.length+6];
        System.arraycopy(cmd,(short)0,cmd_1,(short)0,(short)cmd.length);
        System.arraycopy(challenge,(short)0,cmd_1,(short)5,(short)challenge.length);
        cmd_1[21]=(byte)challenge.length;
        CommandAPDU commande=new CommandAPDU(cmd_1);
        ResponseAPDU resp = this.sendAPDU(commande);
        System.arraycopy(resp.getBytes(),0,result,0,challenge.length);
        return result;
    }
    
    private void putServerPublicKey(String modulus, String exponent)
    {
    	byte[] modulus_b = Base64.getDecoder().decode(modulus);
    	byte[] exponent_b = Base64.getDecoder().decode(exponent);
    	
        activateSmartcard();

        try {
			CommandAPDU apdu;
			ResponseAPDU resp;
			
			byte[] cmd = { (byte)CLA_RSA,  (byte)INS_PUT_SERVER_PUBLIC_RSA_KEY, (byte)0x00, (byte)0x00, (byte)0x80 };
            byte[] command = new byte[0x85];

			System.out.println( "Send Server Modulus to smartcard..." );
			System.arraycopy(cmd,0,command,0,5);
			System.arraycopy(modulus_b,0,command,5,0x80);
			apdu = new CommandAPDU(command);
			resp = sendAPDU(apdu);
			
			cmd[2] = (byte)0x01;
			cmd[4] = (byte)0x03;
			
			byte[] command2 = new byte[8];
			System.out.println( "Send Server public exponent to smartcard..." );
			System.arraycopy(cmd,0,command2,0,5);
			System.arraycopy(exponent_b,0,command2,5,3);
			apdu = new CommandAPDU(command2);
			resp = sendAPDU(apdu);
			
        } catch( Exception e ) {}

        System.out.println( "" );
        
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "putServerPublicKey error: " + e.getMessage() );
        }
    }
    
    private String getClientPublicKey()
    {
    	String clientPub="";
		CommandAPDU apdu;
		ResponseAPDU resp;
    	
        activateSmartcard();

        try {
			byte[] mod = new byte[0x80];
			byte[] exp = new byte[0x3];
			
			//apdu = new CommandAPDU(new byte[] { (byte)CLA_RSA,  (byte)INS_GENERATE_RSA_KEY, (byte)0x00, (byte)0x00, (byte)0x00 });
			//resp = sendAPDU(apdu);
			
			byte[] cmd = { (byte)CLA_RSA,  (byte)INS_GET_PUBLIC_RSA_KEY, (byte)0x00, (byte)0x00, (byte)0x81 };

			System.out.println( "Get client Modulus from smartcard..." );
			apdu = new CommandAPDU(cmd);
			resp = sendAPDU(apdu);
			System.arraycopy(resp.getBytes(),1,mod,0,0x80);
			
			clientPub += Base64.getEncoder().withoutPadding().encodeToString(mod);
			
			cmd[3] = (byte)0x01;
			cmd[4] = (byte)0x04;
			System.out.println( "Get client public exponent from smartcard..." );
			apdu = new CommandAPDU(cmd);
			resp = sendAPDU(apdu);
			System.arraycopy(resp.getBytes(),1,exp,0,0x03);
			
			clientPub += " " + Base64.getEncoder().withoutPadding().encodeToString(exp);
			
        } catch( Exception e ) {}

        System.out.println( "" );
        
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "getClientPublicKey error: " + e.getMessage() );
        }
        return clientPub;
    }
    
    private String respondToChallenge(String challenge)
    {
    	String response="";
		CommandAPDU apdu;
		ResponseAPDU resp;
		byte[] chall = new byte[0x80];
    	
        activateSmartcard();

        try {
			System.arraycopy(Base64.getDecoder().decode(challenge),0,chall,0,0x80);
			
			byte[] cmd = { (byte)CLA_RSA,  (byte)INS_RSA_DECRYPT, (byte)0x00, (byte)0x00, (byte)0x80 };
			byte[] command = new byte[5+0x80+1];

			System.out.println( "Send challenge to smartcard,for decryption..." );
			System.arraycopy(cmd,0,command,0,5);
			System.arraycopy(chall,0,command,5,0x80);
			command[0x85] = (byte)0x80;
			apdu = new CommandAPDU(command);
			resp = sendAPDU(apdu);
			System.arraycopy(resp.getBytes(),0,chall,0,0x80);
			
			command[1] = (byte)INS_RSA_SERVER_ENCRYPT;
			System.arraycopy(chall,0,command,5,0x80);
			System.out.println( "Send challenge to smartcard,for encryption..." );
			apdu = new CommandAPDU(command);
			resp = sendAPDU(apdu);
			System.arraycopy(resp.getBytes(),0,chall,0,0x80);
			
			response += Base64.getEncoder().withoutPadding().encodeToString(chall);
			
        } catch( Exception e ) {}

        System.out.println( "" );
        
        try{
            SmartCard.shutdown();
        } catch( Exception e ) {
            System.out.println( "respondToChallenge error: " + e.getMessage() );
        }
        return response;
    }
    
    public void run(){
        listenNetwork();
    }

    private void initInputOutput(Socket s){
        try{
            inputConsole = new BufferedReader( new InputStreamReader( System.in ) );
            inputNetwork = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
            outputConsole = new PrintStream( System.out );
            outputNetwork = new PrintStream( s.getOutputStream() );
            isAlive = true;
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        }
    }

    private void listenConsole(){
        String line;

        try{
            while(isAlive)
            {
                line = inputConsole.readLine();
                if(line.length() > 5 && line.substring(0,5).equals("/file"))
                {
					outputNetwork.println(line);
                    try{
                        String[] textParts = line.split(" ");
                        File file;
                        byte[] bytes = new byte[16*1024];
                        InputStream in;
                        int count;
                        
                        cipherFileByCard(textParts[2]);
                        
                        file = new File(textParts[2]+"_crypt");
                        
                        in = new FileInputStream(file);
                        
                        while ((count = in.read(bytes)) > 0) {
                            byte[] bytes2 = new byte[count];
                            System.arraycopy(bytes, 0, bytes2, 0, count);
                            String encodedString = Base64.getEncoder().withoutPadding().encodeToString(bytes2);
                            outputNetwork.println("/buffer " + textParts[1] + " "+ encodedString);
                        }
                        outputNetwork.println("/endoftransfer " + textParts[1] + " " + textParts[2]);
                        in.close();
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("/file should be used with user name and file name");
                    }
                }
				else
				{
					StringTokenizer st = new StringTokenizer(line);
					if(st.hasMoreTokens()) {
						String text = st.nextToken();

						switch(text)
						{
							case "/quit":
								isAlive = false;
								outputNetwork.println( line );
								break;
							case "/nusers":
								outputNetwork.println( line );
								break;
							case "/list":
								outputNetwork.println( line );
								break;
							case "/nickname":
								outputNetwork.println( line );
								break;
							case "/msg":
								if(st.hasMoreTokens())
								{
									String header = text + " " + st.nextToken() + " ";
									outputNetwork.println(header + cipherMessageByCard(line.substring(header.length(), line.length())));
								}
								break;
							default:
								if(isAuthenticated)
									outputNetwork.println(cipherMessageByCard(line));
								else
								{
									outputNetwork.println(text);
									clientName = text;
								}
								break;
						}
					}
				}
			}
		} catch( IOException e ) {
			System.out.println( "I/O problem" );
        }
    }

    private void listenNetwork(){
        String line;
        try{
            OutputStream out = null;
            while(isAlive)
            {
                line = inputNetwork.readLine();
                if(line.length() > 4 && line.substring(0,4).equals("FILE"))
                {
                    String[] textParts = line.split(" ");
                    
                    try{
                        File dir = new File(textParts[1]);
                        dir.mkdirs();
                        File file = new File(textParts[1]+File.separatorChar+textParts[2]+"_crypt");
                        out = new FileOutputStream(file);
                        transferMap.put(textParts[1], out);
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("FILE transfer initiated without file name");
                    }
                }
                else if(line.length() > 6 && line.substring(0,6).equals("BUFFER"))
                {
                    String[] textParts = line.split(" ");
                    
                    try{
                        out = transferMap.get(textParts[1]);
                        if(out == null)
                            System.out.println("BUFFER received outside file transfer context");
                        else
                        {
                            byte[] decodedBytes = Base64.getDecoder().decode(textParts[2]);
                            out.write(decodedBytes);
                            out.flush();
                        }
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("FILE transfer initiated without file name");
                    }
                }
                else if(line.length() > 9 && line.substring(0,9).equals("ENDOFFILE"))
                {
                    String[] textParts = line.split(" ");
                    
                    out = transferMap.get(textParts[1]);
                    if(out != null)
                    {
                        out.close();
                        transferMap.remove(textParts[1]);
                    }
                    uncipherFileByCard(textParts[1]+File.separatorChar+textParts[2]);
                }
				else if(line.length() > 8 && line.substring(0,8).equals("[SERVER]"))
                {
                    String[] textParts = line.split(" ");
                    try{
                        if(textParts[1].equals("AUTH_PUBKEY"))
						{
							// Client received server public key : put it into smartcard
							//putServerPublicKey(textParts[2], textParts[3]);
							
							// Client to send back his client public key
							outputNetwork.println(getClientPublicKey());
						}
						else if(textParts[1].equals("AUTH_CHALL"))
						{
							// Client received challenge ciphered by server using client public key
							// Decrypt it with smartcard, and encrypt it using server public key
							// Client to send back new ciphered challenge to server
							outputNetwork.println(respondToChallenge(textParts[2]));
						}
						else
						{
							if(textParts[1].equals("Hello"))
								isAuthenticated = true;
							outputConsole.println( line );
						}
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("FILE transfer initiated without file name");
                    }
                }
                else
				{
					if(line.length() > 5)
					{
						String[] textParts = line.split(" ");
						if(textParts.length > 3 && textParts[0].equals("[MSG]") && !textParts[1].equals(clientName))
							outputConsole.println(textParts[1] + " " +textParts[2]+" "+uncipherMessageByCard(textParts[3]));
					}
				}
            }
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        } catch( NullPointerException e ) {
            System.out.println( "Null pointer encountered" );
        }
    }

    public static void main( String[] args ) {
        new ClientChat(args);
    }
}

