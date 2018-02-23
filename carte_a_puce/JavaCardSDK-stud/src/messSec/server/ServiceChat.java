package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import javax.crypto.Cipher;
import opencard.core.util.HexString;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

class ServiceChat extends Thread {

    final static int NBCLIENTSMAX = 3;
	final static int DATASIZE = 128;				//128 to use with RSA1024_NO_PAD
    
	Socket socket;
	BufferedReader input;
	static PrintStream[] outputs = new PrintStream[NBCLIENTSMAX];
    static int nbClients = 0;
    static String clientID[] = new String[NBCLIENTSMAX];
    String clientName;
    static HashMap<String, String> credentialsMap = new HashMap<String, String>();
    boolean isAuthenticated;
    boolean nameProvided;
    boolean pendingTransfer = false;
	static Socket socketTab[] = new Socket[NBCLIENTSMAX];
	
	byte[] challengeBytes = new byte[DATASIZE];
	
	static private byte[] modulus_b = new byte[] {
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
	static private byte[] private_exponent_b = new byte[] {
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
	static private byte[] public_exponent_b = new byte[] { (byte)0x01,(byte)0x00,(byte)0x01 };

	public ServiceChat( Socket socket ) {
		this.socket = socket;
		this.start();
	}
	
	static protected synchronized void showClientsList(){
		System.out.println("List of Users :");
		for(int i = 0; i < getNbClients(); i++)
		{
			System.out.println(""+clientID[i]);
		}
	}
	
	static protected synchronized void killUser(String userName){
		int i;

        try{
			int currentClientIndex;
			for(i = 0; i < getNbClients(); i++)
			{
				if(clientID[i].equals(userName))
				{
					sendMessage(i, "[SERVER] Bye "+userName+"!");
					socketTab[i].close();
					break;
				}
			}
			if(i < getNbClients())
			{
				currentClientIndex = i;

				for(i = currentClientIndex; i < getNbClients()-1; i++)
				{
					outputs[i] = outputs[i+1];
					clientID[i] = clientID[i+1];
					socketTab[i] = socketTab[i+1];
				}

				nbClients--;

				broadcastMessage("[SERVER]", userName + " has left the chat / "+getNbClients()+" users connected.");
				System.out.println(userName + " disconnected");
			}
			else
			{
				System.out.println("No user "+userName+ " connected");
			}
        } catch( IOException e ) {
			System.out.println( "problem during disconnection" );
		}
	}
	
	static protected synchronized void shutdownServer(){
		int num = getNbClients();
		for(int i = 0; i < num; i++)
		{
			killUser(clientID[0]);
		}
	}

    static private synchronized int getNbClients(){
        return nbClients;
    }

    private synchronized int findMatchingName(String texte){
        int i;
        for(i = 0; i < getNbClients(); i++)
        {
            if(clientID[i].equals(texte))
                break;
        }
        return i;
    }

    private int getClientID(){
        return findMatchingName(clientName);
    }

    static private synchronized void sendMessage(int id, String texte){
        outputs[id].println(texte);
    }

    static protected void broadcastMessage(String name, String texte){
        int i;
        for(i = 0; i < getNbClients(); i++)
        {
            sendMessage(i, name + " " + texte);
        }
    }
    
    private void unicastMessage(String userName, String texte){
        int destIndex = findMatchingName(userName);
        if(destIndex < getNbClients())
            sendMessage(destIndex, "[MSG] FROM_" + clientName + "> " + texte );
        else
        {
            destIndex = getClientID();
            sendMessage(destIndex, "Sorry, " + userName + " is not connected to the chat");
        }
    }
    
    private void unicastFileTransfer(String userName, String texte){
        int destIndex = findMatchingName(userName);
        if(destIndex < getNbClients())
        {
            if(!pendingTransfer)
                sendMessage(destIndex, "FILE " + clientName + " " + texte );
            else
                sendMessage(destIndex, "BUFFER " + clientName + " " + texte );
            pendingTransfer = true;
        }   
        else
        {
            destIndex = getClientID();
            sendMessage(destIndex, "Sorry, " + userName + " is not connected to the chat");
        }
    }

    private synchronized void processMessage(String texte){
        if(!isAuthenticated)
        {
            if(texte.equals("") && !nameProvided)
                sendMessage(getClientID(), "Please specify a valid user name");
            else
                authentication(texte);
        }
        else
        {
            try
            {
                if(texte.length() > 6 && !texte.substring(0,7).equals("/buffer"))
                    System.out.println(clientName + ": " + texte);
                if(texte.length() > 0 && texte.substring(0,1).equals("/"))
                    processCommand(texte.substring(1,texte.length()));
                else
                    broadcastMessage("[MSG] "+clientName, "> " + texte);
            }
            catch(StringIndexOutOfBoundsException e)
            {
                broadcastMessage("[MSG] "+clientName, "> " + texte);
            }
        }
    }
    
    private synchronized void generateAndSendChallenge(String clientPubKey){
	
		try{
			String[] textParts = clientPubKey.split(" ");
			byte[] modClient = Base64.getDecoder().decode(textParts[0]);
			byte[] expClient = Base64.getDecoder().decode(textParts[1]);
    	
			String mod_s =  HexString.hexify( modClient );
			mod_s = mod_s.replaceAll( " ", "" );
			mod_s = mod_s.replaceAll( "\n", "" );
			
			System.out.println("Modulus "+mod_s);

			String pub_s =  HexString.hexify( expClient );
			pub_s = pub_s.replaceAll( " ", "" );
			pub_s = pub_s.replaceAll( "\n", "" );
			
			System.out.println("Exponent "+pub_s);

			// Load the keys from String into BigIntegers
			BigInteger modulus = new BigInteger(mod_s, 16);
			BigInteger pubExponent = new BigInteger(pub_s, 16);

			// Create private and public key specs from BinIntegers
			RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, pubExponent);

			// Create the RSA private and public keys
			KeyFactory factory = KeyFactory.getInstance( "RSA" );
			PublicKey pubClient = factory.generatePublic(publicSpec);
			// Get Cipher able to apply RSA_NOPAD
			// (must use "Bouncy Castle" crypto provider)
			Security.addProvider(new BouncyCastleProvider());
			Cipher cRSA_NO_PAD = Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );

			// Get challenge data
			Random r = new Random( (new Date()).getTime() );
			r.nextBytes( challengeBytes );
		
			// Crypt with public key
			cRSA_NO_PAD.init( Cipher.ENCRYPT_MODE, pubClient );
			byte[] ciphered = new byte[DATASIZE];
			System.out.println( "*" );
			while(true)
			{
				try{
					cRSA_NO_PAD.doFinal(challengeBytes, 0, DATASIZE, ciphered, 0);
					break;
				}
				catch(org.bouncycastle.crypto.DataLengthException e)
				{
					// random bytes led to bad input for RSA, trying new.
					r.nextBytes( challengeBytes );
					System.out.println("org.bouncycastle.crypto.DataLengthException : trying new random bytes");
					continue;
				}
			}
			//ciphered = cRSA_NO_PAD.doFinal( challengeBytes );
			System.out.println( "*" );
			System.out.println("[SERVER] AUTH_CHALL Clear " + Base64.getEncoder().withoutPadding().encodeToString(challengeBytes));
			System.out.println("[SERVER] AUTH_CHALL " + Base64.getEncoder().withoutPadding().encodeToString(ciphered));
    	
			sendMessage(getClientID(), "[SERVER] AUTH_CHALL "+ Base64.getEncoder().withoutPadding().encodeToString(ciphered));
		
		} catch( Exception e ) {
			System.out.println( "generateAndSendChallenge: " + e );
		}
    }

	private synchronized void initiatePublicKeyExchange() {
		System.out.println("[SERVER] AUTH_PUBKEY "+ Base64.getEncoder().withoutPadding().encodeToString(modulus_b)+
			" "+Base64.getEncoder().withoutPadding().encodeToString(public_exponent_b));
		sendMessage(getClientID(), "[SERVER] AUTH_PUBKEY "+ Base64.getEncoder().withoutPadding().encodeToString(modulus_b)+
			" "+Base64.getEncoder().withoutPadding().encodeToString(public_exponent_b));
	}
	
	private boolean compareResponseToSentChallenge(String response) {
		byte[] decodedBytes = Base64.getDecoder().decode(response);
		
		try{
		
			String mod_s =  HexString.hexify( modulus_b );
			mod_s = mod_s.replaceAll( " ", "" );
			mod_s = mod_s.replaceAll( "\n", "" );

			String priv_s =  HexString.hexify( private_exponent_b );
			priv_s = priv_s.replaceAll( " ", "" );
			priv_s = priv_s.replaceAll( "\n", "" );

			// Load the keys from String into BigIntegers (step 3)
			BigInteger modulus = new BigInteger(mod_s, 16);
			BigInteger privExponent = new BigInteger(priv_s, 16);

			// Create private and public key specs from BinIntegers
			RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(modulus, privExponent);

			// Create the RSA private and public keys
			KeyFactory factory = KeyFactory.getInstance( "RSA" );
			PrivateKey priv = factory.generatePrivate(privateSpec);
		
			Security.addProvider(new BouncyCastleProvider());
			Cipher cRSA_NO_PAD = Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
		
			// Decrypt with private key
			cRSA_NO_PAD.init( Cipher.DECRYPT_MODE, priv );
			byte[] unciphered = new byte[DATASIZE];
			cRSA_NO_PAD.doFinal( decodedBytes, 0, DATASIZE, unciphered, 0);
			System.out.println("[SERVER] AUTH_CHALL Clear " + Base64.getEncoder().withoutPadding().encodeToString(challengeBytes));
			System.out.println("[SERVER] AUTH_CHALL Unciphered " + Base64.getEncoder().withoutPadding().encodeToString(unciphered));
			return Arrays.equals(challengeBytes, unciphered);
		
		} catch( Exception e ) {
			System.out.println( "compareResponseToSentChallenge: " + e );
		}
		return false;
	}

    private synchronized void authentication(String texte){

        int currentClientIndex = getClientID();
       
        if(!nameProvided)
        {
            if(findMatchingName(texte) < getNbClients())
            {
                sendMessage(currentClientIndex, "[SERVER] Pseudo " + texte + " is already connected\nPlease enter your name" );
            }
            else
            {
                if(credentialsMap.get(texte) != null)
                {
                    sendMessage(currentClientIndex, "[SERVER] Please authenticate" );
                    generateAndSendChallenge(credentialsMap.get(texte));
                }
                else
                {
                    sendMessage(currentClientIndex, "[SERVER] has sent public key" );
                    initiatePublicKeyExchange();
                }
                nameProvided = true;
                clientName = ""+ texte;
                clientID[currentClientIndex] = clientName;
            }
        }
        else if(credentialsMap.get(clientName) == null)
        {
            //Put Base64 public key received from client into Hash map
            credentialsMap.put(clientName, texte); 
            //Send challenge for authentication
            sendMessage(currentClientIndex, "[SERVER] Thanks for public key, please authenticate" );
            generateAndSendChallenge(texte);
        }
        else
        {
        	//Received String for user with an entry in DB : expecting answer to challenge
            if(compareResponseToSentChallenge(texte))
                isAuthenticated = true;
            else
            {
                sendMessage(currentClientIndex, "[SERVER] Authentication failure\nPlease enter your name" );
                isAuthenticated = false;
                nameProvided = false;
                clientName = "";
                clientID[currentClientIndex] = clientName;
            }
        }
        if(isAuthenticated)
        {
            System.out.println(clientName + " connected");
            sendMessage(getClientID(), "[SERVER] Hello "+clientName+"!");
            broadcastMessage("[SERVER]", clientName+" has joined the chat / "+nbClients+" users connected.");
        }
    }

    private synchronized void processCommand(String line){

        int i;
        StringTokenizer st = new StringTokenizer(line);
		if(st.hasMoreTokens())
		{
			String text = st.nextToken();

			switch(text)
			{
				case "nusers":
					sendMessage(getClientID(), "[SERVER] "+ getNbClients());
					break;
				case "list":
					sendMessage(getClientID(), "[SERVER] List of Users :");
					for(i = 0; i < getNbClients(); i++)
					{
						sendMessage(getClientID(), "[SERVER] "+clientID[i]);
					}
					break;
				case "quit":
					deconnexion();
					break;
				case "nickname":
					try{
						if(st.hasMoreTokens())
						{
							String newName = st.nextToken();
							if(credentialsMap.get(newName) == null)
							{
								String pwd = credentialsMap.remove(clientName);
								clientName = newName;
								clientID[getClientID()] = clientName;
								credentialsMap.put(clientName, pwd);
								sendMessage(getClientID(), "[SERVER] New nickname "+clientName);
							}
							else
								sendMessage(getClientID(), "[SERVER] Please choose another nickname");
						}
					} catch(Exception e ) {
						sendMessage(getClientID(), "[SERVER] /nickname <name>    : change nickname");
					}
					break;
				case "file":
					try{
						if(st.hasMoreTokens())
						{
							String dest = st.nextToken();
							unicastFileTransfer(dest, line.substring(text.length()+dest.length()+2, line.length()));
						}
					} catch(Exception e ) {
						sendMessage(getClientID(), "[SERVER] /file <name> <file> : transmit file");
					}
					break;
				case "buffer":
					if(!pendingTransfer)
					{
						sendMessage(getClientID(), "[SERVER] Command "+ line + " unknown");
						sendMessage(getClientID(), "[SERVER] /quit                   : quit the chat");
						sendMessage(getClientID(), "[SERVER] /nusers                 : get number of connected users");
						sendMessage(getClientID(), "[SERVER] /list                   : get list of connected users");
						sendMessage(getClientID(), "[SERVER] /nickname <name>        : change nickname");
						sendMessage(getClientID(), "[SERVER] /file <name> <file>     : transmit file");
						sendMessage(getClientID(), "[SERVER] /msg <username> <text>  : send private message to given user");
					}
				
					try{
						if(st.hasMoreTokens())
						{
							String dest = st.nextToken();
							unicastFileTransfer(dest, line.substring(text.length()+dest.length()+2, line.length()));
						}
					} catch(Exception e ) {
						sendMessage(getClientID(), "[SERVER] Buffer transmission issue");
					}
					break;
				case "endoftransfer":
					try{
						if(st.hasMoreTokens())
						{
							String dest = st.nextToken();
							pendingTransfer = false;
							int ID = findMatchingName(dest);
							sendMessage(ID, "ENDOFFILE " + clientName +" "+ dest);
						}
					} catch(Exception e ) {
						sendMessage(getClientID(), "[SERVER] Buffer end of file issue");
					}
					break;
				
				case "msg":
					try{
						if(st.hasMoreTokens())
						{
							String dest = st.nextToken();
							unicastMessage(dest, line.substring(text.length()+dest.length()+2, line.length()));
						}
					} catch(Exception e ) {
						sendMessage(getClientID(), "[SERVER] /msg <username> <text>  : send private message to given user");
					}
					break;
				
				default:
					if(!text.equals("help"))
						sendMessage(getClientID(), "[SERVER] Command "+ line + " unknown");
					sendMessage(getClientID(), "[SERVER] /quit                   : quit the chat");
					sendMessage(getClientID(), "[SERVER] /nusers                 : get number of connected users");
					sendMessage(getClientID(), "[SERVER] /list                   : get list of connected users");
					sendMessage(getClientID(), "[SERVER] /nickname <name>        : change nickname");
					sendMessage(getClientID(), "[SERVER] /file <name> <file>     : transmit file");
					sendMessage(getClientID(), "[SERVER] /msg <username> <text>  : send private message to given user");
					break;
			}
        }
    }

    private void deconnexion(){
        killUser(clientName);
    }

    private synchronized void connexion(){
        try{
            isAuthenticated = false;
            nameProvided = false;
            outputs[nbClients] = new PrintStream( socket.getOutputStream() );
            sendMessage(nbClients, "[SERVER] Please enter your name");
            clientName = ""+ socket.getPort();
            clientID[nbClients] = clientName;
			socketTab[nbClients] = socket;
            nbClients++;
		} catch( IOException | ArrayIndexOutOfBoundsException e ) {
			try {
				socket.close();
			} catch( IOException e2 ) {
				System.out.println( "problem while closing socket" );
			}
		}
    }

	public void run() {
		try {
			input = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            connexion();
		} catch( IOException e ) {
			try {
				socket.close();
			} catch( IOException e2 ) {
				System.out.println( "problem while closing socket" );
			}
		}

		String texte;

		try {
			while(true)
            {
                texte = input.readLine();
                processMessage(texte);
            }
		} catch( IOException e ) {
			System.out.println( "problem during run" );
		}
        catch(NullPointerException e){}
        
        deconnexion();
	}
}
